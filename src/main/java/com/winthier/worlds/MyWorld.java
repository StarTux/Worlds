package com.winthier.worlds;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.kyori.adventure.util.TriState;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.util.NumberConversions;

/**
 * This is a simple wrapper for a world section in the
 * config.yml. It can deserialize all the settings and then load
 * the world.
 * Always call configure() before loadWorld()!
 */
@RequiredArgsConstructor @Data
public final class MyWorld {
    private final WorldsPlugin plugin;
    // World Creator Settings
    private final String name;
    private boolean autoLoad;
    private WorldType worldType;
    private World.Environment environment;
    private String generator;
    private String generatorSettings;
    private String biomeProvider;
    private boolean generateStructures;
    private Long seed;
    private Map<GameRule<?>, Object> gameRules = null;
    private Settings settings = null;
    private MyLocation spawnLocation = null;
    private Border border = null;
    private GameMode gameMode = null;
    private Long fullTime;
    private boolean didConvert;

    public void configure(ConfigurationSection config) {
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
        biomeProvider = config.getString("BiomeProvider");
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
            for (String key : section.getKeys(false)) {
                GameRule gameRule = GameRule.getByName(key);
                if (gameRule == null) {
                    convertLegacyGameRule(key, section.getString(key));
                    continue;
                }
                Class<?> type = gameRule.getType();
                if (type == Integer.class) {
                    int intValue = NumberConversions.toInt(section.get(key));
                    if (gameRule == GameRules.MAX_ENTITY_CRAMMING && intValue < 0) {
                        intValue = 0;
                    }
                    gameRules.put(gameRule, intValue);
                } else if (type == Boolean.class) {
                    Boolean boolValue = toBoolean(section.get(key), null);
                    if (boolValue == null) {
                        plugin.getLogger().severe("[" + name + "] Invalid boolean gamerule: " + gameRule.getKey() + " = " + section.get(key));
                    } else {
                        gameRules.put(gameRule, boolValue);
                    }
                }
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
        if (config.isLong("FullTime") || config.isInt("FullTime")) {
            fullTime = config.getLong("FullTime");
        }
    }

    private void convertLegacyGameRule(String key, String value) {
        GameRule gameRule = null;
        if (key.equals("doFireTick") && value.equals("false")) {
            gameRule = GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER;
            value = "0";
            plugin.getLogger().info("[" + name + "] GameRule updated: " + gameRule + " = " + value);
            didConvert = true;
            gameRules.put(gameRule, value);
            return;
        }
        if (Util.GAME_RULES_REMOVED.contains(key)) {
            plugin.getLogger().info("[" + name + "] GameRule removed: " + key + " = " + value);
            didConvert = true;
            return;
        }
        String key2 = Util.camelToLowerCase(key).toLowerCase();
        gameRule = GameRule.getByName(key2);
        if (gameRule != null) {
            plugin.getLogger().info("[" + name + "] GameRule cameled: " + key + " => " + key2 + " = " + value);
            didConvert = true;
            gameRules.put(gameRule, value);
            return;
        }
        gameRule = Util.GAME_RULE_CONVERSION.get(key);
        if (gameRule != null) {
            if (Util.GAME_RULES_INVERTED.contains(gameRule)) {
                if ("true".equalsIgnoreCase(value)) {
                    value = "false";
                } else if ("false".equalsIgnoreCase(value)) {
                    value = "true";
                } else {
                    plugin.getLogger().severe("[" + name + "] Invalid inverted GameRule value: " + gameRule + " = " + value);
                }
            }
            if (gameRule != null) {
                plugin.getLogger().info("[" + name + "] GameRule converted: " + key + " => " + gameRule + " = " + value);
                didConvert = true;
                gameRules.put(gameRule, value);
                return;
            }
        }
        plugin.getLogger().severe("[" + name + "] Unknown GameRule: " + key);
    }

    /**
     * This function only saves some of the settings. In general,
     * it is intended for admins to edit the config.yml, then use
     * reload to apply the changes.
     */
    public void save() {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("worlds").createSection(name);
        config.set("AutoLoad", autoLoad);
        if (worldType != null) {
            config.set("Type", worldType.name());
        }
        config.set("Environment", environment.name());
        config.set("Generator", generator);
        config.set("GeneratorSettings", generatorSettings);
        config.set("BiomeProvider", biomeProvider);
        config.set("GenerateStructures", generateStructures);
        config.set("Seed", seed);
        if (gameRules != null) {
            ConfigurationSection section = config.getConfigurationSection("GameRules");
            if (section == null) section = config.createSection("GameRules");
            for (Map.Entry<GameRule<?>, Object> entry : gameRules.entrySet()) {
                section.set(entry.getKey().getKey().getKey(), entry.getValue());
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
        if (gameMode != null) config.set("GameMode", gameMode.name());
        if (fullTime != null) config.set("FullTime", fullTime);
    }

    public void configure(World world) {
        // WorldType removed because Bukkit says it's @Deprecated
        environment = world.getEnvironment();
        // Generator name has no getter...
        seed = world.getSeed();
        if (gameRules == null) gameRules = new HashMap<>();
        for (GameRule<?> gameRule : GameRule.values()) {
            final Object value;
            try {
                value = world.getGameRuleValue(gameRule);
            } catch (IllegalArgumentException iae) {
                plugin.getLogger().log(Level.SEVERE, "[" + name + "] gameRule=" + gameRule, iae);
                continue;
            }
            gameRules.put(gameRule, value);
        }
        settings = new Settings();
        settings.configure(world);
        spawnLocation = MyLocation.of(getSpawnLocation());
        border = new Border();
        border.configure(world);
    }

    protected WorldCreator getWorldCreator() {
        WorldCreator creator = WorldCreator.name(name);
        creator.type(worldType);
        creator.environment(environment);
        creator.generator(generator);
        creator.generateStructures(generateStructures);
        if (generatorSettings != null && !generatorSettings.isEmpty()) {
            creator.generatorSettings(generatorSettings);
        }
        creator.biomeProvider(biomeProvider);
        if (seed != null) {
            creator.seed(seed);
        }
        if (settings != null) {
            creator.keepSpawnLoaded(TriState.byBoolean(settings.keepSpawnInMemory));
        }
        return creator;
    }

    public World getWorld() {
        return plugin.getServer().getWorld(name);
    }

    public World loadWorld() {
        World world = getWorld();
        if (world == null) {
            WorldCreator creator = getWorldCreator();
            world = creator.createWorld();
            if (world != null) apply(world);
        }
        return world;
    }

    private static Boolean toBoolean(Object in, Boolean dfl) {
        if (in instanceof Boolean bool) {
            return bool;
        }
        try {
            return Boolean.parseBoolean(in.toString());
        } catch (IllegalArgumentException iae) { }
        return dfl;
    }

    public void apply(World world) {
        if (gameRules != null) {
            for (Map.Entry<GameRule<?>, Object> entry : gameRules.entrySet()) {
                Class<?> type = entry.getKey().getType();
                if (type == Integer.class) {
                    @SuppressWarnings("unchecked")
                    GameRule<Integer> gameRule = (GameRule<Integer>) entry.getKey();
                    int value = NumberConversions.toInt(entry.getValue());
                    try {
                        world.setGameRule(gameRule, value);
                    } catch (IllegalArgumentException iae) {
                        plugin.getLogger().log(Level.SEVERE, "[" + name + "] " + gameRule.getKey() + " = " + value, iae);
                    }
                } else if (type == Boolean.class) {
                    @SuppressWarnings("unchecked")
                    GameRule<Boolean> gameRule = (GameRule<Boolean>) entry.getKey();
                    Boolean value = toBoolean(entry.getValue(), world.getGameRuleDefault(gameRule));
                    if (value == null) {
                        plugin.getLogger().severe("[" + name + "] Invalid boolean gamerule: " + gameRule.getKey() + " = " + entry.getValue());
                    } else {
                        world.setGameRule(gameRule, value);
                    }
                }
            }
        }
        if (settings != null) settings.apply(world);
        if (spawnLocation != null) spawnLocation.setSpawn(world);
        if (border != null) border.apply(world);
        if (fullTime != null) world.setFullTime(fullTime);
    }

    public Location getSpawnLocation() {
        World world = getWorld();
        if (world == null) return null;
        Location result = null;
        if (spawnLocation != null) result = spawnLocation.getLocation(world);
        if (result == null) result = world.getSpawnLocation();
        return result;
    }

    public void setSpawnLocation(Location location) {
        spawnLocation = MyLocation.of(location);
        spawnLocation.setSpawn(location.getWorld());
    }

    @Data
    public final class Settings {
        //   General
        private Boolean autoSave;
        private Difficulty difficulty;
        private Boolean keepSpawnInMemory;
        private Boolean pvp;
        private Integer viewDistance;
        private Integer simulationDistance;
        //   Mob Spawning
        //     Allow
        private Boolean allowMonsters;
        private Boolean allowAnimals;
        private final Map<SpawnCategory, Integer> spawnLimits = new EnumMap<>(SpawnCategory.class);
        private final Map<SpawnCategory, Integer> ticksPerSpawns = new EnumMap<>(SpawnCategory.class);

        protected void configure(ConfigurationSection config) {
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
            if (config.isSet("SimulationDistance")) {
                simulationDistance = config.getInt("SimulationDistance");
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
                for (SpawnCategory spawnCategory : SpawnCategory.values()) {
                    if (section.isSet(spawnCategory.name())) {
                        spawnLimits.put(spawnCategory, section.getInt(spawnCategory.name()));
                    }
                }
            }
            section = config.getConfigurationSection("TicksPerSpawns");
            if (section != null) {
                for (SpawnCategory spawnCategory : SpawnCategory.values()) {
                    if (section.isSet(spawnCategory.name())) {
                        ticksPerSpawns.put(spawnCategory, section.getInt(spawnCategory.name()));
                    }
                }
            }
        }

        protected void save(ConfigurationSection config) {
            config.set("AutoSave", autoSave);
            if (difficulty != null) {
                config.set("Difficulty", difficulty.name());
            }
            config.set("KeepSpawnInMemory", keepSpawnInMemory);
            config.set("PvP", pvp);
            config.set("ViewDistance", viewDistance);
            config.set("SimulationDistance", simulationDistance);
            config.set("AllowSpawns.Monster", allowMonsters);
            config.set("AllowSpawns.Animal", allowAnimals);
            for (SpawnCategory spawnCategory : SpawnCategory.values()) {
                config.set("SpawnLimits." + spawnCategory.name(), spawnLimits.get(spawnCategory));
                config.set("TicksPerSpawns." + spawnCategory.name(), ticksPerSpawns.get(spawnCategory));
            }
        }

        protected void configure(World world) {
            autoSave = world.isAutoSave();
            difficulty = world.getDifficulty();
            pvp = world.getPVP();
            viewDistance = world.getViewDistance();
            simulationDistance = world.getSimulationDistance();
            allowMonsters = world.getAllowMonsters();
            allowAnimals = world.getAllowAnimals();
            for (SpawnCategory spawnCategory : SpawnCategory.values()) {
                if (spawnCategory == SpawnCategory.MISC) continue;
                spawnLimits.put(spawnCategory, world.getSpawnLimit(spawnCategory));
                ticksPerSpawns.put(spawnCategory, (int) world.getTicksPerSpawns(spawnCategory));
            }
        }

        protected void apply(World world) {
            if (autoSave != null) world.setAutoSave(autoSave);
            if (difficulty != null) world.setDifficulty(difficulty);
            if (pvp != null) world.setPVP(pvp);
            if (viewDistance != null) world.setViewDistance(viewDistance);
            if (simulationDistance != null) world.setSimulationDistance(simulationDistance);
            if (allowMonsters != null && allowAnimals != null) {
                world.setSpawnFlags(allowMonsters, allowAnimals);
            }
            for (SpawnCategory spawnCategory : SpawnCategory.values()) {
                if (spawnCategory == SpawnCategory.MISC) continue;
                if (spawnLimits.containsKey(spawnCategory)) {
                    world.setSpawnLimit(spawnCategory, spawnLimits.get(spawnCategory));
                }
                if (ticksPerSpawns.containsKey(spawnCategory)) {
                    world.setTicksPerSpawns(spawnCategory, ticksPerSpawns.get(spawnCategory));
                }
            }
        }
    }

    @Value
    public static final class MyLocation {
        private double x;
        private double y;
        private double z;
        private float pitch;
        private float yaw;

        protected static MyLocation of(Location loc) {
            return new MyLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw());
        }

        protected static MyLocation of(ConfigurationSection config) {
            double x = config.getDouble("x");
            double y = config.getDouble("y");
            double z = config.getDouble("z");
            float pitch = (float) config.getDouble("pitch");
            float yaw = (float) config.getDouble("yaw");
            return new MyLocation(x, y, z, pitch, yaw);
        }

        protected void save(ConfigurationSection config) {
            config.set("x", x);
            config.set("y", y);
            config.set("z", z);
            config.set("pitch", pitch);
            config.set("yaw", yaw);
        }

        protected Location getLocation(World world) {
            return new Location(world, x, y, z, yaw, pitch);
        }

        protected void setSpawn(World world) {
            world.setSpawnLocation(getLocation(world));
        }
    }

    @Data
    public static final class Border {
        private double centerX;
        private double centerZ;
        private double size;
        private double damageAmount;
        private double damageBuffer;
        private int warningDistance;
        private int warningTime;

        protected void configure(ConfigurationSection config) {
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

        protected void save(ConfigurationSection config) {
            config.set("Center", Arrays.asList(centerX, centerZ));
            config.set("Size", size);
            config.set("DamageAmount", damageAmount);
            config.set("DamageBuffer", damageBuffer);
            config.set("WarningDistance", warningDistance);
            config.set("WarningTime", warningTime);
        }

        protected void configure(World world) {
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

        protected void apply(World world) {
            WorldBorder worldBorder = world.getWorldBorder();
            worldBorder.setCenter(centerX, centerZ);
            worldBorder.setSize(size);
            worldBorder.setDamageAmount(damageAmount);
            worldBorder.setDamageBuffer(damageBuffer);
            worldBorder.setWarningTime(warningTime);
        }
    }
}
