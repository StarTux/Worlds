package com.winthier.worlds;

import lombok.RequiredArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
@RequiredArgsConstructor
final class PlayerListener implements Listener {
    final WorldsPlugin plugin;

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
        if (player.hasPermission("worlds.override") && player.isPermissionSet("worlds.override")) return;
        final MyWorld myWorld = plugin.worldByName(player.getWorld().getName());
        if (myWorld == null) return;
        final GameMode gameMode = myWorld.getGameMode();
        if (gameMode != null) player.setGameMode(gameMode);
    }
}
