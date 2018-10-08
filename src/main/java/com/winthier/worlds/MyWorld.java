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
import org.bukkit.PortalType;
import org.bukkit.TravelAgent;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

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
    private String generator, generatorSettings;
    private boolean generateStructures;
    private Long seed;
    private Map<String, String> gameRules = null;
    private Settings settings = null;
    private MyLocation spawnLocation = null;
    private Border border = null;
    private Portal netherPortal, endPortal;
    private RushNight rushNight = null;
    private GameMode gameMode = null;
    private static final String META_PORTING = "worlds.porting";

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
            this.gameMode = GameMode.SURVIVAL;
        } else {
            try {
                this.gameMode = GameMode.valueOf(gameModeString.toUpperCase());
            } catch (IllegalArgumentException iae) {
                plugin.getLogger().warning("Unknown GameMode setting for " + name + ": " + gameModeString);
                this.gameMode = null;
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
        if (rushNight != null) config.set("RushNight", rushNight.name());
        if (gameMode != null) config.set("GameMode", gameMode.name());
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

    /**
     * Return true if event shall be cancelled because we take over,
     * false wise.
     */
    boolean applyPortalTravel(TravelAgent travelAgent, Entity entity, Location from, PortalType portalType) {
        if (portalType == PortalType.NETHER) {
            if (netherPortal == null) return false;
            if (netherPortal.cancel) {
                return true;
            } else {
                return netherPortal.apply(travelAgent, entity, from);
            }
        } else if (portalType == PortalType.ENDER) {
            if (endPortal == null) return false;
            if (endPortal.cancel) {
                return true;
            } else {
                return endPortal.apply(travelAgent, entity, from);
            }
        } else {
            return false;
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
        //   Mob Spawning
        //     Allow
        private Boolean allowMonsters;
        private Boolean allowAnimals;
        //    Spawn Limits
        private Integer ambientSpawnLimit;
        private Integer animalSpawnLimit;
        private Integer monsterSpawnLimit;
        private Integer waterAnimalSpawnLimit;
        //     Ticks Per
        private Long ticksPerAnimalSpawns;
        private Long ticksPerMonsterSpawns;

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
        private double x, y, z;
        private float pitch, yaw;

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
        private double centerX, centerZ;
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

    class Portal {
        private String destination;
        private double ratio;
        private boolean cancel;
        private boolean createPortal;
        private boolean toWorldSpawn;
        private int searchRadius = 128;
        private int creationRadius = 16;
        private transient boolean warned = false;

        /**
         * Return true if the event shall be cancelled because we take
         * over, false otherwise.
         */
        boolean apply(TravelAgent travelAgent, Entity entity, Location from) {
            if (entity.hasMetadata(META_PORTING)) return true;
            travelAgent.setCanCreatePortal(createPortal);
            travelAgent.setSearchRadius(searchRadius);
            travelAgent.setCreationRadius(creationRadius);
            if (destination == null || destination.isEmpty()) return false;
            World world = plugin.getServer().getWorld(destination);
            if (world == null) {
                if (!warned) {
                    plugin.getLogger().warning("Portal destination world not found: " + destination);
                    warned = true;
                }
                return true;
            }
            Location to;
            if (toWorldSpawn) {
                to = world.getSpawnLocation();
            } else {
                to = new Location(world, from.getX() * ratio, from.getY(), from.getZ() * ratio, from.getYaw(), from.getPitch());
            }
            entity.setMetadata(META_PORTING, new FixedMetadataValue(plugin, true));
            world.getChunkAtAsync(to, (c) -> {
                    if (!entity.isValid()) return;
                    entity.removeMetadata(META_PORTING, plugin);
                    Location dest;
                    if (createPortal) {
                        dest = travelAgent.findOrCreate(to);
                    } else {
                        dest = travelAgent.findPortal(to);
                    }
                    if (dest == null) dest = to;
                    dest = dest.add(0.5, 0.0, 0.5);
                    // The event likes to put entities right below
                    // the portal so they either suffocate or drop
                    // to their doom.  Therefore, we cancel the
                    // event and do the teleport manually.
                    if (!entity.isValid()) return;
                    entity.teleport(dest);
                    if (entity instanceof Player) {
                        plugin.getLogger().info(String.format("Portal teleport %s to %s %.02f %.02f %.02f", entity.getName(), to.getWorld().getName(), to.getX(), to.getY(), to.getZ()));
                    }
                });
            return true;
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
