package kwangdong.pingplugin.manager;

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
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 자연 발동(밤)일 때만 준비 단계(60초 카운트다운)를 거치고,
 * 명령어 시작은 즉시 라운드 시작.
 */
public class WaveManager {

	private final Plugin plugin;
	private final Random random = new Random();

	// 진행/라운드
	private boolean isWaveActive = false;
	private int currentRound = 0;

	// 참가자/상태
	private List<Player> participants = new ArrayList<>();
	private final Map<UUID, Integer> deathCounts = new HashMap<>();
	private final Set<UUID> ghosts = new HashSet<>();
	private final Set<UUID> aliveMobIds = new HashSet<>();
	private final GhostManager ghostManager = new GhostManager();

	// 준비 단계
	private boolean isPreparing = false;
	private BukkitTask prepareTask;
	private int prepareRemainSec;
	private Location prepareCenter;
	private List<Player> prepareParticipants = new ArrayList<>();
	private final RespawnPointManager respawnPoints = new RespawnPointManager();

	private Location waveCenter;

	// UI
	private final DeathSidebarManager deathSidebar = new DeathSidebarManager();

	public WaveManager(Plugin plugin) {
		this.plugin = plugin;
	}

	public boolean isWaveActive() { return isWaveActive; }
	public boolean isPreparing() { return isPreparing; }
	public int getCurrentRound() { return currentRound; }
	public Plugin getPlugin() { return plugin; }
	public boolean isGhost(Player p) { return ghosts.contains(p.getUniqueId()); }

	public Location getRespawnPoint(Player p) {
		return respawnPoints.getWaveRespawn(p);
	}

	// ===== 자연 발동 진입점 =====
	/** 밤 시작 시 10% 확률로 '준비 단계(60초)' 진입 */
	public void tryStartWaveNight(Collection<? extends Player> onlinePlayers) {
		if (isWaveActive || isPreparing) return;
		if (onlinePlayers == null || onlinePlayers.isEmpty()) return;

		// 10% 확률
		if (random.nextInt(10) != 0) return;

		List<Player> online = new ArrayList<>(onlinePlayers);
		Player center = online.get(random.nextInt(online.size()));

		beginPreparation(center, 60); // 준비 60초
	}

