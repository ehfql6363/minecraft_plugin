package kwangdong.pingplugin.tasks;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class WaveBarManager {

	private BossBar waveBar;

	public void createWaveBar(int totalMobs) {
		waveBar = Bukkit.createBossBar(
			"웨이브 진행도: 0 / " + totalMobs,
			BarColor.PURPLE,
			BarStyle.SOLID
		);
		waveBar.setProgress(0.0);
		for (Player p : Bukkit.getOnlinePlayers()) {
			waveBar.addPlayer(p);
		}
	}

	public void createWaveBar(int totalMobs, Collection<Player> players) {
		// 이전 보스바 클리어
		removeWaveBar();

		waveBar = Bukkit.createBossBar(
			"웨이브 진행도: 0 / " + totalMobs,
			BarColor.PURPLE,
			BarStyle.SOLID
		);
		waveBar.setProgress(0.0);
		for (Player p : players) {
			waveBar.addPlayer(p);
		}
	}

	public void updateWaveBar(int killed, int total) {
		if (waveBar == null) return;
		double progress = Math.max(0.0, Math.min(1.0, (double) killed / total));
		waveBar.setProgress(progress);
		waveBar.setTitle("웨이브 진행도: " + killed + " / " + total);
	}

	public void removeWaveBar() {
		if (waveBar != null) {
			waveBar.removeAll();
			waveBar = null;
		}
	}
}
