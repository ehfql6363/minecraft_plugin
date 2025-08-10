package kwangdong.pingplugin.manager;

import java.util.HashMap;
import java.util.UUID;

import kwangdong.pingplugin.tasks.DeathBeamTask;

public class DeathBeamTaskManager {
	private static final HashMap<UUID, DeathBeamTask> tasks = new HashMap<>();

	public static void register(UUID uuid, DeathBeamTask task) {
		tasks.put(uuid, task);
	}

	public static void clear(UUID uuid) {
		DeathBeamTask task = tasks.remove(uuid);
		if (task != null) task.cancelAndRemove();
	}
}
