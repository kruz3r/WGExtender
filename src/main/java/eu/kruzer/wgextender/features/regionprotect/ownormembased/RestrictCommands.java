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

package eu.kruzer.wgextender.features.regionprotect.ownormembased;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import eu.kruzer.wgextender.Config;
import eu.kruzer.wgextender.WGExtender;
import eu.kruzer.wgextender.utils.CommandUtils;
import eu.kruzer.wgextender.utils.WGRegionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class RestrictCommands implements Listener {
	private final Pattern SPACE_PATTERN = Pattern.compile("\\s+");

	protected final Config config;
	protected volatile String[] restrictedCommands;

	public RestrictCommands(Config config) {
		this.config = config;
		restrictedCommands = config.restrictedCommandsInRegion.toArray(new String[0]);
		Bukkit.getScheduler().runTaskTimerAsynchronously(WGExtender.getInstance(), () -> {
			if (!config.restrictCommandsInRegionEnabled) {
				return;
			}
			List<String> computedRestrictedCommands = new ArrayList<>();
			for (String restrictedCommand : config.restrictedCommandsInRegion) {
				String[] split = SPACE_PATTERN.split(restrictedCommand);
				String toAdd = split.length > 1 ? String.join(" ", Arrays.copyOfRange(split, 1, split.length)) : "";
				for (String alias : CommandUtils.getCommandAliases(split[0])) {
					computedRestrictedCommands.add(String.join(" ", alias, toAdd));
				}
			}
			restrictedCommands = computedRestrictedCommands.toArray(new String[0]);
		}, 1, 100);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (!config.restrictCommandsInRegionEnabled) {
			return;
		}
		Player player = event.getPlayer();
		if (WGRegionUtils.canBypassProtection(player)) {
			return;
		}
		if (WGRegionUtils.isInWGRegion(player.getLocation()) && !WGRegionUtils.canBuild(player, player.getLocation())) {
			String message = event.getMessage().substring(1).toLowerCase();
			for (String rcommand : restrictedCommands) {
				if (message.startsWith(rcommand) && ((message.length() == rcommand.length()) || (message.charAt(rcommand.length()) == ' '))) {
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED + "Вы не можете использовать эту команду на чужом регионе");
					return;
				}
			}
		}
	}
}
