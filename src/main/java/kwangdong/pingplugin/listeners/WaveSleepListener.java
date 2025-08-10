package kwangdong.pingplugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import kwangdong.pingplugin.manager.WaveManager;

public class WaveSleepListener implements Listener {

	private final WaveManager waveManager;

	public WaveSleepListener(WaveManager waveManager) {
		this.waveManager = waveManager;
	}

	@EventHandler
	public void onBedEnter(PlayerBedEnterEvent e) {
		if (!waveManager.isWaveActive()) return;

		// 웨이브 중에는 잠들 수 없게 막고, 리스폰은 설정되도록 처리
		// (이벤트를 취소하면 기본적으로 스폰 설정도 막히므로 직접 지정)
		e.setCancelled(true);
		e.getPlayer().setRespawnLocation(e.getBed().getLocation(), true);
		e.getPlayer().sendMessage(Component.text("웨이브 진행 중엔 잠들 수 없지만 리스폰 포인트는 설정되었습니다.", NamedTextColor.BLUE));
	}
}
