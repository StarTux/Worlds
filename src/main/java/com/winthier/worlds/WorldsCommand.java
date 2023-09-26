package com.winthier.worlds;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

final class WorldsCommand extends AbstractCommand<WorldsPlugin> {
    protected WorldsCommand(final WorldsPlugin plugin) {
        super(plugin, "worlds");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("list").denyTabCompletion()
            .description("List worlds")
            .senderCaller(this::list);
        rootNode.addChild("who").denyTabCompletion()
            .description("List players in worlds");
        rootNode.addChild("reload").denyTabCompletion()
            .description("Reload config")
            .senderCaller(this::reload);
        rootNode.addChild("apply").denyTabCompletion()
            .description("(Re)apply world settings")
            .senderCaller(this::apply);
        rootNode.addChild("listloaded").denyTabCompletion()
            .description("List loaded Bukkit worlds")
            .senderCaller(this::listLoaded);
        rootNode.addChild("import").arguments("<world> [generator]")
            .description("Import Bukkit world settings")
            .completers(CommandArgCompleter.supplyList(this::listLoadedWorlds),
                        CommandArgCompleter.list(List.of("VoidGenerator")))
            .senderCaller(this::importCommand);
        rootNode.addChild("spawn").denyTabCompletion()
            .description("Teleport to spawn")
            .playerCaller(this::spawn);
        rootNode.addChild("setspawn").denyTabCompletion()
            .description("Set world spawn")
            .playerCaller(this::setSpawn);
        rootNode.addChild("load").arguments("<world> [environment] [generator]")
            .description("Load world")
            .completers(CommandArgCompleter.supplyList(this::listWorldFolders),
                        CommandArgCompleter.enumLowerList(World.Environment.class),
                        CommandArgCompleter.list(List.of("VoidGenerator")))
            .senderCaller(this::load);
        rootNode.addChild("unload").arguments("<world>")
            .description("Unload Bukkit world")
            .completers(CommandArgCompleter.supplyList(this::listLoadedWorlds))
            .senderCaller(this::unload);
    }

    private List<String> listWorldFolders() {
        List<String> result = new ArrayList<>();
        for (File file : Bukkit.getWorldContainer().listFiles()) {
            if (file.isDirectory()) result.add(file.getName());
        }
        return result;
    }

    private List<String> listLoadedWorlds() {
        List<String> result = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            result.add(world.getName());
        }
        return result;
    }

    private void who(CommandSender sender) {
        sender.sendMessage(text("World Player List", GRAY));
        for (World world: plugin.getServer().getWorlds()) {
            List<Player> players = world.getPlayers();
            if (players.isEmpty()) continue;
            List<Component> msgs = new ArrayList<>();
            msgs.add(textOfChildren(text(world.getName() + " ", GRAY),
                                    text("&8(", DARK_GRAY),
                                    text(players.size(), WHITE),
                                    text(")", DARK_GRAY)));
            for (Player p: players) {
                msgs.add(text(" " + p.getName(), WHITE));
            }
            sender.sendMessage(join(noSeparators(), msgs));
        }
    }

    private boolean load(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 3) return false;
        String name = args[0];
        if (plugin.getServer().getWorld(name) != null) {
            throw new CommandWarn("World already loaded: " + name);
        }
        MyWorld myWorld = plugin.worldByName(name);
        if (myWorld != null) {
            World world = myWorld.loadWorld();
            sender.sendMessage(text("World loaded: " + world.getName(), YELLOW));
        } else {
            final WorldCreator creator = WorldCreator.name(name);
            final World.Environment env = args.length >= 2
                ? CommandArgCompleter.requireEnum(World.Environment.class, args[1])
                : World.Environment.NORMAL;
            creator.environment(env);
            final String generator = args.length >= 3
                ? args[2]
                : null;
            if (generator != null) creator.generator(generator);
            World world = creator.createWorld();
            sender.sendMessage(text("Unconfigured world loaded: " + world.getName(), YELLOW));
        }
        return true;
    }

    private void listLoaded(CommandSender sender) {
        int count = 0;
        for (World world: plugin.getServer().getWorlds()) {
            if (plugin.worldByName(world.getName()) == null) {
                sender.sendMessage(textOfChildren(text(world.getName(), RED),
                                                  text(" (unregistered)", GRAY)));
            } else {
                sender.sendMessage(textOfChildren(text(world.getName(), GREEN),
                                                  text(" (registered)", WHITE)));
            }
            count += 1;
        }
        sender.sendMessage(text("" + count + " worlds are currently loaded", AQUA));
    }

    private void reload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.reloadWorlds();
        plugin.loadAllWorlds();
        sender.sendMessage(text("Worlds configurations reloaded", YELLOW));
    }

    private void apply(CommandSender sender) {
        int count = 0;
        for (MyWorld myWorld: plugin.getWorlds()) {
            World world = myWorld.getWorld();
            if (world != null) {
                myWorld.apply(world);
                count += 1;
            }
        }
        sender.sendMessage(text("Applied " + count + " world settings", YELLOW));
    }

    private boolean importCommand(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) return false;
        String name = args[0];
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
            sender.sendMessage(text("Imported " + count + " worlds", YELLOW));
        } else {
            final String generator = args.length >= 2
                ? args[1]
                : null;
            World world = plugin.getServer().getWorld(name);
            if (world == null) {
                throw new CommandWarn("World not found: " + name);
            }
            name = world.getName();
            MyWorld myWorld = plugin.worldOf(world);
            if (myWorld == null) myWorld = new MyWorld(plugin, name);
            myWorld.configure(world);
            if (generator != null) myWorld.setGenerator(generator);
            myWorld.save();
            plugin.saveConfig();
            sender.sendMessage(text("Imported world " + name, YELLOW));
        }
        return true;
    }

    private void list(CommandSender sender) {
        sender.sendMessage(text(plugin.getWorlds().size() + " worlds", AQUA));
        for (MyWorld myWorld: plugin.getWorlds()) {
            if (myWorld.getWorld() == null) {
                sender.sendMessage(textOfChildren(text(" " + myWorld.getName(), RED),
                                                  text(" (not loaded)", GRAY)));
            } else {
                sender.sendMessage(textOfChildren(text(" " + myWorld.getName(), GREEN),
                                                  text(" (loaded)", WHITE)));
            }
        }
    }

    private void spawn(Player player) {
        World world = player.getWorld();
        MyWorld myWorld = plugin.worldByName(world.getName());
        if (myWorld == null) {
            player.teleport(world.getSpawnLocation());
        } else {
            player.teleport(myWorld.getSpawnLocation());
        }
        player.sendMessage(text("Teleported to world spawn", YELLOW));
    }

    private void setSpawn(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation();
        MyWorld myWorld = plugin.worldByName(world.getName());
        if (myWorld == null) {
            world.setSpawnLocation(loc);
        } else {
            myWorld.setSpawnLocation(loc);
            myWorld.save();
            plugin.saveConfig();
        }
        player.sendMessage(text(String.format("World spawn set to %.02f %.02f %.02f",
                                              loc.getX(), loc.getY(), loc.getZ()), YELLOW));
    }

    private boolean unload(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String name = args[0];
        World world = plugin.getServer().getWorld(name);
        if (world == null) {
            throw new CommandWarn("World not found: " + name);
        }
        if (!plugin.getServer().unloadWorld(world, true)) {
            throw new CommandWarn("Could not unload world " + world.getName());
        }
        sender.sendMessage(text("Unloaded world " + world.getName(), YELLOW));
        return true;
    }
}
