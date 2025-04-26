package eu.kruzer.wgextender.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CommandUtils {

	private static SimpleCommandMap getCommandMap() {
		try {
			Object server = Bukkit.getServer();
			Field commandMapField = server.getClass().getDeclaredField("commandMap");
			commandMapField.setAccessible(true);
			return (SimpleCommandMap) commandMapField.get(server);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get CommandMap", e);
		}
	}

	public static Map<String, Command> getCommands() {
		try {
			SimpleCommandMap commandMap = getCommandMap();
			Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
			knownCommandsField.setAccessible(true);
			return (Map<String, Command>) knownCommandsField.get(commandMap);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get known commands", e);
		}
	}

	public static List<String> getCommandAliases(String commandName) {
		Command command = getCommands().get(commandName);
		if (command == null) {
			return Collections.singletonList(commandName);
		} else {
			List<String> aliases = new ArrayList<>();
			aliases.add(commandName);
			for (Map.Entry<String, Command> entry : getCommands().entrySet()) {
				if (entry.getValue().equals(command)) {
					aliases.add(entry.getKey());
				}
			}
			return aliases;
		}
	}

	public static void replaceCommand(Command oldCommand, Command newCommand) {
		String cmdName = oldCommand.getName();
		Map<String, Command> commandMap = getCommands();
		if (commandMap.get(cmdName).equals(oldCommand)) {
			commandMap.put(cmdName, newCommand);
		}
		for (String alias : oldCommand.getAliases()) {
			if (commandMap.get(alias).equals(oldCommand)) {
				commandMap.put(alias, newCommand);
			}
		}
	}
}