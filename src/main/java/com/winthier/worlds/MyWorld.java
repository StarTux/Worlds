package com.winthier.worlds;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.PortalType;
import org.bukkit.TravelAgent;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Cancellable;

/**
 * This is a simple wrapper for a world section in the
 * config.yml. It can deserialize all the settings and then load
 * the world.
 * Always call configure() before loadWorld()!
 */
@RequiredArgsConstructor
@Getter
public class MyWorld {
    final WorldsPlugin plugin;
    // World Creator Settings
    final String name;
    boolean autoLoad;
    WorldType worldType;
    World.Environment environment;
    String generator, generatorSettings;
    boolean generateStructures;
    Long seed;
    Map<String, String> gameRules = null;
    Settings settings = null;
    MyLocation spawnLocation = null;
    Border border = null;
    Portal netherPortal, endPortal;

    void configure(ConfigurationSection config) {
        autoLoad = config.getBoolean("AutoLoad", false);
        // World Creator
        try {
            worldType = WorldType.valueOf(config.getString("WorldType", "NORMAL"));
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        }
        try {
            environment = World.Environment.valueOf(config.getString("Environment", "NORMAL"));
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        }
        generator = config.getString("Generator");
        generatorSettings = config.getString("GeneratorSettings");
        generateStructures = config.getBoolean("GenerateStructures", true);
        String tmp = config.getString("Seed");
        if (tmp != null && !tmp.isEmpty()) {
            try {
                seed = Long.parseLong(tmp);
            } catch (NumberFormatException nfe) {
                seed = (long)tmp.hashCode();
            }
        }
        ConfigurationSection section = config.getConfigurationSection("GameRules");
        if (section != null) {
            if (gameRules == null) gameRules = new HashMap<>();
            for (String key: section.getKeys(false)) {
                gameRules.put(key, section.getString(key));
            }
        }
        section = config.getConfigurationSection("Settings");
        if (section != null) {
            settings = new Settings();
            settings.configure(section);
        }
        section = config.getConfigurationSection("SpawnLocation");
        if (section != null) {
            spawnLocation = MyLocation.of(section);
        }
        section = config.getConfigurationSection("Border");
        if (section != null) {
            border = new Border();
            border.configure(section);
        }
        section = config.getConfigurationSection("Portal");
        if (section != null) {
            ConfigurationSection portalSection;
            portalSection = section.getConfigurationSection("Nether");
            if (portalSection != null) {
                netherPortal = new Portal();
                netherPortal.configure(portalSection);
            }
            portalSection = section.getConfigurationSection("End");
            if (portalSection != null) {
                endPortal = new Portal();
                endPortal.configure(portalSection);
            }
        }
    }

    /**
     * This function only saves some of the settings. In general,
     * it is intended for admins to edit the config.yml, then use
     * reload to apply the changes.
     */
    void save() {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("worlds").getConfigurationSection(name);
        if (config == null) config = plugin.getConfig().getConfigurationSection("worlds").createSection(name);
        config.set("AutoLoad", autoLoad);
        config.set("WorldType", worldType.name());
        config.set("Environment", environment.name());
        config.set("Generator", generator);
        config.set("GeneratorSettings", generatorSettings);
        config.set("GenerateStructures", generateStructures);
        config.set("Seed", seed);
        if (gameRules != null) {
            ConfigurationSection section = config.getConfigurationSection("GameRules");
            if (section == null) section = config.createSection("GameRules");
            for (Map.Entry<String, String> entry: gameRules.entrySet()) {
                section.set(entry.getKey(), entry.getValue());
            }
        }
        if (settings != null) {
            ConfigurationSection section = config.getConfigurationSection("Settings");
            if (section == null) section = config.createSection("Settings");
            settings.save(section);
        }
        if (spawnLocation != null) {
            ConfigurationSection section = config.getConfigurationSection("SpawnLocation");
            if (section == null) section = config.createSection("SpawnLocation");
            spawnLocation.save(section);
        }
        if (border != null) {
            ConfigurationSection section = config.getConfigurationSection("Border");
            if (section == null) section = config.createSection("Border");
            border.save(section);
        }
        if (netherPortal != null) {
            ConfigurationSection section = config.getConfigurationSection("Portal.Nether");
            if (section == null) section = config.createSection("Portal.Nether");
            netherPortal.save(section);
        }
        if (endPortal != null) {
            ConfigurationSection section = config.getConfigurationSection("Portal.End");
            if (section == null) section = config.createSection("Portal.End");
            endPortal.save(section);
        }
    }

