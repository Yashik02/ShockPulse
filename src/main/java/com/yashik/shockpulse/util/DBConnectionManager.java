package com.yashik.shockpulse.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnectionManager {
    private static final String URL = System.getenv("SUPABASE_DB_URL");
    private static final String USER = System.getenv("SUPABASE_DB_USER");
    private static final String PASSWORD = System.getenv("SUPABASE_DB_PASSWORD");

    public static Connection getConnection() throws SQLException {
        if (URL == null || URL.isBlank() || USER == null || PASSWORD == null) {
            throw new SQLException(
                    "Missing Supabase DB environment variables. Check SUPABASE_DB_URL, SUPABASE_DB_USER, and SUPABASE_DB_PASSWORD in .env");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Lab 15: Inspect DatabaseMetaData
    public static void printDatabaseMetadata() {
        try (Connection conn = getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println(
                    "  Database Product : " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
            System.out.println("  JDBC Driver      : " + meta.getDriverName() + " (" + meta.getDriverVersion() + ")");
            System.out.println("  Connected User   : " + meta.getUserName());
        } catch (SQLException e) {
            printSqlException(e);
        }
    }

    // Lab 16: Detailed SQLException diagnostics (SQLState, ErrorCode, Message)
    public static void printSqlException(SQLException e) {
        System.err.printf("  \u001B[31m[SQL ERROR] State: %s | Error Code: %d | Message: %s\u001B[0m%n",
                e.getSQLState(), e.getErrorCode(), e.getMessage());
    }
}