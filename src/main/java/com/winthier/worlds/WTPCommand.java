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
public class WTPCommand implements CommandExecutor {
    final WorldsPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        final Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null || args.length != 1) return false;
        String name = args[0];
        MyWorld myWorld = plugin.worldByName(name);
        Location loc = null;
        if (myWorld != null) {
            loc = myWorld.getSpawnLocation();
        } else {
            World world = plugin.getServer().getWorld(name);
            if (world != null) loc = world.getSpawnLocation();
        }
        if (loc == null) {
            player.sendMessage(ChatColor.RED + "World not found: " + name);
            return true;
        }
        player.teleport(loc);
        player.sendMessage(ChatColor.YELLOW + "Teleported to spawn location of world " + name);
        return true;
    }
}