    void configure(World world) {
        worldType = world.getWorldType();
        environment = world.getEnvironment();
        // Generator name has no getter...
        seed = world.getSeed();
        for (String key: world.getGameRules()) {
            if (gameRules == null) gameRules = new HashMap<>();
            gameRules.put(key, world.getGameRuleValue(key));
        }
        settings = new Settings();
        settings.configure(world);
        spawnLocation = MyLocation.of(getSpawnLocation());
        border = new Border();
        border.configure(world);
    }

    WorldCreator getWorldCreator() {
        WorldCreator creator = WorldCreator.name(name);
        creator.type(worldType);
        creator.environment(environment);
        creator.generator(generator);
        creator.generateStructures(generateStructures);
        if (generatorSettings != null && !generatorSettings.isEmpty()) {
            creator.generatorSettings(generatorSettings);
        }
        if (seed != null) {
            creator.seed(seed);
        }
        return creator;
    }

    World getWorld() {
        return plugin.getServer().getWorld(name);
    }

    World loadWorld() {
        World world = getWorld();
        if (world == null && autoLoad) {
            WorldCreator creator = getWorldCreator();
            world = creator.createWorld();
            if (world != null) apply(world);
        }
        return world;
    }

    void apply(World world) {
        if (gameRules != null) {
            for (Map.Entry<String, String> entry: gameRules.entrySet()) {
                boolean ret = world.setGameRuleValue(entry.getKey(), entry.getValue());
                if (!ret) {
                    plugin.getLogger().warning("Failed to set GameRule '" + entry.getKey() + "' to '" + entry.getValue() + "' in world '" + name + "'");
                }
            }
        }
        if (settings != null) settings.apply(world);
        if (spawnLocation != null) spawnLocation.setSpawn(world);
        if (border != null) border.apply(world);
    }

    Location applyPortalTravel(Cancellable event, TravelAgent travelAgent, Location from, PortalType portalType) {
        if (portalType == PortalType.NETHER) {
            if (netherPortal == null) return null;
            if (netherPortal.cancel) {
                event.setCancelled(true);
                return null;
            } else {
                return netherPortal.apply(travelAgent, from);
            }
        } else if (portalType == PortalType.ENDER) {
            if (endPortal == null) return null;
            if (endPortal.cancel) {
                event.setCancelled(true);
                return null;
            } else {
                return endPortal.apply(travelAgent, from);
            }
        } else {
            return null;
        }
    }

    Location getSpawnLocation() {
        World world = getWorld();
        if (world == null) return null;
        Location result = null;
        if (spawnLocation != null) result = spawnLocation.getLocation(world);
        if (result == null) result = world.getSpawnLocation();
        return result;
    }

    void setSpawnLocation(Location location) {
        spawnLocation = MyLocation.of(location);
        spawnLocation.setSpawn(location.getWorld());
    }

    static class Settings {
        //   General
        Boolean autoSave;
        Difficulty difficulty;
        Boolean keepSpawnInMemory;
        Boolean pvp;
        //   Mob Spawning
        //     Allow
        Boolean allowMonsters;
        Boolean allowAnimals;
        //    Spawn Limits
        Integer ambientSpawnLimit;
        Integer animalSpawnLimit;
        Integer monsterSpawnLimit;
        Integer waterAnimalSpawnLimit;
        //     Ticks Per
        Long ticksPerAnimalSpawns;
        Long ticksPerMonsterSpawns;

