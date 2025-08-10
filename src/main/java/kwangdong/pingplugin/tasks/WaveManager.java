package kwangdong.pingplugin.tasks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class WaveManager {

    private final Plugin plugin;
    private final Random random = new Random();

    private boolean isWaveActive = false;
    private int currentRound = 0;
    private List<Player> participants = new ArrayList<>();
    private final Map<UUID, Integer> deathCounts = new HashMap<>();
    private final Set<UUID> ghosts = new HashSet<>();
    private Location waveCenter;
    private UUID currentMobId = null;

    public WaveManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isWaveActive() { return isWaveActive; }
    public int getCurrentRound() { return currentRound; }

    public void tryStartWaveNight(Collection<? extends Player> onlinePlayers) {
        if (isWaveActive) return;
        if (onlinePlayers == null || onlinePlayers.isEmpty()) return;
        if (random.nextInt(100) != 0) return; // 1%

        List<Player> online = new ArrayList<>(onlinePlayers);
        Player center = online.get(random.nextInt(online.size()));
        startWave(center);
    }

    public void startWave(Player center) {
        if (center == null) return;
        if (isWaveActive) {
            center.sendMessage(Component.text("이미 웨이브가 진행 중입니다.", NamedTextColor.YELLOW));
            return;
        }

        isWaveActive = true;
        currentRound = 0;
        waveCenter = center.getLocation().clone();

        participants = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getWorld().equals(center.getWorld())
                        && p.getLocation().distance(center.getLocation()) <= 50)
                .collect(Collectors.toList());

        if (participants.isEmpty()) {
            isWaveActive = false;
            waveCenter = null;
            center.sendMessage(Component.text("반경 50블록 내 참여자가 없습니다.", NamedTextColor.RED));
            return;
        }

        deathCounts.clear();
        ghosts.clear();
        for (Player p : participants) {
            deathCounts.put(p.getUniqueId(), 10);
            WavePlayerState.saveState(p);
        }

        Component startMsg = Component.text("[Wave] 몬스터 웨이브가 시작됩니다!", NamedTextColor.GOLD);
        participants.forEach(p -> p.sendMessage(startMsg));

        startNextRound();
    }

    private void announceRound() {
        Component title = Component.text("Round " + currentRound + " / 10", NamedTextColor.GOLD);
        Component sub   = Component.text("엘리트 확률 10%", NamedTextColor.GRAY);
        Title t = Title.title(title, sub, Times.times(
                Duration.ofMillis(200), Duration.ofMillis(1200), Duration.ofMillis(200)));
        for (Player p : participants) {
            p.showTitle(t);
            p.sendActionBar(Component.text("라운드 시작!", NamedTextColor.YELLOW));
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.2f);
        }
    }

    private void startNextRound() {
        if (!isWaveActive) return;
        if (currentRound >= 10) { finishWave(true); return; }

        currentRound++;
        announceRound();
        LivingEntity mob = WaveSpawner.spawnMonster(waveCenter, currentRound, participants);
        currentMobId = mob.getUniqueId();
    }

    public void onPlayerDeath(Player player) {
        if (!isWaveActive || player == null) return;
        UUID id = player.getUniqueId();
        if (!deathCounts.containsKey(id)) return;

        int left = deathCounts.get(id) - 1;
        deathCounts.put(id, left);

        if (left <= 0) {
            ghosts.add(id);
            player.sendMessage(Component.text("당신은 유령 상태가 되었습니다. 웨이브 종료까지 관전만 가능합니다.", NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("남은 데스카운트: " + left, NamedTextColor.GRAY));
        }

        WavePlayerState.restoreOnDeath(player);

        if (ghosts.size() == participants.size()) {
            finishWave(false);
        }
    }

    public void onWaveMobKilled(UUID mobId) {
        if (!isWaveActive) return;
        if (currentMobId == null || !currentMobId.equals(mobId)) return;

        if (currentRound >= 10) {
            finishWave(true);
            return;
        }
        // 다음 라운드로 부드럽게
        Bukkit.getScheduler().runTaskLater(plugin, this::startNextRound, 20L * 2);
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
    }

    public void finishWave(boolean success) {
        if (!isWaveActive) return;
        isWaveActive = false;

        if (success) celebrateSuccess();

        for (Player p : participants) {
            WavePlayerState.restoreState(p);
            if (success) {
                RewardManager.giveReward(p);
            } else {
                p.sendMessage(Component.text("몬스터 웨이브에 실패했습니다.", NamedTextColor.DARK_RED));
                p.playSound(p, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }

        participants.clear();
        deathCounts.clear();
        ghosts.clear();
        waveCenter = null;
        currentRound = 0;
        currentMobId = null;
    }

    public void skipWave() { finishWave(true); }
    public void stopWave() { finishWave(false); }
}
