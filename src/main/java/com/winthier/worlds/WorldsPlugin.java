package com.winthier.worlds;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldsPlugin extends JavaPlugin {
    private List<MyWorld> worlds = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        new WorldsCommand(this).enable();
        new WTPCommand(this).enable();
        // Apply settings to default worlds (and other previously
        // loaded ones)
        for (World world: getServer().getWorlds()) {
            MyWorld myWorld = worldByName(world.getName());
            if (myWorld != null) myWorld.apply(world);
        }
        loadAllWorlds();
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    @Override
    public void onDisable() { }

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
        for (MyWorld myWorld : getWorlds()) {
            if (myWorld.isAutoLoad()) {
                World world = myWorld.loadWorld();
                if (world != null) getLogger().info("Loaded world " + world.getName());
            }
        }
    }

    public MyWorld worldByName(String name) {
        for (MyWorld myWorld : getWorlds()) {
            if (name.equals(myWorld.getName())) {
                return myWorld;
            }
        }
        return null;
    }

    public MyWorld worldOf(World world) {
        return worldByName(world.getName());
    }
}
