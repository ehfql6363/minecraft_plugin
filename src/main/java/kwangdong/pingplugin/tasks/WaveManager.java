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
    private final Set<UUID> aliveMobIds = new HashSet<>(); // ✅ 현재 라운드 살아있는 웨이브 몹

    private Location waveCenter;

    // 라운드당 몬스터 수 (테스트는 1, 나중엔 라운드/난이도에 따라 늘리면 됨)
    private int getMobsPerRound(int round) {
        // 예) round 1~10: 1,2,2,3,3,4,4,5,5,6 처럼 점진 증가하고 싶다면 로직 바꾸면 됨
        return 1; // 지금은 테스트니까 1
    }

    public WaveManager(Plugin plugin) { this.plugin = plugin; }

    public boolean isWaveActive() { return isWaveActive; }
    public int getCurrentRound() { return currentRound; }

    public void tryStartWaveNight(Collection<? extends Player> onlinePlayers) {
        if (isWaveActive) return;
        if (onlinePlayers == null || onlinePlayers.isEmpty()) return;
        if (random.nextInt(100) != 0) return;

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
        aliveMobIds.clear();

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
        Title t = Title.title(title, sub,
                Times.times(Duration.ofMillis(200), Duration.ofMillis(1200), Duration.ofMillis(200)));
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

        aliveMobIds.clear(); // ✅ 라운드 시작 시 초기화

        int count = getMobsPerRound(currentRound);
        List<LivingEntity> mobs = WaveSpawner.spawnMonsters(waveCenter, currentRound, participants, count);
        for (LivingEntity m : mobs) {
            aliveMobIds.add(m.getUniqueId());
        }
        // ⛔ 자동 진행 타이머 없음 — 전부 처치되어 aliveMobIds가 비어야 다음 라운드로 이동
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

    /** 웨이브 몹이 죽을 때마다 호출됨 */
    public void onWaveMobKilled(UUID mobId) {
        if (!isWaveActive) return;
        if (!aliveMobIds.remove(mobId)) return; // 현재 라운드 몹이 아니면 무시

        // 아직 라운드 몹이 남아있으면 대기
        if (!aliveMobIds.isEmpty()) {
            // 남은 수 안내(선택)
            int left = aliveMobIds.size();
            for (Player p : participants) {
                p.sendActionBar(Component.text("남은 적: " + left, NamedTextColor.YELLOW));
            }
            return;
        }

        // 이 라운드 몬스터 전부 처치 → 다음 라운드
        if (currentRound >= 10) {
            finishWave(true);
            return;
        }
        // 연출 텀
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
        aliveMobIds.clear();
        waveCenter = null;
        currentRound = 0;
    }

    public void skipWave() { finishWave(true); }
    public void stopWave() { finishWave(false); }
}
