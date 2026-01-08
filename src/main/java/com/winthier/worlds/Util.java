package com.winthier.worlds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.GameRule;
import org.bukkit.GameRules;

public final class Util {
    private Util() { }

    public static List<String> splitCamelCase(String src) {
        List<String> tokens = new ArrayList<>();
        int wordStart = 0;
        char c = src.charAt(0);
        int capsCount = Character.isUpperCase(c) ? 1 : 0;
        for (int i = 1; i < src.length(); ++i) {
            c = src.charAt(i);
            if (Character.isUpperCase(c)) {
                switch (capsCount) {
                case 0:
                    tokens.add(src.substring(wordStart, i));
                    wordStart = i;
                    break;
                default:
                    break;
                }
                capsCount += 1;
            } else {
                switch (capsCount) {
                case 0:
                case 1:
                    break;
                default:
                    tokens.add(src.substring(wordStart, i - 1));
                    wordStart = i - 1;
                }
                capsCount = 0;
            }
        }
        tokens.add(src.substring(wordStart, src.length()));
        return tokens;
    }

    public static String camelToLowerCase(String src) {
        List<String> tokens = splitCamelCase(src);
        return String.join("_", tokens.toArray(new String[0]));
    }

    public static final Map<String, GameRule> GAME_RULE_CONVERSION = new HashMap<>() {{
        put("doFireTick", GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER);
        put("allowFireTicksAwayFromPlayer", GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER);
        put("mobGriefing", GameRules.MOB_GRIEFING);
        put("keepInventory", GameRules.KEEP_INVENTORY);
        put("doMobSpawning", GameRules.SPAWN_MOBS);
        put("doMobLoot", GameRules.MOB_DROPS);
        put("projectilesCanBreakBlocks", GameRules.PROJECTILES_CAN_BREAK_BLOCKS);
        put("doTileDrops", GameRules.BLOCK_DROPS);
        put("doEntityDrops", GameRules.ENTITY_DROPS);
        put("commandBlockOutput", GameRules.COMMAND_BLOCK_OUTPUT);
        put("naturalRegeneration", GameRules.NATURAL_HEALTH_REGENERATION);
        put("doDaylightCycle", GameRules.ADVANCE_TIME);
        put("logAdminCommands", GameRules.LOG_ADMIN_COMMANDS);
        put("showDeathMessages", GameRules.SHOW_DEATH_MESSAGES);
        put("randomTickSpeed", GameRules.RANDOM_TICK_SPEED);
        put("sendCommandFeedback", GameRules.SEND_COMMAND_FEEDBACK);
        put("reducedDebugInfo", GameRules.REDUCED_DEBUG_INFO);
        put("spectatorsGenerateChunks", GameRules.SPECTATORS_GENERATE_CHUNKS);
        put("spawnRadius", GameRules.RESPAWN_RADIUS);
        put("disablePlayerMovementCheck", GameRules.PLAYER_MOVEMENT_CHECK);
        put("disableElytraMovementCheck", GameRules.ELYTRA_MOVEMENT_CHECK);
        put("maxEntityCramming", GameRules.MAX_ENTITY_CRAMMING);
        put("doWeatherCycle", GameRules.ADVANCE_WEATHER);
        put("doLimitedCrafting", GameRules.LIMITED_CRAFTING);
        put("maxCommandChainLength", GameRules.MAX_COMMAND_SEQUENCE_LENGTH);
        put("maxCommandForkCount", GameRules.MAX_COMMAND_FORKS);
        put("commandModificationBlockLimit", GameRules.MAX_BLOCK_MODIFICATIONS);
        put("announceAdvancements", GameRules.SHOW_ADVANCEMENT_MESSAGES);
        put("disableRaids", GameRules.RAIDS);
        put("doInsomnia", GameRules.SPAWN_PHANTOMS);
        put("doImmediateRespawn", GameRules.IMMEDIATE_RESPAWN);
        put("playersNetherPortalDefaultDelay", GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY);
        put("playersNetherPortalCreativeDelay", GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY);
        put("drowningDamage", GameRules.DROWNING_DAMAGE);
        put("fallDamage", GameRules.FALL_DAMAGE);
        put("fireDamage", GameRules.FIRE_DAMAGE);
        put("freezeDamage", GameRules.FREEZE_DAMAGE);
        put("doPatrolSpawning", GameRules.SPAWN_PATROLS);
        put("doTraderSpawning", GameRules.SPAWN_WANDERING_TRADERS);
        put("doWardenSpawning", GameRules.SPAWN_WARDENS);
        put("forgiveDeadPlayers", GameRules.FORGIVE_DEAD_PLAYERS);
        put("universalAnger", GameRules.UNIVERSAL_ANGER);
        put("playersSleepingPercentage", GameRules.PLAYERS_SLEEPING_PERCENTAGE);
        put("blockExplosionDropDecay", GameRules.BLOCK_EXPLOSION_DROP_DECAY);
        put("mobExplosionDropDecay", GameRules.MOB_EXPLOSION_DROP_DECAY);
        put("tntExplosionDropDecay", GameRules.TNT_EXPLOSION_DROP_DECAY);
        put("snowAccumulationHeight", GameRules.MAX_SNOW_ACCUMULATION_HEIGHT);
        put("waterSourceConversion", GameRules.WATER_SOURCE_CONVERSION);
        put("lavaSourceConversion", GameRules.LAVA_SOURCE_CONVERSION);
        put("globalSoundEvents", GameRules.GLOBAL_SOUND_EVENTS);
        put("doVinesSpread", GameRules.SPREAD_VINES);
        put("enderPearlsVanishOnDeath", GameRules.ENDER_PEARLS_VANISH_ON_DEATH);
        put("minecartMaxSpeed", GameRules.MAX_MINECART_SPEED);
        put("tntExplodes", GameRules.TNT_EXPLODES);
        put("locatorBar", GameRules.LOCATOR_BAR);
        put("pvp", GameRules.PVP);
        put("allowEnteringNetherUsingPortals", GameRules.ALLOW_ENTERING_NETHER_USING_PORTALS);
        put("spawnMonsters", GameRules.SPAWN_MONSTERS);
        put("commandBlocksEnabled", GameRules.COMMAND_BLOCKS_WORK);
        put("spawnerBlocksEnabled", GameRules.SPAWNER_BLOCKS_WORK);
    }};

    public static final Set GAME_RULES_REMOVED = Set.of(
        "allowFireTicksAwayFromPlayer",
        "doFireTick",
        "spawnChunkRadius"
    );

    public static final Set<GameRule> GAME_RULES_INVERTED = Set.of(
        GameRules.ELYTRA_MOVEMENT_CHECK,
        GameRules.PLAYER_MOVEMENT_CHECK,
        GameRules.RAIDS
    );
}
