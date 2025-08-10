package kwangdong.pingplugin.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 웨이브 내 유령 상태 전환/복원 담당.
 * - setGhost: 관전 모드로 전환, 안내 메시지
 * - restore: 웨이브 종료 시 원복
 */
public class GhostManager {

	private final Map<UUID, GameMode> prevMode = new HashMap<>();

	/** 유령 상태 적용 (관전 모드) */
	public void setGhost(Player p) {
		if (p == null) return;
		// 이미 관전이면 스킵
		if (p.getGameMode() == GameMode.SPECTATOR) return;

		// 이전 모드 백업 (없으면 SURVIVAL로 가정)
		prevMode.putIfAbsent(p.getUniqueId(), p.getGameMode());

		p.setGameMode(GameMode.SPECTATOR);
		p.sendMessage(Component.text("유령 상태로 전환됨. 웨이브 종료까지 관전만 가능", NamedTextColor.GRAY));
	}

	/** 유령 상태 복원 */
	public void restore(Player p) {
		if (p == null) return;
		GameMode mode = prevMode.remove(p.getUniqueId());
		if (mode == null) mode = GameMode.SURVIVAL;
		// 이미 그 모드면 스킵
		if (p.getGameMode() != mode) {
			p.setGameMode(mode);
		}
	}

	/** 웨이브 완전 종료 시 내부 캐시 클리어 */
	public void clearAll() {
		prevMode.clear();
	}
}
