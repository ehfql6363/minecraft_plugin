package kwangdong.pingplugin.manager;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class RespawnPointManager {

	private final Map<UUID, Location> original = new HashMap<>();
	private final Map<UUID, Location> waveRespawns = new HashMap<>();

	/** 웨이브 시작 시: 기존 리스폰 저장 + 웨이브 리스폰 지정 */
	public void setWaveRespawn(Player p, Location waveLoc) {
		UUID id = p.getUniqueId();
		original.putIfAbsent(id, p.getRespawnLocation()); // null 가능
		waveRespawns.put(id, waveLoc.clone());

		// ✅ Paper API: 침대/앵커 검사 우회하고 다음 리스폰 지점 지정
		p.setRespawnLocation(waveLoc);
	}

	/** 웨이브 종료 시: 원래 리스폰으로 복원 (null이면 해제) */
	public void restore(Player p) {
		UUID id = p.getUniqueId();
		Location prev = original.remove(id);
		waveRespawns.remove(id);
		p.setRespawnLocation(prev); // prev == null 이면 해제
	}

	public void restoreAll(Collection<? extends Player> players) {
		for (Player p : players) restore(p);
	}

	public Location getWaveRespawn(UUID id) {
		return waveRespawns.get(id);
	}
}
