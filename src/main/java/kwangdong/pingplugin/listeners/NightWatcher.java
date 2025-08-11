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
			long t = world.getTime();
			boolean nowNight = (t >= 13000 && t < 23000);

			UUID id = world.getUID();
			Boolean prev = wasNight.get(id);

			// 서버 첫 사이클: 상태만 기록하고 넘김(즉시 트리거 방지)
			if (prev == null) {
				wasNight.put(id, nowNight);
				continue;
			}

			// 낮 -> 밤으로 넘어가는 순간에만
			if (!prev && nowNight) {
				if (!world.getPlayers().isEmpty()) {
					waveManager.tryStartWaveNight(world.getPlayers()); // 내부에서 10% & 60초 준비
				}
			}
			wasNight.put(id, nowNight);
		}
	}

}
