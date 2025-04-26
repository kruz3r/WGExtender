package eu.kruzer.wgextender.features.regionprotect.ownormembased;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.cause.Cause;
import com.sk89q.worldguard.bukkit.event.DelegateEvent;
import com.sk89q.worldguard.bukkit.event.entity.DamageEntityEvent;
import com.sk89q.worldguard.bukkit.internal.WGMetadata;
import com.sk89q.worldguard.bukkit.listener.RegionProtectionListener;
import com.sk89q.worldguard.bukkit.protection.events.DisallowedPVPEvent;
import com.sk89q.worldguard.bukkit.util.Entities;
import com.sk89q.worldguard.bukkit.util.Events;
import com.sk89q.worldguard.bukkit.util.InteropUtils;
import com.sk89q.worldguard.domains.Association;
import com.sk89q.worldguard.protection.association.Associables;
import com.sk89q.worldguard.protection.association.DelayedRegionOverlapAssociation;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import eu.kruzer.wgextender.Config;
import eu.kruzer.wgextender.features.regionprotect.WGOverrideListener;
import eu.kruzer.wgextender.utils.WGRegionUtils;

import java.util.Arrays;
import java.util.List;

public class PvPHandlingListener extends WGOverrideListener {

	protected final Config config;

	public PvPHandlingListener(Config config) {
		this.config = config;
	}

	@Override
	protected Class<? extends Listener> getClassToReplace() {
		return RegionProtectionListener.class;
	}

	@EventHandler(ignoreCancelled = true)
	public void onDamageEntity(DamageEntityEvent event) {
		if (event.getResult() == Result.ALLOW) {
			return;
		}
		if (!WGRegionUtils.getWorldConfig(event.getWorld()).useRegions) {
			return;
		}

		Player playerAttacker = event.getCause().getFirstPlayer();
		if (playerAttacker == null) {
			return;
		}

		// Block PvP like normal even if the player has an override permission
		// because (1) this is a frequent source of confusion and
		// (2) some users want to block PvP even with the bypass permission
		boolean pvp = event.getEntity() instanceof Player && !playerAttacker.equals(event.getEntity());
		if (isWhitelisted(event.getCause(), event.getWorld(), pvp)) {
			return;
		}

		RegionQuery query = WGRegionUtils.REGION_QUERY;

		boolean canDamage;
		String what;

		Location target = event.getTarget();
		RegionAssociable associable = createRegionAssociable(event.getCause());
		com.sk89q.worldedit.util.Location weTarget = BukkitAdapter.adapt(target);
		com.sk89q.worldedit.util.Location weAttacker = BukkitAdapter.adapt(playerAttacker.getLocation());

		/* Hostile / ambient mob override */
		if (
				Entities.isHostile(event.getEntity()) ||
						Entities.isAmbient(event.getEntity()) ||
						Entities.isVehicle(event.getEntity().getType())
		) {
			canDamage = event.getRelevantFlags().isEmpty() || (query.queryState(weTarget, associable, combine(event)) != State.DENY);
			what = "hit that";

			/* Paintings, item frames, etc. */
		} else if (Entities.isConsideredBuildingIfUsed(event.getEntity())) {
			canDamage = query.testBuild(weTarget, associable, combine(event));
			what = "change that";

			/* PVP */
		} else if (pvp) {
			Player defender = (Player) event.getEntity();
			LocalPlayer localPlayerAttacker = WorldGuardPlugin.inst().wrapPlayer(playerAttacker);

			// add possibility to change how pvp none flag works
			// null - default wg pvp logic
			// true - allow pvp when flag not set
			// false - disallow pvp when flag not set
			if (config.miscDefaultPvPFlagOperationMode == null) {
				canDamage =
						query.testBuild(weTarget, associable, combine(event, Flags.PVP)) &&
								(query.queryState(weAttacker, localPlayerAttacker, combine(event, Flags.PVP)) != State.DENY) &&
								(query.queryState(weTarget, localPlayerAttacker, combine(event, Flags.PVP)) != State.DENY);
			} else if (config.miscDefaultPvPFlagOperationMode) {
				canDamage =
						(query.queryState(weAttacker, localPlayerAttacker, combine(event, Flags.PVP)) != State.DENY) &&
								(query.queryState(weTarget, localPlayerAttacker, combine(event, Flags.PVP)) != State.DENY);
			} else {
				if (!WGRegionUtils.isInWGRegion(playerAttacker.getLocation()) && !WGRegionUtils.isInWGRegion(target)) {
					canDamage = true;
				} else {
					canDamage = (query.queryState(weAttacker, localPlayerAttacker, combine(event, Flags.PVP)) == State.ALLOW) && (query.queryState(weTarget, localPlayerAttacker, combine(event, Flags.PVP)) == State.ALLOW);
				}

			}

			// Fire disallow PVP event
			if (!canDamage && Events.fireAndTestCancel(new DisallowedPVPEvent(playerAttacker, defender, event.getOriginalEvent()))) {
				canDamage = true;
			}

			what = "PvP";

			/* Player damage not caused by another player */
		} else if (event.getEntity() instanceof Player) {
			canDamage = event.getRelevantFlags().isEmpty() || (query.queryState(weTarget, associable, combine(event)) != State.DENY);
			what = "damage that";

			/* damage to non-hostile mobs (e.g. animals) */
		} else if (Entities.isNonHostile(event.getEntity())) {
			canDamage = query.testBuild(weTarget, associable, combine(event, Flags.DAMAGE_ANIMALS));
			what = "harm that";

			/* Everything else */
		} else {
			canDamage = query.testBuild(weTarget, associable, combine(event, Flags.INTERACT));
			what = "hit that";
		}

		if (!canDamage) {
			tellErrorMessage(event, event.getCause(), target, what);
			event.setCancelled(true);
		}
	}

