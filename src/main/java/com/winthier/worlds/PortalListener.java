package com.winthier.worlds;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.PortalType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;

@RequiredArgsConstructor
class PortalListener implements Listener {
    final WorldsPlugin plugin;
    final int PORTAL_COOLDOWN = 100;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerPortal(final PlayerPortalEvent event) {
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
        final Location to = myWorld.applyPortalTravel(event, event.getPortalTravelAgent(), from, portalType);
        final Player player = event.getPlayer();
        if (to != null) {
            event.setCancelled(true);
            new BukkitRunnable() {
                @Override public void run() {
                    // The event likes to put players right below
                    // the portal so they either suffocate or drop
                    // to their doom.  Therefore, we cancel the
                    // event and do the teleport manually.
                    if (!player.isValid()) return;
                    player.teleport(to);
                    player.setPortalCooldown(Math.max(player.getPortalCooldown(), PORTAL_COOLDOWN));
                    plugin.getLogger().info(String.format("Portal teleport %s to %s %.02f %.02f %.02f", player.getName(), to.getWorld().getName(), to.getX(), to.getY(), to.getZ()));
                }
            }.runTask(plugin);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityPortal(EntityPortalEvent event) {
        Location from = event.getFrom();
        MyWorld myWorld = plugin.worldByName(from.getWorld().getName());
        if (myWorld == null) return;
        PortalType portalType = null;
        Block block = event.getEntity().getLocation().getBlock();
        for (BlockFace dir: Arrays.asList(BlockFace.SELF, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.DOWN, BlockFace.UP)) {
            Block block2 = block.getRelative(dir);
            Material mat = block2.getType();
            if (mat == Material.PORTAL) {
                portalType = PortalType.NETHER;
                break;
            } else if (mat == Material.ENDER_PORTAL) {
                portalType = PortalType.ENDER;
                break;
            }
        }
        if (portalType == null) return;
        Location to = myWorld.applyPortalTravel(event, event.getPortalTravelAgent(), from, portalType);
        if (to != null) event.setTo(to);
        if (event.isCancelled()) event.getEntity().setPortalCooldown(Math.max(event.getEntity().getPortalCooldown(), PORTAL_COOLDOWN));
    }
}
