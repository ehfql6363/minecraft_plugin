package kwangdong.pingplugin.tasks;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;

public class DeathBeamTask extends BukkitRunnable {
	private final Location origin;
	private final ArmorStand stand;
	private final Color beamColor;
	private int ticks = 0;

	public DeathBeamTask(Location origin, Color beamColor) {
		this.origin = origin;
		this.beamColor = beamColor;

		this.stand = origin.getWorld().spawn(origin.clone().add(0.5, 0, 0.5), ArmorStand.class);
		stand.setVisible(false);
		stand.setMarker(true);
		stand.setGravity(false);
		stand.setInvulnerable(true);
		stand.setSilent(true);
	}

	@Override
	public void run() {
		if (ticks++ > 36000 || !stand.isValid()) {
			cancelAndRemove();
			return;
		}

		World world = origin.getWorld();
		double x = origin.getX() + 0.5;
		double z = origin.getZ() + 0.5;

		for (double y = origin.getY(); y < origin.getY() + 100; y += 0.5) {
			world.spawnParticle(
				Particle.DUST,
				x, y, z,
				1,
				0, 0, 0,
				0,
				new Particle.DustOptions(beamColor, 2.0f)
			);
			world.spawnParticle(Particle.END_ROD, x, y, z, 1, 0.1, 0.1, 0.1, 0.01);
		}
	}

	public void cancelAndRemove() {
		cancel();
		if (stand != null && !stand.isDead()) {
			stand.remove();
		}
	}
}
