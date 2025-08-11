package kwangdong.pingplugin.listeners;

import kwangdong.pingplugin.manager.WaveManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

public class WaveSleepListener implements Listener {

	private final WaveManager waveManager;

	public WaveSleepListener(WaveManager waveManager) {
		this.waveManager = waveManager;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBedEnter(PlayerBedEnterEvent e) {
		Player p = e.getPlayer();

		// ✅ 준비 카운트다운 중: 수면 완전 금지
		if (waveManager.isPreparing()) {
			e.setCancelled(true);
			p.sendMessage(Component.text("웨이브 준비 중에는 잠을 잘 수 없습니다.", NamedTextColor.YELLOW));
			return;
		}

		// ✅ 웨이브 진행 중: 수면 금지 + 리스폰 지정만 허용
		if (waveManager.isWaveActive()) {
			e.setCancelled(true);

			// 침대의 '발(FOOT)' 위치를 리스폰으로 지정
			Block bedBlock = e.getBed();
			if (bedBlock.getBlockData() instanceof Bed bed) {
				Block foot = (bed.getPart() == Bed.Part.HEAD)
					? bedBlock.getRelative(bed.getFacing().getOppositeFace())
					: bedBlock;

				Location respawn = foot.getLocation().add(0.5, 0.1, 0.5);
				try {
					// Paper 1.20+ 권장 API
					p.setRespawnLocation(respawn);
				} catch (NoSuchMethodError ignored) {
					// (구버전 대응이 필요하면 여기에 대체 로직)
				}
			}

			p.sendMessage(Component.text("웨이브 중에는 잠을 잘 수 없습니다. 리스폰 지점만 지정되었습니다.", NamedTextColor.GRAY));
		}
	}
}
