package kwangdong.pingplugin.tasks;

import kwangdong.pingplugin.manager.WaveManager;
import kwangdong.pingplugin.util.WaveTags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class WaveSpawner {

	private WaveSpawner() {}

    private static final Random random = new Random();

    private static final Set<EntityType> BLOCKED = Set.of(
            EntityType.WARDEN, EntityType.ENDER_DRAGON, EntityType.WITHER,
            EntityType.SLIME, EntityType.MAGMA_CUBE, EntityType.VEX, EntityType.GUARDIAN,
	        EntityType.GIANT, EntityType.PIGLIN_BRUTE, EntityType.PIGLIN
    );

	private enum Tier { T1, T2, T3, T4 }

	// 안전하게 “바깥 필드에서 잘 싸우는” 몹 위주로 구성
	private static final Map<Tier, List<EntityType>> POOL = Map.of(
		Tier.T1, List.of(
			EntityType.ZOMBIE,
			EntityType.SKELETON,
			EntityType.SPIDER,
			EntityType.HUSK,
			EntityType.STRAY,
			EntityType.PHANTOM
		),
		Tier.T2, List.of(
			EntityType.PILLAGER,
			EntityType.WITCH,
			EntityType.WITHER_SKELETON,
			EntityType.BREEZE,
			EntityType.BOGGED
		),
		Tier.T3, List.of(
			EntityType.VINDICATOR,
			EntityType.EVOKER,
			EntityType.ILLUSIONER
		),
		Tier.T4, List.of(
			EntityType.RAVAGER, // R10 보스격
			EntityType.ELDER_GUARDIAN
		)
	);

	// 라운드 → 티어 가중치
	private static Map<Tier, Integer> weightsForRound(int round) {
		if (round <= 3) return Map.of(Tier.T1, 80, Tier.T2, 20);
		if (round <= 6) return Map.of(Tier.T2, 70, Tier.T1, 20, Tier.T3, 10);
		if (round <= 9) return Map.of(Tier.T3, 70, Tier.T2, 20, Tier.T1, 10);
		return Map.of(Tier.T4, 100); // R10
	}

	private static boolean isEliteRound(int round) {
		return round == 3 || round == 6 || round == 10;
	}

	/**
	 * 웨이브 몬스터 스폰 (count 마리)
	 * @return 스폰된 개체 리스트
	 */
	public static List<LivingEntity> spawnMonsters(Location center, int round, List<Player> participants, int count) {
		if (center == null || center.getWorld() == null || count <= 0) return List.of();
		World world = center.getWorld();
		ThreadLocalRandom rnd = ThreadLocalRandom.current();

		List<Player> validTargets = participants == null
			? List.of()
			: participants.stream().filter(Objects::nonNull).collect(Collectors.toList());

		List<LivingEntity> out = new ArrayList<>();

		// ✅ 엘리트 라운드면 스폰 수를 '무조건 1'로 강제하고, 엘리트 1마리 보장
		final boolean eliteRound = isEliteRound(round);
		final int spawnCount = eliteRound ? 1 : count;
		int elitesRemaining = eliteRound ? 1 : 0;

		for (int i = 0; i < spawnCount; i++) {
			EntityType type = pickEntityTypeForRound(world, round, rnd);
			if (type == null) continue;

			Location spawn = pickSpawnLocationAround(center, rnd);

			// ✅ 엘리트 여부 결정: 엘리트 라운드가 아니면 '무조건 일반'
			final boolean makeElite = (elitesRemaining > 0);

			LivingEntity mob = world.spawn(
				spawn,
				type.getEntityClass().asSubclass(LivingEntity.class),
				entity -> {
					tagWaveMob(entity);

					String label = displayName(type);
					if (makeElite) {
						boostElite(entity);
						entity.customName(Component.text("[Wave] 엘리트 " + label, NamedTextColor.RED));
					} else {
						entity.customName(Component.text("[Wave] " + label, NamedTextColor.DARK_PURPLE));
					}
					entity.setCustomNameVisible(true);
					entity.setGlowing(true);

					if (!validTargets.isEmpty() && entity instanceof Monster monster) {
						Player target = validTargets.get(rnd.nextInt(validTargets.size()));
						monster.setTarget(target);
					}
				}
			);

			if (makeElite) elitesRemaining--;
			out.add(mob);
		}
		return out;
	}


	/** 라운드→가중치에서 티어 선택 → 해당 티어 풀에서 엔티티 타입 선택(불가시 점진적 폴백) */
	private static EntityType pickEntityTypeForRound(World world, int round, ThreadLocalRandom rnd) {
		Map<Tier, Integer> weights = weightsForRound(round);

		// 티어 가중치로 하나 뽑기
		Tier wanted = pickWeighted(weights, rnd);
		if (wanted == null) return null;

		// 선택된 티어에서 가능한 타입 시도 → 없으면 이웃 티어 쪽으로 폴백
		List<Tier> order = fallbackOrderFor(wanted);
		for (Tier tier : order) {
			List<EntityType> pool = POOL.getOrDefault(tier, List.of()).stream()
				.filter(t -> t.isSpawnable() && t.isAlive())
				.filter(t -> !BLOCKED.contains(t))
				.filter(t -> t.getEntityClass() != null && LivingEntity.class.isAssignableFrom(t.getEntityClass()))
				.toList();
			if (pool.isEmpty()) continue;
			return pool.get(rnd.nextInt(pool.size()));
		}
		return null;
	}

	private static List<Tier> fallbackOrderFor(Tier t) {
		return switch (t) {
			case T1 -> List.of(Tier.T1, Tier.T2, Tier.T3);
			case T2 -> List.of(Tier.T2, Tier.T1, Tier.T3);
			case T3 -> List.of(Tier.T3, Tier.T2, Tier.T1);
			case T4 -> List.of(Tier.T4, Tier.T3, Tier.T2);
		};
	}

	private static <T> T pickWeighted(Map<T, Integer> weights, ThreadLocalRandom rnd) {
		int sum = 0;
		for (int w : weights.values()) sum += Math.max(0, w);
		if (sum <= 0) return null;
		int r = rnd.nextInt(sum);
		int acc = 0;
		for (Map.Entry<T, Integer> e : weights.entrySet()) {
			acc += Math.max(0, e.getValue());
			if (r < acc) return e.getKey();
		}
		return null;
	}

	/** 중심에서 랜덤 오프셋 (지상 안전 Y 보정) */
	private static Location pickSpawnLocationAround(Location center, ThreadLocalRandom rnd) {
		Location base = center.clone();
		double radius = 8 + rnd.nextInt(7); // 8~14
		double angle = rnd.nextDouble(0, Math.PI * 2);
		double dx = Math.cos(angle) * radius;
		double dz = Math.sin(angle) * radius;

		Location loc = new Location(base.getWorld(), base.getX() + dx, base.getY(), base.getZ() + dz, rnd.nextFloat(360f), 0);
		int y = Math.max(loc.getBlockY(), loc.getWorld().getHighestBlockYAt(loc)) + 1;
		loc.setY(y);
		return loc;
	}

	/** 웨이브 태그 부여 */
	private static void tagWaveMob(LivingEntity e) {
		e.getPersistentDataContainer().set(WaveTags.WAVE_MOB, PersistentDataType.BYTE, (byte) 1);

		if (e instanceof Mob mob) {
			mob.setCanPickupItems(false); // 전리품 줍기 방지(있으면)

			var eq = mob.getEquipment();
			if (eq != null) {
				try {
					eq.setItemInMainHandDropChance(0f);
					eq.setItemInOffHandDropChance(0f);
					eq.setHelmetDropChance(0f);
					eq.setChestplateDropChance(0f);
					eq.setLeggingsDropChance(0f);
					eq.setBootsDropChance(0f);
				} catch (Throwable ignored) {}
			}
		}
	}

	/** 엘리트 강화 (HP/공격력 2배) */
	private static void boostElite(LivingEntity e) {
		try {
			var maxHp = e.getAttribute(Attribute.MAX_HEALTH);
			if (maxHp != null) {
				maxHp.setBaseValue(maxHp.getBaseValue() * 2.0);
				e.setHealth(Math.min(e.getHealth() * 2.0, maxHp.getBaseValue()));
			}
		} catch (Throwable ignored) {}

		try {
			var dmg = e.getAttribute(Attribute.ATTACK_DAMAGE);
			if (dmg != null) dmg.setBaseValue(dmg.getBaseValue() * 2.0);
		} catch (Throwable ignored) {}
	}

	// 한국어 표기
	private static String displayName(EntityType t) {
		return switch (t) {
			case ZOMBIE -> "좀비";
			case SKELETON -> "스켈레톤";
			case SPIDER -> "거미";
			case HUSK -> "허스크";
			case STRAY -> "스트레이";
			case PHANTOM -> "팬텀";

			case PILLAGER -> "약탈자";
			case WITCH -> "마녀";
			case WITHER_SKELETON -> "위더 스켈레톤";
			case BREEZE -> "브리즈";
			case BOGGED -> "늪지 스켈레톤";

			case VINDICATOR -> "사냥꾼";
			case EVOKER -> "소환사";
			case ILLUSIONER -> "환술사";

			case RAVAGER -> "파괴수";
			case ELDER_GUARDIAN -> "엘더 가디언";

			default -> t.name();
		};
	}

}
