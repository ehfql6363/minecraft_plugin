package kwangdong.pingplugin.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import kwangdong.pingplugin.manager.WaveManager;

import java.util.List;

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
                // 예: start/skip/stop/mobs 분기 내부 맨 앞에 한 줄씩
                p.getServer().getLogger().info("[Wave] Command '" + args[0].toLowerCase() + "' by " + p.getName());

                if (!sender.isOp()) {
                    p.sendMessage("OP 전용 명령어입니다.");
                    return true;
                }
                waveManager.startWave(p);
                break;
            case "skip":
                // 예: start/skip/stop/mobs 분기 내부 맨 앞에 한 줄씩
                p.getServer().getLogger().info("[Wave] Command '" + args[0].toLowerCase() + "' by " + p.getName());

                if (!sender.isOp()) {
                    p.sendMessage("OP 전용 명령어입니다.");
                    return true;
                }
                waveManager.skipWave();
                p.sendMessage("웨이브를 강제로 성공 처리했습니다.");
                break;
			case "stop":
                // 예: start/skip/stop/mobs 분기 내부 맨 앞에 한 줄씩
                p.getServer().getLogger().info("[Wave] Command '" + args[0].toLowerCase() + "' by " + p.getName());

                waveManager.stopWave();
				p.sendMessage("웨이브를 강제로 중지(실패) 처리했습니다.");
				break;
            case "mobs":
                // 예: start/skip/stop/mobs 분기 내부 맨 앞에 한 줄씩
                p.getServer().getLogger().info("[Wave] Command '" + args[0].toLowerCase() + "' by " + p.getName());

                if (!waveManager.isWaveActive()) {
                    p.sendMessage(Component.text("[Wave] 현재 진행 중인 웨이브가 없습니다.", NamedTextColor.GRAY));
                    return true;
                }

                List<Entity> mobs = waveManager.getAliveWaveMobs();
                if (mobs.isEmpty()) {
                    p.sendMessage(Component.text("[Wave] 남은 몬스터가 없습니다.", NamedTextColor.GRAY));
                    return true;
                }

                p.sendMessage(Component.text("[Wave] 남은 몬스터 " + mobs.size() + "마리:", NamedTextColor.YELLOW));
                for (Entity e : mobs) {
                    Location loc = e.getLocation();
                    String line = "- [" + e.getType().name() + "] X=" + loc.getBlockX() + " Y=" + loc.getBlockY() + " Z=" + loc.getBlockZ();
                    p.sendMessage(Component.text(line, NamedTextColor.WHITE));
                }
                break;
			default:
				p.sendMessage("/wave start | skip | stop | mobs");
		}
		return true;
	}
}
