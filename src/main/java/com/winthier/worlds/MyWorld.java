package com.winthier.worlds;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.PortalType;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;

/**
 * This is a simple wrapper for a world section in the
 * config.yml. It can deserialize all the settings and then load
 * the world.
 * Always call configure() before loadWorld()!
 */
@RequiredArgsConstructor
@Getter
final class MyWorld {
    private final WorldsPlugin plugin;
    // World Creator Settings
    private final String name;
    private boolean autoLoad;
    private WorldType worldType;
    private World.Environment environment;
    private String generator;
    private String generatorSettings;
    private boolean generateStructures;
    private Long seed;
    private Map<String, String> gameRules = null;
    private Settings settings = null;
    private MyLocation spawnLocation = null;
    private Border border = null;
    private Portal netherPortal;
    private Portal endPortal;
    private Portal cryingPortal;
    private RushNight rushNight = null;
    private GameMode gameMode = null;
    private String copyTime;
    private Long fullTime;

    enum RushNight {
        NEVER, SLEEP, ALWAYS;
    }

    void configure(ConfigurationSection config) {
        autoLoad = config.getBoolean("AutoLoad", false);
        // World Creator
        try {
            worldType = WorldType.valueOf(config.getString("Type", "NORMAL"));
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
                seed = (long) tmp.hashCode();
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
                netherPortal = new Portal(plugin, this, PortalType.NETHER);
                netherPortal.configure(portalSection);
            }
            portalSection = section.getConfigurationSection("End");
            if (portalSection != null) {
                endPortal = new Portal(plugin, this, PortalType.ENDER);
                endPortal.configure(portalSection);
            }
            portalSection = section.getConfigurationSection("Crying");
            if (portalSection != null) {
                cryingPortal = new Portal(plugin, this, PortalType.NETHER);
                cryingPortal.configure(portalSection);
            }
        }
        final String rushNightString = config.getString("RushNight");
        if (rushNightString == null) {
            this.rushNight = null;
        } else {
            try {
                this.rushNight = RushNight.valueOf(rushNightString.toUpperCase());
            } catch (IllegalArgumentException iae) {
                plugin.getLogger().warning("Unknown RushNight setting for " + name + ": " + rushNightString);
                this.rushNight = null;
            }
        }
        final String gameModeString = config.getString("GameMode");
        if (gameModeString == null) {
            this.gameMode = null;
        } else {
            try {
                this.gameMode = GameMode.valueOf(gameModeString.toUpperCase());
            } catch (IllegalArgumentException iae) {
                plugin.getLogger().warning("Unknown GameMode setting for " + name + ": " + gameModeString);
                this.gameMode = null;
            }
        }
        copyTime = config.getString("CopyTime");
        if (config.isLong("FullTime") || config.isInt("FullTime")) {
            fullTime = config.getLong("FullTime");
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
        config.set("Type", worldType.name());
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
        if (cryingPortal != null) {
            ConfigurationSection section = config.getConfigurationSection("Portal.Crying");
            if (section == null) section = config.createSection("Portal.Crying");
            cryingPortal.save(section);
        }
        if (rushNight != null) config.set("RushNight", rushNight.name());
        if (gameMode != null) config.set("GameMode", gameMode.name());
        if (copyTime != null) config.set("CopyTime", copyTime);
        if (fullTime != null) config.set("FullTime", fullTime);
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
        if (fullTime != null) world.setFullTime(fullTime);
    }

    /**
     * Find the portal frame belonging to the entity location.  We
     * assume the entity is in the portal frame block or right next to
     * it; the rest is guesswork.
     * @param the location of an entity that's traveling through a nether portal.
     * @return the frame block right below, or null if none was found
     */
    private Block findFrame(Location from) {
        Block block = from.getBlock();
        if (block.getType() != Material.NETHER_PORTAL) {
            for (BlockFace face : Arrays.asList(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)) {
                Block rel = block.getRelative(face);
                if (rel.getType() == Material.NETHER_PORTAL) {
                    block = rel;
                    break;
                }
            }
        }
        if (block.getType() != Material.NETHER_PORTAL) return null;
        while (block.getType() == Material.NETHER_PORTAL) block = block.getRelative(BlockFace.DOWN);
        return block;
    }

