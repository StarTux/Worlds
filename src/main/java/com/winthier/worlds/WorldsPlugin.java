package com.winthier.worlds;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldsPlugin extends JavaPlugin {
    List<MyWorld> worlds = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        getCommand("worlds").setExecutor(new WorldsCommand(this));
        getCommand("wtp").setExecutor(new WTPCommand(this));
        loadAllWorlds();
    }

    @Override
    public void onDisable() {
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
