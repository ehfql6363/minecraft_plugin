package kwangdong.pingplugin.listeners;

import kwangdong.pingplugin.manager.RewardManager;
import kwangdong.pingplugin.util.EnchantUtil;
import kwangdong.pingplugin.util.EnchantUtil.UpgradeResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Optional;

public class ScrollUseListener implements Listener {

	@EventHandler
	public void onUse(PlayerInteractEvent e) {
		// 메인핸드 우클릭만 처리 (양손 중복 방지)
		if (e.getHand() != EquipmentSlot.HAND) return;
		if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

		Player p = e.getPlayer();
		ItemStack hand = p.getInventory().getItemInMainHand();
		if (hand == null || hand.getType() == Material.AIR) return;

		boolean isUpgrade = RewardManager.isUpgradeScroll(hand);
		boolean isRepair  = RewardManager.isRepairScroll(hand);
		if (!isUpgrade && !isRepair) return;

		// 스크롤 사용은 기본 행위 덮어쓰기
		e.setCancelled(true);

		// 대상: 왼손 아이템
		ItemStack off = p.getInventory().getItemInOffHand();
		if (off == null || off.getType() == Material.AIR) {
			p.sendMessage(Component.text("왼손에 대상 아이템을 들고 사용하세요.", NamedTextColor.YELLOW));
			return;
		}

		if (isUpgrade) {
			// 허용 인첸트 중 '현재 달린 것들'에서 랜덤 +1 (제한 무시)
			Optional<UpgradeResult> resOpt = EnchantUtil.upgradeEnchantmentRandomDetailed(off);
			if (resOpt.isPresent()) {
				UpgradeResult res = resOpt.get();
				String nameKo = EnchantUtil.displayName(res.enchantment);
				p.sendMessage(
					Component.text("[강화] ", NamedTextColor.GOLD)
						.append(Component.text(nameKo + " ", NamedTextColor.AQUA))
						.append(Component.text(res.oldLevel + " → " + res.newLevel, NamedTextColor.GREEN))
				);
				p.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
				consumeOne(p, hand);
			} else {
				// 허용 인첸트가 없거나 인첸트 자체가 없는 경우
				p.sendMessage(Component.text("강화할 수 있는 인첸트를 찾지 못했습니다.", NamedTextColor.RED));
			}
			return;
		}

		if (isRepair) {
			ItemMeta meta = off.getItemMeta();
			if (!(meta instanceof Damageable dmgMeta) || off.getType().getMaxDurability() <= 0) {
				p.sendMessage(Component.text("이 아이템은 내구도가 없습니다.", NamedTextColor.RED));
				return;
			}
			dmgMeta.setDamage(0);
			off.setItemMeta(dmgMeta);

			p.sendMessage(Component.text("왼손 아이템의 내구도를 최대치로 회복했습니다!", NamedTextColor.GREEN));
			p.playSound(p, Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.2f);
			consumeOne(p, hand);
		}
	}

	private void consumeOne(Player p, ItemStack hand) {
		int amt = hand.getAmount();
		if (amt <= 1) {
			p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
		} else {
			hand.setAmount(amt - 1);
		}
	}
}
