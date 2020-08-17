package com.winthier.worlds;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.PortalType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

@RequiredArgsConstructor
final class Portal {
    final WorldsPlugin plugin;
    final MyWorld myWorld;
    final PortalType type;
    String destination;
    double ratio;
    boolean cancel;
    boolean createPortal;
    boolean toWorldSpawn;
    int searchRadius = 16;
    transient boolean warned = false;
    static final String META_PORTING = "worlds.porting";

    /**
     * Return true if the event shall be cancelled because we take
     * over, false otherwise.
     */
    boolean apply(Entity entity, Location from, Consumer<Location> cons) {
        //if (entity.hasMetadata(META_PORTING)) return true;
        if (destination == null || destination.isEmpty()) return true;
        World world = plugin.getServer().getWorld(destination);
        if (world == null) {
            if (!warned) {
                plugin.getLogger()
                    .warning("Portal destination world not found: "
                             + destination);
                warned = true;
            }
            return true;
        }
        Location to;
        if (toWorldSpawn) {
            to = world.getSpawnLocation();
        } else {
            to = new Location(world,
                              from.getX() * ratio,
                              from.getY(),
                              from.getZ() * ratio,
                              from.getYaw(), from.getPitch());
        }
        cons.accept(to);
        return false;
        //entity.setMetadata(META_PORTING, new FixedMetadataValue(plugin, true));
        //world.getChunkAtAsync(to, (c) -> targetChunkLoaded(to, entity));
        //return targetChunkLoaded(to, entity, cons);
    }

    boolean targetChunkLoaded(Location loc, Entity entity, Consumer<Location> cons) {
        if (!entity.isValid()) return true;
        //entity.removeMetadata(META_PORTING, plugin);
        Location dest = toWorldSpawn ? loc : findDestination(loc);
        if (dest == null) return true;
        // if (createPortal) {
        //     // TODO
        // }
        // entity.setVelocity(new Vector(0.0, 0.0, 0.0));
        // entity.setFallDistance(0.0f);
        cons.accept(dest);
        if (entity instanceof Player) {
            String msg = String
                .format("Portal teleport %s to %s %.02f %.02f %.02f",
                        entity.getName(), dest.getWorld().getName(),
                        dest.getX(), dest.getY(), dest.getZ());
            plugin.getLogger().info(msg);
        }
        return false;
    }

    class SearchResult {
        Block target = null;
        Block fallback = null;
    }

    private boolean searchColumn(final World world,
                                 final int x,
                                 final int oldy,
                                 final int z,
                                 final SearchResult result) {
        int by = world.getEnvironment() == World.Environment.NETHER
            ? 124 : world.getHighestBlockYAt(x, z);
        searchBlock(world.getBlockAt(x, oldy, z), result);
        int maxi = Math.max(oldy, 255 - oldy);
        for (int i = 0; i <= maxi; i += 1) {
            int y1 = oldy + i;
            if (y1 >= 0 && y1 <= by) {
                if (searchBlock(world.getBlockAt(x, y1, z), result)) {
                    return true;
                }
            }
            int y2 = oldy - i - 1;
            if (y2 >= 0 && y2 <= by) {
                if (searchBlock(world.getBlockAt(x, y2, z), result)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean searchBlock(final Block block,
                                final SearchResult result) {
        if (!block.getType().isSolid()) return false;
        final Block l1 = block.getRelative(0, 1, 0);
        final Block l2 = block.getRelative(0, 2, 0);
        if (type == PortalType.NETHER
            && l1.getType() == Material.NETHER_PORTAL
            && l2.getType() == Material.NETHER_PORTAL) {
            result.target = l1;
            return true;
        }
        if (result.fallback == null
            && l1.isEmpty() && l2.isEmpty()) {
            if (type == PortalType.NETHER) {
                result.fallback = l1;
            } else {
                result.target = l1;
                return true;
            }
        }
        return false;
    }

    Location findDestination(Location loc) {
        final World world = loc.getWorld();
        final int cx = loc.getBlockX();
        final int cz = loc.getBlockZ();
        final SearchResult result = new SearchResult();
        OUTER:
        for (int r = 0; r <= searchRadius; r += 1) {
            final int ax = cx - r;
            final int bx = cx + r;
            final int az = cz - r;
            final int bz = cz + r;
            final int y = loc.getBlockY();
            for (int x = ax; x <= bx; x += 1) {
                if (searchColumn(world, x, y, az, result)) break OUTER;
                if (az == bz) continue;
                if (searchColumn(world, x, y, bz, result)) break OUTER;
                if (result.target != null) break OUTER;
            }
            for (int z = az + 1; z < bz; z += 1) {
                if (searchColumn(world, ax, y, z, result)) break OUTER;
                if (ax == bx) continue;
                if (searchColumn(world, bx, y, z, result)) break OUTER;
            }
        }
        Block target;
        if (result.target != null) {
            target = result.target;
        } else if (result.fallback != null) {
            target = result.fallback;
        } else {
            return null;
        }
        return new Location(world,
                            (double) target.getX() + 0.5,
                            (double) target.getY(),
                            (double) target.getZ() + 0.5,
                            loc.getYaw(), loc.getPitch());
    }

    void save(ConfigurationSection config) {
        config.set("Destination", destination);
        config.set("Ratio", ratio);
        config.set("Cancel", cancel);
        config.set("CreatePortal", createPortal);
        config.set("ToWorldSpawn", toWorldSpawn);
        config.set("SearchRadius", searchRadius);
    }

    void configure(ConfigurationSection config) {
        destination = config.getString("Destination", null);
        ratio = config.getDouble("Ratio", 1.0);
        cancel = config.getBoolean("Cancel", false);
        createPortal = config.getBoolean("CreatePortal", false);
        toWorldSpawn = config.getBoolean("ToWorldSpawn", false);
        searchRadius = config.getInt("SearchRadius", searchRadius);
    }
}
