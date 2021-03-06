package com.gpergrossi.aerogen.commands;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public class CommandTest implements ICommand {

	@Override
	public int compareTo(ICommand arg0) {
		return 0;
	}

	@Override
	public String getName() {
		return "test";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/test [player]";
	}

	@Override
	public List<String> getAliases() {
		List<String> aliases = new ArrayList<>();
		aliases.add("test");
		return aliases;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		WorldServer dim2 = DimensionManager.getWorld(2);
		
		EntityPlayer p = null;
		
		if (args.length > 1) throw new CommandException("Invalid arguments");
		if (args.length == 1) {
			p = server.getPlayerList().getPlayerByUsername(args[0]);
			if (p == null) new CommandException(args[0]+" is not online");
		}
		if (args.length == 0) {
			Entity e = sender.getCommandSenderEntity();
			if (e instanceof EntityPlayer) p = (EntityPlayer) e;
			else throw new CommandException("Command must be issued by a player");
		}

		System.out.println("From: "+p.getEntityWorld().getWorldType().getName());
		System.out.println("To: "+dim2.getWorldType().getName());
		
		p.setPortal(p.getPosition().add(2, 0, 0));
		p.changeDimension(2);
		
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return true;
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
		return new ArrayList<>();
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index) {
		if (index == 0) return true;
		return false;
	}

}
