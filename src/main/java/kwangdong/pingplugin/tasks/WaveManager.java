package kwangdong.pingplugin.tasks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

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
    private BukkitTask waveTask;

    public WaveManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isWaveActive() {
        return isWaveActive;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    /**
     * 밤 시작(자연 진입) 시 1% 확률로 자동 시작 시도
     */
    public void tryStartWaveNight(Collection<? extends Player> onlinePlayers) {
        if (isWaveActive) return;
        if (onlinePlayers == null || onlinePlayers.isEmpty()) return;
        if (random.nextInt(100) != 0) return; // 1%

        List<Player> online = new ArrayList<>(onlinePlayers);
        Player center = online.get(random.nextInt(online.size()));
        startWave(center);
    }

    /**
     * 테스트/명령어로 수동 시작
     */
    public void startWave(Player center) {
        if (center == null) return;

        if (isWaveActive) {
            center.sendMessage(Component.text("이미 웨이브가 진행 중입니다.", NamedTextColor.YELLOW));
            return;
        }

        // 상태 초기화
        isWaveActive = true;
        currentRound = 0;
        waveCenter = center.getLocation().clone(); // 안전하게 복제

        // 50블럭 내 확정 참여자
        participants = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getWorld().equals(center.getWorld())
                        && p.getLocation().distance(center.getLocation()) <= 50)
                .collect(Collectors.toList());

        if (participants.isEmpty()) {
            isWaveActive = false;
            waveCenter = null;
            center.sendMessage(Component.text("반경 50블럭 내 참여자가 없습니다.", NamedTextColor.RED));
            return;
        }

        // 데스카운트/세이브
        deathCounts.clear();
        ghosts.clear();
        for (Player p : participants) {
            deathCounts.put(p.getUniqueId(), 10);
            WavePlayerState.saveState(p); // 인벤/경험치 백업
        }

        participants.forEach(p ->
                p.sendMessage(Component.text("[Wave] 몬스터 웨이브가 시작됩니다!", NamedTextColor.GOLD)));

        startNextRound();
    }

    /**
     * 라운드 안내(타이틀 + 액션바)
     */
    private void announceRound() {
        String title = "§6Round " + currentRound + " / 10";
        String sub = "§7엘리트 확률 10%";
        for (Player p : participants) {
            // 구버전 호환을 위해 문자열 타이틀 사용
            p.sendTitle(title, sub, 10, 40, 10);
            p.sendActionBar(Component.text("라운드 시작!", NamedTextColor.YELLOW));
        }
    }

    /**
     * 다음 라운드 시작(1마리 소환 후 N초 대기)
     */
    private void startNextRound() {
        if (!isWaveActive) return;

        if (currentRound >= 10) {
            finishWave(true);
            return;
        }

        currentRound++;

        // 라운드 안내
        announceRound();

        // 한 마리 스폰 (WaveSpawner가 안전 오프셋/태깅/엘리트 강화/드랍확률 0 처리)
        WaveSpawner.spawnMonster(waveCenter, currentRound, participants);

        // 혹시 남아있을 스케줄러 방지
        if (waveTask != null) {
            waveTask.cancel();
            waveTask = null;
        }

        // 다음 라운드 예약 (10초 후)
        waveTask = Bukkit.getScheduler().runTaskLater(plugin, this::startNextRound, 20L * 10);
    }

    /**
     * 웨이브 중 플레이어 사망 처리(리스너에서 호출)
     */
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

        // 죽어도 아이템/경험치 유지
        WavePlayerState.restoreOnDeath(player);

        // 전원 유령이면 실패
        if (ghosts.size() == participants.size()) {
            finishWave(false);
        }
    }

    /**
     * 웨이브 종료(성공/실패 공통 마무리)
     */
    public void finishWave(boolean success) {
        if (!isWaveActive) return;
        isWaveActive = false;

        // 라운드 예약 취소(자동 재시작 방지)
        if (waveTask != null) {
            waveTask.cancel();
            waveTask = null;
        }

        // ✅ 성공 효과 먼저
        if (success) celebrateSuccess();

        // 상태 복원 및 보상/메시지
        for (Player p : participants) {
            WavePlayerState.restoreState(p); // 복원 먼저

            if (success) {
                RewardManager.giveReward(p); // 전원 지급(원하면 조건 다시 걸 수 있음)
            } else {
                p.sendMessage(Component.text("몬스터 웨이브에 실패했습니다.", NamedTextColor.DARK_RED));
            }
        }

        // 상태 초기화
        participants.clear();
        deathCounts.clear();
        ghosts.clear();
        waveCenter = null;
        currentRound = 0;
    }

    /**
     * 강제 성공
     */
    public void skipWave() {
        finishWave(true);
    }

    /**
     * 강제 중지(실패)
     */
    public void stopWave() {
        finishWave(false);
    }

    private void celebrateSuccess() {
        for (Player p : participants) {
            p.sendMessage(Component.text("몬스터 웨이브 성공!", NamedTextColor.GOLD));
            p.sendMessage(Component.text("보상이 지급되었습니다."));
            p.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            p.playSound(p, Sound.EVENT_RAID_HORN, 0.7f, 1.0f);
            p.spawnParticle(Particle.HEART, p.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.01);
        }
    }
}
