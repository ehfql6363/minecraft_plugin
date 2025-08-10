package kwangdong.pingplugin.listeners;

import kwangdong.pingplugin.manager.RewardManager;
import kwangdong.pingplugin.util.EnchantUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ScrollUseListener implements Listener {

    @EventHandler
    public void onUseScroll(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack used = event.getItem();
        if (!RewardManager.isUpgradeScroll(used)) return;

        Player player = event.getPlayer();
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null || offhand.getType() == Material.AIR) {
            player.sendMessage(Component.text("왼손에 강화할 아이템이 없습니다.", NamedTextColor.RED));
            return;
        }

        var resultOpt = EnchantUtil.upgradeEnchantmentRandomDetailed(offhand);
        if (resultOpt.isEmpty()) {
            player.sendMessage(Component.text("강화 가능한 인첸트가 없습니다.", NamedTextColor.RED));
            return;
        }

        var res = resultOpt.get();
        String name = EnchantUtil.displayName(res.enchantment);
        player.sendMessage(Component.text("강화 스크롤 사용! ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(name + " ", NamedTextColor.WHITE))
                .append(Component.text(res.oldLevel + " → ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(res.newLevel), NamedTextColor.GREEN)));

        // 스크롤 1장 소비
        if (used.getAmount() > 1) used.setAmount(used.getAmount() - 1);
        else player.getInventory().setItemInMainHand(null);

        event.setCancelled(true);
    }
}
