package kwangdong.pingplugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import kwangdong.pingplugin.manager.WaveManager;

public class NightWatcher implements Runnable {

	private final WaveManager waveManager;
	private final Map<UUID, Boolean> wasNight = new HashMap<>();

	public NightWatcher(WaveManager waveManager) {
		this.waveManager = waveManager;
	}

	@Override
	public void run() {
		for (World world : Bukkit.getWorlds()) {
			// "밤" 정의: 13000 <= time < 23000
			long t = world.getTime();
			boolean nowNight = (t >= 13000 && t < 23000);
			boolean prevNight = wasNight.getOrDefault(world.getUID(), false);

			// 낮 -> 밤으로 "자연스럽게" 넘어가는 순간에만 트리거
			if (!prevNight && nowNight) {
				waveManager.tryStartWaveNight(world.getPlayers());
			}
			wasNight.put(world.getUID(), nowNight);
		}
	}
}
