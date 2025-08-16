package kwangdong.pingplugin.listeners;

import kwangdong.pingplugin.manager.WaveManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class NightWatcher implements Runnable {

    private final WaveManager waveManager;

    // 밤(13000~22999) 진입 감지용
    private final Map<UUID, Boolean> wasNight = new HashMap<>();
    // 월드별 "그 날" 주사위를 이미 굴렸는지 추적 (하루 = 24000틱)
    private final Map<UUID, Long> lastRolledDay = new HashMap<>();
    private final Random rng = new Random();

    // 독립 시행 확률 18%
    private static final int NATURAL_TRIGGER_PERCENT = 18;

    public NightWatcher(WaveManager waveManager) {
        this.waveManager = waveManager;
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds()) {
            // ✅ 오버월드만 감지
            if (world.getEnvironment() != Environment.NORMAL) continue;

            long t = world.getTime();               // 0 ~ 23999 (시각)
            long day = world.getFullTime() / 24000; // 누적 일자
            boolean nowNight = (t >= 13000 && t < 23000);
            boolean prevNight = wasNight.getOrDefault(world.getUID(), false);

            // "낮->밤"으로 막 진입한 순간만 처리
            if (!prevNight && nowNight) {
                Long last = lastRolledDay.get(world.getUID());
                if (last == null || last != day) {
                    // 오늘 아직 주사위 안 굴렸으면 굴림
                    lastRolledDay.put(world.getUID(), day);

                    int roll = rng.nextInt(100); // 0~99
                    waveManager.getPlugin().getLogger().info(
                            String.format("[NightWatcher] world=%s day=%d night-start roll=%d%%",
                                    world.getName(), day, roll));

                    if (roll < NATURAL_TRIGGER_PERCENT) {
                        // 이 월드의 온라인 플레이어만 전달
                        waveManager.getPlugin().getLogger().info(
                                String.format("[NightWatcher] Trigger accepted (%d%%). Starting preparation.", NATURAL_TRIGGER_PERCENT));
                        waveManager.tryStartWaveNight(world.getPlayers());
                    } else {
                        waveManager.getPlugin().getLogger().info(
                                String.format("[NightWatcher] Trigger skipped (%d%%). Try tomorrow.", NATURAL_TRIGGER_PERCENT));
                    }
                } else {
                    // 같은 날 밤 재진입(아마 시간 조작) 시에도 재굴림 안 함
                    waveManager.getPlugin().getLogger().info(
                            String.format("[NightWatcher] Night re-enter detected world=%s day=%d (already rolled)", world.getName(), day));
                }
            }
            wasNight.put(world.getUID(), nowNight);
        }
    }
}
