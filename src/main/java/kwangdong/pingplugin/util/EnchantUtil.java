package kwangdong.pingplugin.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class EnchantUtil {

	private EnchantUtil() {}

	// 허용된 인첸트 목록 (네가 준 세트 그대로 사용)
	private static final Set<Enchantment> ALLOWED_ENCHANTS = Set.of(
		Enchantment.SHARPNESS,                 // 날카로움
		Enchantment.SMITE,                     // 강타
		Enchantment.BANE_OF_ARTHROPODS,        // 살충
		Enchantment.FEATHER_FALLING,           // 가벼운 착지
		Enchantment.FIRE_ASPECT,               // 발화
		Enchantment.PROTECTION,                // 보호
		Enchantment.PROJECTILE_PROTECTION,     // 투사체 보호
		Enchantment.BLAST_PROTECTION,          // 폭발 보호
		Enchantment.FIRE_PROTECTION,           // 화염 보호
		Enchantment.UNBREAKING,                // 내구성
		Enchantment.POWER,                     // 힘
		Enchantment.LOOTING,                   // 약탈
		Enchantment.FORTUNE,                   // 행운
		Enchantment.THORNS                     // 가시
	);

	/**
	 * 아이템의 "허용된 인첸트 중 하나"를 골라 레벨을 +1 올린다.
	 * - 이미 붙은 인첸트만 대상으로 한다(새 인첸트는 부여하지 않음)
	 * - 여러 개면 현재 레벨이 가장 높은 인첸트를 우선, 동률이면 이름 순
	 * - 바닐라 제한을 무시(unsafe 강화)하여 실제 성능이 증가한다.
	 *
	 * @param item 강화 대상 아이템(예: 오프핸드)
	 * @return 강화 성공 시 true, 강화할 인첸트가 없으면 false
	 */
    public static boolean upgradeEnchantmentRandom(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        Map<Enchantment, Integer> current = meta.getEnchants();
        if (current.isEmpty()) return false;

        List<Enchantment> candidates = current.keySet().stream()
                .filter(ALLOWED_ENCHANTS::contains)
                .collect(Collectors.toList());

        if (candidates.isEmpty()) return false;

        Enchantment target = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        int newLevel = current.get(target) + 1;

        meta.addEnchant(target, newLevel, true); // unsafe 허용
        item.setItemMeta(meta);
        return true;
    }

	/**
	 * 이 아이템에 붙어있는 "허용 인첸트" 목록을 반환(디버그/표시용).
	 */
	public static Map<Enchantment, Integer> getAllowedEnchantsOn(ItemStack item) {
		Map<Enchantment, Integer> result = new HashMap<>();
		if (item == null || item.getType() == Material.AIR) return result;
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return result;

		for (Map.Entry<Enchantment, Integer> e : meta.getEnchants().entrySet()) {
			if (ALLOWED_ENCHANTS.contains(e.getKey())) {
				result.put(e.getKey(), e.getValue());
			}
		}
		return result;
	}

	/**
	 * 허용된 인첸트인지 여부(외부에서 검증 필요할 때 사용).
	 */
	public static boolean isAllowed(Enchantment enchantment) {
		return ALLOWED_ENCHANTS.contains(enchantment);
	}
}
