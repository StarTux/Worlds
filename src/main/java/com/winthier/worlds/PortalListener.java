package com.winthier.worlds;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.PortalType;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerTeleportEvent;

@RequiredArgsConstructor
final class PortalListener implements Listener {
    final WorldsPlugin plugin;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerPortal(final PlayerPortalEvent event) {
        final Player player = event.getPlayer();
        Location from = event.getFrom();
        MyWorld myWorld = plugin.worldByName(from.getWorld().getName());
        if (myWorld == null) return;
        PortalType portalType;
        TeleportCause cause = event.getCause();
        if (cause == TeleportCause.NETHER_PORTAL) {
            portalType = PortalType.NETHER;
        } else if (cause == TeleportCause.END_PORTAL) {
            portalType = PortalType.ENDER;
        } else {
            return;
        }
        Portal portal = myWorld.applyPortalTravel(player, from, portalType);
        if (portal == null) return;
        Location to = portal.apply(player, from);
        if (portal.cancel) {
            event.setCancelled(true);
            if (to != null) {
                Bukkit.getScheduler().runTask(plugin, () -> player.teleport(to, event.getCause()));
            }
        } else {
            if (to != null) {
                event.setTo(to);
            } else {
                event.setCancelled(true);
            }
        }
        event.setSearchRadius(portal.searchRadius);
        event.setCanCreatePortal(portal.createPortal);
        event.setCreationRadius(portal.searchRadius);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityPortal(EntityPortalEvent event) {
        Entity entity = event.getEntity();
        Location from = event.getFrom();
        MyWorld myWorld = plugin.worldByName(from.getWorld().getName());
        if (myWorld == null) return;
        PortalType portalType = null;
        Block block = entity.getLocation().getBlock();
        for (BlockFace dir: Arrays.asList(BlockFace.SELF, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.DOWN, BlockFace.UP)) {
            Block block2 = block.getRelative(dir);
            Material mat = block2.getType();
            if (mat == Material.NETHER_PORTAL) {
                portalType = PortalType.NETHER;
                break;
            } else if (mat == Material.END_PORTAL) {
                portalType = PortalType.ENDER;
                break;
            }
        }
        if (portalType == null) return;
        if (portalType == PortalType.ENDER && entity instanceof FallingBlock) {
            // Quick fix: Falling blocks end up in public locations.
            event.setCancelled(true);
        }
        Portal portal = myWorld.applyPortalTravel(entity, from, portalType);
        if (portal == null) return;
        Location to = portal.apply(entity, from);
        if (portal.cancel) {
            event.setCancelled(true);
            if (to != null) {
                Bukkit.getScheduler().runTask(plugin, () -> entity.teleport(to));
            }
        } else {
            if (to != null) {
                event.setTo(to);
            } else {
                event.setCancelled(true);
            }
        }
        event.setSearchRadius(portal.searchRadius);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL && to.getWorld().getEnvironment() == World.Environment.NETHER) {
            Block block = to.getBlock();
            Block above = block.getRelative(0, 3, 0);
            if (block.getType() != Material.NETHER_PORTAL && above.getType() == Material.NETHER_PORTAL) {
                String msg = String
                    .format("Portal teleport %s to %s:%.02f,%.02f,%.02f",
                            event.getPlayer().getName(),
                            to.getWorld().getName(), to.getX(), to.getY(), to.getZ());
                plugin.getLogger().info(msg);
                event.setTo(above.getLocation().add(0.5, 0.0, 0.5));
            }
        }
        if (event.getPlayer().isOp()) return;
        WorldBorder border = event.getTo().getWorld().getWorldBorder();
        Location center = border.getCenter();
        double size = border.getSize() * 0.5;
        double x = event.getTo().getX();
        double z = event.getTo().getZ();
        if (x > center.getX() + size
            || x < center.getX() - size
            || z > center.getZ() + size
            || z < center.getZ() - size) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();
        if (player.hasPermission("worlds.override")) return;
        final MyWorld myWorld = plugin.worldByName(player.getWorld().getName());
        if (myWorld == null) return;
        final GameMode gameMode = myWorld.getGameMode();
        if (gameMode != null) player.setGameMode(gameMode);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (player.hasPermission("worlds.override")) return;
        final MyWorld myWorld = plugin.worldByName(player.getWorld().getName());
        if (myWorld == null) return;
        final GameMode gameMode = myWorld.getGameMode();
        if (gameMode != null) player.setGameMode(gameMode);
    }
}
