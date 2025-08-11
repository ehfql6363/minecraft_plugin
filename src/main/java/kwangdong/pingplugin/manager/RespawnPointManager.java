package kwangdong.pingplugin.manager;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RespawnPointManager {
	private final Map<UUID, Location> waveRespawns = new HashMap<>();

	/** 웨이브 중 사용할 임시 리스폰 위치만 기억 (영구 리스폰은 손대지 않음) */
	public void rememberWaveRespawn(Player p, Location loc) {
		waveRespawns.put(p.getUniqueId(), loc.clone());
	}

	public Location getWaveRespawn(Player p) {
		return waveRespawns.get(p.getUniqueId());
	}

	public void clearAll() {
		waveRespawns.clear();
	}
}
