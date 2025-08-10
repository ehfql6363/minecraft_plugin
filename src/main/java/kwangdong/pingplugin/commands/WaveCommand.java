package kwangdong.pingplugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import kwangdong.pingplugin.manager.WaveManager;

public class WaveCommand implements CommandExecutor {

	private final WaveManager waveManager;

	public WaveCommand(WaveManager waveManager) {
		this.waveManager = waveManager;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player p)) return true;

		if (args.length == 0) {
			p.sendMessage("/wave start | skip | stop");
			return true;
		}

		switch (args[0].toLowerCase()) {
			case "start":
				waveManager.startWave(p);
				break;
			case "skip":
				waveManager.skipWave();
				p.sendMessage("웨이브를 강제로 성공 처리했습니다.");
				break;
			case "stop":
				waveManager.stopWave();
				p.sendMessage("웨이브를 강제로 중지(실패) 처리했습니다.");
				break;
			default:
				p.sendMessage("/wave start | skip | stop");
		}
		return true;
	}
}
