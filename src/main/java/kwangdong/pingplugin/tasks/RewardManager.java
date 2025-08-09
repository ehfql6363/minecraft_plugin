package kwangdong.pingplugin.tasks;

import java.util.Objects;

import kwangdong.pingplugin.util.EnchantUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class RewardManager {

	private static NamespacedKey SCROLL_KEY;
	private static final String SCROLL_NAME = "강화 스크롤";
	private static final int ADDITIONAL_EXP = 12000;

	public static void init(Plugin plugin) {
		SCROLL_KEY = new NamespacedKey(plugin, "upgrade_scroll");
	}

	public static NamespacedKey getScrollKey() {
		return SCROLL_KEY;
	}

	public static void giveReward(Player player) {
		player.giveExp(ADDITIONAL_EXP);

		player.getInventory().addItem(makeScroll());
		player.sendMessage(Component.text("강화 스크롤을 획득했습니다! 왼손 아이템에 사용하세요.", NamedTextColor.GREEN));
	}

	public static ItemStack makeScroll() {
		ItemStack item = new ItemStack(Material.PAPER);
		ItemMeta meta = item.getItemMeta();

		Objects.requireNonNull(meta.displayName()).append(Component.text(SCROLL_NAME, NamedTextColor.AQUA));
		Objects.requireNonNull(meta.lore()).add(Component.text("왼손 아이템의 인첸트를 +1 강화합니다."));
		Objects.requireNonNull(meta.lore()).add(Component.text("인첸트 제한 무시!"));

		meta.getPersistentDataContainer().set(SCROLL_KEY, PersistentDataType.BYTE, (byte) 1);
		item.setItemMeta(meta);
		return item;
	}

	public static boolean isUpgradeScroll(ItemStack stack) {
		if(stack == null || stack.getType() == Material.AIR)
			return false;

		ItemMeta meta = stack.getItemMeta();
		if(meta == null)
			return false;

		Byte tag = meta.getPersistentDataContainer().get(SCROLL_KEY, PersistentDataType.BYTE);
		return tag != null && tag == (byte)1;
	}
}
