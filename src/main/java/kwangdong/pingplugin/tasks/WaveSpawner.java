package kwangdong.pingplugin.tasks;

import kwangdong.pingplugin.util.WaveTags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class WaveSpawner {

    private static final Random random = new Random();

    private static final Set<EntityType> BLOCKED = Set.of(
            EntityType.WARDEN, EntityType.ENDER_DRAGON, EntityType.WITHER,
            EntityType.SLIME, EntityType.MAGMA_CUBE
    );

    private static final List<EntityType> ALLOWED = Arrays.stream(EntityType.values())
            .filter(EntityType::isSpawnable)
            .filter(EntityType::isAlive)
            .filter(t -> t.getEntityClass() != null
                    && LivingEntity.class.isAssignableFrom(t.getEntityClass()))
            .filter(t -> !BLOCKED.contains(t))
            .filter(t -> {
                Class<?> clazz = t.getEntityClass();
                return Monster.class.isAssignableFrom(clazz) || t == EntityType.ENDERMAN;
            })
            .toList();


    private static Location findSafeSpawnNear(Location center, Collection<Player> participants,
                                              int minR, int maxR) {
        World w = center.getWorld();
        for (int tries = 0; tries < 40; tries++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = minR + random.nextDouble() * (maxR - minR);

            int x = (int) Math.floor(center.getX() + Math.cos(angle) * radius);
            int z = (int) Math.floor(center.getZ() + Math.sin(angle) * radius);
            int y = w.getHighestBlockYAt(x, z) + 1;

            Location loc = new Location(w, x + 0.5, y, z + 0.5);

            if (!w.getBlockAt(loc).isEmpty()) continue;
            if (!w.getBlockAt(loc.clone().add(0, 1, 0)).isEmpty()) continue;

            boolean tooClose = false;
            for (Player p : participants) {
                if (!p.getWorld().equals(w)) continue;
                if (loc.distanceSquared(p.getLocation()) < 10 * 10) { tooClose = true; break; }
            }
            if (tooClose) continue;

            return loc;
        }
        // fallback
        Location fb = center.clone().add(15, 0, 0);
        int fy = center.getWorld().getHighestBlockYAt(fb.getBlockX(), fb.getBlockZ()) + 1;
        fb.setY(fy);
        return fb;
    }

    public static List<LivingEntity> spawnMonsters(Location center, int round,
                                                   Collection<Player> participants, int count) {
        List<LivingEntity> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(spawnMonster(center, round, participants));
        }
        return list;
    }


    public static LivingEntity spawnMonster(Location center, int round, Collection<Player> participants) {
        Location loc = findSafeSpawnNear(center, participants, 12, 24);
        EntityType type = ALLOWED.get(random.nextInt(ALLOWED.size()));
        LivingEntity mob = (LivingEntity) loc.getWorld().spawnEntity(loc, type);

        // 태그
        mob.getPersistentDataContainer().set(WaveTags.WAVE_MOB, PersistentDataType.BYTE, (byte)1);
        mob.getPersistentDataContainer().set(WaveTags.WAVE_ROUND, PersistentDataType.INTEGER, round);

        // 드랍 확률 0
        if (mob.getEquipment() != null) {
            mob.getEquipment().setItemInMainHandDropChance(0f);
            mob.getEquipment().setItemInOffHandDropChance(0f);
            mob.getEquipment().setHelmetDropChance(0f);
            mob.getEquipment().setChestplateDropChance(0f);
            mob.getEquipment().setLeggingsDropChance(0f);
            mob.getEquipment().setBootsDropChance(0f);
        }

        boolean elite = random.nextInt(10) == 0;
        if (elite) {
            if (mob.getAttribute(Attribute.MAX_HEALTH) != null) {
                var a = mob.getAttribute(Attribute.MAX_HEALTH);
                Objects.requireNonNull(a).setBaseValue(a.getBaseValue() * 2.0);
                mob.setHealth(a.getBaseValue());
            }
            if (mob.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                var a = mob.getAttribute(Attribute.ATTACK_DAMAGE);
                Objects.requireNonNull(a).setBaseValue(a.getBaseValue() * 2.0);
            }
        }

        // 이름(Component) + 글로우
        Component name = Component.empty()
                .append(elite ? Component.text("[ELITE] ", NamedTextColor.RED)
                        : Component.text("[WAVE] ", NamedTextColor.GOLD))
                .append(Component.text("R" + round + " ", NamedTextColor.WHITE))
                .append(Component.text(type.name(), NamedTextColor.GRAY));
        mob.customName(name);
        mob.setCustomNameVisible(true);
        mob.setGlowing(true);

        // 스폰 이펙트(옵션)
        loc.getWorld().spawnParticle(Particle.CLOUD, loc, 12, 0.3, 0.2, 0.3, 0.01);

        return mob;
    }
}
