package com.winthier.worlds;

import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
final class WTPCommand implements CommandExecutor {
    final WorldsPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        final Player player = sender instanceof Player ? (Player)sender : null;
        Player target;
        String name;
        if (args.length == 1) {
            target = player;
            if (target == null) {
                sender.sendMessage("Player expected.");
                return true;
            }
            name = args[0];
        } else if (args.length == 2) {
            target = plugin.getServer().getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage("Player not found: " + args[0]);
                return true;
            }
            name = args[1];
        } else {
            return false;
        }   
        MyWorld myWorld = plugin.worldByName(name);
        Location loc = null;
        if (myWorld != null) {
            loc = myWorld.getSpawnLocation();
        } else {
            World world = plugin.getServer().getWorld(name);
            if (world != null) loc = world.getSpawnLocation();
        }
        if (loc == null) {
            sender.sendMessage(ChatColor.RED + "World not found: " + name);
            return true;
        }
        target.teleport(loc);
        sender.sendMessage(ChatColor.YELLOW + "Teleported " + target.getName()
                           + " to spawn location of world " + name);
        return true;
    }
}
