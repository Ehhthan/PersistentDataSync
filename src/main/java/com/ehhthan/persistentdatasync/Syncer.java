package com.ehhthan.persistentdatasync;

import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTPersistentDataContainer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Syncer {
    // Name of the table
    private final static String TABLE_NAME = "persistent_data_sync";

    // The main connection of the syncer.
    private static Connection CONNECTION;

    /**
     * Create a syncer instance.
     * @param database Database config section
     * @throws SQLException If there is an issue with sql.
     * @throws IllegalArgumentException If the database is not defined correctly.
     */
    public Syncer(ConfigurationSection database) throws SQLException {
        // Fail if database info does not exist in the config.yml
        if (database == null) {
            throw new IllegalArgumentException("No database has been defined in the config.yml.");
        }

        // Initialize all connection info from the config.yml into variables.
        String host = database.getString("host", "");
        int port = database.getInt("port", 0);
        String user = database.getString("user", "");
        String password = database.getString("password", "");
        String databaseName = database.getString("database-name", "");

        // Attempt to make a connection.
        CONNECTION = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s?autoReconnect=true", host, port, databaseName), user, password);

        try (Statement statement = CONNECTION.createStatement()) {
            // Attempt to create the database if it already does not exist.
            if (!statement.executeQuery(String.format("SHOW DATABASES LIKE '%s';", databaseName)).next())
                statement.execute(String.format("CREATE DATABASE %s;", databaseName));
            // Select the correct database.
            statement.execute(String.format("USE %s;", databaseName));
            // Attempt to create the table if it already does not exist.
            statement.execute(String.format("CREATE TABLE IF NOT EXISTS %s (uuid VARCHAR(36) NOT NULL, container LONGTEXT, PRIMARY KEY (uuid));", TABLE_NAME));
        }
    }

    /**
     * Save the player's current persistent data container to the database.
     * @param player Syncing player
     */
    public void syncToDatabase(Player player) {
        NBTPersistentDataContainer container = new NBTPersistentDataContainer(player.getPersistentDataContainer());
        try (PreparedStatement statement = CONNECTION.prepareStatement(String.format("INSERT INTO %s(uuid,container) VALUES(?,?) ON DUPLICATE KEY UPDATE container = ?;", TABLE_NAME))) {
            statement.setString(1, player.getUniqueId().toString());
            statement.setString(2, container.toString());
            statement.setString(3, container.toString());
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the player's current persistent data container to the one in the database.
     * @param player Syncing player
     */
    public void syncFromDatabase(Player player) {
        try (PreparedStatement statement = CONNECTION.prepareStatement(String.format("SELECT container FROM %s WHERE uuid = ?;", TABLE_NAME))) {
            // Select the player's stored data.
            statement.setString(1, player.getUniqueId().toString());
            ResultSet resultSet = statement.executeQuery();
            // If the player has stored data, we set their persistent data container to it.
            if (resultSet.next()) {
                // Get current container.
                NBTPersistentDataContainer container = new NBTPersistentDataContainer(player.getPersistentDataContainer());
                // Clear old nbt.
                container.clearNBT();
                // Set the persistent data container to the container saved in the database.
                container.mergeCompound(new NBTContainer(resultSet.getString("container")));
            } // If no stored data, we save their current persistent data.
            else {
                syncToDatabase(player);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Close the connection to the database.
     */
    public void close() {
        try {
            if (CONNECTION != null && !CONNECTION.isClosed()) {
                CONNECTION.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