	private RegionAssociable createRegionAssociable(Cause cause) {
		Object rootCause = cause.getRootCause();

		if (!cause.isKnown()) {
			return Associables.constant(Association.NON_MEMBER);
		} else if (rootCause instanceof Player) {
			Player player = (Player) rootCause;
			return WorldGuardPlugin.inst().wrapPlayer(player);
		} else if (rootCause instanceof OfflinePlayer) {
			OfflinePlayer offlinePlayer = (OfflinePlayer) rootCause;
			return WorldGuardPlugin.inst().wrapOfflinePlayer(offlinePlayer);
		} else if (rootCause instanceof Entity) {
			Entity entity = (Entity) rootCause;
			return new DelayedRegionOverlapAssociation(WGRegionUtils.REGION_QUERY, BukkitAdapter.adapt(entity.getLocation()));
		} else if (rootCause instanceof Block) {
			Block block = (Block) rootCause;
			return new DelayedRegionOverlapAssociation(WGRegionUtils.REGION_QUERY, BukkitAdapter.adapt(block.getLocation()));
		} else {
			return Associables.constant(Association.NON_MEMBER);
		}
	}

	private boolean isWhitelisted(Cause cause, World world, boolean pvp) {
		Object rootCause = cause.getRootCause();

		if (rootCause instanceof Block) {
			Block block = (Block) rootCause;
			Material type = block.getType();
			return (type == Material.HOPPER) || (type == Material.DROPPER);
		} else if (rootCause instanceof Player) {
			Player player = (Player) rootCause;
			if (WGRegionUtils.getWorldConfig(world).fakePlayerBuildOverride && InteropUtils.isFakePlayer(player)) {
				return true;
			}
			return !pvp && WGRegionUtils.canBypassProtection(player);
		} else {
			return false;
		}
	}

	private static final String DENY_MESSAGE_KEY = "worldguard.region.lastMessage";
	private static final int LAST_MESSAGE_DELAY = 500;

	private void tellErrorMessage(DelegateEvent event, Cause cause, Location location, String what) {
		if (event.isSilent() || cause.isIndirect()) {
			return;
		}

		Object rootCause = cause.getRootCause();

		if (rootCause instanceof Player) {
			Player player = (Player) rootCause;
			long now = System.currentTimeMillis();
			Long lastTime = WGMetadata.getIfPresent(player, DENY_MESSAGE_KEY, Long.class);
			if ((lastTime == null) || ((now - lastTime) >= LAST_MESSAGE_DELAY)) {
				@SuppressWarnings("deprecation")
				String message = WGRegionUtils.REGION_QUERY.queryValue(BukkitAdapter.adapt(location), WorldGuardPlugin.inst().wrapPlayer(player), Flags.DENY_MESSAGE);
				if (message != null && !message.isEmpty()) {
					player.sendMessage(message.replace("%what%", what));
				}
				WGMetadata.put(player, DENY_MESSAGE_KEY, now);
			}
		}
	}

	private static StateFlag[] combine(DelegateEvent event, StateFlag... flag) {
		List<StateFlag> extra = event.getRelevantFlags();
		StateFlag[] flags = Arrays.copyOf(flag, flag.length + extra.size());
		for (int i = 0; i < extra.size(); i++) {
			flags[flag.length + i] = extra.get(i);
		}
		return flags;
	}
}