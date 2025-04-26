package eu.kruzer.wgextender;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

public class VaultIntegration implements Listener {
	private static final VaultIntegration instance = new VaultIntegration();
	public static VaultIntegration getInstance() {
		return instance;
	}

	private Permission permissions;

	public Permission getPermissions() {
		return permissions;
	}

	public void initialize(WGExtender plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		hook();
	}

	private void hook() {
		try {
			permissions = Bukkit.getServicesManager().getRegistration(Permission.class).getProvider();
			if (!permissions.hasGroupSupport()) {
				throw new IllegalStateException();
			}
		} catch (Exception e) {
			permissions = null;
		}
	}

	@EventHandler
	public void onPluginEnable(PluginEnableEvent event) {
		hook();
	}

	@EventHandler
	public void onPluginDisable(PluginDisableEvent event) {
		hook();
	}
}
