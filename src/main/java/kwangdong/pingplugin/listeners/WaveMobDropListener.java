package kwangdong.pingplugin.listeners;

import kwangdong.pingplugin.manager.WaveManager;
import kwangdong.pingplugin.util.WaveTags;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

public class WaveMobDropListener implements Listener {

    private final WaveManager waveManager;

    public WaveMobDropListener(WaveManager waveManager) {
        this.waveManager = waveManager;
    }

    @EventHandler
    public void onWaveMobDeath(EntityDeathEvent e) {
        var pdc = e.getEntity().getPersistentDataContainer();
        if (!pdc.has(WaveTags.WAVE_MOB, PersistentDataType.BYTE)) return;

        // 드랍/경험치 제거
        e.getDrops().clear();
        e.setDroppedExp(0);

        // ⭕ 진행 중 라운드 몬스터라면 다음 라운드로
        waveManager.onWaveMobKilled(e.getEntity().getUniqueId());
    }
}
