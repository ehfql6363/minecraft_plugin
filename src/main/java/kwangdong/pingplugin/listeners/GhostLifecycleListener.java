package kwangdong.pingplugin.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import kwangdong.pingplugin.manager.GhostManager;
import kwangdong.pingplugin.manager.WaveManager;

/**
 * 유령 상태 유지/적용을 보조하는 리스너:
 * - 사망 후 리스폰 시: 유령이면 관전 모드 적용
 * - 재접속 시: 유령이면 관전 모드 적용
 *
 * (WaveManager가 "누가 유령인지"는 관리하고, GhostManager가 실제 모드 전환을 수행)
 */
public class GhostLifecycleListener implements Listener {

	private final WaveManager waveManager;
	private final GhostManager ghostManager;

	public GhostLifecycleListener(WaveManager waveManager, GhostManager ghostManager) {
		this.waveManager = waveManager;
		this.ghostManager = ghostManager;
	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent e) {
		Player p = e.getPlayer();
		if (!waveManager.isWaveActive()) return;
		if (!waveManager.isGhost(p)) return;

		// 리스폰 직후 1틱 뒤 관전 모드 적용 (장비 복원/이동 등 먼저 끝나게)
		p.getServer().getScheduler().runTask(waveManager.getPlugin(), () -> ghostManager.setGhost(p));
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		if (!waveManager.isWaveActive()) return;
		if (!waveManager.isGhost(p)) return;

		// 재접속 즉시 관전 모드 재적용
		p.getServer().getScheduler().runTask(waveManager.getPlugin(), () -> ghostManager.setGhost(p));
	}
}
