package com.winthier.worlds;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.PortalType;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
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
        if (myWorld.applyPortalTravel(event.getPortalTravelAgent(), player, from, portalType)) {
            event.setCancelled(true);
        }
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
        if (myWorld.applyPortalTravel(event.getPortalTravelAgent(), entity, from, portalType)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
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