        void configure(ConfigurationSection config) {
            autoSave = config.getBoolean("AutoSave", true);
            try {
                difficulty = Difficulty.valueOf(config.getString("Difficulty", "NORMAL"));
            } catch (IllegalArgumentException iae) {
                iae.printStackTrace();
            }
            keepSpawnInMemory = config.getBoolean("KeepSpawnInMemory");
            pvp = config.getBoolean("PvP");
            ConfigurationSection section = config.getConfigurationSection("AllowSpawns");
            if (section != null) {
                allowMonsters = section.getBoolean("Monster", true);
                allowAnimals = section.getBoolean("Animal", true);
            }
            section = config.getConfigurationSection("SpawnLimits");
            if (section != null) {
                ambientSpawnLimit = section.getInt("Ambient");
                animalSpawnLimit = section.getInt("Animal");
                monsterSpawnLimit = section.getInt("Monster");
                waterAnimalSpawnLimit = section.getInt("WaterAnimal");
            }
            section = config.getConfigurationSection("TicksPer");
            if (section != null) {
                ticksPerAnimalSpawns = section.getLong("AnimalSpawn");
                ticksPerMonsterSpawns = section.getLong("MonsterSpawn");
            }
        }

        void save(ConfigurationSection config) {
            config.set("AutoSave", autoSave);
            config.set("Difficulty", difficulty.name());
            config.set("KeepSpawnInMemory", keepSpawnInMemory);
            config.set("PvP", pvp);
            config.set("AllowSpawns.Monster", allowMonsters);
            config.set("AllowSpawns.Animal", allowAnimals);
            config.set("SpawnLimits.Ambient", ambientSpawnLimit);
            config.set("SpawnLimits.Animal", animalSpawnLimit);
            config.set("SpawnLimits.Monster", monsterSpawnLimit);
            config.set("SpawnLimits.WaterAnimal", waterAnimalSpawnLimit);
            config.set("TicksPer.AnimalSpawn", ticksPerAnimalSpawns);
            config.set("TicksPer.MonsterSpawn", ticksPerMonsterSpawns);
        }

        void configure(World world) {
            autoSave = world.isAutoSave();
            difficulty = world.getDifficulty();
            keepSpawnInMemory = world.getKeepSpawnInMemory();
            pvp = world.getPVP();
            allowMonsters = world.getAllowMonsters();
            allowAnimals = world.getAllowAnimals();
            ambientSpawnLimit = world.getAmbientSpawnLimit();
            animalSpawnLimit = world.getAnimalSpawnLimit();
            monsterSpawnLimit = world.getMonsterSpawnLimit();
            waterAnimalSpawnLimit = world.getWaterAnimalSpawnLimit();
            ticksPerAnimalSpawns = world.getTicksPerAnimalSpawns();
            ticksPerMonsterSpawns = world.getTicksPerMonsterSpawns();
        }

        void apply(World world) {
            if (autoSave != null) world.setAutoSave(autoSave);
            if (difficulty != null) world.setDifficulty(difficulty);
            if (keepSpawnInMemory != null) world.setKeepSpawnInMemory(keepSpawnInMemory);
            if (pvp != null) world.setPVP(pvp);
            if (allowMonsters != null && allowAnimals != null) {
                world.setSpawnFlags(allowMonsters, allowAnimals);
            }
            if (ambientSpawnLimit != null) world.setAmbientSpawnLimit(ambientSpawnLimit);
            if (animalSpawnLimit != null) world.setAnimalSpawnLimit(animalSpawnLimit);
            if (monsterSpawnLimit != null) world.setMonsterSpawnLimit(monsterSpawnLimit);
            if (waterAnimalSpawnLimit != null) world.setWaterAnimalSpawnLimit(waterAnimalSpawnLimit);
            if (ticksPerAnimalSpawns != null) world.setTicksPerAnimalSpawns(ticksPerAnimalSpawns.intValue());
            if (ticksPerMonsterSpawns != null) world.setTicksPerMonsterSpawns(ticksPerMonsterSpawns.intValue());
        }
    }

    @Value
    static class MyLocation {
        double x, y, z;
        float pitch, yaw;

        static MyLocation of(Location loc) {
            return new MyLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw());
        }

        static MyLocation of(ConfigurationSection config) {
            double x = config.getDouble("x");
            double y = config.getDouble("y");
            double z = config.getDouble("z");
            float pitch = (float)config.getDouble("pitch");
            float yaw = (float)config.getDouble("yaw");
            return new MyLocation(x, y, z, pitch, yaw);
        }

