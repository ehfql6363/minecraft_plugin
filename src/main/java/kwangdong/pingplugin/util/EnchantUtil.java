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

    private static final Set<Enchantment> ALLOWED_ENCHANTS = Set.of(
            Enchantment.SHARPNESS, Enchantment.SMITE, Enchantment.BANE_OF_ARTHROPODS,
            Enchantment.FEATHER_FALLING, Enchantment.FIRE_ASPECT, Enchantment.PROTECTION,
            Enchantment.PROJECTILE_PROTECTION, Enchantment.BLAST_PROTECTION, Enchantment.FIRE_PROTECTION,
            Enchantment.UNBREAKING, Enchantment.POWER, Enchantment.LOOTING, Enchantment.FORTUNE, Enchantment.THORNS
    );

    public static class UpgradeResult {
        public final Enchantment enchantment;
        public final int oldLevel;
        public final int newLevel;
        public UpgradeResult(Enchantment e, int oldL, int newL) {
            this.enchantment = e; this.oldLevel = oldL; this.newLevel = newL;
        }
    }

    /** 허용 인첸트 중 '랜덤' 하나 레벨 +1 (unsafe). 성공 시 상세 결과 반환 */
    public static Optional<UpgradeResult> upgradeEnchantmentRandomDetailed(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Optional.empty();

        Map<Enchantment, Integer> current = meta.getEnchants();
        if (current.isEmpty()) return Optional.empty();

        List<Enchantment> candidates = current.keySet().stream()
                .filter(ALLOWED_ENCHANTS::contains)
                .collect(Collectors.toList());
        if (candidates.isEmpty()) return Optional.empty();

        Enchantment target = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        int oldLevel = current.getOrDefault(target, 0);
        int newLevel = oldLevel + 1;

        meta.addEnchant(target, newLevel, true);
        item.setItemMeta(meta);

        return Optional.of(new UpgradeResult(target, oldLevel, newLevel));
    }

    /** 한글 표시 이름(간단 버전) */
    public static String displayName(Enchantment e) {
        return switch (e.getKey().getKey()) {
            case "sharpness" -> "날카로움";
            case "smite" -> "강타";
            case "bane_of_arthropods" -> "살충";
            case "feather_falling" -> "가벼운 착지";
            case "fire_aspect" -> "발화";
            case "protection" -> "보호";
            case "projectile_protection" -> "투사체 보호";
            case "blast_protection" -> "폭발 보호";
            case "fire_protection" -> "화염 보호";
            case "unbreaking" -> "내구성";
            case "power" -> "힘";
            case "looting" -> "약탈";
            case "fortune" -> "행운";
            case "thorns" -> "가시";
            default -> e.getKey().getKey();
        };
    }
}
