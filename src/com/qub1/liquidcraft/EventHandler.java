package com.qub1.liquidcraft;

import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

public class EventHandler implements Listener {
	/**
	 * The corresponding plugin.
	 */
	private LiquidCraft plugin;

	/**
	 * Creates a new EventHandler.
	 *
	 * @param plugin The plugin to use.
	 */
	public EventHandler(LiquidCraft plugin) {
		this.plugin = plugin;
	}

	@org.bukkit.event.EventHandler
	public void onBlockFromToEvent(BlockFromToEvent event) {
		// Cancel any original liquid spread and add any spreading liquids to the list
		final Block block = event.getBlock();
		if (LiquidCraft.isLiquid(block, false)) {
			event.setCancelled(true);
		}

		plugin.addBlock(block);
	}

	@org.bukkit.event.EventHandler
	public void onBlockPlaceEvent(final BlockPlaceEvent event) {
		// Add any placed liquids to the handle list
		plugin.addBlock(event.getBlock());
	}

	@org.bukkit.event.EventHandler
	public void onPlayerBucketEmptyEvent(final PlayerBucketEmptyEvent event) {
		// Add any liquids from buckets to the handle list
		plugin.addBlock(event.getBlockClicked().getRelative(event.getBlockFace()));
	}

	@org.bukkit.event.EventHandler
	public void onBlockPhysicsEvent(BlockPhysicsEvent event) {
		// Cancel any liquid physics, since we handle those ourselves
		final Block block = event.getBlock();
		if (LiquidCraft.isLiquid(block, false)) {
			event.setCancelled(true);
		}

		plugin.addBlock(block);
	}

	@org.bukkit.event.EventHandler
	public void onBlockSpreadEvent(BlockSpreadEvent event) {
		// Cancel any liquid spreading, since we handle those ourselves
		final Block block = event.getBlock();
		if (LiquidCraft.isLiquid(block, false)) {
			event.setCancelled(true);
		}

		plugin.addBlock(block);
	}
}
