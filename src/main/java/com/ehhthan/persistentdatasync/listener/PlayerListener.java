package com.ehhthan.persistentdatasync.listener;

import com.ehhthan.persistentdatasync.Syncer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final Syncer syncer;

    public PlayerListener(Syncer syncer) {
        this.syncer = syncer;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        syncer.syncFromDatabase(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        syncer.syncToDatabase(event.getPlayer());
    }
}
