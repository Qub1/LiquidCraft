package com.qub1.liquidcraft;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class LiquidCraft extends JavaPlugin implements Listener {
	/**
	 * The amount of blocks to handle each loop.
	 */
	private static final int BLOCKS_PER_LOOP = 200;
	/**
	 * The amount of blocks that have to be connected to a block for it to be an infinite source.
	 */
	private static final int INFINITE_SOURCE_SIZE = 500000;
	/**
	 * The minimum amount of open air blocks connected to a target block for it to be considered as flowable (this is to simulate pressure).
	 */
	private static final int FLOW_MINIMUM_OPEN_AIR = 30;

	/**
	 * The amount of water levels to flow per loop.
	 */
	private static final int FLOW_SPEED = 1;

	/**
	 * The blocks that need to be handled in the next tick.
	 */
	private Queue<Block> blocksToHandle = new LinkedList<>();

	@Override
	public void onDisable() {
		// When disabling, make sure to handle all remaining blocks
		while (!blocksToHandle.isEmpty()) {
			try {
				handleBlock(blocksToHandle.remove());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onEnable() {
		// Register event listeners
		getServer().getPluginManager().registerEvents(this, this);

		// Register scheduled task
		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			int blocksToHandleCount = blocksToHandle.size() < BLOCKS_PER_LOOP ? blocksToHandle.size() : BLOCKS_PER_LOOP;

			// Handle blocks this loop
			for (int i = 0; i < blocksToHandleCount; ++i) {
				// Remove block
				Block currentBlock = blocksToHandle.remove();

				// Check if the block is still a liquid
				if(isLiquid(currentBlock)) {
					try {
						// If so, handle it
						handleBlock(currentBlock);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}, 1l, 1l); // TODO: Set duration to one tick
	}

	@EventHandler
	public void onBlockFromToEvent(BlockFromToEvent event) {
		if (isLiquid(event.getBlock())) {
			event.setCancelled(true);

			blocksToHandle.add(event.getBlock());
		}
	}

	/**
	 * Handles the specified block.
	 *
	 * @param block The block to handle.
	 * @throws Exception
	 */
	private void handleBlock(Block block) throws Exception {
		if (!isLiquid(block)) {
			throw (new Exception("Block is not a liquid"));
		}

		System.out.println("0");

		// Flow the specified amount of blocks
		for (int i = 0; i < FLOW_SPEED; ++i) {
			// Only flow if we can flow without disrupting the balance
			if (isLiquidLevelHigherThanHorizontalNeighbors(block, 2)) {
				flowFromTo(block, getNearestDestination(block), 1);
			} else {
				// If not, stop flowing
				break;
			}
		}
	}

	/**
	 * Gets the nearest destination blow for the liquid in the source block to flow to.
	 *
	 * @param block The source block.
	 * @return The nearest destination block.
	 */
	private Block getNearestDestination(Block block) throws Exception {
		// Get the liquid type of the source
		LiquidType sourceLiquidType = LiquidType.fromMaterial(block.getType());

		// Relative blocks
		Block northBlock = block.getRelative(BlockFace.NORTH);
		Block northEastBlock = block.getRelative(BlockFace.NORTH_EAST);
		Block eastBlock = block.getRelative(BlockFace.NORTH);
		Block southEastBlock = block.getRelative(BlockFace.SOUTH_EAST);
		Block southBlock = block.getRelative(BlockFace.NORTH);
		Block southWestBlock = block.getRelative(BlockFace.SOUTH_WEST);
		Block westBlock = block.getRelative(BlockFace.NORTH);
		Block northWestBlock = block.getRelative(BlockFace.NORTH_WEST);

		// First, check if the block is on a liquid of the same type
		Block downBlock = block.getRelative(BlockFace.DOWN);
		if (isLiquid(downBlock) && LiquidType.fromMaterial(downBlock.getType()) == sourceLiquidType) {
			// If so, find all connected blocks that are lower and work upwards layer by layer until air is found
			// Then find the closest connected air block and return it as destination
			return block;
		} else {
			// If not, simply keep spreading over to the lowest neighbor while there is water to spread
			return getLowestLiquidLevelHorizontalNeighbor(block);
		}
	}

	/**
	 * Gets the horizontal neighbor with the lowest liquid level.
	 *
	 * @param block The block to check around.
	 * @return The neighbor with the lowest liquid level, or null if there is none.
	 */
	private Block getLowestLiquidLevelHorizontalNeighbor(Block block) {
		int lowestLiquidLevel = -1;
		Block result = null;

		// Loop neighbors and try to find the lowest one
		for (Block neighborBlock : getHorizontalNeighbors(block)) {
			System.out.println("ae");
			// Try to get the neighbor's liquid level
			try {
				int neighborLiquidLevel = getLiquidLevel(neighborBlock);

				if (lowestLiquidLevel == -1 || neighborLiquidLevel < lowestLiquidLevel) {
					System.out.println("4");
					lowestLiquidLevel = neighborLiquidLevel;
					result = neighborBlock;
				}
			} catch (Exception e) {
				// Ignore
			}
		}

		System.out.println("adsf");
		return result;
	}

	/**
	 * Checks whether the liquid level of the specified block is at least the specified amount higher than all of its horizontal neighbors.
	 *
	 * @param block  The block to check.
	 * @param amount The amount the block needs to be higher than its neighbors.
	 * @return Whether the block is at least the specified amount higher than all its neighbors.
	 * @throws Exception
	 */
	private boolean isLiquidLevelHigherThanHorizontalNeighbors(Block block, int amount) throws Exception {
		int liquidLevel = getLiquidLevel(block);

		// Loop neighbors
		for (Block neighborBlock : getHorizontalNeighbors(block)) {
			// Attempt to get the neighbors liquid level
			try {
				// If the neighbor isn't at least the correct amount of levels lower, return false
				if (getLiquidLevel(neighborBlock) > liquidLevel - amount) {
					System.out.println(neighborBlock.getLocation().toString());
					System.out.println(block.getLocation().toString());
					return false;
				}
			} catch (Exception e) {
				// Ignore
			}
		}

		// If we're here, everything went well
		return true;
	}

	/**
	 * Gets the horizontal neighbors of the specified block.
	 *
	 * @param block The block whose horizontal neighbors to get.
	 * @return The horizontal neighbors of the specified block.
	 */
	private List<Block> getHorizontalNeighbors(Block block) {
		List<Block> result = new ArrayList<>();

		// Loop in a rectangle
		for (int x = block.getX() - 1; x <= block.getX() + 1; ++x) {
			for (int z = block.getZ() - 1; z <= block.getZ() + 1; ++z) {
				Block neighborBlock = block.getWorld().getBlockAt(x, block.getY(), z);

				// Skip self
				if(x != block.getX() && z != block.getZ()) {
					result.add(neighborBlock);
				}
			}
		}

		return result;
	}

	/**
	 * Gets all neighbors of the specified block.
	 *
	 * @param block The block whose neighbors to get.
	 * @return The neighbors of the specified block.
	 */
	private List<Block> getNeighbors(Block block) {
		List<Block> result = new ArrayList<>();

		// Loop in a cube around the block
		for (int x = block.getX() - 1; x <= block.getX() + 1; ++x) {
			for (int y = block.getY() - 1; y <= block.getY() + 1; ++y) {
				for (int z = block.getZ() - 1; z <= block.getZ() + 1; ++z) {
					Block neighborBlock = block.getWorld().getBlockAt(x, block.getY(), z);

					// Skip self
					if(x != block.getX() && y != block.getY() && z != block.getZ()) {
						result.add(neighborBlock);
					}
				}
			}
		}

		return result;
	}

	/**
	 * Checks whether the specified block is part of an "infinite" body of liquid.
	 *
	 * @param block The block to check.
	 * @return Whether the block is part of an "infinite" body of liquid.
	 */
	private boolean isInfinite(Block block) {
		if (isLiquid(block)) {
			// The blocks that we've handled already
			List<Block> handledBlocks = new ArrayList<>();
			Stack<Block> blocksToHandle = new Stack<>();

			// Add starting block
			blocksToHandle.push(block);

			while (blocksToHandle.size() > 0 && handledBlocks.size() < INFINITE_SOURCE_SIZE) {
				Block currentBlock = blocksToHandle.pop();

				// First set block as handled and increase connected
				handledBlocks.add(currentBlock);

				// Loop neighbors
				for (Block neighborBlock : getNeighbors(currentBlock)) {
					// Add to blocks to handle if not yet handled and correct type
					if (!handledBlocks.contains(neighborBlock) && neighborBlock.getType() == block.getType()) {
						blocksToHandle.add(neighborBlock);
					}
				}
			}

			return handledBlocks.size() >= INFINITE_SOURCE_SIZE;
		} else {
			return false;
		}
	}

	/**
	 * Flows liquid from the specified source to the specified target.
	 *
	 * @param sourceBlock The source block.
	 * @param targetBlock The target block.
	 * @param amount      The amount of liquid to flow.
	 * @throws Exception
	 */
	private void flowFromTo(Block sourceBlock, Block targetBlock, int amount) throws Exception {
		if (!isLiquid(sourceBlock)) {
			throw (new Exception("Source block is not a liquid"));
		}

		// Check if a block is infinite
		if (isInfinite(sourceBlock)) {
			// If the source is infinite, only raise the target
			raiseLiquidLevel(targetBlock, amount, sourceBlock.getType());
		} else if (isInfinite(targetBlock)) {
			// If the target is infinite, only lower the source
			lowerLiquidLevel(sourceBlock, amount, sourceBlock.getType());
		} else {
			// If both are finite, do a normal transaction
			raiseLiquidLevel(targetBlock, amount, sourceBlock.getType());
			lowerLiquidLevel(sourceBlock, amount, sourceBlock.getType());
		}
	}

	/**
	 * @see com.qub1.liquidcraft.LiquidCraft#setLiquidLevel(Block, int, Material)
	 */
	private void setLiquidLevel(Block block, int level) throws Exception {
		setLiquidLevel(block, level, null);
	}

	/**
	 * Sets the liquid level of the specified block, where 0 is the lowest level (air/empty) and 8 the highest (full).
	 *
	 * @param block      The block to set.
	 * @param level      The level to set.
	 * @param liquidType The liquid type to set the block to.
	 * @throws Exception
	 */
	private void setLiquidLevel(Block block, int level, @Nullable Material liquidType) throws Exception {
		if (level == 0) {
			// The block should be air
			block.setType(Material.AIR);
		} else {
			// Check if the block is air
			if (block.isEmpty()) {
				// Check if the target type is non-null
				if (liquidType == null) {
					throw (new Exception("Cannot convert target block to liquid since no target liquid type is specified"));
				}

				// Then, check if the target type is a liquid
				if (!isLiquid(liquidType)) {
					throw (new Exception("Target material is not a liquid"));
				}

				// If the block is air, change it to the correct target type
				block.setType(liquidType);
			}

			// Now check if the block's type is a liquid
			if (isLiquid(block)) {
				// If so, simply change the water level
				block.setData((byte) (8 - level));
			} else {
				// If the block is not air or the target type, throw an exception
				throw (new Exception("Block is not of the correct liquid type"));
			}
		}
	}

	/**
	 * Checks whether the specified block is a liquid.
	 *
	 * @param block The block to check.
	 * @return Whether the block is a liquid.
	 */
	private boolean isLiquid(Block block) {
		return isLiquid(block.getType());
	}

	/**
	 * Checks whether the specified material is a liquid.
	 *
	 * @param material The material to check.
	 * @return Whether the material is a liquid.
	 */
	private boolean isLiquid(Material material) {
		return LiquidType.contains(material);
	}

	/**
	 * @see com.qub1.liquidcraft.LiquidCraft#raiseLiquidLevel(Block, int, Material)
	 */
	private void raiseLiquidLevel(Block block, int amount) throws Exception {
		raiseLiquidLevel(block, amount, null);
	}

	/**
	 * Raises the target block's liquid level by the specified amount.
	 *
	 * @param block      The block to raise.
	 * @param liquidType The liquid type to set the block to.
	 * @param amount     The amount to raise.
	 * @throws Exception
	 */
	private void raiseLiquidLevel(Block block, int amount, Material liquidType) throws Exception {
		setLiquidLevel(block, getLiquidLevel(block) + amount, liquidType);
	}

	/**
	 * Determines the liquid level of the specified block, where 0 is the lowest level (air/empty) and 8 the highest (full).
	 *
	 * @param block The block to check.
	 * @return The specified block's liquid level.
	 * @throws Exception
	 */
	private int getLiquidLevel(Block block) throws Exception {
		// Check if the block is air
		if (block.isEmpty()) {
			// If so, the level is 0
			return 0;
		} else {
			if (!isLiquid(block)) {
				throw (new Exception("Block is not a liquid"));
			}

			// Get block information
			int rawLiquidLevel = block.getData();
			boolean isFalling = rawLiquidLevel >= 8;

			// Check the water level
			if (isFalling) {
				// If the block is a source block or a falling block, the water level is the maximum
				return 8;
			} else {
				// Otherwise, calculate the water level
				return 8 - rawLiquidLevel;
			}
		}
	}

	/**
	 * @see com.qub1.liquidcraft.LiquidCraft#lowerLiquidLevel(Block, int, Material)
	 */
	private void lowerLiquidLevel(Block block, int amount) throws Exception {
		lowerLiquidLevel(block, amount, null);
	}

	/**
	 * Lowers the target block's liquid level by the specified amount.
	 *
	 * @param block      The block to lower.
	 * @param liquidType The liquid type to set the block to.
	 * @param amount     The amount to lower.
	 * @throws Exception
	 */
	private void lowerLiquidLevel(Block block, int amount, Material liquidType) throws Exception {
		setLiquidLevel(block, getLiquidLevel(block) - amount, liquidType);
	}

	/**
	 * The Material types of liquids.
	 */
	private enum LiquidType {
		WATER(Material.WATER),
		LAVA(Material.LAVA);

		private Material value;

		LiquidType(Material value) {
			this.value = value;
		}

		public Material getValue() {
			return value;
		}

		public static boolean contains(Material value) {
			if(value == Material.STATIONARY_WATER || value == Material.STATIONARY_LAVA) {
				return true;
			}

			for (LiquidType liquidType : values()) {
				if (liquidType.getValue() == value) {
					return true;
				}
			}

			return false;
		}

		public static LiquidType fromMaterial(Material value) throws Exception {
			if(value == Material.STATIONARY_WATER) {
				return LiquidType.WATER;
			} else if(value == Material.STATIONARY_LAVA) {
				return LiquidType.LAVA;
			}

			for (LiquidType liquidType : values()) {
				if (liquidType.getValue() == value) {
					return liquidType;
				}
			}

			throw (new Exception("Invalid material"));
		}
	}
}
