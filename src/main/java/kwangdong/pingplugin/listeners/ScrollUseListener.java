package kwangdong.pingplugin.listeners;

import kwangdong.pingplugin.tasks.RewardManager;
import kwangdong.pingplugin.util.EnchantUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ScrollUseListener implements Listener {

	@EventHandler
	public void onUseScroll(PlayerInteractEvent event) {
		if(event.getHand() != EquipmentSlot.HAND)
			return;

		ItemStack used = event.getItem();
		if(!RewardManager.isUpgradeScroll(used))
			return;

		Player player = event.getPlayer();
		ItemStack offhand = player.getInventory().getItemInOffHand();

		if (offhand == null || offhand.getType() == Material.AIR) {
			player.sendMessage(
				Component.text("왼손에 강화할 아이템이 없습니다. 1개 이상의 인첸트가 적용된 아이템을 사용해주세요.", NamedTextColor.YELLOW));
			return;
		}

		// 강화 수행
		boolean upgraded = EnchantUtil.upgradeEnchantmentRandom(offhand);
		if (!upgraded) {
			player.sendMessage("§c강화 가능한 인첸트가 없습니다.");
			return;
		}

		// 스크롤 1장 소비
		if (used.getAmount() > 1) used.setAmount(used.getAmount() - 1);
		else player.getInventory().setItemInMainHand(null);

		player.sendMessage("§d강화 스크롤 사용! 왼손 아이템이 +1 강화되었습니다.");
		event.setCancelled(true); // 종이 기본 상호작용 방지
	}
}
