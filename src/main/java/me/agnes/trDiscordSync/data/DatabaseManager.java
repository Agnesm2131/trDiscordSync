package me.agnes.trDiscordSync.data;

import me.agnes.trDiscordSync.trDiscordSync;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static Connection connection;

    public static void init() {
        try {
            File dbFile = new File(trDiscordSync.getInstance().getDataFolder(), "data.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);

            try (Statement st = connection.createStatement()) {
                st.execute(
                        "CREATE TABLE IF NOT EXISTS eslestirmeler (" +
                                "uuid TEXT PRIMARY KEY, " +
                                "discord_id TEXT UNIQUE, " +
                                "iki_fa INTEGER DEFAULT 0, " +
                                "ip TEXT, " +
                                "odul INTEGER DEFAULT 0, " +
                                "is_booster INTEGER DEFAULT 0" +
                                ");"
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        return connection;
    }
}