    /**
     * Return true if event shall be cancelled because we take over,
     * false wise.
     */
    public Portal applyPortalTravel(Entity entity, Location from, PortalType portalType) {
        if (portalType == PortalType.NETHER) {
            Block frame = findFrame(from);
            if (frame != null && frame.getType() == Material.CRYING_OBSIDIAN) {
                return cryingPortal;
            } else {
                return netherPortal;
            }
        } else if (portalType == PortalType.ENDER) {
            return endPortal;
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
        private Boolean autoSave;
        private Difficulty difficulty;
        private Boolean keepSpawnInMemory;
        private Boolean pvp;
        private Integer viewDistance;
        //   Mob Spawning
        //     Allow
        private Boolean allowMonsters;
        private Boolean allowAnimals;
        //    Spawn Limits
        private Integer ambientSpawnLimit;
        private Integer animalSpawnLimit;
        private Integer monsterSpawnLimit;
        private Integer waterAmbientSpawnLimit;
        private Integer waterAnimalSpawnLimit;
        private Integer waterUndergroundCreatureSpawnLimit;
        //     Ticks Per
        private Long ticksPerAmbientSpawns;
        private Long ticksPerAnimalSpawns;
        private Long ticksPerMonsterSpawns;
        private Long ticksPerWaterAmbientSpawns;
        private Long ticksPerWaterSpawns;
        private Long ticksPerWaterUndergroundCreatureSpawns;

        void configure(ConfigurationSection config) {
            if (config.isSet("AutoSave")) {
                autoSave = config.getBoolean("AutoSave");
            }
            if (config.isSet("Difficulty")) {
                try {
                    difficulty = Difficulty.valueOf(config.getString("Difficulty"));
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                }
            }
            if (config.isSet("KeepSpawnInMemory")) {
                keepSpawnInMemory = config.getBoolean("KeepSpawnInMemory");
            }
            if (config.isSet("PvP")) {
                pvp = config.getBoolean("PvP");
            }
            if (config.isSet("ViewDistance")) {
                viewDistance = config.getInt("ViewDistance");
            }
            ConfigurationSection section = config.getConfigurationSection("AllowSpawns");
            if (section != null) {
                if (section.isSet("Monster")) {
                    allowMonsters = section.getBoolean("Monster");
                }
                if (section.isSet("Animal")) {
                    allowAnimals = section.getBoolean("Animal");
                }
            }
            section = config.getConfigurationSection("SpawnLimits");
            if (section != null) {
                if (section.isSet("Ambient")) {
                    ambientSpawnLimit = section.getInt("Ambient");
                }
                if (section.isSet("Animal")) {
                    animalSpawnLimit = section.getInt("Animal");
                }
                if (section.isSet("Monster")) {
                    monsterSpawnLimit = section.getInt("Monster");
                }
                if (section.isSet("WaterAmbient")) {
                    waterAmbientSpawnLimit = section.getInt("WaterAmbient");
                }
                if (section.isSet("WaterAnimal")) {
                    waterAnimalSpawnLimit = section.getInt("WaterAnimal");
                }
                if (section.isSet("WaterUndergroundCreature")) {
                    waterUndergroundCreatureSpawnLimit = section.getInt("WaterUndergroundCreature");
                }
            }
            section = config.getConfigurationSection("TicksPer");
            if (section != null) {
                if (section.isSet("AmbientSpawn")) {
                    ticksPerAmbientSpawns = section.getLong("AmbientSpawn");
                }
                if (section.isSet("AnimalSpawn")) {
                    ticksPerAnimalSpawns = section.getLong("AnimalSpawn");
                }
                if (section.isSet("MonsterSpawn")) {
                    ticksPerMonsterSpawns = section.getLong("MonsterSpawn");
                }
                if (section.isSet("WaterAmbient")) {
                    ticksPerWaterAmbientSpawns = section.getLong("WaterAmbient");
                }
                if (section.isSet("WaterSpawn")) {
                    ticksPerWaterSpawns = section.getLong("WaterSpawn");
                }
                if (section.isSet("WaterUndergroundCreature")) {
                    ticksPerWaterUndergroundCreatureSpawns = section.getLong("WaterUndergroundCreature");
                }
            }
        }

        void save(ConfigurationSection config) {
            config.set("AutoSave", autoSave);
            config.set("Difficulty", difficulty.name());
            config.set("KeepSpawnInMemory", keepSpawnInMemory);
            config.set("PvP", pvp);
            config.set("ViewDistance", viewDistance);
            config.set("AllowSpawns.Monster", allowMonsters);
            config.set("AllowSpawns.Animal", allowAnimals);
            // SpawnLimits
            config.set("SpawnLimits.Ambient", ambientSpawnLimit);
            config.set("SpawnLimits.Animal", animalSpawnLimit);
            config.set("SpawnLimits.Monster", monsterSpawnLimit);
            config.set("SpawnLimits.WaterAmbient", waterAmbientSpawnLimit);
            config.set("SpawnLimits.WaterAnimal", waterAnimalSpawnLimit);
            config.set("SpawnLimits.WaterUndergroundCreature", waterUndergroundCreatureSpawnLimit);
            // TicksPer
            config.set("TicksPer.AmbientSpawn", ticksPerAmbientSpawns);
            config.set("TicksPer.AnimalSpawn", ticksPerAnimalSpawns);
            config.set("TicksPer.MonsterSpawn", ticksPerMonsterSpawns);
            config.set("TicksPer.WaterAmbientSpawn", ticksPerWaterAmbientSpawns);
            config.set("TicksPer.WaterSpawn", ticksPerWaterSpawns);
            config.set("TicksPer.WaterUndergroundSpawn", ticksPerWaterUndergroundCreatureSpawns);
        }

        void configure(World world) {
            autoSave = world.isAutoSave();
            difficulty = world.getDifficulty();
            keepSpawnInMemory = world.getKeepSpawnInMemory();
            pvp = world.getPVP();
            viewDistance = world.getViewDistance();
            allowMonsters = world.getAllowMonsters();
            allowAnimals = world.getAllowAnimals();
            // SpawnLimits
            ambientSpawnLimit = world.getAmbientSpawnLimit();
            animalSpawnLimit = world.getAnimalSpawnLimit();
            monsterSpawnLimit = world.getMonsterSpawnLimit();
            waterAmbientSpawnLimit = world.getWaterAmbientSpawnLimit();
            waterAnimalSpawnLimit = world.getWaterAnimalSpawnLimit();
            waterUndergroundCreatureSpawnLimit = world.getWaterUndergroundCreatureSpawnLimit();
            // TicksPer
            ticksPerAmbientSpawns = world.getTicksPerAmbientSpawns();
            ticksPerAnimalSpawns = world.getTicksPerAnimalSpawns();
            ticksPerMonsterSpawns = world.getTicksPerMonsterSpawns();
            ticksPerWaterAmbientSpawns = world.getTicksPerWaterAmbientSpawns();
            ticksPerWaterSpawns = world.getTicksPerWaterSpawns();
            ticksPerWaterUndergroundCreatureSpawns = world.getTicksPerWaterUndergroundCreatureSpawns();
        }

        void apply(World world) {
            if (autoSave != null) world.setAutoSave(autoSave);
            if (difficulty != null) world.setDifficulty(difficulty);
            if (keepSpawnInMemory != null) world.setKeepSpawnInMemory(keepSpawnInMemory);
            if (pvp != null) world.setPVP(pvp);
            if (viewDistance != null) world.setViewDistance(viewDistance);
            if (allowMonsters != null && allowAnimals != null) {
                world.setSpawnFlags(allowMonsters, allowAnimals);
            }
            // SpawnLimits
            if (ambientSpawnLimit != null) world.setAmbientSpawnLimit(ambientSpawnLimit);
            if (animalSpawnLimit != null) world.setAnimalSpawnLimit(animalSpawnLimit);
            if (monsterSpawnLimit != null) world.setMonsterSpawnLimit(monsterSpawnLimit);
            if (waterAmbientSpawnLimit != null) world.setWaterAmbientSpawnLimit(waterAmbientSpawnLimit);
            if (waterAnimalSpawnLimit != null) world.setWaterAnimalSpawnLimit(waterAnimalSpawnLimit);
            if (waterUndergroundCreatureSpawnLimit != null) {
                world.setWaterUndergroundCreatureSpawnLimit(waterUndergroundCreatureSpawnLimit);
            }
            // TicksPer
            if (ticksPerAmbientSpawns != null) world.setTicksPerAmbientSpawns(ticksPerAmbientSpawns.intValue());
            if (ticksPerAnimalSpawns != null) world.setTicksPerAnimalSpawns(ticksPerAnimalSpawns.intValue());
            if (ticksPerMonsterSpawns != null) world.setTicksPerMonsterSpawns(ticksPerMonsterSpawns.intValue());
            if (ticksPerWaterAmbientSpawns != null) world.setTicksPerWaterAmbientSpawns(ticksPerWaterAmbientSpawns.intValue());
            if (ticksPerWaterSpawns != null) world.setTicksPerWaterSpawns(ticksPerWaterSpawns.intValue());
            if (ticksPerWaterUndergroundCreatureSpawns != null) {
                world.setTicksPerWaterUndergroundCreatureSpawns(ticksPerWaterUndergroundCreatureSpawns.intValue());
            }
        }
    }

    @Value
    static class MyLocation {
        private double x;
        private double y;
        private double z;
        private float pitch;
        private float yaw;

        static MyLocation of(Location loc) {
            return new MyLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw());
        }

        static MyLocation of(ConfigurationSection config) {
            double x = config.getDouble("x");
            double y = config.getDouble("y");
            double z = config.getDouble("z");
            float pitch = (float) config.getDouble("pitch");
            float yaw = (float) config.getDouble("yaw");
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
            world.setSpawnLocation((int) x, (int) y, (int) z);
        }
    }

    static class Border {
        private double centerX;
        private double centerZ;
        private double size;
        private double damageAmount;
        private double damageBuffer;
        private int warningDistance;
        private int warningTime;

        void configure(ConfigurationSection config) {
            List<Double> list = config.getDoubleList("Center");
            if (list.size() == 2) {
                centerX = list.get(0);
                centerZ = list.get(1);
            }
            size = config.getDouble("Size");
            damageAmount = config.getDouble("DamageAmount");
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
