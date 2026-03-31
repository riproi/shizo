package ru.obsidianspire.sanity.data;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class SQLiteProvider implements DataProvider {
    private final JavaPlugin plugin;
    private final String connectionString;
    private Connection connection;

    public SQLiteProvider(JavaPlugin plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File dbFile = new File(dataFolder, "sanity.db");
        this.connectionString = "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    @Override
    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(connectionString);
            createTables();
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite connection", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "sanity DOUBLE DEFAULT 100.0, " +
                    "tol_aminazine INT DEFAULT 0, " +
                    "tol_haloperidol INT DEFAULT 0, " +
                    "tol_clozapine INT DEFAULT 0, " +
                    "aminazine_until BIGINT DEFAULT 0, " +
                    "haloperidol_until BIGINT DEFAULT 0, " +
                    "clozapine_until BIGINT DEFAULT 0, " +
                    "last_aminazine_use BIGINT DEFAULT 0, " +
                    "last_haloperidol_use BIGINT DEFAULT 0, " +
                    "last_clozapine_use BIGINT DEFAULT 0" +
                    ");");

            statement.execute("CREATE TABLE IF NOT EXISTS server_data (" +
                    "key VARCHAR(64) PRIMARY KEY, " +
                    "value VARCHAR(255)" +
                    ");");
        }
    }

    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        String sql = "SELECT * FROM players WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                data.setSanity(rs.getDouble("sanity"));
                data.setToleranceAminazine(rs.getInt("tol_aminazine"));
                data.setToleranceHaloperidol(rs.getInt("tol_haloperidol"));
                data.setToleranceClozapine(rs.getInt("tol_clozapine"));
                data.setAminazineActiveUntil(rs.getLong("aminazine_until"));
                data.setHaloperidolActiveUntil(rs.getLong("haloperidol_until"));
                data.setClozapineActiveUntil(rs.getLong("clozapine_until"));
                data.setLastAminazineUse(rs.getLong("last_aminazine_use"));
                data.setLastHaloperidolUse(rs.getLong("last_haloperidol_use"));
                data.setLastClozapineUse(rs.getLong("last_clozapine_use"));
            } else {
                savePlayerData(data); // First time join
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + uuid, e);
        }
        return data;
    }

    @Override
    public void savePlayerData(PlayerData data) {
        String sql = "INSERT OR REPLACE INTO players (uuid, sanity, tol_aminazine, tol_haloperidol, tol_clozapine, aminazine_until, haloperidol_until, clozapine_until, last_aminazine_use, last_haloperidol_use, last_clozapine_use) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, data.getUuid().toString());
            statement.setDouble(2, data.getSanity());
            statement.setInt(3, data.getToleranceAminazine());
            statement.setInt(4, data.getToleranceHaloperidol());
            statement.setInt(5, data.getToleranceClozapine());
            statement.setLong(6, data.getAminazineActiveUntil());
            statement.setLong(7, data.getHaloperidolActiveUntil());
            statement.setLong(8, data.getClozapineActiveUntil());
            statement.setLong(9, data.getLastAminazineUse());
            statement.setLong(10, data.getLastHaloperidolUse());
            statement.setLong(11, data.getLastClozapineUse());
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + data.getUuid(), e);
        }
    }

    @Override
    public void saveWipeStatus(boolean active) {
        String sql = "INSERT OR REPLACE INTO server_data (key, value) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "wipe_event_active");
            statement.setString(2, String.valueOf(active));
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save wipe status", e);
        }
    }

    @Override
    public boolean loadWipeStatus() {
        String sql = "SELECT value FROM server_data WHERE key = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "wipe_event_active");
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return Boolean.parseBoolean(rs.getString("value"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load wipe status", e);
        }
        return false;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to close SQLite connection", e);
        }
    }
}
