package kwangdong.pingplugin.manager;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 웨이브 동안 임시 리스폰 지점 지정 + 종료 시 복원.
 * - 기존 리스폰이 없던 경우(null)도 그대로 기억했다가 종료 시 null로 복원.
 */
public class RespawnPointManager {

	private final Map<UUID, Location> original = new HashMap<>();

	/** 플레이어의 "기존 리스폰"을 저장하고, 새 리스폰을 지정 */
	public void setWaveRespawn(Player p, Location newRespawn) {
		UUID id = p.getUniqueId();
		// 기존 리스폰을 한 번만 저장 (여러 번 호출되어도 최초 값 유지)
		original.putIfAbsent(id, p.getRespawnLocation()); // Paper API

		// 웨이브용 리스폰 위치 지정
		p.setRespawnLocation(newRespawn);
	}

	/** 한 명 복원 */
	public void restore(Player p) {
		UUID id = p.getUniqueId();
		Location prev = original.remove(id); // 없으면 null
		// 원래 리스폰으로 복원 (null이면 리스폰 지정 해제)
		p.setRespawnLocation(prev);
	}

	/** 여러 명 복원 */
	public void restoreAll(Collection<? extends Player> players) {
		for (Player p : players) restore(p);
		original.clear();
	}
}
