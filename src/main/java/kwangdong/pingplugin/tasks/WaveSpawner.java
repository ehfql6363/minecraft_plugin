package kwangdong.pingplugin.tasks;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import kwangdong.pingplugin.util.WaveTags;
import org.bukkit.util.Vector;

public class WaveSpawner {

	private static final Random random = new Random();

	// 워든, 드래곤, 위더, 슬라임류 제외
	private static final List<EntityType> ALLOWED = Arrays.asList(
		EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
		EntityType.PILLAGER, EntityType.DROWNED, EntityType.HUSK, EntityType.ENDERMAN,
		EntityType.STRAY, EntityType.WITCH, EntityType.VINDICATOR, EntityType.EVOKER
	);

	public static void spawnMonster(Location center, int round, Collection<Player> participants) {
        Location loc = findSafeSpawnNear(center, participants, 12, 24);
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

    // 중심에서 [minR, maxR] 사이로만, 참여자와도 최소 거리 확보
    private static Location findSafeSpawnNear(Location center, Collection<Player> participants,
                                              int minR, int maxR) {
        World w = center.getWorld();
        Random r = random;

        final double minDist2Center = minR * minR;
        final double minDist2Player = 10 * 10; // 참여자와 최소 10블록 이상

        for (int tries = 0; tries < 40; tries++) {
            double angle = r.nextDouble() * Math.PI * 2;
            double radius = minR + r.nextDouble() * (maxR - minR);

            double tx = center.getX() + Math.cos(angle) * radius;
            double tz = center.getZ() + Math.sin(angle) * radius;
            int x = (int) Math.floor(tx);
            int z = (int) Math.floor(tz);

            int y = w.getHighestBlockYAt(x, z) + 1;
            Location loc = new Location(w, x + 0.5, y, z + 0.5);

            // 중심과의 거리 보장
            if (loc.distanceSquared(center) < minDist2Center) continue;

            // 참여자와의 거리 보장
            boolean tooCloseToPlayer = false;
            for (Player p : participants) {
                if (!p.getWorld().equals(w)) continue;
                if (loc.distanceSquared(p.getLocation()) < minDist2Player) {
                    tooCloseToPlayer = true; break;
                }
            }
            if (tooCloseToPlayer) continue;

            // 머리 위 2칸 비었는지
            if (!w.getBlockAt(loc).isEmpty()) continue;
            if (!w.getBlockAt(loc.clone().add(0, 1, 0)).isEmpty()) continue;

            return loc;
        }
        // 그래도 못 찾으면 마지막 수단: 중심에서 15블록 직선 이동 후 y보정
        Vector dir = new Vector(1, 0, 0).rotateAroundY(r.nextDouble() * Math.PI * 2);
        Location fallback = center.clone().add(dir.multiply(15));
        int fy = w.getHighestBlockYAt(fallback.getBlockX(), fallback.getBlockZ()) + 1;
        fallback.setY(fy);
        return fallback;
    }
}
