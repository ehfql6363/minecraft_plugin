package kwangdong.pingplugin.tasks;

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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class RewardManager {

    private static NamespacedKey SCROLL_KEY;
    private static NamespacedKey SCROLL_UID_KEY; // 스택 방지용 개별 식별자
    private static final String SCROLL_NAME = "강화 스크롤";
    private static final int ADDITIONAL_EXP = 12000;

    public static void init(Plugin plugin) {
        SCROLL_KEY = new NamespacedKey(plugin, "upgrade_scroll");
        SCROLL_UID_KEY = new NamespacedKey(plugin, "upgrade_scroll_uid");
    }

    public static NamespacedKey getScrollKey() {
        return SCROLL_KEY;
    }

    public static void giveReward(Player player) {
        // 엔더드래곤급 경험치
        player.giveExp(ADDITIONAL_EXP);

        // 스크롤 지급 (인벤 꽉 차면 바닥으로 드롭)
        ItemStack scroll = makeScroll();
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(scroll);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItem(player.getLocation(), scroll);
        }

        player.sendMessage(Component.text(SCROLL_NAME + "을(를) 획득했습니다! 왼손 아이템에 사용하세요.", NamedTextColor.GREEN));
    }

    public static ItemStack makeScroll() {
        Objects.requireNonNull(SCROLL_KEY, "RewardManager.init(plugin) 먼저 호출해야 합니다.");

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item; // 방어적 처리

        // 이름/로어
        meta.displayName(Component.text(SCROLL_NAME, NamedTextColor.AQUA));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("왼손 아이템의 인첸트를 +1 강화합니다."));
        lore.add(Component.text("인첸트 제한 무시!"));
        meta.lore(lore);

        // 스크롤 식별 태그
        meta.getPersistentDataContainer().set(SCROLL_KEY, PersistentDataType.BYTE, (byte) 1);

        // 스택 방지: 개별 UID 부여 (같은 스크롤도 서로 다른 아이템으로 취급)
        if (SCROLL_UID_KEY != null) {
            meta.getPersistentDataContainer().set(SCROLL_UID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        }

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isUpgradeScroll(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;

        Byte tag = meta.getPersistentDataContainer().get(SCROLL_KEY, PersistentDataType.BYTE);
        return tag != null && tag == (byte) 1;
    }
}
