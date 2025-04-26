package eu.kruzer.wgextender.features.claimcommand;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.entity.Player;
import eu.kruzer.wgextender.Config;
import eu.kruzer.wgextender.VaultIntegration;
import eu.kruzer.wgextender.utils.WEUtils;

import java.math.BigInteger;

public class BlockLimits {
	private static final BigInteger MAX_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);

	public ProcessedClaimInfo processClaimInfo(Config config, Player player) {
		Region selection;
		try {
			selection = WEUtils.getSelection(player);
		} catch (IncompleteRegionException e) {
			return ProcessedClaimInfo.EMPTY_ALLOW;
		}
		BlockVector3 min = selection.getMinimumPoint();
		BlockVector3 max = selection.getMaximumPoint();

		BigInteger yDistance = distance(min.getY(), max.getY());
		BigInteger xDistance = distance(min.getX(), max.getX());
		BigInteger zDistance = distance(min.getZ(), max.getZ());
		BigInteger minHorizontal = xDistance.min(zDistance);

		BigInteger volume = BigInteger.ONE
				.multiply(xDistance)
				.multiply(zDistance)
				.multiply(yDistance);
		if (volume.compareTo(MAX_VALUE) > 0) {
			return new ProcessedClaimInfo(
					Result.DENY_MAX_VOLUME,
					volume,
					MAX_VALUE
			);
		}
		if (config.claimBlockLimitsEnabled) {
			if (player.hasPermission("worldguard.region.unlimited")) {
				return ProcessedClaimInfo.EMPTY_ALLOW;
			}
			if (volume.compareTo(config.claimBlockMinimalVolume) < 0) {
				return new ProcessedClaimInfo(
						Result.DENY_MIN_VOLUME,
						volume,
						config.claimBlockMinimalVolume
				);
			}
			if (minHorizontal.compareTo(config.claimBlockMinimalHorizontal) < 0) {
				return new ProcessedClaimInfo(
						Result.DENY_MIN_VOLUME,
						minHorizontal,
						config.claimBlockMinimalHorizontal
				);
			}
			if (yDistance.compareTo(config.claimBlockMinimalVertical) < 0) {
				return new ProcessedClaimInfo(
						Result.DENY_VERTICAL,
						yDistance,
						config.claimBlockMinimalVertical
				);
			}
			String[] groups = VaultIntegration.getInstance().getPermissions().getPlayerGroups(player);
			if (groups.length == 0) {
				return ProcessedClaimInfo.EMPTY_ALLOW;
			}
			BigInteger maxBlocks = config.claimBlockLimitDefault;
			for (String group : groups) {
				maxBlocks = maxBlocks.max(config.claimBlockLimits.getOrDefault(group.toLowerCase(), BigInteger.ZERO));
			}
			if (volume.compareTo(maxBlocks) > 0) {
				return new ProcessedClaimInfo(
						Result.DENY_MAX_VOLUME,
						volume,
						maxBlocks
				);
			}
		}
		return ProcessedClaimInfo.EMPTY_ALLOW;
	}

	public static final class ProcessedClaimInfo {
		private final Result result;
		private final BigInteger assignedSize;
		private final BigInteger assignedLimit;

		public static final ProcessedClaimInfo EMPTY_ALLOW = new ProcessedClaimInfo(Result.ALLOW, BigInteger.ZERO, BigInteger.ZERO);

		public ProcessedClaimInfo(Result result, BigInteger assignedSize, BigInteger assignedLimit) {
			this.result = result;
			this.assignedSize = assignedSize;
			this.assignedLimit = assignedLimit;
		}

		public Result result() {
			return result;
		}

		public BigInteger assignedSize() {
			return assignedSize;
		}

		public BigInteger assignedLimit() {
			return assignedLimit;
		}
	}

	public enum Result {
		ALLOW, DENY_MAX_VOLUME, DENY_MIN_VOLUME, DENY_HORIZONTAL, DENY_VERTICAL
	}

	private static BigInteger distance(int min, int max) {
		return BigInteger.valueOf(max - min + 1L);
	}
}