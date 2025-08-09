package kwangdong.pingplugin.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PingCommand implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(Component.text("플레이어만 사용 가능합니다.").color(NamedTextColor.RED));
			return true;
		}

		if (args.length == 1 && args[0].equalsIgnoreCase("cur")) {
			Location loc = player.getLocation();

			Component msg = Component.text("[PING] ").color(NamedTextColor.LIGHT_PURPLE)
				.append(Component.text(player.getName()).color(NamedTextColor.AQUA))
				.append(Component.text("의 현재 위치 → ").color(NamedTextColor.WHITE))
				.append(Component.text("X=" + loc.getBlockX() + ", Y=" + loc.getBlockY() + ", Z=" + loc.getBlockZ())
					.color(NamedTextColor.YELLOW));

			// 전체 플레이어에게 전송
			Bukkit.getServer().sendMessage(msg);
			return true;
		}

		player.sendMessage(Component.text("사용법: /ping cur").color(NamedTextColor.YELLOW));
		return true;
	}
}
