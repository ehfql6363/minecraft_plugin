package kwangdong.pingplugin.util;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.entity.Player;

public class ColorUtil {

	public static Color getColorForPlayer(Player player) {
		// UUID를 기반으로 Random 시드 고정 → 항상 같은 색 생성
		Random rand = new Random(player.getUniqueId().hashCode());

		int r = rand.nextInt(256);
		int g = rand.nextInt(256);
		int b = rand.nextInt(256);

		return Color.fromRGB(r, g, b);
	}
}
