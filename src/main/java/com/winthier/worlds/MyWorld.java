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
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.ConfigurationSection;

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
    Long seed;
    Map<String, String> gameRules = null;
    Settings settings = null;
    SpawnLocation spawnLocation = null;
    Border border = null;

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
            spawnLocation = SpawnLocation.of(section);
        }
        section = config.getConfigurationSection("Border");
        if (section != null) {
            border = new Border();
            border.configure(section);
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
        spawnLocation = SpawnLocation.of(getSpawnLocation());
        border = new Border();
        border.configure(world);
    }

    WorldCreator getWorldCreator() {
        WorldCreator creator = WorldCreator.name(name);
        creator.type(worldType);
        creator.environment(environment);
        creator.generator(generator);
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
        }
        if (world != null) apply(world);
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
        if (spawnLocation != null) spawnLocation.apply(world);
        if (border != null) border.apply(world);
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
        spawnLocation = SpawnLocation.of(location);
        spawnLocation.apply(location.getWorld());
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
    static class SpawnLocation {
        double x, y, z;
        float pitch, yaw;

        static SpawnLocation of(Location loc) {
            return new SpawnLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw());
        }

        static SpawnLocation of(ConfigurationSection config) {
            double x = config.getDouble("x");
            double y = config.getDouble("y");
            double z = config.getDouble("z");
            float pitch = (float)config.getDouble("pitch");
            float yaw = (float)config.getDouble("yaw");
            return new SpawnLocation(x, y, z, pitch, yaw);
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

        void apply(World world) {
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
}
