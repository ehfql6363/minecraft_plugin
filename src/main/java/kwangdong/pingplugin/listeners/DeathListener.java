package kwangdong.pingplugin.listeners;

import kwangdong.pingplugin.PingPlugin;
import kwangdong.pingplugin.manager.WaveManager;
import kwangdong.pingplugin.manager.DeathBeamTaskManager;
import kwangdong.pingplugin.tasks.DeathBeamTask;
import kwangdong.pingplugin.util.ColorUtil;
import kwangdong.pingplugin.util.DeathSnapshotManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

public class DeathListener implements Listener {
	private final Plugin plugin;
	private final WaveManager waveManager;

	public DeathListener(Plugin plugin, WaveManager waveManager) {
		this.plugin = plugin;
		this.waveManager = waveManager;
	}

	/** 1) 사망 직전 스냅샷 저장 + 웨이브 참가자일 때만 인벤 세이브 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onDeathWaveScope(PlayerDeathEvent event) {
		Player player = event.getEntity();

		// 죽기 직전 상태 스냅샷 (다른 플러그인이 이전 스냅샷으로 복원해도, 우리가 되돌려 씌울 것)
		DeathSnapshotManager.save(
			player.getUniqueId(),
			player.getInventory().getContents(),
			player.getInventory().getArmorContents(),
			player.getInventory().getExtraContents()
		);

		if (waveManager.isWaveActive() && waveManager.isParticipant(player)) {
			event.setKeepInventory(true);
			event.getDrops().clear();
			event.setKeepLevel(true);
			event.setDroppedExp(0);

			waveManager.onPlayerDeath(player);
		} else {
			// 웨이브 외: 좌표/빔 처리만
			Location loc = player.getLocation();
			PingPlugin.deathMap.put(
				player.getUniqueId(),
				new PingPlugin.LocationInfo(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName())
			);
			DeathBeamTask beam = new DeathBeamTask(loc, ColorUtil.getColorForPlayer(player));
			beam.runTaskTimer(plugin, 0L, 20L);
			DeathBeamTaskManager.register(player.getUniqueId(), beam);
		}
	}

	/** 2) 리스폰 직후 1틱 뒤, 우리 스냅샷을 강제로 적용 (죽기 직전 상태를 보장) */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onRespawnApply(PlayerRespawnEvent event) {
		Player p = event.getPlayer();
		boolean inWave = waveManager.isWaveActive() && waveManager.isParticipant(p);

		if (!inWave) {
			// 웨이브 밖이면 스냅샷 폐기
			DeathSnapshotManager.clear(p.getUniqueId());
			return;
		}

		plugin.getServer().getScheduler().runTask(plugin, () -> {
			var c = DeathSnapshotManager.getContents(p.getUniqueId());
			var a = DeathSnapshotManager.getArmor(p.getUniqueId());
			var x = DeathSnapshotManager.getExtra(p.getUniqueId());
			if (c != null) p.getInventory().setContents(c);
			if (a != null) p.getInventory().setArmorContents(a);
			if (x != null) p.getInventory().setExtraContents(x);
			p.updateInventory();
			DeathSnapshotManager.clear(p.getUniqueId());
		});
	}

	/** 3) 최종 확정: 웨이브 외 사망은 세이브 강제 해제 (다른 플러그인이 켰어도 끔) */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDeathEnforce(PlayerDeathEvent event) {
		Player player = event.getEntity();
		boolean inWave = waveManager.isWaveActive() && waveManager.isParticipant(player);
		if (!inWave) {
			event.setKeepInventory(false);
			event.setKeepLevel(false);
		}
	}
}
