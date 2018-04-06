package com.winthier.worlds;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class WorldsPlugin extends JavaPlugin {
    List<MyWorld> worlds = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        getCommand("worlds").setExecutor(new WorldsCommand(this));
        getCommand("wtp").setExecutor(new WTPCommand(this));
        // Apply settings to default worlds (and other previously
        // loaded ones)
        for (World world: getServer().getWorlds()) {
            MyWorld myWorld = worldByName(world.getName());
            if (myWorld != null) myWorld.apply(world);
        }
        loadAllWorlds();
        getServer().getPluginManager().registerEvents(new PortalListener(this), this);
        new BukkitRunnable() {
            @Override public void run() {
                onTick();
            }
        }.runTaskTimer(this, 1, 1);
    }

    @Override
    public void onDisable() {
    }

    void onTick() {
        for (MyWorld myWorld: getWorlds()) {
            World world = myWorld.getWorld();
            if (world == null) continue;
            if (world.getEnvironment() == World.Environment.NORMAL) {
                long time;
                switch (myWorld.rushNight) {
                case NEVER:
                    break;
                case SLEEP:
                    time = world.getTime();
                    if (time > 13000 && time < 23000) {
                        int total = 0;
                        int sleep = 0;
                        for (Player player: world.getPlayers()) {
                            total += 1;
                            if (player.isSleeping()) sleep += 1;
                        }
                        if (sleep > 0) {
                            long skip = (long)((20 * sleep) / total);
                            if (skip > 1) {
                                world.setTime(time + skip - 1);
                            }
                        }
                    }
                    break;
                case ALWAYS:
                    time = world.getTime();
                    if (time > 13000 && time < 23000) {
                        world.setTime(time + 19);
                    }
                    break;
                }
            }
        }
    }

    List<MyWorld> getWorlds() {
        if (worlds == null) {
            worlds = new ArrayList<>();
            ConfigurationSection config = getConfig().getConfigurationSection("worlds");
            for (String key: config.getKeys(false)) {
                MyWorld myWorld = new MyWorld(this, key);
                myWorld.configure(config.getConfigurationSection(key));
                worlds.add(myWorld);
            }
        }
        return worlds;
    }

    void reloadWorlds() {
        worlds = null;
    }

    void loadAllWorlds() {
        for (MyWorld myWorld: getWorlds()) {
            World world = myWorld.loadWorld();
            if (world != null) getLogger().info("Loaded world " + world.getName());
        }
    }

    MyWorld worldByName(String name) {
        for (MyWorld myWorld: getWorlds()) {
            if (name.equals(myWorld.name)) {
                return myWorld;
            }
        }
        return null;
    }
}
