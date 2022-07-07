# Worlds
This plugin loads worlds on demand and provides some basic world configurations.

## Concepts
All worlds are configured in the `config.yml` file, see below. For each entry in the worlds section, the plugin will keep tabs on the world of that name, if it exists.

Once the configuration is (re)loaded, which happens at plugin load time and when the `reload` command is issued, if the `AutoLoad` option is set to `true` and the world is not currently loaded, it will be loaded at that time, from the server's world folder. All the other settings are then applied, such as difficulty and game rules.

The way to add a world is to make the necessary entries in the configuration file: Name, environment, then issue a reload. Further settings can be imported to the config with the `/world import` command.

To make changes to world settings during runtime, it is necessary to first issue a `reload`, then an `apply`, see below. Applying settings to worlds takes time and causes significant lag spikes, which is why this step is not automated for every reload.

## Commands
- `worlds` - The admin interface.
- `wtp` - Teleport to a named world.
- `/world list` - List worlds
- `/world who` - List players in worlds
- `/world reload` - Reload config
- `/world apply` - (Re)apply world settings
- `/world listloaded` - List loaded Bukkit worlds
- `/world import <world>` - Import Bukkit world settings
- `/world setspawn` - Set world spawn
- `/world unload <world>` - Unload Bukkit world

## Permissions
- `worlds.worlds` - Use the `/worlds` command.
- `worlds.wtp` - Teleport to worlds via `/wtp`.
- `worlds.override` - Override player based world settings, such as gamemode.

## Configuration
Each world gets a named entry in the worlds section. There are various options, some of which mirror world options in Spigot's Server or World classes, others also exist in the global server settings. Not setting an option will use the default value, or cause the plugin not to take action where it applies.
```yaml
worlds:
  Example:
    AutoLoad: false
    Type: NORMAL # AMPLIFIED, CUSTOMIZED, FLAT, LARGE_BIOMES, NORMAL, VERSION_1_1
    Environment: NORMAL # NETHER, NORMAL, THE_END
    GenerateStructures: true
    Generator: VoidGenerator
    GeneratorSettings: ''
    Seed: ''
    RushNight: NEVER # NEVER, SLEEP, ALWAYS
    GameMode: SURVIVAL # ADVENTURE, CREATIVE, SPECTATOR, SURVIVAL
    GameRules:
      doMobGriefing: false
    Settings:
      AutoSave: true
      Difficulty: NORMAL # EASY, HARD, NORMAL, PEACEFUL
      KeepSpawnInMemory: true
      PvP: false
      AllowSpawns:
        Monster: true
        Animal: true
      SpawnLimits:
        Amient: 15
        Animal: 15
        Monster: 70
        WaterAnimal: 5
      TicksPer:
        AnimalSpawn: 400
        MonsterSpawn: 1
    SpawnLocation:
      x: 0.0
      y: 65.0
      z: 0.0
      pitch: 0.0
      yaw: 0.0
    Border:
      Center: [ 0, 0 ]
      Size: 10000
      DamageBuffer: 0
      WarningDistance: 0
      WarningTime: 0
```