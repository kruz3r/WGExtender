
package eu.kruzer.wgextender;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Config {

	private final Plugin plugin;
	protected final File configFile;
	public Config(WGExtender plugin) {
		this.plugin = plugin;
		configFile = new File(plugin.getDataFolder(), "config.yml");
	}

	public boolean claimExpandSelectionVertical = false;

	public boolean claimBlockLimitsEnabled = false;
	public Map<String, BigInteger> claimBlockLimits = new LinkedHashMap<>();
	public BigInteger claimBlockLimitDefault = BigInteger.ZERO;
	public BigInteger claimBlockMinimalVolume = BigInteger.ZERO;
	public BigInteger claimBlockMinimalHorizontal = BigInteger.ZERO;
	public BigInteger claimBlockMinimalVertical = BigInteger.ZERO;

	public boolean checkLavaFlow = false;
	public boolean checkWaterFlow = false;
	public boolean checkOtherLiquidFlow = false;
	public boolean checkFireSpreadToRegion = false;
	public boolean disableFireSpreadInRegion = false;
	public boolean disableBlockBurnInRegion = false;
	public boolean checkExplosionBlockDamage = false;
	public boolean checkExplosionEntityDamage = false;

	public boolean claimAutoFlagsEnabled = false;
	public Map<Flag<?>, String> claimAutoFlags = new HashMap<>();

	public boolean restrictCommandsInRegionEnabled = false;
	public Set<String> restrictedCommandsInRegion = new HashSet<>();

	public boolean extendedWorldEditWandEnabled = false;

	public Boolean miscDefaultPvPFlagOperationMode = null;

	protected static final String miscPvPFlagOperationModeAllow = "allow";
	protected static final String miscPvPFlagOperationModeDeny = "deny";
	protected static final String miscPvPFlagOperationModeDefault = "default";

	public void loadConfig() {
		plugin.saveDefaultConfig();
		loadAll();
	}

	private void loadAll() {
		FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

		claimExpandSelectionVertical = config.getBoolean("claim.vertexpand", claimExpandSelectionVertical);

		claimBlockLimitsEnabled = config.getBoolean("claim.blocklimits.enabled", claimBlockLimitsEnabled);
		claimBlockLimits.clear();
		ConfigurationSection limitsSection = config.getConfigurationSection("claim.blocklimits.limits");
		if (limitsSection != null) {
			claimBlockLimitDefault = asBig(limitsSection, "default");
			for (String group : limitsSection.getKeys(false)) {
				claimBlockLimits.put(
						group.toLowerCase(),
						asBig(limitsSection, group)
				);
			}
		} else {
			claimBlockLimitDefault = BigInteger.ZERO;
		}
		ConfigurationSection minLimitsSection = config.getConfigurationSection("claim.blocklimits.minimal");
		if (minLimitsSection != null) {
			claimBlockMinimalVolume = asBig(minLimitsSection, "volume");
			claimBlockMinimalHorizontal = asBig(minLimitsSection, "horizontal");
			claimBlockMinimalVertical = asBig(minLimitsSection, "vertical");
		} else {
			claimBlockMinimalVolume = BigInteger.ZERO;
			claimBlockMinimalHorizontal = BigInteger.ZERO;
			claimBlockMinimalVertical = BigInteger.ZERO;
		}

		checkLavaFlow = config.getBoolean("regionprotect.flow.lava", checkLavaFlow);
		checkWaterFlow = config.getBoolean("regionprotect.flow.water", checkWaterFlow);
		checkOtherLiquidFlow = config.getBoolean("regionprotect.flow.other", checkOtherLiquidFlow);
		checkFireSpreadToRegion = config.getBoolean("regionprotect.fire.spread.toregion", checkFireSpreadToRegion);
		disableFireSpreadInRegion = config.getBoolean("regionprotect.fire.spread.inregion", disableFireSpreadInRegion);
		disableBlockBurnInRegion = config.getBoolean("regionprotect.fire.burn", disableBlockBurnInRegion);
		checkExplosionBlockDamage = config.getBoolean("regionprotect.explosion.block", checkExplosionBlockDamage);
		checkExplosionEntityDamage = config.getBoolean("regionprotect.explosion.entity", checkExplosionEntityDamage);

		claimAutoFlagsEnabled = config.getBoolean("autoflags.enabled",claimAutoFlagsEnabled);
		claimAutoFlags.clear();
		ConfigurationSection autoflagsSection = config.getConfigurationSection("autoflags.flags");
		if (autoflagsSection != null) {
			for (String flagStr : autoflagsSection.getKeys(false)) {
				Flag<?> flag = Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagStr);
				if (flag != null) {
					claimAutoFlags.put(flag, autoflagsSection.getString(flagStr));
				}
			}
		}

		restrictCommandsInRegionEnabled = config.getBoolean("restrictcommands.enabled", restrictCommandsInRegionEnabled);
		restrictedCommandsInRegion = new HashSet<>(config.getStringList("restrictcommands.commands"));

		extendedWorldEditWandEnabled = config.getBoolean("extendedwewand", extendedWorldEditWandEnabled);

		String miscPvpModeStr = config.getString("misc.pvpmode", miscPvPFlagOperationModeDefault);
		if (miscPvpModeStr.equalsIgnoreCase(miscPvPFlagOperationModeAllow)) {
			miscDefaultPvPFlagOperationMode = Boolean.TRUE;
		} else if (miscPvpModeStr.equalsIgnoreCase(miscPvPFlagOperationModeDeny)) {
			miscDefaultPvPFlagOperationMode = Boolean.FALSE;
		} else {
			miscDefaultPvPFlagOperationMode = null;
		}
	}

	private static BigInteger asBig(ConfigurationSection section, String key) {
		if (section.isInt(key)) {
			return BigInteger.valueOf(section.getInt(key));
		} else {
			String value = section.getString(key, "0");
			if (value.equals("0")) return BigInteger.ZERO;
			return new BigInteger(value);
		}
	}
}
