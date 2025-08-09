package kwangdong.pingplugin.tasks;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WavePlayerState {

	private static final Map<UUID, ItemStack[]> invBackup = new HashMap<>();
	private static final Map<UUID, Integer> expBackup = new HashMap<>();

	public static void saveState(Player player) {
		invBackup.put(player.getUniqueId(), player.getInventory().getContents());
		expBackup.put(player.getUniqueId(), player.getTotalExperience());
	}

	/** 웨이브 종료 시 전체 복원 */
	public static void restoreState(Player player) {
		ItemStack[] items = invBackup.remove(player.getUniqueId());
		Integer exp = expBackup.remove(player.getUniqueId());

		if (items != null) player.getInventory().setContents(items);
		if (exp != null) player.setTotalExperience(exp);
	}

	/** 웨이브 중 사망 시 즉시 복원 (아이템/경험치 보호) */
	public static void restoreOnDeath(Player player) {
		ItemStack[] items = invBackup.get(player.getUniqueId());
		Integer exp = expBackup.get(player.getUniqueId());

		if (items != null) player.getInventory().setContents(items);
		if (exp != null) player.setTotalExperience(exp);
	}
}
