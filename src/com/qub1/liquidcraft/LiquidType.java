package com.qub1.liquidcraft;

import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * The Material types of liquids.
 */
public enum LiquidType {
	WATER(Material.STATIONARY_WATER),
	LAVA(Material.STATIONARY_LAVA);

	private final Material value;

	/**
	 * Creates a new LiquidType.
	 *
	 * @param value The value to use.
	 */
	LiquidType(final Material value) {
		this.value = value;
	}

	/**
	 * Checks if the LiquidType contains the specified block.
	 *
	 * @param block The block to look for.
	 * @return Whether the liquid type contains the specified block.
	 */
	public static boolean contains(final Block block) {
		return contains(block.getType());
	}

	/**
	 * Checks if the LiquidType contains the specified material.
	 *
	 * @param material The material to look for.
	 * @return Whether the liquid type contains the specified material.
	 */
	public static boolean contains(final Material material) {
		if (material == Material.AIR || material == Material.WATER || material == Material.LAVA) {
			return true;
		}

		for (LiquidType liquidType : values()) {
			if (liquidType.getValue() == material) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Gets the current value of the LiquidType.
	 *
	 * @return The current value.
	 */
	public Material getValue() {
		return value;
	}

	/**
	 * Gets the LiquidType associated with the specified block.
	 *
	 * @param block The block to associate with a LiquidType.
	 * @return The associated LiquidType.
	 * @throws Exception If the block is not convertible to a LiquidType.
	 */
	public static LiquidType fromBlock(final Block block) throws Exception {
		return fromMaterial(block.getType());
	}

	/**
	 * Gets the LiquidType associated with the specified material.
	 *
	 * @param material The material to associate with a LiquidType.
	 * @return The associated LiquidType.
	 * @throws Exception If the material is not convertible to a LiquidType.
	 */
	public static LiquidType fromMaterial(final Material material) throws Exception {
		if (material == Material.WATER) {
			return LiquidType.WATER;
		} else if (material == Material.LAVA) {
			return LiquidType.LAVA;
		}

		for (LiquidType liquidType : values()) {
			if (liquidType.getValue() == material) {
				return liquidType;
			}
		}

		throw (new Exception("Invalid material, \"" + material.toString() + "\" cannot be converted to a liquid"));
	}
}
