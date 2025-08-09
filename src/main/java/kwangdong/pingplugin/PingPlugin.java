package kwangdong.pingplugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import kwangdong.pingplugin.commands.DeathCommand;
import kwangdong.pingplugin.commands.PingCommand;
import kwangdong.pingplugin.commands.WaveCommand;
import kwangdong.pingplugin.listeners.DeathListener;
import kwangdong.pingplugin.listeners.ScrollUseListener;
import kwangdong.pingplugin.listeners.WaveListener;
import kwangdong.pingplugin.listeners.WaveMobDropListener;
import kwangdong.pingplugin.listeners.WaveSleepListener;
import kwangdong.pingplugin.tasks.RewardManager;
import kwangdong.pingplugin.tasks.WaveManager;
import kwangdong.pingplugin.util.WaveTags;

public class PingPlugin extends JavaPlugin {

	private static PingPlugin instance;
	private BukkitTask autoWaveTask;
	private WaveManager waveManager;
	// 사망 좌표 저장소 (플레이어 UUID → 좌표 문자열)
	public static Map<UUID, LocationInfo> deathMap = new HashMap<>();
	@Override
	public void onEnable() {
		this.waveManager = new WaveManager(this);

		RewardManager.init(this);
		WaveTags.init(this);

		getCommand("ping").setExecutor(new PingCommand());
		getCommand("death").setExecutor(new DeathCommand());
		getCommand("wave").setExecutor(new WaveCommand(waveManager));

		getServer().getPluginManager().registerEvents(new DeathListener(this), this);
		getServer().getPluginManager().registerEvents(new WaveListener(waveManager), this);
		getServer().getPluginManager().registerEvents(new ScrollUseListener(), this);
		getServer().getPluginManager().registerEvents(new WaveSleepListener(waveManager), this);
		getServer().getPluginManager().registerEvents(new WaveMobDropListener(), this);

		getLogger().info("PingPlugin 활성화됨");
	}

	@Override
	public void onDisable() {
		getLogger().info("PingPlugin 비활성화됨");
	}

	public static PingPlugin getInstance() {
		return instance;
	}

	public record LocationInfo(int x, int y, int z, String world) {}

}
