package com.qub1.liquidcraft;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

// TODO: Add more event handlers
// TODO: Check for and remove redundant event handlers
// TODO: Divide liquid level over neighbors

public class LiquidCraft extends JavaPlugin {
	/**
	 * The amount of ticks between each flow.
	 */
	private static final long TICKS_PER_FLOW = 4;

	/**
	 * The highest liquid level.
	 */
	private static final int MAXIMUM_LIQUID_LEVEL = 8;

	/**
	 * The lowest liquid level.
	 */
	private static final int MINIMUM_LIQUID_LEVEL = 0;

	/**
	 * All active liquid blocks, cached.
	 */
	private Queue<Block> liquidBlocks = new LinkedList<>();

	/**
	 * Checks whether a two blocks are the same type of liquid.
	 *
	 * @param block1 The first block to compare.
	 * @param block2 The second block to compare.
	 * @return Whether the two blocks are the same type of liquid.
	 */
	public static boolean isSameLiquid(final Block block1, final Block block2) {
		return isSameLiquid(block1.getType(), block2.getType());
	}

	/**
	 * Checks whether a two materials are the same type of liquid. If one of the materials is AIR, they will match.
	 *
	 * @param material1 The first material to compare.
	 * @param material2 The second material to compare.
	 * @return Whether the two materials are the same type of liquid.
	 */
	public static boolean isSameLiquid(final Material material1, final Material material2) {
		try {
			return isLiquid(material1, true) && isLiquid(material2, true) && (material1 == Material.AIR || material2 == Material.AIR || LiquidType.fromMaterial(material1) == LiquidType.fromMaterial(material2));
		} catch (Exception e) {
			// This should not happen
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Gets the blocks with the lowest liquid level.
	 *
	 * @param blocks     The block to check.
	 * @param liquidType The liquid type the blocks need to be.
	 * @return The blocks with the lowest liquid level.
	 */
	public static List<Block> getLowestLiquidLevel(final List<Block> blocks, final LiquidType liquidType) {
		int lowestLiquidLevel = -1;
		List<Block> result = new ArrayList<>();

		// Loop blocks and try to find the lowest one
		for (Block block : blocks.stream().filter(o -> isLiquid(o, true) && isSameLiquid(o.getType(), liquidType.getValue())).collect(Collectors.toList())) {
			// Try to get the block's liquid level
			try {
				int liquidLevel = getLiquidLevel(block);

				// If the liquid level is lower than the lowest until now, clear the current results
				if (liquidLevel < lowestLiquidLevel) {
					result.clear();
				}

				// If there is no liquid level, or the current block is lower or equal to the results, add it
				if (lowestLiquidLevel == -1 || liquidLevel <= lowestLiquidLevel) {
					lowestLiquidLevel = liquidLevel;
					result.add(block);
				}
			} catch (Exception e) {
				// This shouldn't happen
				e.printStackTrace();
			}
		}

		return result;
	}

	/**
	 * Checks whether the specified block can accept water.
	 *
	 * @param block The block to check.
	 * @return Whether the block can accept water.
	 */
	public static boolean canAcceptWater(Block block) {
		try {
			return isLiquid(block, true) && getLiquidLevel(block) < MAXIMUM_LIQUID_LEVEL;
		} catch (Exception e) {
			return false; // Should never happen
		}
	}

	/**
	 * Checks if the specified block can flow to another block.
	 *
	 * @param block The block to check.
	 * @return Whether the block can flow.
	 */
	public static boolean canFlow(Block block) {
		try {
			return isLiquid(block, false) && getLiquidLevel(block) > MINIMUM_LIQUID_LEVEL;
		} catch (Exception e) {
			// This should not happen
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Gets the horizontal neighbors around a block.
	 *
	 * @param block The block to check from.
	 * @return The horizontal neighbors.
	 */
	public static List<Block> getHorizontalNeighbors(final Block block) {
		List<Block> result = new ArrayList<>();

		result.add(block.getRelative(BlockFace.NORTH));
		result.add(block.getRelative(BlockFace.EAST));
		result.add(block.getRelative(BlockFace.SOUTH));
		result.add(block.getRelative(BlockFace.WEST));

		return result;
	}

	/**
	 * Gets the horizontal liquid neighbors around a block.
	 *
	 * @param block    The block to check from.
	 * @param allowAir Whether to allow air.
	 * @return The horizontal liquid neighbors.
	 */
	public static List<Block> getHorizontalLiquidNeighbors(final Block block, final boolean allowAir) {
		return getHorizontalNeighbors(block).stream().filter(o -> isLiquid(o, allowAir)).collect(Collectors.toList());
	}

	/**
	 * Gets the liquid neighbors around a block.
	 *
	 * @param block    The block to check from.
	 * @param allowAir Whether to allow air.
	 * @return The liquid neighbors.
	 */
	public static List<Block> getLiquidNeighbors(final Block block, final boolean allowAir) {
		return getNeighbors(block).stream().filter(o -> isLiquid(o, allowAir)).collect(Collectors.toList());
	}

	/**
	 * Gets the neighbors around a block.
	 *
	 * @param block The block to check from.
	 * @return The neighbors.
	 */
	public static List<Block> getNeighbors(final Block block) {
		List<Block> result = new ArrayList<>();

		result.add(block.getRelative(BlockFace.UP));
		result.add(block.getRelative(BlockFace.DOWN));
		result.add(block.getRelative(BlockFace.NORTH));
		result.add(block.getRelative(BlockFace.EAST));
		result.add(block.getRelative(BlockFace.SOUTH));
		result.add(block.getRelative(BlockFace.WEST));

		return result;
	}

	/**
	 * Checks whether the specified block is a liquid.
	 *
	 * @param block    The block to check.
	 * @param allowAir Whether to allow air.
	 * @return Whether the block is a liquid.
	 */
	public static boolean isLiquid(final Block block, final boolean allowAir) {
		return isLiquid(block.getType(), allowAir);
	}

	/**
	 * Checks whether the specified material is a liquid.
	 *
	 * @param material The material to check.
	 * @param allowAir Whether to allow air.
	 * @return Whether the material is a liquid.
	 */
	public static boolean isLiquid(final Material material, final boolean allowAir) {
		return !(!allowAir && material == Material.AIR) && LiquidType.contains(material);
	}

	/**
	 * Determines the liquid level of the specified block, where MINIMUM_LIQUID_LEVEL is the lowest level (air/empty) and MAXIMUM_LIQUID_LEVEL the highest (full).
	 *
	 * @param block The block to check.
	 * @return The specified block's liquid level.
	 * @throws Exception When the specified block is not a liquid.
	 */
	public static int getLiquidLevel(Block block) throws Exception {
		// Check if the block is air
		if (block.isEmpty()) {
			// If so, the level is MINIMUM_LIQUID_LEVEL
			return MINIMUM_LIQUID_LEVEL;
		} else {
			if (!isLiquid(block, false)) {
				throw (new Exception("Block \"" + block.toString() + "\" is not a liquid"));
			}

			// Get block information
			int rawLiquidLevel = block.getData();
			boolean isFalling = rawLiquidLevel >= MAXIMUM_LIQUID_LEVEL;

			// Check the liquid level
			int level;
			if (isFalling) {
				// If the block is a source block or a falling block, the liquid level is the maximum
				level = MAXIMUM_LIQUID_LEVEL;
			} else {
				// Otherwise, calculate the liquid level
				level = MAXIMUM_LIQUID_LEVEL - rawLiquidLevel;
			}

			return level;
		}
	}

	@Override
	public void onDisable() {
	}

	@Override
	public void onEnable() {
		getLogger().info("Registering events...");

		// Register event listeners
		getServer().getPluginManager().registerEvents(new EventHandler(this), this);

		getLogger().info("Registering scheduled tasks...");

		// Register scheduled task
		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			// First copy all current blocks, so as to loop through all blocks once per tick
			Queue<Block> liquidBlocksToHandle = new LinkedList<>(liquidBlocks);

			// Process all blocks
			while (!liquidBlocksToHandle.isEmpty()) {
				Block block = liquidBlocksToHandle.remove();
				liquidBlocks.remove();

				// Check if the block can still flow
				if (canFlow(block)) {
					// If so, handle it
					try {
						handleLiquidBlock(block);
					} catch (Exception e) {
						// This should not happen
						e.printStackTrace();
					}
				}
			}
		}, TICKS_PER_FLOW, TICKS_PER_FLOW);
	}

	public void addBlock(final Block block) {
		// Add block and neighbors
		if (isLiquid(block, false) && !liquidBlocks.contains(block)) {
			liquidBlocks.add(block);

			// Add all neighbors which are liquids
			liquidBlocks.addAll(getNeighbors(block).stream().filter(o -> isLiquid(o, false) && !liquidBlocks.contains(o)).collect(Collectors.toList()));
		}
	}

	/**
	 * Handles the specified liquid block.
	 *
	 * @param block The block to handle.
	 * @throws Exception When the specified block cannot flow.
	 */
	private void handleLiquidBlock(Block block) throws Exception {
		if (!canFlow(block)) {
			throw (new Exception("Block \"" + block.toString() + "\" cannot flow"));
		}

		// First, move as much water as possible downward
		if (flowDown(block)) {
			return;
		}

		// Now divide the remaining water over the direct neighbors
		if (flowHorizontally(block)) {
			return;
		}

		// Now perform a flood fill algorithm, if the water wasn't completely divided yet
		// TODO: Check if the cell below is full, and if so teleport the water to simulate pressure if there is an open air location lower and connected
	}

	/**
	 * Flows as much water as possible downward.
	 *
	 * @param block The block to flow down from.
	 * @return A boolean telling us if we're done.
	 * @throws Exception If the specified block is not a liquid.
	 */
	public boolean flowDown(Block block) throws Exception {
		Block downBlock = block.getRelative(BlockFace.DOWN);

		// Check if there is room
		if (isSameLiquid(block, downBlock) && canAcceptWater(downBlock)) {
			// Calculate how much room
			int availableRoom = MAXIMUM_LIQUID_LEVEL - getLiquidLevel(downBlock);

			// Check if we can move everything
			if (availableRoom >= getLiquidLevel(block)) {
				// If so, move everything and stop
				flowLiquidFromTo(block, downBlock, getLiquidLevel(block));

				return true;
			} else {
				// If not, move what fits
				flowLiquidFromTo(block, downBlock, availableRoom);
			}
		}

		return false;
	}

	/**
	 * Flows as much water as possible to the horizontal neighbors.
	 *
	 * @param block The blow to flow from.
	 * @return A boolean telling us if we're done.
	 * @throws Exception If the specified block is not a liquid.
	 */
	public boolean flowHorizontally(Block block) throws Exception {
		/*int totalLiquidLevel = 0;
		List<Block> toDivide = new ArrayList<>();
		toDivide.add(block);
		toDivide.addAll(getHorizontalLiquidNeighbors(block, true));

		for (Block divideBlock : toDivide) {
			// Add its liquid level
			totalLiquidLevel += getLiquidLevel(divideBlock);
		}

		// Calculate the liquid level for each block and the remainder
		int liquidLevelPerBlock = totalLiquidLevel / toDivide.size();
		int remainingLiquidLevel = totalLiquidLevel % toDivide.size();

		// Divide liquid
		LiquidType liquidType = LiquidType.fromBlock(block);
		for (Block divideBlock : toDivide) {
			// Calculate the amount of remainder to add to this block
			int remainderToAdd = 0;
			if (remainingLiquidLevel > 0) {
				// If we can add some remainder, do so
				remainderToAdd = Math.min(MAXIMUM_LIQUID_LEVEL - liquidLevelPerBlock, remainingLiquidLevel);
				remainingLiquidLevel -= remainderToAdd;
			}

			setLiquidLevel(divideBlock, liquidLevelPerBlock + remainderToAdd, liquidType);
		}*/

		while (true) {
			// Get neighbors with lowest liquid level
			List<Block> destinationBlocks = getLowestLiquidLevel(getHorizontalLiquidNeighbors(block, true), LiquidType.fromBlock(block));

			// Check if there is one
			if (destinationBlocks.size() > 0) {
				// If so, pick a random one
				Random random = new Random();
				Block destinationBlock = destinationBlocks.get(random.nextInt(destinationBlocks.size()));

				// Check if we can flow to it
				int liquidLevelDifference = getLiquidLevel(block) - getLiquidLevel(destinationBlock);
				if (liquidLevelDifference > 1) {
					// If there is more than 1 difference in between the levels, simply flow and continue
					flowLiquidFromTo(block, destinationBlock, 1);
				} else if (liquidLevelDifference == 1) {
					// If there is exactly one level difference, we couldn't equalize the water so we're not done
					return false;
				} else {
					// If the other case the level difference was 0, which means we've successfully equalized the water so we're done
					return true;
				}
			} else {
				// If not, we're not done
				return false;
			}
		}
	}

	/**
	 * Flows liquid from the specified source to the specified target.
	 *
	 * @param sourceBlock The source block.
	 * @param targetBlock The target block.
	 * @param amount      The amount of liquid to flow.
	 * @throws Exception If the source can't flow or the target block can't receive.
	 */
	public void flowLiquidFromTo(Block sourceBlock, Block targetBlock, final int amount) throws Exception {
		if (!isLiquid(sourceBlock, false)) {
			throw (new Exception("Source block \"" + sourceBlock.toString() + "\" is not a liquid"));
		}

		if (!targetBlock.isEmpty() && LiquidType.fromMaterial(targetBlock.getType()) != LiquidType.fromMaterial(sourceBlock.getType())) {
			throw (new Exception("Target block \"" + targetBlock.toString() + "\" is not empty, not a liquid or not the correct liquid"));
		}

		// Transact liquid
		LiquidType liquidType = LiquidType.fromBlock(sourceBlock);
		lowerLiquidLevel(sourceBlock, amount, liquidType);
		raiseLiquidLevel(targetBlock, amount, liquidType);
	}

	/**
	 * Sets the liquid level of the specified block, where MINIMUM_LIQUID_LEVEL is the lowest level (air/empty) and MAXIMUM_LIQUID_LEVEL the highest (full).
	 *
	 * @param block      The block to set.
	 * @param level      The level to set.
	 * @param liquidType The liquid type to set the block to.
	 * @throws Exception If the block is not a liquid, or if the liquid level is invalid.
	 */
	public void setLiquidLevel(Block block, final int level, @Nullable final LiquidType liquidType) throws Exception {
		// Only do something if necessary
		if (getLiquidLevel(block) != level) {
			if (level < MINIMUM_LIQUID_LEVEL || level > MAXIMUM_LIQUID_LEVEL) {
				throw (new Exception("Invalid liquid level \"" + level + "\""));
			}

			if (level == MINIMUM_LIQUID_LEVEL) {
				// The block should be air
				block.setType(Material.AIR);
			} else {
				// Check if the block is air
				if (block.isEmpty()) {
					// Check if the target type is non-null
					if (liquidType == null) {
						throw (new Exception("Cannot convert target block \"" + block.toString() + "\" to liquid since no target liquid type is specified"));
					}

					// If the block is air, change it to the correct target type
					block.setType(liquidType.getValue());
				}

				// Stabilize block type
				block.setType(LiquidType.fromBlock(block).getValue());

				// If so, simply change the liquid level
				block.setData((byte) (MAXIMUM_LIQUID_LEVEL - level));

				// Add to handle list if not there
				addBlock(block);
			}
		}
	}

	/**
	 * Raises the target block's liquid level by the specified amount.
	 *
	 * @param block      The block to raise.
	 * @param liquidType The liquid type to set the block to.
	 * @param amount     The amount to raise.
	 * @throws Exception If the block is not a liquid.
	 */
	public void raiseLiquidLevel(Block block, final int amount, final LiquidType liquidType) throws Exception {
		setLiquidLevel(block, getLiquidLevel(block) + amount, liquidType);
	}

	/**
	 * Lowers the target block's liquid level by the specified amount.
	 *
	 * @param block      The block to lower.
	 * @param liquidType The liquid type to set the block to.
	 * @param amount     The amount to lower.
	 * @throws Exception If the block is not a liquid.
	 */
	public void lowerLiquidLevel(Block block, final int amount, final LiquidType liquidType) throws Exception {
		setLiquidLevel(block, getLiquidLevel(block) - amount, liquidType);
	}
}
