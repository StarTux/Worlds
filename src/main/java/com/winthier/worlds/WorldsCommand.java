package com.winthier.worlds;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
final class WorldsCommand implements CommandExecutor {
    final WorldsPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        final Player player = sender instanceof Player ? (Player)sender : null;
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (cmd == null) {
            return false;
        } else if (cmd.equals("who")) {
            Msg.info(sender, "World Player List");
            for (World world: plugin.getServer().getWorlds()) {
                List<Player> players = world.getPlayers();
                if (players.isEmpty()) continue;
                StringBuilder sb = new StringBuilder(Msg.format("&7%s &8(&r%d&8)&r", world.getName(), players.size()));
                for (Player p: players) {
                    sb.append(" ").append(p.getName());
                }
                sender.sendMessage(sb.toString());
            }
        } else if (cmd.equals("load")) {
            if (args.length < 2) return false;
            if (args.length > 3) return false;
            String name = args[1];
            if (plugin.getServer().getWorld(name) != null) {
                sender.sendMessage(ChatColor.RED + "World already loaded: " + name + "!");
                return true;
            }
            WorldCreator creator = WorldCreator.name(name);
            World.Environment env;
            if (args.length >= 3) {
                String arg = args[2];
                try {
                    env = World.Environment.valueOf(arg.toUpperCase());
                } catch (IllegalArgumentException iae) {
                    sender.sendMessage(ChatColor.RED + "Invalid environment: " + arg);
                    return true;
                }
            } else {
                env = World.Environment.NORMAL;
            }
            creator.environment(env);
            World world = creator.createWorld();
            sender.sendMessage("World loaded: " + world.getName());
        } else if (cmd.equals("listloaded")) {
            int count = 0;
            for (World world: plugin.getServer().getWorlds()) {
                if (plugin.worldByName(world.getName()) == null) {
                    sender.sendMessage(ChatColor.RED + world.getName() + ChatColor.RESET + " (unregistered)");
                } else {
                    sender.sendMessage(ChatColor.GREEN + world.getName() + ChatColor.RESET + " (registered)");
                }
                count += 1;
            }
            sender.sendMessage("" + count + " worlds are currently loaded.");
        } else if (cmd.equals("reload")) {
            plugin.reloadConfig();
            plugin.reloadWorlds();
            plugin.loadAllWorlds();
            Msg.info(sender, "Worlds reloaded");
        } else if (cmd.equals("apply")) {
            int count = 0;
            for (MyWorld myWorld: plugin.getWorlds()) {
                World world = myWorld.getWorld();
                if (world != null) {
                    myWorld.apply(world);
                    count += 1;
                }
            }
            Msg.info(sender, "Applied %d world settings", count);
        } else if (cmd.equals("import")) {
            if (args.length != 2) return false;
            String name = args[1];
            if (name.equals("*")) {
                int count = 0;
                for (World world: plugin.getServer().getWorlds()) {
                    MyWorld myWorld = plugin.worldOf(world);
                    if (myWorld == null) myWorld = new MyWorld(plugin, name);
                    myWorld.configure(world);
                    myWorld.save();
                    count += 1;
                }
                plugin.saveConfig();
                Msg.info(sender, "Imported %d worlds.", count);
            } else {
                World world = plugin.getServer().getWorld(name);
                if (world == null) {
                    Msg.warn(sender, "World not found: %s", name);
                    return true;
                }
                name = world.getName();
                MyWorld myWorld = plugin.worldOf(world);
                if (myWorld == null) myWorld = new MyWorld(plugin, name);
                myWorld.configure(world);
                myWorld.save();
                plugin.saveConfig();
                Msg.info(sender, "Imported world '%s'", name);
            }
        } else if (cmd.equals("list")) {
            Msg.info(sender, "%d Worlds", plugin.getWorlds().size());
            for (MyWorld myWorld: plugin.getWorlds()) {
                if (myWorld.getWorld() == null) {
                    Msg.send(sender, " &c%s&r &7(&rnot loaded&7)", myWorld.getName());
                } else {
                    Msg.send(sender, " &a%s&r &7(&rloaded&7)", myWorld.getName());
                }
            }
        } else if (cmd.equals("spawn")) {
            World world = player.getWorld();
            MyWorld myWorld = plugin.worldByName(world.getName());
            if (myWorld == null) {
                player.teleport(world.getSpawnLocation());
            } else {
                player.teleport(myWorld.getSpawnLocation());
            }
            Msg.info(sender, "Teleported to world spawn.");
        } else if (cmd.equals("setspawn")) {
            World world = player.getWorld();
            Location loc = player.getLocation();
            MyWorld myWorld = plugin.worldByName(world.getName());
            if (myWorld == null) {
                int x = loc.getBlockX();
                int y = loc.getBlockY();
                int z = loc.getBlockZ();
                world.setSpawnLocation(x, y, z);
                Msg.info(sender, "World spawn set to %d,%d,%d.", x, y, z);
            } else {
                myWorld.setSpawnLocation(loc);
                myWorld.save();
                plugin.saveConfig();
                Msg.info(sender, "World spawn set to %.02f,%.02f,%.02f.", loc.getX(), loc.getY(), loc.getZ());
            }
        } else if (cmd.equals("unload") && args.length == 2) {
            String name = args[1];
            World world = plugin.getServer().getWorld(name);
            if (world == null) {
                Msg.warn(sender, "World not found: %s", name);
                return true;
            }
            boolean ret = plugin.getServer().unloadWorld(world, true);
            if (ret) {
                Msg.info(sender, "Unloaded world %s.", world.getName());
            } else {
                Msg.warn(sender, "Could not unload world %s.", world.getName());
            }
        } else {
            return false;
        }
        return true;
    }
}
