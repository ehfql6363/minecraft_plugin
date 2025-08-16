package kwangdong.pingplugin.listeners;

import kwangdong.pingplugin.util.WaveTags;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.persistence.PersistentDataType;

public class WaveMobTransformListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTransform(EntityTransformEvent e) {
        // 웨이브 몹이면 어떤 이유(물빠짐, 감염, 번식 등)로든 변이 취소
        if (e.getEntity().getPersistentDataContainer().has(WaveTags.WAVE_MOB, PersistentDataType.BYTE)) {
            e.setCancelled(true);
            // 서버 로그에만 남김 (플레이어에게는 표시 안 함)
            e.getEntity().getServer().getLogger().info(
                    "[Wave] Prevented transform for wave mob: " + e.getEntity().getType().name()
            );
        }
    }
}
