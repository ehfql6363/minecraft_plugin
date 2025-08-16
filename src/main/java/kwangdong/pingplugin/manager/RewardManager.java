package kwangdong.pingplugin.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class RewardManager {

    private static final int DRAGON_EXP = 12000;
    private static final Random RNG = new Random();

    private static int UPGRADE_WEIGHT = 70;
    private static int REPAIR_WEIGHT  = 30;

    private static NamespacedKey UPGRADE_SCROLL_KEY;
    private static NamespacedKey REPAIR_SCROLL_KEY;

    private static final String NAME_UPGRADE = "강화 스크롤";
    private static final String NAME_REPAIR  = "내구도 회복 스크롤";

    private static Plugin pluginRef; // 로그용

    public static void init(Plugin plugin) {
        pluginRef = plugin;
        UPGRADE_SCROLL_KEY = new NamespacedKey(plugin, "upgrade_scroll");
        REPAIR_SCROLL_KEY  = new NamespacedKey(plugin, "repair_scroll");

        plugin.getConfig();
        UPGRADE_WEIGHT = plugin.getConfig().getInt("rewards.upgrade_scroll_weight", UPGRADE_WEIGHT);
        REPAIR_WEIGHT  = plugin.getConfig().getInt("rewards.repair_scroll_weight",  REPAIR_WEIGHT);
    }

    /** 웨이브 클리어 보상 지급 */
    public static void giveReward(Player player) {
        player.giveExp(DRAGON_EXP);

        ItemStack scroll = rollOneScroll();
        player.getInventory().addItem(scroll);

        boolean isUp = isUpgradeScroll(scroll);
        player.sendMessage(Component.text(
                isUp ? "[보상] 강화 스크롤을 획득했습니다! 왼손 아이템에 사용하세요."
                        : "[보상] 내구도 회복 스크롤을 획득했습니다! 왼손 아이템에 사용하세요.",
                NamedTextColor.GREEN
        ));

        if (pluginRef != null) {
            pluginRef.getLogger().info(String.format(
                    "[Wave] Reward given to %s: EXP=%d, scroll=%s",
                    player.getName(), DRAGON_EXP, (isUp ? "UPGRADE" : "REPAIR")
            ));
        }
    }

	// ===== 확률 롤 =====
	private static ItemStack rollOneScroll() {
		int sum = Math.max(0, UPGRADE_WEIGHT) + Math.max(0, REPAIR_WEIGHT);
		if (sum <= 0) {
			// 가중치가 0/0 등 비정상인 경우 안전하게 강화 스크롤
			return makeUpgradeScroll();
		}
		int r = RNG.nextInt(sum);
		return (r < UPGRADE_WEIGHT) ? makeUpgradeScroll() : makeRepairScroll();
	}

	// ===== 스크롤 제작 & 판별 =====
	public static ItemStack makeUpgradeScroll() {
		ItemStack item = new ItemStack(Material.PAPER);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return item;

		meta.displayName(Component.text(NAME_UPGRADE, NamedTextColor.AQUA));
		List<Component> lore = new ArrayList<>();
		lore.add(Component.text("왼손 아이템의 인첸트를 +1 강화합니다.", NamedTextColor.WHITE));
		lore.add(Component.text("인첸트 제한 무시!", NamedTextColor.GRAY));
		meta.lore(lore);

		meta.getPersistentDataContainer().set(Objects.requireNonNull(UPGRADE_SCROLL_KEY), PersistentDataType.BYTE, (byte)1);
		item.setItemMeta(meta);
		return item;
	}

	public static ItemStack makeRepairScroll() {
		ItemStack item = new ItemStack(Material.PAPER);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return item;

		meta.displayName(Component.text(NAME_REPAIR, NamedTextColor.AQUA));
		List<Component> lore = new ArrayList<>();
		lore.add(Component.text("왼손 아이템의 내구도를 최대치로 회복합니다.", NamedTextColor.WHITE));
		lore.add(Component.text("내구도 있는 장비에만 사용 가능", NamedTextColor.GRAY));
		meta.lore(lore);

		meta.getPersistentDataContainer().set(Objects.requireNonNull(REPAIR_SCROLL_KEY), PersistentDataType.BYTE, (byte)1);
		item.setItemMeta(meta);
		return item;
	}

	public static boolean isUpgradeScroll(ItemStack stack) {
		if (stack == null || stack.getType() == Material.AIR) return false;
		ItemMeta meta = stack.getItemMeta();
		if (meta == null) return false;
		Byte tag = meta.getPersistentDataContainer().get(UPGRADE_SCROLL_KEY, PersistentDataType.BYTE);
		return tag != null && tag == (byte)1;
	}

	public static boolean isRepairScroll(ItemStack stack) {
		if (stack == null || stack.getType() == Material.AIR) return false;
		ItemMeta meta = stack.getItemMeta();
		if (meta == null) return false;
		Byte tag = meta.getPersistentDataContainer().get(REPAIR_SCROLL_KEY, PersistentDataType.BYTE);
		return tag != null && tag == (byte)1;
	}
}