	// ===== 준비 단계 =====
	private void beginPreparation(Player center, int seconds) {
		if (center == null || isWaveActive || isPreparing) return;

		// 50블록 내 확정 참가자 스냅샷
		List<Player> snap = Bukkit.getOnlinePlayers().stream()
			.filter(p -> p.getWorld().equals(center.getWorld())
				&& p.getLocation().distance(center.getLocation()) <= 50)
			.collect(Collectors.toList());

		if (snap.isEmpty()) {
			center.sendMessage(Component.text("[Wave] 준비 실패: 반경 50블록 내 참가자가 없습니다.", NamedTextColor.YELLOW));
			return;
		}

		isPreparing = true;
		prepareRemainSec = Math.max(5, seconds);
		prepareCenter = center.getLocation().clone();
		prepareParticipants = new ArrayList<>(snap);

		// 안내
		broadcastTo(prepareParticipants,
			Component.text("[Wave] 몬스터 웨이브의 징조가 느껴집니다...", NamedTextColor.GOLD));
		broadcastTo(prepareParticipants,
			Component.text("1분 뒤 웨이브가 시작됩니다! (반경 50블록 내 현재 인원만 참가)", NamedTextColor.GRAY));

		// 1초마다 카운트다운
		prepareTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			// 취소 조건: 참가자 전원 이탈, 준비자 전원 오프라인/다른 월드, 또는 관리자 취소
			prepareParticipants.removeIf(p -> !p.isOnline() || !p.getWorld().equals(prepareCenter.getWorld()));
			if (prepareParticipants.isEmpty()) {
				cancelPreparation(Component.text("[Wave] 준비가 취소되었습니다: 참가자가 없습니다.", NamedTextColor.YELLOW));
				return;
			}

			// 남은 시간 안내 (액션바 + 간헐적 사운드)
			for (Player p : prepareParticipants) {
				p.sendActionBar(Component.text("웨이브 준비 중... " + prepareRemainSec + "초", NamedTextColor.YELLOW));
				if (prepareRemainSec <= 5) {
					p.playSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 1.8f);
				} else if (prepareRemainSec % 10 == 0) {
					p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.2f);
				}
			}

			if (--prepareRemainSec <= 0) {
				// 준비 완료 → 실제 시작
				endPreparationAndStart();
			}
		}, 20L, 20L);
	}

	private void cancelPreparation(Component reason) {
		if (!isPreparing) return;
		if (prepareTask != null) {
			prepareTask.cancel();
			prepareTask = null;
		}
		broadcastTo(prepareParticipants, reason);
		isPreparing = false;
		prepareParticipants.clear();
		prepareCenter = null;
		prepareRemainSec = 0;
	}

	private void endPreparationAndStart() {
		if (prepareTask != null) {
			prepareTask.cancel();
			prepareTask = null;
		}
		// 준비 단계 정보로 실제 시작
		List<Player> locked = new ArrayList<>(prepareParticipants);
		Location center = prepareCenter == null ? locked.get(0).getLocation() : prepareCenter.clone();

		// 준비 상태 초기화
		isPreparing = false;
		prepareParticipants.clear();
		prepareCenter = null;
		prepareRemainSec = 0;

		startWaveWithLockedParticipants(center, locked);
	}

	// ===== 명령어 즉시 시작 =====
	/** 테스트/명령어: 즉시 시작(준비 단계 없음) */
	public void startWave(Player center) {
		if (isWaveActive || isPreparing) {
			if (center != null) center.sendMessage(Component.text("이미 웨이브가 진행(또는 준비) 중입니다.", NamedTextColor.YELLOW));
			return;
		}
		// 즉시 시작용: 50블록 스냅샷을 곧바로 참가자 리스트로
		List<Player> snap = Bukkit.getOnlinePlayers().stream()
			.filter(p -> p.getWorld().equals(center.getWorld())
				&& p.getLocation().distance(center.getLocation()) <= 50)
			.collect(Collectors.toList());

		if (snap.isEmpty()) {
			if (center != null) center.sendMessage(Component.text("반경 50블록 내 참가자가 없습니다.", NamedTextColor.RED));
			return;
		}
		startWaveWithLockedParticipants(center.getLocation().clone(), snap);
	}

	private void startWaveWithLockedParticipants(Location center, List<Player> lockedParticipants) {
		isWaveActive = true;
		currentRound = 0;
		waveCenter = center;

		participants = new ArrayList<>(lockedParticipants);
		deathCounts.clear();
		ghosts.clear();
		aliveMobIds.clear();

		for (Player p : participants) {
			deathCounts.put(p.getUniqueId(), 10);
			respawnPoints.rememberWaveRespawn(p, waveCenter);
		}

		// 사이드바 표시
		deathSidebar.start(participants, deathCounts);

		Component startMsg = Component.text("[Wave] 몬스터 웨이브가 시작됩니다!", NamedTextColor.GOLD);
		broadcastTo(participants, startMsg);

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
		if (currentRound >= 10) {
			finishWave(true);
			return;
		}

		currentRound++;
		announceRound();

		aliveMobIds.clear();

		int count = getMobsPerRound(currentRound); // 필요시 난이도 곡선
		List<LivingEntity> mobs = WaveSpawner.spawnMonsters(waveCenter, currentRound, participants, count);
		for (LivingEntity m : mobs) aliveMobIds.add(m.getUniqueId());

		deathSidebar.updateAll(deathCounts);
	}

	private int getMobsPerRound(int round) {
		return 1; // 현재는 1마리; 이후 라운드별 증가 로직로 교체 가능
	}

	public void onPlayerDeath(Player player) {
		if (!isWaveActive || player == null) return;
		UUID id = player.getUniqueId();
		if (!deathCounts.containsKey(id)) return;

		int left = deathCounts.get(id) - 1;
		deathCounts.put(id, left);

		if (left <= 0) {
			ghosts.add(id);
			player.sendMessage(Component.text("당신은 유령 상태가 되었습니다. 웨이브 종료까지 관전만 가능합니다.", NamedTextColor.GRAY));
			plugin.getServer().getScheduler().runTask(plugin, () -> ghostManager.setGhost(player));
		} else {
			player.sendMessage(Component.text("남은 데스카운트: " + left, NamedTextColor.GRAY));
		}

		deathSidebar.update(player, Math.max(left, 0));

		if (ghosts.size() == participants.size()) {
			Bukkit.getScheduler().runTask(plugin, () -> finishWave(false));
		}
	}

	/** 웨이브 몹 사망 보고 (WaveMobDropListener에서 호출) */
	public void onWaveMobKilled(UUID mobId) {
		if (!isWaveActive) return;
		if (!aliveMobIds.remove(mobId)) return;

		if (!aliveMobIds.isEmpty()) {
			int leftMobs = aliveMobIds.size();
			for (Player p : participants) {
				p.sendActionBar(Component.text("남은 적: " + leftMobs, NamedTextColor.YELLOW));
			}
			return;
		}

		if (currentRound >= 10) {
			Bukkit.getScheduler().runTask(plugin, () -> finishWave(true));
		} else {
			Bukkit.getScheduler().runTaskLater(plugin, this::startNextRound, 20L * 2);
		}
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

	public void finishWave(boolean success) {
		// 준비 중이라면 '웨이브'가 아니라 '준비' 취소로 처리
		if (isPreparing && !isWaveActive) {
			cancelPreparation(Component.text("[Wave] 준비가 취소되었습니다.", NamedTextColor.YELLOW));
			return;
		}

		if (!isWaveActive) return;
		isWaveActive = false;

		if (success) celebrateSuccess();

		// 관전 → 원래 모드 복구
		for (Player p : participants) ghostManager.restore(p);
		// 사이드바 해제
		deathSidebar.stop();

		respawnPoints.clearAll();

		for (Player p : participants) {
			if (success) {
				RewardManager.giveReward(p);
			} else {
				p.sendMessage(Component.text("몬스터 웨이브에 실패했습니다.", NamedTextColor.DARK_RED));
				p.playSound(p, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
			}
		}

		ghostManager.clearAll();
		participants.clear();
		deathCounts.clear();
		ghosts.clear();
		aliveMobIds.clear();
		waveCenter = null;
		currentRound = 0;
	}

	// 명령어: 강제 성공/중지
	public void skipWave() {
		if (isPreparing && !isWaveActive) {
			cancelPreparation(Component.text("[Wave] 준비가 관리자로 인해 취소되었습니다.", NamedTextColor.YELLOW));
			return;
		}
		finishWave(true);
	}

	public void stopWave() {
		if (isPreparing && !isWaveActive) {
			cancelPreparation(Component.text("[Wave] 준비가 관리자로 인해 취소되었습니다.", NamedTextColor.YELLOW));
			return;
		}
		finishWave(false);
	}

	public Integer getDeathLeft(Player p) { return deathCounts.get(p.getUniqueId()); }

	public boolean isParticipant(Player player) {
		if (player == null) return false;
		return participants.stream().anyMatch(p -> p.getUniqueId().equals(player.getUniqueId()));
	}

	private void broadcastTo(Collection<Player> list, Component msg) {
		for (Player p : list) p.sendMessage(msg);
	}
}
