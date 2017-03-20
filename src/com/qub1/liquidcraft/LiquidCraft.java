package com.qub1.liquidcraft;

import com.qub1.liquidcraft.commandhandlers.MakeInfiniteSourceCommand;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

// TODO: Add more event handlers
// TODO: Check for and remove redundant event handlers
// TODO: Divide liquid level over neighbors
// BUG: Sometimes liquids disappear when you remove the block next to them
// BUG: Infinite blocks won't flow if there is 1 difference (apparently)
// BUG: FlowToNearest gets slower if the body of water it is traversing is deeper

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
	 * How much liquid can flow from a block in one tick.
	 * The default is one entire block of liquid (8 levels).
	 */
	private static final int FLOW_RATE = 8;

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
	 * Checks whether the specified block can accept liquid.
	 *
	 * @param block The block to check.
	 * @return Whether the block can accept liquid.
	 */
	public static boolean canAcceptLiquid(Block block) {
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
			boolean belowFlowRate = getFlowed(block) < FLOW_RATE;
			boolean hasLiquid = getLiquidLevel(block) > MINIMUM_LIQUID_LEVEL;

			return isLiquid(block, false) && belowFlowRate && hasLiquid;
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

	/**
	 * Checks if liquid can flow from the source block to the target block.
	 *
	 * @param from The source block.
	 * @param to   The target block.
	 * @return Whether liquid can flow from the source block to the target block.
	 */
	public static boolean canFlowFromTo(Block from, Block to) {
		try {
			boolean isDownBlock = to.equals(from.getRelative(BlockFace.DOWN));
			boolean hasHorizontalPotential = getLiquidLevel(from) - getLiquidLevel(to) >= 2;

			return canFlow(from) && canAcceptLiquid(to) && isSameLiquid(from, to) && (isDownBlock || hasHorizontalPotential);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Checks if the specified block is an infinite liquid source.
	 *
	 * @param block The block to check.
	 * @return Whether the block is an infinite liquid source.
	 */
	public static boolean isInfiniteLiquidSource(Block block) {
		return block.hasMetadata("Infinite") && block.getMetadata("Infinite").get(0).asBoolean();
	}

	@Override
	public void onDisable() {
	}

	@Override
	public void onEnable() {
		getLogger().info("Registering events...");
		getServer().getPluginManager().registerEvents(new EventHandler(this), this);

		getLogger().info("Registering commands...");
		getCommand("makeinfinitesource").setExecutor(new MakeInfiniteSourceCommand(this));

		getLogger().info("Registering scheduled tasks...");
		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			getLogger().info("Processing " + new Integer(liquidBlocks.size()).toString() + " blocks...");

			// First copy all current blocks, so as to loop through all blocks once per tick
			Queue<Block> liquidBlocksToHandle = new LinkedList<>(liquidBlocks);

			// Process all blocks
			while (!liquidBlocksToHandle.isEmpty()) {
				Block block = liquidBlocksToHandle.remove();

				try {
					handleLiquidBlock(block);
				} catch (Exception e) {
					// This should not happen
					e.printStackTrace();
				}
			}
		}, TICKS_PER_FLOW, TICKS_PER_FLOW);
	}

	public void addBlock(final Block block) {
		// Add block and neighbors
		if (isLiquid(block, false) && !liquidBlocks.contains(block)) {
			liquidBlocks.add(block);

			// Add all neighbors which are liquids and equal height or higher than the current block
			liquidBlocks.addAll(getLiquidNeighbors(block, false).stream().filter(o -> !liquidBlocks.contains(o) && o.getY() >= block.getY()).collect(Collectors.toList()));
		}
	}

	/**
	 * Raises the amount of liquid that has flowed from the specified block.
	 *
	 * @param block  The block to set.
	 * @param amount The amount to raise the flowed value by.
	 */
	private void raiseFlowed(Block block, int amount) {
		setFlowed(block, getFlowed(block) + amount);
	}

	/**
	 * Lowers the amount of liquid that has flowed from the specified block.
	 *
	 * @param block  The block to set.
	 * @param amount The amount to lower the flowed value by.
	 */
	private void lowerFlowed(Block block, int amount) {
		setFlowed(block, getFlowed(block) - amount);
	}

	/**
	 * Sets the amount of liquid that has flowed from the specified block.
	 *
	 * @param block  The block to set.
	 * @param amount The amount that has flowed.
	 */
	public void setFlowed(Block block, int amount) {
		block.setMetadata("Flowed", new FixedMetadataValue(this, amount));
	}

	/**
	 * Gets the amount of liquid that has flowed from a block in the current tick.
	 *
	 * @param block The block to check.
	 * @return The amount of liquid that flowed from the specified block.
	 */
	public static int getFlowed(Block block) {
		if (block.hasMetadata("Flowed")) {
			return block.getMetadata("Flowed").get(0).asInt();
		} else {
			return 0;
		}
	}

	/**
	 * Handles the specified liquid block.
	 *
	 * @param block The block to handle.
	 * @throws Exception When the specified block cannot flow.
	 */
	private void handleLiquidBlock(Block block) throws Exception {
		// First, let's set the blocks flow rate to nothing
		if(isLiquid(block, false)) {
			setFlowed(block, 0);
		}

		// Check if the block can flow
		if (canFlow(block)) {
			// First, move as much liquid as possible downward
			if (!flowDown(block)) {
				// If we're not done, divide the remaining liquid over the direct neighbors
				if(!flowHorizontally(block)) {
					// If we're still not done, perform a flood fill algorithm
					flowToNearest(block);
				}
			}
		}

		// Check if anything flowed
		if(getFlowed(block) == 0) {
			// If not, remove the block from the handle list
			liquidBlocks.remove(block);
		}
	}

	/**
	 * Flows as much liquid as possible downward.
	 *
	 * @param block The block to flow down from.
	 * @return A boolean telling us if we're done.
	 * @throws Exception If the specified block is not a liquid.
	 */
	public boolean flowDown(Block block) throws Exception {
		Block downBlock = block.getRelative(BlockFace.DOWN);

		// Move as much as possible downward
		while (canFlowFromTo(block, downBlock)) {
			flowLiquidFromTo(block, downBlock, 1);
		}

		// Check if we're done
		return !canFlow(block);
	}

	/**
	 * Flows as much liquid as possible to the horizontal neighbors.
	 *
	 * @param block The blow to flow from.
	 * @return A boolean telling us if we're done.
	 * @throws Exception If the source block is not a liquid.
	 */
	public boolean flowHorizontally(Block block) throws Exception {
		while (true) {
			// Get neighbors with lowest liquid level
			List<Block> destinationBlocks = getLowestLiquidLevel(getHorizontalLiquidNeighbors(block, true), LiquidType.fromBlock(block));

			// Check if there is one
			if (destinationBlocks.size() > 0) {
				// If so, pick a random one
				Random random = new Random();
				Block destinationBlock = destinationBlocks.get(random.nextInt(destinationBlocks.size()));

				// Check if we can flow to it
				if (canFlowFromTo(block, destinationBlock)) {
					// If there is more than 1 difference in between the levels, simply flow and continue
					flowLiquidFromTo(block, destinationBlock, 1);
				} else {
					// If we can't, then we need to check if all liquid flowed or if there is some left
					// Get the difference in levels
					int liquidLevelDifference = getLiquidLevel(block) - getLiquidLevel(destinationBlock);

					// Check if we're done
					if (liquidLevelDifference == 0) {
						// If the level difference is 0 we've successfully equalized the liquid so we're done
						return true;
					} else {
						// If there is one or more levels difference we couldn't equalize the liquid so we're not done
						return false;
					}
				}
			} else {
				// If not, we're not done
				return false;
			}
		}
	}

	/**
	 * Flows the block to the nearest block that can receive its flow without disrupting the balance.
	 *
	 * @param block The block to flow.
	 * @throws Exception If the source block is not a liquid.
	 */
	public void flowToNearest(Block block) throws Exception {
		// List of blocks that have been handled already
		List<Block> handled = new ArrayList<>();

		// List of blocks to handle in the first iteration (all direct neighbors which are lower or as high as the start block)
		List<Block> toHandleNext = new ArrayList<>(getLiquidNeighbors(block, true).stream().filter(o -> o.getY() <= block.getY()).collect(Collectors.toList()));

		// The current layer
		int currentY = block.getY();

		// Keep looping while there are blocks left to process
		while (canFlow(block) && toHandleNext.size() > 0) {
			// We need to copy the latest Y for use in lambda expressions
			final int latestY = currentY;

			// Get all blocks that we'll handle in this iteration
			List<Block> toHandle = new ArrayList<>(toHandleNext.stream().filter(o -> o.getY() >= latestY).collect(Collectors.toList()));

			// Check if the current layer still has blocks
			if (toHandle.size() > 0) {
				// If so, remove all blocks that we're handling from the wait list
				toHandleNext.removeAll(toHandle);

				// Shuffle the list that needs to be handled
				// Since all blocks that need to be handled are direct neighbors of blocks which have been handled already, the flow distance remains the same
				Collections.shuffle(toHandle);

				// Handle all blocks in the current iteration
				while (canFlow(block) && toHandle.size() > 0) {
					// Get the next block to handle and remove it from the list
					Block currentBlock = toHandle.remove(toHandle.size() - 1);

					// Add the block to the handled list
					handled.add(currentBlock);

					// Check if the current block is an air block
					// We need to check this, or else we get a chain reaction of air blocks adding air blocks
					if (!currentBlock.isEmpty()) {
						// If not, it's safe to add neighbors
						// Add all neighbors which are not yet in any list and lower than the start block to the next iteration to be processed
						toHandleNext.addAll(getLiquidNeighbors(currentBlock, true).stream().filter(o -> o.getY() <= block.getY() && !toHandleNext.contains(o) && !handled.contains(o)).collect(Collectors.toList()));
					}

					// Finally, check if we can flow to the current block, and if so, flow as much as possible
					while (canFlowFromTo(block, currentBlock)) {
						flowLiquidFromTo(block, currentBlock, 1);
					}
				}
			} else {
				// If not, we move on to the next layer
				--currentY;
			}
		}
	}

	/**
	 * Flows liquid from the specified source to the specified target.
	 *
	 * @param sourceBlock The source block.
	 * @param targetBlock The target block.
	 * @param amount      The amount to flow.
	 * @throws Exception If the source can't flow or the target block can't receive.
	 */
	public void flowLiquidFromTo(Block sourceBlock, Block targetBlock, int amount) throws Exception {
		if (!isLiquid(sourceBlock, false)) {
			throw (new Exception("Source block \"" + sourceBlock.toString() + "\" is not a liquid"));
		}

		if (!targetBlock.isEmpty() && LiquidType.fromMaterial(targetBlock.getType()) != LiquidType.fromMaterial(sourceBlock.getType())) {
			throw (new Exception("Target block \"" + targetBlock.toString() + "\" is not empty, not a liquid or not the correct liquid"));
		}

		if (amount > FLOW_RATE - getFlowed(sourceBlock)) {
			throw (new Exception("Flow exceeds flow rate"));
		}

		// Transact liquid
		LiquidType liquidType = LiquidType.fromBlock(sourceBlock);
		lowerLiquidLevel(sourceBlock, amount, liquidType);
		raiseLiquidLevel(targetBlock, amount, liquidType);

		// Raise flow rate
		raiseFlowed(sourceBlock, amount);
	}

	/**
	 * Sets whether the specified block is an infinite liquid source.
	 *
	 * @param block The block to set.
	 * @param value Whether the block is an infinite liquid source.
	 */
	public void setInfiniteLiquidSource(Block block, boolean value) {
		block.setMetadata("Infinite", new FixedMetadataValue(this, value));
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
		// If the block is an infinite source, the level won't change
		if (getLiquidLevel(block) != level && !isInfiniteLiquidSource(block)) {
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
			}
		}

		// Add to handle list if not there
		addBlock(block);
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
