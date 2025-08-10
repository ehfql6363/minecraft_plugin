package kwangdong.pingplugin.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;

import kwangdong.pingplugin.manager.WaveManager;
import kwangdong.pingplugin.tasks.DeathBeamTask;
import kwangdong.pingplugin.manager.DeathBeamTaskManager;
import kwangdong.pingplugin.PingPlugin;
import kwangdong.pingplugin.util.ColorUtil;

public class DeathListener implements Listener {
	private final Plugin plugin;
	private final WaveManager waveManager;

	public DeathListener(Plugin plugin, WaveManager waveManager) {
		this.plugin = plugin;
		this.waveManager = waveManager;
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		Location loc = player.getLocation();

		// ✅ 웨이브 진행 중 + 해당 라운드 참가자일 때만 세이브 적용
		if (waveManager.isWaveActive() && waveManager.isParticipant(player)) {
			event.setKeepInventory(true);
			event.getDrops().clear();
			event.setKeepLevel(true);
			event.setDroppedExp(0);

			// 데스카운트/유령 처리 등 내부 로직
			waveManager.onPlayerDeath(player);
		} else {
			PingPlugin.deathMap.put(player.getUniqueId(),
				new PingPlugin.LocationInfo(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName()));

			// UUID 기반 색상
			DeathBeamTask beam = new DeathBeamTask(loc, ColorUtil.getColorForPlayer(player));
			beam.runTaskTimer(plugin, 0L, 20L);
			DeathBeamTaskManager.register(player.getUniqueId(), beam);
		}

	}
}
