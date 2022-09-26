package com.ehhthan.persistentdatasync;

import com.ehhthan.persistentdatasync.listener.PlayerListener;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * PersistentDataSync
 * Sync player PersistentDataContainers between servers using sql.
 * @author Ehhthan
 */
@SuppressWarnings("unused")
public final class PersistentDataSync extends JavaPlugin {
    private Syncer syncer;

    // Supplies an updater task for each player.
    private final Supplier<BukkitRunnable> runnable = () -> new BukkitRunnable() {
        @Override
        public void run() {
            for (Player player : Bukkit.getOnlinePlayers()) {
                syncer.syncToDatabase(player);
            }
        }
    };

    @Override
    public void onEnable() {
        // Create default config if it doesn't exist.
        saveDefaultConfig();

        // Create metrics.
        new Metrics(this, 16506);

        // Create the syncer instance, if this fails so does the plugin.
        try {
            this.syncer = new Syncer(getConfig().getConfigurationSection("database"));
        } catch (IllegalArgumentException | SQLException e) {
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }

        // Check if plugin is enabled before registering things.
        if (Bukkit.getPluginManager().isPluginEnabled(this)) {
            // Create auto save scheduler.
            runnable.get().runTaskTimer(this, 1L, getConfig().getLong("auto-save", 300) * 20);

            // Register player listener.
            Bukkit.getPluginManager().registerEvents(new PlayerListener(syncer), this);
        }
    }

    @Override
    public void onDisable() {
        if (syncer != null)
            syncer.close();
    }

    public Syncer getSyncer() {
        return syncer;
    }
}
