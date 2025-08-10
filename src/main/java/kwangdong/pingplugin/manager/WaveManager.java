package kwangdong.pingplugin.manager;

import kwangdong.pingplugin.tasks.WavePlayerState;
import kwangdong.pingplugin.tasks.WaveSpawner;
import kwangdong.pingplugin.ui.DeathSidebarManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class WaveManager {

	private final Plugin plugin;
	private final Random random = new Random();

	private boolean isWaveActive = false;
	private int currentRound = 0;

	private List<Player> participants = new ArrayList<>();
	private final Map<UUID, Integer> deathCounts = new HashMap<>();
	private final Set<UUID> ghosts = new HashSet<>();
	private final Set<UUID> aliveMobIds = new HashSet<>();

	private final GhostManager ghostManager = new GhostManager();

	private Location waveCenter;

	// ✅ 사이드바 UI 매니저(데스카운트 오버레이)
	private final DeathSidebarManager deathSidebar = new DeathSidebarManager();

	// 라운드당 몬스터 수 (필요 시 난이도에 맞춰 조절)
	private int getMobsPerRound(int round) {
		// 예시: round가 오를수록 증가시키고 싶다면 로직 변경
		return 1;
	}

	public WaveManager(Plugin plugin) {
		this.plugin = plugin;
	}

	public boolean isWaveActive() { return isWaveActive; }
	public int getCurrentRound() { return currentRound; }
	public Plugin getPlugin() { return plugin; }
	public boolean isGhost(Player p) { return ghosts.contains(p.getUniqueId()); }

	public void tryStartWaveNight(Collection<? extends Player> onlinePlayers) {
		if (isWaveActive) return;
		if (onlinePlayers == null || onlinePlayers.isEmpty()) return;
		if (random.nextInt(100) != 0) return; // 1%

		List<Player> online = new ArrayList<>(onlinePlayers);
		Player center = online.get(random.nextInt(online.size()));
		startWave(center);
	}

	public void startWave(Player center) {
		if (center == null) return;
		if (isWaveActive) {
			center.sendMessage(Component.text("이미 웨이브가 진행 중입니다.", NamedTextColor.YELLOW));
			return;
		}

		isWaveActive = true;
		currentRound = 0;
		waveCenter = center.getLocation().clone();

		participants = Bukkit.getOnlinePlayers().stream()
			.filter(p -> p.getWorld().equals(center.getWorld())
				&& p.getLocation().distance(center.getLocation()) <= 50)
			.collect(Collectors.toList());

		if (participants.isEmpty()) {
			isWaveActive = false;
			waveCenter = null;
			center.sendMessage(Component.text("반경 50블록 내 참여자가 없습니다.", NamedTextColor.RED));
			return;
		}

		deathCounts.clear();
		ghosts.clear();
		aliveMobIds.clear();

		for (Player p : participants) {
			deathCounts.put(p.getUniqueId(), 10);
			WavePlayerState.saveState(p);
		}

		// ✅ 사이드바 표시 (초기값 세팅)
		deathSidebar.start(participants, deathCounts);

		Component startMsg = Component.text("[Wave] 몬스터 웨이브가 시작됩니다!", NamedTextColor.GOLD);
		participants.forEach(p -> p.sendMessage(startMsg));

		startNextRound();
	}

	private void announceRound() {
		Component title = Component.text("Round " + currentRound + " / 10", NamedTextColor.GOLD);
		Component sub   = Component.text("엘리트 확률 10%", NamedTextColor.GRAY);
		Title t = Title.title(title, sub,
			Times.times(Duration.ofMillis(200), Duration.ofMillis(1200), Duration.ofMillis(200)));
		for (Player p : participants) {
			p.showTitle(t);
			p.sendActionBar(Component.text("라운드 시작!", NamedTextColor.YELLOW));
			p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.2f);
		}
	}

	private void startNextRound() {
		if (!isWaveActive) return;
		if (currentRound >= 10) { finishWave(true); return; }

		currentRound++;
		announceRound();

		aliveMobIds.clear();

		int count = getMobsPerRound(currentRound);
		List<LivingEntity> mobs = WaveSpawner.spawnMonsters(waveCenter, currentRound, participants, count);
		for (LivingEntity m : mobs) {
			aliveMobIds.add(m.getUniqueId());
		}

		// 필요 시: 라운드 시작할 때 전체 사이드바 값을 다시 재확인/갱신
		deathSidebar.updateAll(deathCounts);
	}

	// onPlayerDeath 내부 변경
	public void onPlayerDeath(Player player) {
		if (!isWaveActive || player == null) return;
		UUID id = player.getUniqueId();
		if (!deathCounts.containsKey(id)) return;

		int left = deathCounts.get(id) - 1;
		deathCounts.put(id, left);

		if (left <= 0) {
			ghosts.add(id);
			player.sendMessage(Component.text("당신은 유령 상태가 되었습니다. 웨이브 종료까지 관전만 가능합니다.", NamedTextColor.GRAY));

			// 인벤/경험치 복원
			WavePlayerState.restoreOnDeath(player);

			// ⬇️ 리스폰 직후 관전 모드 적용 (리스너에서도 안전망이 있으나 즉시 처리 보조)
			plugin.getServer().getScheduler().runTask(plugin, () -> ghostManager.setGhost(player));

		} else {
			player.sendMessage(Component.text("남은 데스카운트: " + left, NamedTextColor.GRAY));
			// 인벤/경험치 복원
			WavePlayerState.restoreOnDeath(player);
		}

		deathSidebar.update(player, Math.max(left, 0));

		// 전원 유령이면 실패
		if (ghosts.size() == participants.size()) {
			finishWave(false);
		}
	}

	/** 웨이브 몹이 죽을 때마다 호출됨 (WaveMobDropListener에서) */
	public void onWaveMobKilled(UUID mobId) {
		if (!isWaveActive) return;
		if (!aliveMobIds.remove(mobId)) return;

		// 아직 몹 남아있으면 액션바로 남은 수 안내(선택)
		if (!aliveMobIds.isEmpty()) {
			int leftMobs = aliveMobIds.size();
			for (Player p : participants) {
				p.sendActionBar(Component.text("남은 적: " + leftMobs, NamedTextColor.YELLOW));
			}
			return;
		}

		// 이 라운드 몬스터 전부 처치 → 다음 라운드
		if (currentRound >= 10) { // 일반적으로 여기 오면 == 10
			finishWave(true);
			return;
		}
		Bukkit.getScheduler().runTaskLater(plugin, this::startNextRound, 20L * 2);
	}

	private void celebrateSuccess() {
		Title title = Title.title(
			Component.text("웨이브 성공!", NamedTextColor.GREEN),
			Component.text("보상이 지급되었습니다.", NamedTextColor.WHITE),
			Times.times(Duration.ofMillis(200), Duration.ofMillis(1400), Duration.ofMillis(300))
		);
		for (Player p : participants) {
			p.showTitle(title);
			p.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
			p.playSound(p, Sound.EVENT_RAID_HORN, 0.7f, 1.0f);
			p.spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.01);
		}
	}

	// finishWave 내부 변경 (유령 복원 먼저)
	public void finishWave(boolean success) {
		if (!isWaveActive) return;
		isWaveActive = false;

		if (success) celebrateSuccess();

		// ⬇️ 유령 복원: 먼저 게임모드부터 돌려놓자 (관전→원래 모드)
		for (Player p : participants) {
			ghostManager.restore(p);
		}

		// 사이드바 해제 등 UI 정리 (넣어놨다면)
		deathSidebar.stop();

		// 상태 복원 & 보상/메시지
		for (Player p : participants) {
			WavePlayerState.restoreState(p);
			if (success) {
				RewardManager.giveReward(p);
			} else {
				p.sendMessage(Component.text("몬스터 웨이브에 실패했습니다.", NamedTextColor.DARK_RED));
				p.playSound(p, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
			}
		}

		// 캐시 정리
		ghostManager.clearAll();
		participants.clear();
		deathCounts.clear();
		ghosts.clear();
		aliveMobIds.clear();
		waveCenter = null;
		currentRound = 0;
	}

	public void skipWave() { finishWave(true); }
	public void stopWave() { finishWave(false); }

	/** (선택) 외부 리스너에서 재접속 시 사이드바 복원하려면 사용 */
	public Integer getDeathLeft(Player p) {
		return deathCounts.get(p.getUniqueId());
	}
}
