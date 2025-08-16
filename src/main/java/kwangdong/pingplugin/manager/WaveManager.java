package kwangdong.pingplugin.manager;

import kwangdong.pingplugin.tasks.WaveSpawner;
import kwangdong.pingplugin.ui.DeathSidebarManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class WaveManager {

    private final Plugin plugin;
    private final Random random = new Random();

    // 진행/라운드
    private boolean isWaveActive = false;
    private int currentRound = 0;

    // 참가자/상태
    private List<Player> participants = new ArrayList<>();
    private final Map<UUID, Integer> deathCounts = new HashMap<>();
    private final Set<UUID> ghosts = new HashSet<>();
    private final Set<UUID> aliveMobIds = new HashSet<>();
    private final GhostManager ghostManager = new GhostManager();

    // 준비 단계
    private boolean isPreparing = false;
    private BukkitTask prepareTask;
    private int prepareRemainSec;
    private Location prepareCenter;
    private List<Player> prepareParticipants = new ArrayList<>();
    private final RespawnPointManager respawnPoints = new RespawnPointManager();

    private Location waveCenter;

    // UI
    private final DeathSidebarManager deathSidebar = new DeathSidebarManager();

    private static final Set<Integer> ELITE_ROUNDS = Set.of(3, 6, 10);

    // ✅ 1차 패치: 글로벌 제한시간 15분
    private BukkitTask waveTimeoutTask;
    private static final int MAX_WAVE_DURATION_TICKS = 20 * 60 * 15; // 15min

    public WaveManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin getPlugin() { return plugin; }
    public boolean isWaveActive() { return isWaveActive; }
    public boolean isPreparing() { return isPreparing; }
    public int getCurrentRound() { return currentRound; }
    public boolean isGhost(Player p) { return ghosts.contains(p.getUniqueId()); }
    public Location getRespawnPoint(Player p) { return respawnPoints.getWaveRespawn(p); }

    /** 외부에서 현재 웨이브 몹 목록을 조회 (/wave mobs) */
    public List<Entity> getAliveWaveMobs() {
        if (!isWaveActive) return List.of();
        List<Entity> list = new ArrayList<>();
        for (UUID id : aliveMobIds) {
            Entity e = Bukkit.getEntity(id);
            if (e != null && e.isValid()) list.add(e);
        }
        return list;
    }

    // ===== 자연 발동 진입점 =====
    /** 밤 시작 시 호출: (✅ 2차 패치) 여기서는 "확률"을 더 이상 판단하지 않음 (NightWatcher가 18%를 담당) */
    public void tryStartWaveNight(Collection<? extends Player> onlinePlayers) {
        if (isWaveActive || isPreparing) return;
        if (onlinePlayers == null || onlinePlayers.isEmpty()) return;

        List<Player> online = new ArrayList<>(onlinePlayers);
        Player center = online.get(random.nextInt(online.size()));

        plugin.getLogger().info(String.format(
                "[Wave] Natural trigger accepted. prepare center=%s(%s) players=%d",
                center.getName(), center.getWorld().getName(), online.size()));

        beginPreparation(center, 60); // 준비 60초
    }

    // ===== 준비 단계 =====
    private void beginPreparation(Player center, int seconds) {
        if (center == null || isWaveActive || isPreparing) return;

        List<Player> snap = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getWorld().equals(center.getWorld())
                        && p.getLocation().distance(center.getLocation()) <= 50)
                .collect(Collectors.toList());

        if (snap.isEmpty()) {
            center.sendMessage(Component.text("[Wave] 준비 실패: 반경 50블록 내 참가자가 없습니다.", NamedTextColor.YELLOW));
            plugin.getLogger().info("[Wave] Preparation aborted: no participants in 50 blocks.");
            return;
        }

        isPreparing = true;
        prepareRemainSec = Math.max(5, seconds);
        prepareCenter = center.getLocation().clone();
        prepareParticipants = new ArrayList<>(snap);

        plugin.getLogger().info(String.format(
                "[Wave] Preparation started center=(%s,%s,%s) world=%s participants=%s",
                prepareCenter.getBlockX(), prepareCenter.getBlockY(), prepareCenter.getBlockZ(),
                prepareCenter.getWorld().getName(),
                prepareParticipants.stream().map(Player::getName).toList()));

        broadcastTo(prepareParticipants,
                Component.text("[Wave] 몬스터 웨이브의 징조가 느껴집니다...", NamedTextColor.GOLD));
        broadcastTo(prepareParticipants,
                Component.text("1분 뒤 웨이브가 시작됩니다! (반경 50블록 내 현재 인원만 참가)", NamedTextColor.GRAY));

        prepareTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            prepareParticipants.removeIf(p -> !p.isOnline() || !p.getWorld().equals(prepareCenter.getWorld()));
            if (prepareParticipants.isEmpty()) {
                cancelPreparation(Component.text("[Wave] 준비가 취소되었습니다: 참가자가 없습니다.", NamedTextColor.YELLOW));
                return;
            }

            for (Player p : prepareParticipants) {
                p.sendActionBar(Component.text("웨이브 준비 중... " + prepareRemainSec + "초", NamedTextColor.YELLOW));
            }

            if (--prepareRemainSec <= 0) {
                endPreparationAndStart();
            }
        }, 20L, 20L);
    }

    private void cancelPreparation(Component reason) {
        if (!isPreparing) return;
        if (prepareTask != null) {
            prepareTask.cancel();
            prepareTask = null;
        }
        broadcastTo(prepareParticipants, reason);
        plugin.getLogger().info("[Wave] Preparation cancelled: " + reason.toString());

        isPreparing = false;
        prepareParticipants.clear();
        prepareCenter = null;
        prepareRemainSec = 0;
    }

    private void endPreparationAndStart() {
        if (prepareTask != null) {
            prepareTask.cancel();
            prepareTask = null;
        }
        List<Player> locked = new ArrayList<>(prepareParticipants);
        Location center = prepareCenter == null ? locked.getFirst().getLocation() : prepareCenter.clone();

        isPreparing = false;
        prepareParticipants.clear();
        prepareCenter = null;
        prepareRemainSec = 0;

        startWaveWithLockedParticipants(center, locked);
    }

    // ===== 명령어 즉시 시작 =====
    public void startWave(Player center) {
        if (isWaveActive || isPreparing) {
            if (center != null) center.sendMessage(Component.text("이미 웨이브가 진행(또는 준비) 중입니다.", NamedTextColor.YELLOW));
            return;
        }
        if (center.getWorld().getEnvironment() != org.bukkit.World.Environment.NORMAL) {
            center.sendMessage("§c오버월드에서만 웨이브를 시작할 수 있습니다.");
            return;
        }

        List<Player> snap = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getWorld().equals(center.getWorld())
                        && p.getLocation().distance(center.getLocation()) <= 50)
                .collect(Collectors.toList());

        if (snap.isEmpty()) {
            if (center != null) center.sendMessage(Component.text("반경 50블록 내 참가자가 없습니다.", NamedTextColor.RED));
            return;
        }
        startWaveWithLockedParticipants(center.getLocation().clone(), snap);
    }

    private void startWaveWithLockedParticipants(Location center, List<Player> lockedParticipants) {
        isWaveActive = true;
        currentRound = 0;
        waveCenter = center;

        participants = new ArrayList<>(lockedParticipants);
        deathCounts.clear();
        ghosts.clear();
        aliveMobIds.clear();

        for (Player p : participants) {
            deathCounts.put(p.getUniqueId(), 10);
            respawnPoints.rememberWaveRespawn(p, waveCenter);
        }

        // 사이드바 표시
        deathSidebar.start(participants, deathCounts);

        plugin.getLogger().info(String.format(
                "[Wave] Started center=(%s,%s,%s) world=%s participants=%s",
                waveCenter.getBlockX(), waveCenter.getBlockY(), waveCenter.getBlockZ(),
                waveCenter.getWorld().getName(),
                participants.stream().map(Player::getName).toList()));

        broadcastTo(participants, Component.text("[Wave] 몬스터 웨이브가 시작됩니다!", NamedTextColor.GOLD));

        // ✅ 1차 패치: 글로벌 제한시간 15분 타이머
        waveTimeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info("[Wave] Failed by TIMEOUT (15min).");
            finishWave(false);
        }, MAX_WAVE_DURATION_TICKS);

        startNextRound();
    }

    private void announceRound() {
        Component title = Component.text("Round " + currentRound + " / 10", NamedTextColor.GOLD);
        Component sub = Component.text("일반 라운드", NamedTextColor.GRAY);
        if (ELITE_ROUNDS.contains(currentRound))
            sub = Component.text("엘리트 라운드", NamedTextColor.DARK_RED);

        Title t = Title.title(title, sub,
                Times.times(Duration.ofMillis(200), Duration.ofMillis(1200), Duration.ofMillis(200)));

        for (Player p : participants) {
            p.showTitle(t);
            p.sendActionBar(Component.text("라운드 시작!", NamedTextColor.YELLOW));
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.2f);
        }

        plugin.getLogger().info(String.format("[Wave] Round %d announced (%s)",
                currentRound, ELITE_ROUNDS.contains(currentRound) ? "ELITE" : "NORMAL"));
    }

    private void startNextRound() {
        if (!isWaveActive) return;
        if (currentRound >= 10) {
            finishWave(true);
            return;
        }

        currentRound++;
        announceRound();

        aliveMobIds.clear();

        int count = getMobsPerRound(currentRound);
        List<LivingEntity> mobs = WaveSpawner.spawnMonsters(waveCenter, currentRound, participants, count);
        for (LivingEntity m : mobs) aliveMobIds.add(m.getUniqueId());

        plugin.getLogger().info(String.format(
                "[Wave] Round %d spawned count=%d (alive=%d)", currentRound, mobs.size(), aliveMobIds.size()));

        deathSidebar.updateAll(deathCounts);
    }

    private int getMobsPerRound(int round) {
        if (ELITE_ROUNDS.contains(round)) return 1;
        long soloCountSoFar = ELITE_ROUNDS.stream().filter(r -> r <= round).count();
        int nonSoloSoFar = round - (int) soloCountSoFar;
        return Math.max(1, nonSoloSoFar);
    }

    public void onPlayerDeath(Player player) {
        if (!isWaveActive || player == null) return;
        UUID id = player.getUniqueId();
        if (!deathCounts.containsKey(id)) return;

        int left = deathCounts.get(id) - 1;
        deathCounts.put(id, left);

        plugin.getLogger().info(String.format("[Wave] Death player=%s left=%d", player.getName(), left));

        if (left <= 0) {
            ghosts.add(id);
            player.sendMessage(Component.text("당신은 유령 상태가 되었습니다. 웨이브 종료까지 관전만 가능합니다.", NamedTextColor.GRAY));
            plugin.getServer().getScheduler().runTask(plugin, () -> ghostManager.setGhost(player));
        } else {
            player.sendMessage(Component.text("남은 데스카운트: " + left, NamedTextColor.GRAY));
        }

        deathSidebar.update(player, Math.max(left, 0));

        if (ghosts.size() == participants.size()) {
            Bukkit.getScheduler().runTask(plugin, () -> finishWave(false));
        }
    }

    /** 웨이브 몹 사망 보고 */
    public void onWaveMobKilled(UUID mobId) {
        if (!isWaveActive) return;
        if (!aliveMobIds.remove(mobId)) return;

        plugin.getLogger().info(String.format(
                "[Wave] Mob killed (remaining=%d)", aliveMobIds.size()));

        if (!aliveMobIds.isEmpty()) {
            int leftMobs = aliveMobIds.size();
            for (Player p : participants) {
                p.sendActionBar(Component.text("남은 적: " + leftMobs, NamedTextColor.YELLOW));
            }
            return;
        }

        if (currentRound >= 10) {
            Bukkit.getScheduler().runTask(plugin, () -> finishWave(true));
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, this::startNextRound, 20L * 2);
        }
    }

    private void celebrateSuccess() {
        Title title = Title.title(
                Component.text("웨이브 성공!", NamedTextColor.GREEN),
                Component.text("보상이 지급되었습니다.", NamedTextColor.WHITE),
                Times.times(Duration.ofMillis(200), Duration.ofMillis(1400), Duration.ofMillis(300))
        );
        for (Player p : participants) {
            p.showTitle(title);
            p.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            p.playSound(p, Sound.EVENT_RAID_HORN, 0.7f, 1.0f);
            p.spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.01);
        }
        plugin.getLogger().info("[Wave] Success! (all 10 rounds cleared)");
    }

    public void finishWave(boolean success) {
        // 준비 중이라면 준비 취소로
        if (isPreparing && !isWaveActive) {
            cancelPreparation(Component.text("[Wave] 준비가 취소되었습니다.", NamedTextColor.YELLOW));
            return;
        }

        if (!isWaveActive) return;
        isWaveActive = false;

        if (waveTimeoutTask != null) {
            waveTimeoutTask.cancel();
            waveTimeoutTask = null;
        }

        despawnAllWaveMobs();

        if (success) celebrateSuccess();

        for (Player p : participants) ghostManager.restore(p);
        deathSidebar.stop();
        respawnPoints.clearAll();

        for (Player p : participants) {
            if (success) {
                RewardManager.giveReward(p);
            } else {
                p.sendMessage(Component.text("몬스터 웨이브에 실패했습니다.", NamedTextColor.DARK_RED));
                p.playSound(p, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }

        plugin.getLogger().info("[Wave] Finished success=" + success
                + " participants=" + participants.stream().map(Player::getName).toList());

        ghostManager.clearAll();
        participants.clear();
        deathCounts.clear();
        ghosts.clear();
        aliveMobIds.clear();
        waveCenter = null;
        currentRound = 0;
    }

    public void skipWave() {
        if (isPreparing && !isWaveActive) {
            cancelPreparation(Component.text("[Wave] 준비가 관리자로 인해 취소되었습니다.", NamedTextColor.YELLOW));
            return;
        }
        plugin.getLogger().info("[Wave] Admin SKIP (force success).");
        finishWave(true);
    }

    public void stopWave() {
        if (isPreparing && !isWaveActive) {
            cancelPreparation(Component.text("[Wave] 준비가 관리자로 인해 취소되었습니다.", NamedTextColor.YELLOW));
            return;
        }
        plugin.getLogger().info("[Wave] Admin STOP (force fail).");
        finishWave(false);
    }

    public Integer getDeathLeft(Player p) { return deathCounts.get(p.getUniqueId()); }
    public boolean isParticipant(Player player) {
        if (player == null) return false;
        return participants.stream().anyMatch(p -> p.getUniqueId().equals(player.getUniqueId()));
    }

    private void broadcastTo(Collection<Player> list, Component msg) {
        for (Player p : list) p.sendMessage(msg);
    }

    private void despawnAllWaveMobs() {
        for (UUID id : new HashSet<>(aliveMobIds)) {
            Entity e = Bukkit.getEntity(id);
            if (e != null && e.isValid()) e.remove();
        }
        plugin.getLogger().info("[Wave] Despawned all wave mobs count=" + aliveMobIds.size());
        aliveMobIds.clear();
    }
}