        void save(ConfigurationSection config) {
            config.set("x", x);
            config.set("y", y);
            config.set("z", z);
            config.set("pitch", pitch);
            config.set("yaw", yaw);
        }

        Location getLocation(World world) {
            return new Location(world, x, y, z, yaw, pitch);
        }

        void setSpawn(World world) {
            world.setSpawnLocation((int)x, (int)y, (int)z);
        }
    }

    static class Border {
        double centerX, centerZ;
        double size;
        double damageAmount;
        double damageBuffer;
        int warningDistance;
        int warningTime;

        void configure(ConfigurationSection config) {
            List<Double> list = config.getDoubleList("Center");
            if (list.size() == 2) {
                centerX = list.get(0);
                centerZ = list.get(1);
            }
            size = config.getDouble("Size");
            damageBuffer = config.getDouble("DamageBuffer");
            warningDistance = config.getInt("WarningDistance");
            warningTime = config.getInt("WarningTime");
        }

        void save(ConfigurationSection config) {
            config.set("Center", Arrays.asList(centerX, centerZ));
            config.set("Size", size);
            config.set("DamageAmount", damageAmount);
            config.set("DamageBuffer", damageBuffer);
            config.set("WarningDistance", warningDistance);
            config.set("WarningTime", warningTime);
        }

        void configure(World world) {
            WorldBorder worldBorder = world.getWorldBorder();
            Location loc = worldBorder.getCenter();
            centerX = loc.getX();
            centerZ = loc.getZ();
            size = worldBorder.getSize();
            damageAmount = worldBorder.getDamageAmount();
            damageBuffer = worldBorder.getDamageBuffer();
            warningDistance = worldBorder.getWarningDistance();
            warningTime = worldBorder.getWarningTime();
        }

        void apply(World world) {
            WorldBorder worldBorder = world.getWorldBorder();
            worldBorder.setCenter(centerX, centerZ);
            worldBorder.setSize(size);
            worldBorder.setDamageAmount(damageAmount);
            worldBorder.setDamageBuffer(damageBuffer);
            worldBorder.setWarningTime(warningTime);
        }
    }

    class Portal {
        String destination;
        double ratio;
        boolean cancel;
        boolean createPortal;
        boolean toWorldSpawn;
        int searchRadius = 128;
        int creationRadius = 16;

        Location apply(TravelAgent travelAgent, Location from) {
            travelAgent.setCanCreatePortal(createPortal);
            travelAgent.setSearchRadius(searchRadius);
            travelAgent.setCreationRadius(creationRadius);
            if (destination == null || destination.isEmpty()) return null;
            World world = plugin.getServer().getWorld(destination);
            if (world == null) {
                plugin.getLogger().warning("Portal destination world not found: " + destination);
                return null;
            }
            Location to;
            if (toWorldSpawn) {
                to = world.getSpawnLocation();
            } else {
                to = new Location(world, from.getX() * ratio, from.getY(), from.getZ() * ratio, from.getYaw(), from.getPitch());
            }
            Location result;
            if (createPortal) {
                result = travelAgent.findOrCreate(to);
            } else {
                result = travelAgent.findPortal(to);
            }
            if (result == null) result = to;
            return result.add(0.5, 0.0, 0.5);
        }

        void save(ConfigurationSection config) {
            config.set("Destination", destination);
            config.set("Ratio", ratio);
            config.set("Cancel", cancel);
            config.set("CreatePortal", createPortal);
            config.set("ToWorldSpawn", toWorldSpawn);
            config.set("SearchRadius", searchRadius);
            config.set("CreationRadius", creationRadius);
        }

        void configure(ConfigurationSection config) {
            destination = config.getString("Destination", null);
            ratio = config.getDouble("Ratio", 1.0);
            cancel = config.getBoolean("Cancel", false);
            createPortal = config.getBoolean("CreatePortal", false);
            toWorldSpawn = config.getBoolean("ToWorldSpawn", false);
            searchRadius = config.getInt("SearchRadius", searchRadius);
            creationRadius = config.getInt("CreationRadius", creationRadius);
        }
    }
}
