/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package eu.kruzer.wgextender.commands;

import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.EnumFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import eu.kruzer.wgextender.Config;
import eu.kruzer.wgextender.features.claimcommand.AutoFlags;
import eu.kruzer.wgextender.utils.WEUtils;
import eu.kruzer.wgextender.utils.WGRegionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.bukkit.util.StringUtil.copyPartialMatches;

//TODO: refactor
public class Commands implements CommandExecutor, TabCompleter {

	protected final Config config;

	public Commands(Config config) {
		this.config = config;
	}

	protected static List<String> getRegionsInPlayerSelection(Player player) throws IncompleteRegionException {
		Region psel = WEUtils.getSelection(player);
		ProtectedRegion fakerg = new ProtectedCuboidRegion("wgexfakerg", psel.getMaximumPoint(), psel.getMinimumPoint());
		ApplicableRegionSet ars = WGRegionUtils.getRegionManager(player.getWorld()).getApplicableRegions(fakerg);
		List<String> regions = new ArrayList<>();
		for (ProtectedRegion region : ars) {
			regions.add(region.getId());
		}
		return regions;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command arg1, String label, String[] args) {
		if (!sender.hasPermission("wgextender.admin")) {
			sender.sendMessage(ChatColor.RED + "Недостаточно прав");
			return true;
		}
		if (args.length >= 1) {
			switch (args[0].toLowerCase()) {
				case "help":
					sender.sendMessage(ChatColor.BLUE + "wgex reload - перезагрузить конфиг");
					sender.sendMessage(ChatColor.BLUE + "wgex search - ищет регионы в выделенной области");
					sender.sendMessage(ChatColor.BLUE + "wgex setflag {world} {flag} {value}  - устанавливает флаг {flag} со значением {value} на все регионы в мире {world}");
					sender.sendMessage(ChatColor.BLUE + "wgex removeowner {name} - удаляет игрока из списков владельцев всех регионов");
					sender.sendMessage(ChatColor.BLUE + "wgex removemember {name} - удаляет игрока из списков членов всех регионов");
					return true;
				case "reload":
					config.loadConfig();
					sender.sendMessage(ChatColor.BLUE + "Конфиг перезагружен");
					return true;
				case "search":
					if (sender instanceof Player) {
						Player player = (Player) sender;
						try {
							List<String> regions = getRegionsInPlayerSelection(player);
							if (regions.isEmpty()) {
								sender.sendMessage(ChatColor.BLUE + "Регионов пересекающихся с выделенной зоной не найдено");
							} else {
								sender.sendMessage(ChatColor.BLUE + "Найдены регионы пересекающиеся с выделенной зоной: " + regions);
							}
						} catch (IncompleteRegionException e) {
							sender.sendMessage(ChatColor.BLUE + "Сначала выделите зону поиска");
						}
						return true;
					}
					return false;
				case "setflag":
					if (args.length < 4) {
						return false;
					}
					World world = Bukkit.getWorld(args[1]);
					if (world == null) {
						sender.sendMessage(ChatColor.BLUE + "Мир не найден");
						return true;
					}
					Flag<?> flag = Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), args[2]);
					if (flag == null) {
						sender.sendMessage(ChatColor.BLUE + "Флаг не найден");
						return true;
					}
					try {
						StringBuilder valueBuilder = new StringBuilder();
						for (int i = 3; i < args.length; i++) {
							valueBuilder.append(args[i]).append(" ");
						}
						String value = valueBuilder.toString().trim();
						for (ProtectedRegion region : WGRegionUtils.getRegionManager(world).getRegions().values()) {
							if (region instanceof GlobalProtectedRegion) {
								continue;
							}
							AutoFlags.setFlag(WGRegionUtils.wrapAsPrivileged(sender), world, region, flag, value);
						}
						sender.sendMessage(ChatColor.BLUE + "Флаги установлены");
					} catch (CommandException e) {
						sender.sendMessage(ChatColor.BLUE + "Неправильный формат флага " + flag.getName() + ": " + e.getMessage());
					}
					return true;
				case "removeowner":
				case "removemember":
					if (args.length != 2) {
						return false;
					}
					boolean owner = args[0].equalsIgnoreCase("removeowner");
					OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(args[1]);
					String name = (offPlayer.getName() == null ? args[1] : offPlayer.getName()).toLowerCase();
					UUID uuid = offPlayer.getUniqueId();
					for (RegionManager manager : WGRegionUtils.getRegionContainer().getLoaded()) {
						for (ProtectedRegion region : manager.getRegions().values()) {
							DefaultDomain members = owner ? region.getOwners() : region.getMembers();
							members.removePlayer(uuid);
							members.removePlayer(name);
							if (owner) {
								region.setOwners(members);
							} else {
								region.setMembers(members);
							}
						}
					}
					sender.sendMessage(ChatColor.BLUE + "Игрок удалён из списков " + (owner ? "владельцев" : "участников") + " всех регионов");
					return true;
				default:
					return false;
			}
		}
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 0 || !sender.hasPermission("wgextender.admin")) {
			return Collections.emptyList();
		}
		if (args.length == 1) {
			List<String> commands = new ArrayList<>();
			commands.add("help");
			commands.add("reload");
			if (sender instanceof Player) {
				commands.add("search");
			}
			commands.add("setflag");
			commands.add("removeowner");
			commands.add("removemember");
			return copyPartialMatches(args[0], commands, new ArrayList<>());
		}
		if (!"setflag".equalsIgnoreCase(args[0])) {
			return Collections.emptyList();
		}
		switch (args.length) {
			case 2:
				List<String> worldNames = new ArrayList<>();
				for (World world : Bukkit.getWorlds()) {
					worldNames.add(world.getName());
				}
				return copyPartialMatches(args[1], worldNames, new ArrayList<>());
			case 3:
				List<String> flagNames = new ArrayList<>();
				for (Flag<?> flag : WorldGuard.getInstance().getFlagRegistry()) {
					flagNames.add(flag.getName());
				}
				return copyPartialMatches(args[2], flagNames, new ArrayList<>());
			case 4:
				Flag<?> flag = Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), args[2]);
				if (flag instanceof StateFlag) {
					List<String> states = new ArrayList<>();
					for (State state : State.values()) {
						states.add(state.toString());
					}
					return copyPartialMatches(args[3], states, new ArrayList<>());
				}
				if (flag instanceof BooleanFlag) {
					return copyPartialMatches(args[3], Arrays.asList("true", "false"), new ArrayList<>());
				}
				if (flag instanceof EnumFlag) {
					EnumFlag<?> enumFlag = (EnumFlag<?>) flag;
					try {
						List<String> enumValues = new ArrayList<>();
						for (Object enumConstant : enumFlag.getEnumClass().getEnumConstants()) {
							enumValues.add(enumConstant.toString());
						}
						return copyPartialMatches(args[3], enumValues, new ArrayList<>());
					} catch (Exception ignored) {
					}
				}
				break;
		}
		return Collections.emptyList();
	}
}