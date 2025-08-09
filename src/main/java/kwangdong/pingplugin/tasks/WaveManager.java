package kwangdong.pingplugin.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class WaveManager {

	private final Plugin plugin;
	private final Random random = new Random();

	private boolean isWaveActive = false;
	private int currentRound = 0;
	private List<Player> participants = new ArrayList<>();
	private final Map<UUID, Integer> deathCounts = new HashMap<>();
	private final Set<UUID> ghosts = new HashSet<>();
	private Location waveCenter;
	private BukkitTask waveTask;

	public WaveManager(Plugin plugin) {
		this.plugin = plugin;
	}

	public boolean isWaveActive() { return isWaveActive; }
	public int getCurrentRound() { return currentRound; }

	/** 밤 시작 시 1% 확률로 자동 시작 (외부 리스너에서 호출) */
	public void tryStartWaveNight(Collection<? extends Player> onlinePlayers) {
		if (isWaveActive) return;
		if (onlinePlayers.isEmpty()) return;
		if (random.nextInt(100) != 0) return; // 1%

		List<Player> online = new ArrayList<>(onlinePlayers);
		Player center = online.get(random.nextInt(online.size()));
		startWave(center);
	}

	/** 테스트/명령어 시작 */
	public void startWave(Player center) {
		if (isWaveActive) {
			Bukkit.getLogger().info("이미 웨이브가 진행 중 입니다.");
			return;
		}
		isWaveActive = true;
		currentRound = 0;
		waveCenter = center.getLocation();

		// 50블럭 내 확정 참여자
		participants = Bukkit.getOnlinePlayers().stream()
			.filter(p -> p.getWorld().equals(center.getWorld()) && p.getLocation().distance(center.getLocation()) <= 50)
			.collect(Collectors.toList());

		if (participants.isEmpty()) {
			isWaveActive = false;
			center.sendMessage("반경 50블럭 내 참여자가 없습니다.");
			return;
		}

		// 데스카운트/세이브
		deathCounts.clear();
		ghosts.clear();
		for (Player p : participants) {
			deathCounts.put(p.getUniqueId(), 10);
			WavePlayerState.saveState(p); // 인벤/경험치 백업
		}

		participants.forEach(p -> p.sendMessage("§6[Wave] §f몬스터 웨이브가 시작됩니다!"));

		startNextRound();
	}

	private void startNextRound() {
		if (!isWaveActive) return;
		if (currentRound >= 10) {
			finishWave(true);
			return;
		}
		currentRound++;

		// 한 마리 스폰
		WaveSpawner.spawnMonster(waveCenter, currentRound);

		// 다음 라운드 예약 (10초 후)
		waveTask = Bukkit.getScheduler().runTaskLater(plugin, this::startNextRound, 20L * 10);
	}

	/** 사망 시 호출(리스너에서) */
	public void onPlayerDeath(Player player) {
		if (!isWaveActive) return;
		if (!deathCounts.containsKey(player.getUniqueId())) return;

		int left = deathCounts.get(player.getUniqueId()) - 1;
		deathCounts.put(player.getUniqueId(), left);

		if (left <= 0) {
			ghosts.add(player.getUniqueId());
			player.sendMessage("§7당신은 유령 상태가 되었습니다. 웨이브 종료까지 관전만 가능합니다.");
		} else {
			player.sendMessage("§7남은 데스카운트: §f" + left);
		}

		// 죽었어도 아이템/경험치 유지
		WavePlayerState.restoreOnDeath(player);

		// 모두 유령이면 실패
		if (ghosts.size() == participants.size()) {
			finishWave(false);
		}
	}

	public void finishWave(boolean success) {
		if (!isWaveActive) return;
		isWaveActive = false;

		if (waveTask != null) {
			waveTask.cancel();
			waveTask = null;
		}

		for (Player p : participants) {
			// 상태 복원
			WavePlayerState.restoreState(p);

			if (success) {
				RewardManager.giveReward(p);
			} else {
				p.sendMessage(Component.text("몬스터 웨이브에 실패했습니다.", NamedTextColor.DARK_RED));
			}
		}

		participants.clear();
		deathCounts.clear();
		ghosts.clear();
		waveCenter = null;
		currentRound = 0;
	}

	public void skipWave() { // 강제 성공
		finishWave(true);
	}

	public void stopWave() { // 강제 중지(실패)
		finishWave(false);
	}
}
