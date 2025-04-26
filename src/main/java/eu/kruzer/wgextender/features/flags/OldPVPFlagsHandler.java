package eu.kruzer.wgextender.features.flags;

import com.google.common.base.Function;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import eu.kruzer.wgextender.WGExtender;
import eu.kruzer.wgextender.utils.ReflectionUtils;
import eu.kruzer.wgextender.utils.WGRegionUtils;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class OldPVPFlagsHandler implements Listener {
	@SuppressWarnings("deprecation")
	private static final Set<EntityDamageEvent.DamageModifier> PVP_MODIFIERS = EnumSet.of(
			DamageModifier.ARMOR, DamageModifier.RESISTANCE, DamageModifier.MAGIC, DamageModifier.ABSORPTION
	);
	protected final Map<UUID, Double> oldValues = new HashMap<>();
	protected Field functionsField;

	public void start() {
		functionsField = ReflectionUtils.getField(EntityDamageEvent.class, "modifierFunctions");
		Bukkit.getPluginManager().registerEvents(this, WGExtender.getInstance());
		Bukkit.getScheduler().runTaskTimer(WGExtender.getInstance(), () -> {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (WGRegionUtils.isFlagTrue(player.getLocation(), WGExtenderFlags.OLDPVP_ATTACKSPEED)) {
					if (!oldValues.containsKey(player.getUniqueId())) {
						AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
						oldValues.put(player.getUniqueId(), attribute.getBaseValue());
						attribute.setBaseValue(16.0);
					}
				} else {
					reset(player);
				}
			}
		}, 0, 1);
	}

	public void stop() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			reset(player);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onQuit(PlayerQuitEvent event) {
		reset(event.getPlayer());
	}

	private void reset(Player player) {
		Double oldValue = oldValues.remove(player.getUniqueId());
		if (oldValue != null) {
			player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(oldValue);
		}
	}

	@SuppressWarnings({"unchecked", "deprecation"})
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof Player) ||
				!((Player) entity).isBlocking() ||
				!WGRegionUtils.isFlagTrue(entity.getLocation(), WGExtenderFlags.OLDPVP_NOSHIELDBLOCK)) {
			return;
		}
		Player player = (Player) entity;
		Map<DamageModifier, Function<Double, Double>> func;
		try {
			func = (Map<DamageModifier, Function<Double, Double>>) functionsField.get(event);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			WGExtender.getInstance().getLogger().log(Level.SEVERE, "Unable to recalculate blocking damage", e);
			return;
		}
		double totalDamage = event.getDamage() + event.getDamage(DamageModifier.HARD_HAT);
		//reset blocking modifier
		event.setDamage(DamageModifier.BLOCKING, 0);
		//recalculate other modifiers
		for (DamageModifier modifier : PVP_MODIFIERS) {
			double damage = func.get(modifier).apply(totalDamage);
			event.setDamage(modifier, damage);
			totalDamage += damage;
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onInteract(PlayerInteractEvent event) {
		if ((event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR ||
				event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) &&
				event.getHand() == EquipmentSlot.OFF_HAND &&
				event.getItem() != null &&
				event.getItem().getType() == Material.BOW &&
				WGRegionUtils.isFlagTrue(event.getPlayer().getLocation(), WGExtenderFlags.OLDPVP_NOBOW)) {
			event.setCancelled(true);
		}
	}
}