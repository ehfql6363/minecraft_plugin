package kwangdong.pingplugin.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import kwangdong.pingplugin.manager.DeathBeamTaskManager;
import kwangdong.pingplugin.PingPlugin;

public class DeathCommand implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) return true;

		if (args.length == 1 && args[0].equalsIgnoreCase("latest")) {
			PingPlugin.LocationInfo info = PingPlugin.deathMap.get(player.getUniqueId());
			if (info == null) {
				player.sendMessage(Component.text("최근 사망 위치가 없습니다.", NamedTextColor.RED));
			} else {
				player.sendMessage(Component.text("최근 사망 위치: X=" + info.x() + " Y=" + info.y() + " Z=" + info.z(), NamedTextColor.GREEN));
			}
			return true;
		}

		if (args.length == 1 && args[0].equalsIgnoreCase("clear")) {
			PingPlugin.deathMap.remove(player.getUniqueId());
			DeathBeamTaskManager.clear(player.getUniqueId());
			player.sendMessage(Component.text("죽은 위치와 파티클이 제거되었습니다.", NamedTextColor.YELLOW));
			return true;
		}

		player.sendMessage(Component.text("사용법: /death latest | /death clear", NamedTextColor.GRAY));
		return true;
	}
}
