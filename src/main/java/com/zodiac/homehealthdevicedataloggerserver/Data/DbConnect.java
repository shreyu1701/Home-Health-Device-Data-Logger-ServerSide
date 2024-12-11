package com.zodiac.homehealthdevicedataloggerserver.Data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnect {

    public static Connection getConnection() {
        Connection connection = null;
        try {
            String url = "jdbc:oracle:thin:@calvin.humber.ca:1521:grok";
            String user = "n01660845";
            String password = "oracle";
            connection = DriverManager.getConnection(url, user, password);
            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
