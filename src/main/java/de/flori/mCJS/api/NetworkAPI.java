package de.flori.mCJS.api;

import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;

/**
 * API module for network operations (HTTP requests)
 */
public class NetworkAPI extends BaseAPI {
    
    public NetworkAPI(JavaPlugin plugin) {
        super(plugin);
    }
    
    // ===== HTTP REQUESTS =====
    public String httpGet(String url) {
        try {
            URI uri = new URI(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                return response.toString().trim();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error making HTTP GET request to '" + url + "': " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    public String httpPost(String url, String data) {
        try {
            URI uri = new URI(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = data.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                return response.toString().trim();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error making HTTP POST request to '" + url + "': " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }
}
