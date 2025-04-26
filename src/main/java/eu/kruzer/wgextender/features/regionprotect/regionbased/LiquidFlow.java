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

package eu.kruzer.wgextender.features.regionprotect.regionbased;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import eu.kruzer.wgextender.Config;
import eu.kruzer.wgextender.utils.WGRegionUtils;

public class LiquidFlow implements Listener {

	protected final Config config;

	public LiquidFlow(Config config) {
		this.config = config;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onLiquidFlow(BlockFromToEvent event) {
		check(event.getBlock(), event.getToBlock(), event);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onDispense(BlockDispenseEvent event) {
		Block block = event.getBlock();
		BlockData blockData = block.getState().getBlockData();
		if (blockData instanceof Directional) {
			Directional directional = (Directional) blockData;
			check(block, block.getRelative(directional.getFacing()), event);
		}
	}

	private void check(Block source, Block to, Cancellable event) {
		boolean shouldCheck = false;
		switch (source.getType()) {
			case LAVA:
				shouldCheck = config.checkLavaFlow;
				break;
			case WATER:
				shouldCheck = config.checkWaterFlow;
				break;
			default:
				shouldCheck = config.checkOtherLiquidFlow;
				break;
		}

		if (shouldCheck && !WGRegionUtils.isInTheSameRegionOrWild(source.getLocation(), to.getLocation())) {
			event.setCancelled(true);
		}
	}
}