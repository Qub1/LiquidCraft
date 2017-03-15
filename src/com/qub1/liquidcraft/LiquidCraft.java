package com.qub1.liquidcraft;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class LiquidCraft extends JavaPlugin implements Listener {
	/**
	 * The amount of blocks that have to be connected to a block for it to be an infinite source.
	 */
	private static final int INFINITE_SOURCE_SIZE = 500000;
	/**
	 * The minimum amount of open air blocks connected to a target block for it to be considered as flowable (this is to simulate pressure).
	 */
	private static final int FLOW_MINIMUM_OPEN_AIR = 30;

	/**
	 * The amount of liquid levels to flow per loop.
	 */
	private static final int FLOW_SPEED = 1;

	/**
	 * All active liquid blocks, cached.
	 */
	private List<Block> liquidBlocks = new ArrayList<>();

	@Override
	public void onDisable() {
	}

	@Override
	public void onEnable() {
		getLogger().info("Registering events...");

		// Register event listeners
		getServer().getPluginManager().registerEvents(this, this);

		/*getLogger().info("Loading liquid blocks...");

		// Now cache all currently loaded liquid blocks
		// Loop all worlds
		for (World world : getServer().getWorlds()) {
			getLogger().info("Loading liquid blocks in \"" + world.getName() + "\"...");

			// Loop all loaded chunks
			for (Chunk chunk : world.getLoadedChunks()) {
				loadChunkBlocks(chunk);
			}
		}*/

		getLogger().info("Registering scheduled tasks...");

		// Register scheduled task
		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			// First copy all current blocks, so as to loop through all blocks once per tick
			List<Block> liquidBlocksToHandle = new ArrayList<>(liquidBlocks);

			// Process all blocks
			for (Block block : liquidBlocksToHandle) {
				try {
					handleBlock(block);
				} catch (Exception e) {
					e.printStackTrace();
					getLogger().warning("Could not process block: " + block.toString());
					liquidBlocks.remove(block);
				}
			}
		}, 1l, 1l);
	}

	@EventHandler
	public void onChunkLoadEvent(ChunkLoadEvent event) {
		//loadChunkBlocks(event.getChunk());
	}

	@EventHandler
	public void onChunkUnloadEvent(ChunkUnloadEvent event) {
		//unloadChunkBlocks(event.getChunk());
	}

	/**
	 * Loads all liquid blocks in the specified chunk.
	 *
	 * @param chunk The chunk to load.
	 */
	private void loadChunkBlocks(Chunk chunk) {
		// Loop all locations in the chunk
		for (int x = 0; x < 16; ++x) {
			for (int y = 0; y < 128; ++y) {
				for (int z = 0; z < 16; ++z) {
					// Get the block at the current location
					Block block = chunk.getBlock(x, y, z);

					// Handle the block
					if (isLiquid(block)) {
						liquidBlocks.add(block);
					}
				}
			}
		}
	}

	private void unloadChunkBlocks(Chunk chunk) {
		// Loop all locations in the chunk
		for (int x = 0; x < 16; ++x) {
			for (int y = 0; y < 128; ++y) {
				for (int z = 0; z < 16; ++z) {
					// Get the block at the current location
					Block block = chunk.getBlock(x, y, z);

					// Remove the block
					if (isLiquid(block)) {
						liquidBlocks.remove(block);
					}
				}
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

	@EventHandler
	public void onBlockFromToEvent(BlockFromToEvent event) {
		// Cancel any original liquid spread
		if (isLiquid(event.getBlock())) {
			event.setCancelled(true);

			if (!liquidBlocks.contains(event.getBlock())) {
				liquidBlocks.add(event.getBlock());
			}
		}
	}

	@EventHandler
	public void onBlockPlaceEvent(BlockPlaceEvent event) {
		if (isLiquid(event.getBlock()) && !liquidBlocks.contains(event.getBlock())) {
			liquidBlocks.add(event.getBlock());
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

		// Flow the specified amount of blocks
		for (int i = 0; i < FLOW_SPEED; ++i) {
			// Store destination block
			Block destination;

			if(getLiquidLevel(block.getRelative(BlockFace.DOWN)) < getLiquidLevel(block)) {

			}

			if (destination == null || (getLiquidLevel(block) - getLiquidLevel(destination)) < 2) {
				// If the destination doesn't exit or if it is too high to flow to, stop
				break;
			} else {
				// If not, flow
				flowFromTo(block, destination, 1);
			}
		}
	}

	/**
	 * Gets the neighbor with the lowest liquid level.
	 *
	 * @param block The block to check around.
	 * @return The neighbor with the lowest liquid level, or null if there is none.
	 */
	private Block getLowestLiquidLevelNeighbor(Block block) {
		int lowestLiquidLevel = -1;
		Block result = null;

		// Loop neighbors and try to find the lowest one
		for (Block neighborBlock : getNeighbors(block)) {
			// Try to get the neighbor's liquid level
			try {
				int neighborLiquidLevel = getLiquidLevel(neighborBlock);

				if (lowestLiquidLevel == -1 || neighborLiquidLevel < lowestLiquidLevel) {
					lowestLiquidLevel = neighborLiquidLevel;
					result = neighborBlock;
				}
			} catch (Exception e) {
				// Ignore
			}
		}

		return result;
	}

	/**
	 * Checks whether the liquid level of the specified block is at least the specified amount higher than all of its neighbors.
	 *
	 * @param block  The block to check.
	 * @param amount The amount the block needs to be higher than its neighbors.
	 * @return Whether the block is at least the specified amount higher than all its neighbors.
	 * @throws Exception
	 */
	private boolean isLiquidLevelHigherThanNeighbors(Block block, int amount) throws Exception {
		int liquidLevel = getLiquidLevel(block);

		// Loop neighbors
		for (Block neighborBlock : getNeighbors(block)) {
			// Attempt to get the neighbors liquid level
			try {
				// If the neighbor isn't at least the correct amount of levels lower, return false
				if (getLiquidLevel(neighborBlock) > liquidLevel - amount) {
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
					if (x != block.getX() && y != block.getY() && z != block.getZ()) {
						result.add(neighborBlock);
					}
				}
			}
		}

		return result;
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

		if (!targetBlock.isEmpty() && LiquidType.fromMaterial(targetBlock.getType()) != LiquidType.fromMaterial(sourceBlock.getType())) {
			throw (new Exception("Target block is not empty, not a liquid or not the correct liquid"));
		}

		// Transact liquid
		raiseLiquidLevel(targetBlock, amount, sourceBlock.getType());
		lowerLiquidLevel(sourceBlock, amount, sourceBlock.getType());
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
			// Remove
			liquidBlocks.remove(block);

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
				// If so, simply change the liquid level
				byte rawLiquidLevel;
				if (level > 8) {
					rawLiquidLevel = 0;
				} else {
					rawLiquidLevel = (byte) (8 - level);
				}
				block.setData(rawLiquidLevel);
			} else {
				// If the block is not air or the target type, throw an exception
				throw (new Exception("Block is not of the correct liquid type"));
			}

			// Add to handle list if not there
			if(!liquidBlocks.contains(block)) {
				liquidBlocks.add(block);
			}
		}

		// Finally, store the metadata
		block.setMetadata("Liquid level", new FixedMetadataValue(this, level));
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

			// Check if the block already has a level
			if (!block.hasMetadata("Liquid level")) {
				// If not, generate it

				// Get block information
				int rawLiquidLevel = block.getData();
				boolean isFalling = rawLiquidLevel >= 8;

				// Check the liquid level
				int level;
				if (isFalling) {
					// If the block is a source block or a falling block, the liquid level is the maximum
					level = 8;
				} else {
					// Otherwise, calculate the liquid level
					level = 8 - rawLiquidLevel;
				}

				// Store it
				block.setMetadata("Liquid level", new FixedMetadataValue(this, level));
			}

			// Retrieve the stored liquid level
			return block.getMetadata("Liquid level").get(0).asInt();
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

		public static boolean contains(Material value) {
			if (value == Material.STATIONARY_WATER || value == Material.STATIONARY_LAVA) {
				return true;
			}

			for (LiquidType liquidType : values()) {
				if (liquidType.getValue() == value) {
					return true;
				}
			}

			return false;
		}

		public Material getValue() {
			return value;
		}

		public static LiquidType fromMaterial(Material value) throws Exception {
			if (value == Material.STATIONARY_WATER) {
				return LiquidType.WATER;
			} else if (value == Material.STATIONARY_LAVA) {
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
