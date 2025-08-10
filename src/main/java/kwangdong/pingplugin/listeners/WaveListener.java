package kwangdong.pingplugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.world.TimeSkipEvent;

import kwangdong.pingplugin.manager.WaveManager;

public class WaveListener implements Listener {

	private final WaveManager waveManager;

	public WaveListener(WaveManager waveManager) {
		this.waveManager = waveManager;
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) {
		waveManager.onPlayerDeath(e.getEntity());
		// 죽어도 아이템 드랍/경험치 손실 방지
		e.setKeepInventory(true);
		e.setKeepLevel(true);
		e.setDroppedExp(0);
		e.getDrops().clear();
	}

	/** 침대 수면으로 밤이 스킵될 때 (NIGHT_SKIP) 발생.
	 *  서버 세팅에 따라 밤 진입 탐지 방식을 월드 시간틱 검사로 바꿔도 됨 */
	@EventHandler
	public void onNightSkip(TimeSkipEvent e) {
		if (e.getSkipReason() == TimeSkipEvent.SkipReason.NIGHT_SKIP) {
			waveManager.tryStartWaveNight(Bukkit.getOnlinePlayers());
		}
	}

	/** 만약 수면 없이 자연 야간 진입을 쓰고 싶다면,
	 *  주기적으로 월드 시간 확인하는 스케줄러를 PingPlugin에서 돌려도 됨 (시간 13000~23000 등) */
}
