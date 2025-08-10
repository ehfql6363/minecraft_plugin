package kwangdong.pingplugin.manager;

import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;

import java.util.*;

public class RespawnPointManager {

	private final Map<UUID, Location> original = new HashMap<>();
	private final Map<UUID, Location> waveRespawns = new HashMap<>();

	/** 웨이브 시작 시: 기존 리스폰 저장 + 웨이브 리스폰 강제 지정 */
	public void setWaveRespawn(Player p, Location waveLoc) {
		UUID id = p.getUniqueId();
		original.putIfAbsent(id, p.getRespawnLocation()); // Paper 1.20+ (없으면 null)
		waveRespawns.put(id, waveLoc.clone());

		// Overworld면 침대 스폰으로 강제 등록(메시지 방지), 그 외 월드는 이벤트에서 덮어씀
		if (waveLoc.getWorld().getEnvironment() == Environment.NORMAL) {
			// 베드 없어도 강제로 스폰 지점으로 사용
			p.setBedSpawnLocation(waveLoc, true);
		}
		// Paper면 아래도 같이 해두면 좋음(다음 리스폰 기본값)
		try {
			p.setRespawnLocation(waveLoc);
		} catch (NoSuchMethodError ignored) {}
	}

	/** 웨이브 종료 시: 원래 리스폰으로 복원 */
	public void restore(Player p) {
		UUID id = p.getUniqueId();
		Location prev = original.remove(id);
		waveRespawns.remove(id);

		// 기존 값 복원 (null이면 해제)
		if (prev != null) {
			p.setBedSpawnLocation(prev, true);
			try { p.setRespawnLocation(prev); } catch (NoSuchMethodError ignored) {}
		} else {
			p.setBedSpawnLocation(null, true);
			try { p.setRespawnLocation(null); } catch (NoSuchMethodError ignored) {}
		}
	}

	public void restoreAll(Collection<? extends Player> players) {
		for (Player p : players) restore(p);
	}

	public Location getWaveRespawn(UUID id) {
		return waveRespawns.get(id);
	}
}
