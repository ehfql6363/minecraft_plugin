package kwangdong.pingplugin.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;

import kwangdong.pingplugin.tasks.DeathBeamTask;
import kwangdong.pingplugin.tasks.DeathBeamTaskManager;
import kwangdong.pingplugin.PingPlugin;
import kwangdong.pingplugin.util.ColorUtil;

public class DeathListener implements Listener {
	private final Plugin plugin;

	public DeathListener(Plugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		Location loc = player.getLocation();

		PingPlugin.deathMap.put(player.getUniqueId(),
			new PingPlugin.LocationInfo(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName()));

		// UUID 기반 색상
		DeathBeamTask beam = new DeathBeamTask(loc, ColorUtil.getColorForPlayer(player));
		beam.runTaskTimer(plugin, 0L, 20L);
		DeathBeamTaskManager.register(player.getUniqueId(), beam);
	}
}
