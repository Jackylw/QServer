/**
 * @author Jacky Feng
 * @date 2024/3/22 14:57
 */
package top.fexample.qchat.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/qchat";
    private static final String USER = "root";
    private static final String PASSWORD = "root";
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }
}
