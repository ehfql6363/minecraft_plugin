package kwangdong.pingplugin.tasks;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import kwangdong.pingplugin.util.WaveTags;

public class WaveSpawner {

	private static final Random random = new Random();

	// 워든, 드래곤, 위더, 슬라임류 제외
	private static final List<EntityType> ALLOWED = Arrays.asList(
		EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
		EntityType.PILLAGER, EntityType.DROWNED, EntityType.HUSK, EntityType.ENDERMAN,
		EntityType.STRAY, EntityType.WITCH, EntityType.VINDICATOR, EntityType.EVOKER
	);

	public static void spawnMonster(Location loc, int round) {
		EntityType type = ALLOWED.get(random.nextInt(ALLOWED.size()));
		LivingEntity mob = (LivingEntity) loc.getWorld().spawnEntity(loc, type);

		// ✅ 웨이브 몹 태그
		mob.getPersistentDataContainer().set(WaveTags.WAVE_MOB, PersistentDataType.BYTE, (byte)1);

		// 장비 드랍 확률 0 (방어적 조치)
		if (mob.getEquipment() != null) {
			mob.getEquipment().setItemInMainHandDropChance(0f);
			mob.getEquipment().setItemInOffHandDropChance(0f);
			mob.getEquipment().setHelmetDropChance(0f);
			mob.getEquipment().setChestplateDropChance(0f);
			mob.getEquipment().setLeggingsDropChance(0f);
			mob.getEquipment().setBootsDropChance(0f);
		}

		// 10% 엘리트: HP/ATK * 2
		if (random.nextInt(10) == 0) {
			maybeDouble(mob, Attribute.MAX_HEALTH);
			maybeDouble(mob, Attribute.ATTACK_DAMAGE);
			mob.setHealth(mob.getAttribute(Attribute.MAX_HEALTH).getBaseValue());
			mob.setCustomName("§c[엘리트] " + type.name());
			mob.setCustomNameVisible(true);
		}
	}

	private static void maybeDouble(LivingEntity mob, Attribute attr) {
		if (mob.getAttribute(attr) != null) {
			double base = mob.getAttribute(attr).getBaseValue();
			mob.getAttribute(attr).setBaseValue(base * 2.0);
		}
	}
}
