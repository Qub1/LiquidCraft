package com.qub1.liquidcraft.commandhandlers;

import com.qub1.liquidcraft.LiquidCraft;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

public class MakeInfiniteSourceCommand implements CommandExecutor {
	private LiquidCraft plugin;

	/**
	 * Creates a new MakeInfiniteSourceCommand.
	 *
	 * @param plugin The plugin to use.
	 */
	public MakeInfiniteSourceCommand(LiquidCraft plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] parameters) {
		if (sender instanceof Player) {
			Player player = (Player) sender;

			Block targetBlock = player.getTargetBlock((Set<Material>) null, 10);

			// Check if the target is a liquid
			if (LiquidCraft.isLiquid(targetBlock, false)) {
				// If so, toggle its infinite states
				plugin.setInfiniteLiquidSource(targetBlock, !plugin.isInfiniteLiquidSource(targetBlock));

				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
}
