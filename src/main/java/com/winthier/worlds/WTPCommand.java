package com.winthier.worlds;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class WTPCommand extends AbstractCommand<WorldsPlugin> {
    protected WTPCommand(final WorldsPlugin plugin) {
        super(plugin, "wtp");
    }

    protected void onEnable() {
        rootNode.arguments("[player] <world>")
            .description("Teleport to world")
            .completers(CommandArgCompleter.supplyList(this::listLoadedWorlds),
                        CommandArgCompleter.supplyList(this::listLoadedWorlds))
            .senderCaller(this::wtp);
    }

    private boolean wtp(CommandSender sender, String[] args) {
        final Player player = sender instanceof Player ? (Player) sender : null;
        Player target;
        String name;
        if (args.length == 1) {
            target = player;
            if (target == null) {
                throw new CommandWarn("Player expected");
            }
            name = args[0];
        } else if (args.length == 2) {
            target = plugin.getServer().getPlayerExact(args[0]);
            if (target == null) {
                throw new CommandWarn("Player not found: " + args[0]);
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
            throw new CommandWarn("World not found: " + name);
        }
        target.teleport(loc);
        sender.sendMessage(text("Teleported " + target.getName() + " to spawn location of world " + name, YELLOW));
        return true;
    }

    private List<String> listLoadedWorlds() {
        List<String> result = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            result.add(world.getName());
        }
        return result;
    }
}
