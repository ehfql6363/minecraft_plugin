package kwangdong.pingplugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import kwangdong.pingplugin.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import kwangdong.pingplugin.commands.DeathCommand;
import kwangdong.pingplugin.commands.PingCommand;
import kwangdong.pingplugin.commands.WaveCommand;
import kwangdong.pingplugin.tasks.RewardManager;
import kwangdong.pingplugin.tasks.WaveManager;
import kwangdong.pingplugin.util.WaveTags;

public class PingPlugin extends JavaPlugin {

	private static PingPlugin instance;
	private BukkitTask autoWaveTask;
	private WaveManager waveManager;
	// 사망 좌표 저장소 (플레이어 UUID → 좌표 문자열)
	public static Map<UUID, LocationInfo> deathMap = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this; // ✅ 인스턴스 설정

        RewardManager.init(this);   // ✅ PDC 키 초기화 (보상 NPE 방지)
        WaveTags.init(this);        // ✅ 웨이브 몹 태그 키 초기화

        this.waveManager = new WaveManager(this);

        // 명령어
        getCommand("ping").setExecutor(new PingCommand());
        getCommand("death").setExecutor(new DeathCommand());
        getCommand("wave").setExecutor(new WaveCommand(waveManager));

        // 리스너
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new WaveListener(waveManager), this);
        getServer().getPluginManager().registerEvents(new ScrollUseListener(), this);
        getServer().getPluginManager().registerEvents(new WaveSleepListener(waveManager), this);
        getServer().getPluginManager().registerEvents(new WaveMobDropListener(), this);

        // ✅ 자연 야간 감지 타이머 (1초마다, 낮→밤 전이 시 1% 시도)
        autoWaveTask = getServer().getScheduler()
                .runTaskTimer(this, new NightWatcher(waveManager), 20L, 20L);

        getLogger().info("PingPlugin 활성화됨");
    }

    @Override
    public void onDisable() {
        // ✅ 타이머 해제
        if (autoWaveTask != null) {
            autoWaveTask.cancel();
            autoWaveTask = null;
        }
        // ✅ 진행 중 웨이브 강제 정리 (재시작 버그 방지)
        if (waveManager != null && waveManager.isWaveActive()) {
            waveManager.stopWave(); // 또는 skipWave() 원하는 정책대로
        }

        instance = null; // ✅ 정리
        getLogger().info("PingPlugin 비활성화됨");
    }

	public static PingPlugin getInstance() {
		return instance;
	}

	public record LocationInfo(int x, int y, int z, String world) {}

}
