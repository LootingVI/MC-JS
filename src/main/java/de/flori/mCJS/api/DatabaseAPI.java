package de.flori.mCJS.api;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API module for SQLite database operations
 */
public class DatabaseAPI extends BaseAPI {
    
    public DatabaseAPI(JavaPlugin plugin) {
        super(plugin);
    }
    
    private java.sql.Connection getDatabaseConnection(String dbName) throws java.sql.SQLException {
        File dbFile = new File(plugin.getDataFolder(), dbName + ".db");
        return java.sql.DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
    }

    public void executeSQL(String dbName, String sql) {
        try (java.sql.Connection conn = getDatabaseConnection(dbName);
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing SQL '" + sql + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Map<String, Object>> querySQL(String dbName, String sql) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (java.sql.Connection conn = getDatabaseConnection(dbName);
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {

            java.sql.ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                results.add(row);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error querying SQL '" + sql + "': " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }

    public void createTable(String dbName, String tableName, Map<String, String> columns) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (");
        boolean first = true;
        for (Map.Entry<String, String> column : columns.entrySet()) {
            if (!first) sql.append(", ");
            sql.append(column.getKey()).append(" ").append(column.getValue());
            first = false;
        }
        sql.append(")");
        executeSQL(dbName, sql.toString());
    }

    public void insertData(String dbName, String tableName, Map<String, Object> data) {
        insertDataAndGetId(dbName, tableName, data);
    }

    public long insertDataAndGetId(String dbName, String tableName, Map<String, Object> data) {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        List<Object> params = new ArrayList<>();

        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                columns.append(", ");
                values.append(", ");
            }
            columns.append(entry.getKey());
            values.append("?");
            params.add(entry.getValue());
            first = false;
        }

        String sql = "INSERT OR REPLACE INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";

        try (java.sql.Connection conn = getDatabaseConnection(dbName);
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            stmt.executeUpdate();
            
            // Get the generated ID
            try (java.sql.ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
            
            // Fallback: use last_insert_rowid()
            try (java.sql.Statement stmt2 = conn.createStatement();
                 java.sql.ResultSet rs = stmt2.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error inserting data: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }
    
    // ===== EXTENDED DATABASE OPERATIONS =====
    public void updateData(String dbName, String tableName, Map<String, Object> data, String whereClause) {
        StringBuilder setClause = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) setClause.append(", ");
            setClause.append(entry.getKey()).append(" = ?");
            params.add(entry.getValue());
            first = false;
        }
        
        String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + whereClause;
        
        try (java.sql.Connection conn = getDatabaseConnection(dbName);
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            stmt.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("Error updating data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteData(String dbName, String tableName, String whereClause) {
        String sql = "DELETE FROM " + tableName + " WHERE " + whereClause;
        executeSQL(dbName, sql);
    }

    public int countRows(String dbName, String tableName) {
        return countRows(dbName, tableName, null);
    }

    public int countRows(String dbName, String tableName, String whereClause) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        if (whereClause != null && !whereClause.isEmpty()) {
            sql += " WHERE " + whereClause;
        }
        
        List<Map<String, Object>> results = querySQL(dbName, sql);
        if (!results.isEmpty()) {
            Object count = results.get(0).get("COUNT(*)");
            if (count instanceof Number) {
                return ((Number) count).intValue();
            }
        }
        return 0;
    }
}
