package com.ehhthan.persistentdatasync;

import com.ehhthan.persistentdatasync.listener.PlayerListener;
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
public final class PersistentDataSync extends JavaPlugin {
    private Syncer syncer;

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

        // Create the syncer instance, if this fails so does the plugin.
        try {
            this.syncer = new Syncer(getConfig().getConfigurationSection("database"));
        } catch (IllegalArgumentException | SQLException e) {
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }

        // Create auto save scheduler.
        runnable.get().runTaskTimer(this, 1L, getConfig().getLong("auto-save", 300) * 20);

        // Register player listener.
        Bukkit.getPluginManager().registerEvents(new PlayerListener(syncer), this);
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
