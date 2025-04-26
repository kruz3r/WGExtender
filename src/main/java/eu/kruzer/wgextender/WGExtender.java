package eu.kruzer.wgextender;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import eu.kruzer.wgextender.commands.Commands;
import eu.kruzer.wgextender.features.claimcommand.WGRegionCommandWrapper;
import eu.kruzer.wgextender.features.extendedwand.WEWandCommandWrapper;
import eu.kruzer.wgextender.features.extendedwand.WEWandListener;
import eu.kruzer.wgextender.features.flags.ChorusFruitFlagHandler;
import eu.kruzer.wgextender.features.flags.OldPVPFlagsHandler;
import eu.kruzer.wgextender.features.flags.WGExtenderFlags;
import eu.kruzer.wgextender.features.regionprotect.ownormembased.PvPHandlingListener;
import eu.kruzer.wgextender.features.regionprotect.ownormembased.RestrictCommands;
import eu.kruzer.wgextender.features.regionprotect.regionbased.BlockBurn;
import eu.kruzer.wgextender.features.regionprotect.regionbased.Explode;
import eu.kruzer.wgextender.features.regionprotect.regionbased.FireSpread;
import eu.kruzer.wgextender.features.regionprotect.regionbased.LiquidFlow;

import java.util.Objects;
import java.util.logging.Level;

public class WGExtender extends JavaPlugin {

	private static WGExtender instance;
	public static WGExtender getInstance() {
		return instance;
	}

	public WGExtender() {
		instance = this;
	}

	private PvPHandlingListener pvplistener;
	private OldPVPFlagsHandler oldpvphandler;

	@Override
	public void onEnable() {
		VaultIntegration.getInstance().initialize(this);
		Config config = new Config(this);
		config.loadConfig();
		Objects.requireNonNull(getCommand("wgex")).setExecutor(new Commands(config));
		PluginManager pluginManager = getServer().getPluginManager();
		pluginManager.registerEvents(new RestrictCommands(config), this);
		pluginManager.registerEvents(new LiquidFlow(config), this);
		pluginManager.registerEvents(new FireSpread(config), this);
		pluginManager.registerEvents(new BlockBurn(config), this);
		pluginManager.registerEvents(new Explode(config), this);
		pluginManager.registerEvents(new WEWandListener(), this);
		pluginManager.registerEvents(new ChorusFruitFlagHandler(), this);
		try {
			WGRegionCommandWrapper.inject(config);
			WEWandCommandWrapper.inject(config);
			WGExtenderFlags.registerFlags();
			pvplistener = new PvPHandlingListener(config);
			pvplistener.inject();
			oldpvphandler = new OldPVPFlagsHandler();
			oldpvphandler.start();
		} catch (Throwable t) {
			getLogger().log(Level.SEVERE, "Unable to inject, shutting down", t);
			t.printStackTrace();
			Bukkit.shutdown();
		}
	}

	@Override
	public void onDisable() {
		try {
			WEWandCommandWrapper.uninject();
			WGRegionCommandWrapper.uninject();
			pvplistener.uninject();
			oldpvphandler.stop();
		} catch (Throwable t) {
			getLogger().log(Level.SEVERE, "Unable to uninject, shutting down", t);
			Bukkit.shutdown();
		}
	}

}
