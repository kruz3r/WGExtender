package eu.kruzer.wgextender.features.extendedwand;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import eu.kruzer.wgextender.Config;
import eu.kruzer.wgextender.utils.CommandUtils;

public class WEWandCommandWrapper extends Command {

	public static void inject(Config config) {
		WEWandCommandWrapper wrapper = new WEWandCommandWrapper(config, CommandUtils.getCommands().get("/wand"));
		CommandUtils.replaceCommand(wrapper.originalCmd, wrapper);
	}

	public static void uninject() {
		WEWandCommandWrapper wrapper = (WEWandCommandWrapper) CommandUtils.getCommands().get("/wand");
		CommandUtils.replaceCommand(wrapper, wrapper.originalCmd);
	}

	protected final Config config;
	protected final Command originalCmd;

	protected WEWandCommandWrapper(Config config, Command originalCmd) {
		super(originalCmd.getName(), originalCmd.getDescription(), originalCmd.getUsage(), originalCmd.getAliases());
		this.config = config;
		this.originalCmd = originalCmd;
	}

	@Override
	public boolean execute(CommandSender sender, String label, String[] args) {
		if (!config.extendedWorldEditWandEnabled) {
			return originalCmd.execute(sender, label, args);
		}
		if (sender instanceof Player) {
			Player player = (Player) sender;
			player.getInventory().addItem(WEWand.getWand());
			player.sendMessage(ChatColor.LIGHT_PURPLE + "Выдана вещь для выделения территории");
		}
		return true;
	}
}