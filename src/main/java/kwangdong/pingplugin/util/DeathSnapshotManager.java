package kwangdong.pingplugin.util;

import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathSnapshotManager {
	private static final Map<UUID, ItemStack[]> contents = new HashMap<>();
	private static final Map<UUID, ItemStack[]> armor = new HashMap<>();
	private static final Map<UUID, ItemStack[]> extra = new HashMap<>();

	public static void save(UUID id, ItemStack[] invContents, ItemStack[] invArmor, ItemStack[] invExtra) {
		contents.put(id, clone(invContents));
		armor.put(id, clone(invArmor));
		if (invExtra != null) extra.put(id, clone(invExtra));
	}
	public static ItemStack[] getContents(UUID id) { return contents.get(id); }
	public static ItemStack[] getArmor(UUID id) { return armor.get(id); }
	public static ItemStack[] getExtra(UUID id) { return extra.get(id); }
	public static void clear(UUID id) { contents.remove(id); armor.remove(id); extra.remove(id); }

	private static ItemStack[] clone(ItemStack[] arr) {
		if (arr == null) return null;
		ItemStack[] out = new ItemStack[arr.length];
		for (int i = 0; i < arr.length; i++) out[i] = (arr[i] == null) ? null : arr[i].clone();
		return out;
	}
}
