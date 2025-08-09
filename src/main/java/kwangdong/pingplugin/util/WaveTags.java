package kwangdong.pingplugin.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class WaveTags {
	private WaveTags() {}
	public static NamespacedKey WAVE_MOB;

	public static void init(Plugin plugin) {
		WAVE_MOB = new NamespacedKey(plugin, "wave_mob");
	}
}
