package kwangdong.pingplugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import kwangdong.pingplugin.util.WaveTags;

public class WaveMobDropListener implements Listener {

	@EventHandler
	public void onWaveMobDeath(EntityDeathEvent e) {
		if (e.getEntity().getPersistentDataContainer().has(WaveTags.WAVE_MOB, PersistentDataType.BYTE)) {
			e.getDrops().clear();     // ✅ 아이템 드랍 제거
			e.setDroppedExp(0);       // ✅ 경험치 드랍 제거
		}
	}
}
