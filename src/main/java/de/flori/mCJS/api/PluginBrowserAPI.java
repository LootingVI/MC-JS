package de.flori.mCJS.api;

import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API module for plugin browser integration
 * Allows browsing and installing plugins from the MC-JS Plugin Browser
 */
public class PluginBrowserAPI extends BaseAPI {
    
    private final NetworkAPI networkAPI;
    private String browserUrl;
    
    public PluginBrowserAPI(JavaPlugin plugin, NetworkAPI networkAPI) {
        super(plugin);
        this.networkAPI = networkAPI;
        // Default browser URL, can be configured
        this.browserUrl = plugin.getConfig().getString("plugin-browser.url", "https://browser.flori.tv");
    }
    
    /**
     * Set the browser server URL
     */
    public void setBrowserUrl(String url) {
        this.browserUrl = url;
    }
    
    /**
     * Get the browser server URL
     */
    public String getBrowserUrl() {
        return browserUrl;
    }
    
    /**
     * Search for plugins in the browser
     * @param query Search query (optional)
     * @param category Category filter (optional)
     * @return List of plugin info maps
     */
    public List<Map<String, Object>> searchPlugins(String query, String category) {
        List<Map<String, Object>> plugins = new ArrayList<>();
        
        try {
            StringBuilder url = new StringBuilder(browserUrl + "/api/plugins?");
            if (query != null && !query.isEmpty()) {
                url.append("search=").append(java.net.URLEncoder.encode(query, "UTF-8")).append("&");
            }
            if (category != null && !category.isEmpty()) {
                url.append("category=").append(java.net.URLEncoder.encode(category, "UTF-8")).append("&");
            }
            
            String response = networkAPI.httpGet(url.toString());
            if (response == null || response.isEmpty()) {
                plugin.getLogger().warning("Empty response from plugin browser");
                return plugins;
            }
            
            JSONParser parser = new JSONParser();
            JSONArray jsonArray = (JSONArray) parser.parse(response);
            
            for (Object obj : jsonArray) {
                JSONObject pluginObj = (JSONObject) obj;
                Map<String, Object> pluginInfo = new HashMap<>();
                pluginInfo.put("id", pluginObj.get("id"));
                pluginInfo.put("name", pluginObj.get("name"));
                pluginInfo.put("version", pluginObj.get("version"));
                pluginInfo.put("author", pluginObj.get("author"));
                pluginInfo.put("description", pluginObj.get("description"));
                pluginInfo.put("category", pluginObj.get("category"));
                pluginInfo.put("downloads", pluginObj.get("downloads"));
                pluginInfo.put("rating", pluginObj.get("rating"));
                pluginInfo.put("rating_count", pluginObj.get("rating_count"));
                
                // Add uploader information
                if (pluginObj.containsKey("uploader_verified")) {
                    pluginInfo.put("uploader_verified", pluginObj.get("uploader_verified"));
                }
                if (pluginObj.containsKey("uploader_blacklisted")) {
                    pluginInfo.put("uploader_blacklisted", pluginObj.get("uploader_blacklisted"));
                }
                if (pluginObj.containsKey("uploader_name")) {
                    pluginInfo.put("uploader_name", pluginObj.get("uploader_name"));
                }
                
                plugins.add(pluginInfo);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error searching plugins: " + e.getMessage());
            e.printStackTrace();
        }
        
        return plugins;
    }
    
    /**
     * Get plugin details by ID
     */
    public Map<String, Object> getPluginDetails(int pluginId) {
        Map<String, Object> details = new HashMap<>();
        
        try {
            String response = networkAPI.httpGet(browserUrl + "/api/plugins/" + pluginId);
            if (response == null || response.isEmpty()) {
                return details;
            }
            
            JSONParser parser = new JSONParser();
            JSONObject pluginObj = (JSONObject) parser.parse(response);
            
            details.put("id", pluginObj.get("id"));
            details.put("name", pluginObj.get("name"));
            details.put("version", pluginObj.get("version"));
            details.put("author", pluginObj.get("author"));
            details.put("description", pluginObj.get("description"));
            details.put("category", pluginObj.get("category"));
            details.put("downloads", pluginObj.get("downloads"));
            details.put("rating", pluginObj.get("rating"));
            details.put("rating_count", pluginObj.get("rating_count"));
            details.put("file_size", pluginObj.get("file_size"));
            details.put("created_at", pluginObj.get("created_at"));
            
            // Add uploader information
            if (pluginObj.containsKey("uploader_verified")) {
                details.put("uploader_verified", pluginObj.get("uploader_verified"));
            }
            if (pluginObj.containsKey("uploader_blacklisted")) {
                details.put("uploader_blacklisted", pluginObj.get("uploader_blacklisted"));
            }
            if (pluginObj.containsKey("uploader_name")) {
                details.put("uploader_name", pluginObj.get("uploader_name"));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting plugin details: " + e.getMessage());
            e.printStackTrace();
        }
        
        return details;
    }
    
    /**
     * Download and install a plugin from the browser
     * @param pluginId Plugin ID to download
     * @param fileName Optional custom file name (if null, uses plugin name-version.js)
     * @return true if successful
     */
    public boolean installPlugin(int pluginId, String fileName) {
        try {
            // Get plugin details first
            Map<String, Object> details = getPluginDetails(pluginId);
            if (details.isEmpty()) {
                plugin.getLogger().warning("Plugin not found: " + pluginId);
                return false;
            }
            
            // Determine file name
            String finalFileName = fileName;
            if (finalFileName == null || finalFileName.isEmpty()) {
                String name = (String) details.get("name");
                String version = (String) details.get("version");
                finalFileName = sanitizeFileName(name) + "-" + version + ".js";
            }
            
            // Get plugins directory
            File pluginsDir = new File(plugin.getDataFolder().getParentFile(), "js-plugins");
            if (!pluginsDir.exists()) {
                pluginsDir.mkdirs();
            }
            
            File pluginFile = new File(pluginsDir, finalFileName);
            
            // Download plugin
            String downloadUrl = browserUrl + "/api/plugins/" + pluginId + "/download";
            URL url = new URI(downloadUrl).toURL();
            
            try (InputStream in = url.openStream();
                 FileOutputStream out = new FileOutputStream(pluginFile)) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            
            plugin.getLogger().info("Successfully installed plugin: " + finalFileName);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error installing plugin: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Submit a review for a plugin
     */
    @SuppressWarnings("unchecked")
    public boolean submitReview(int pluginId, String author, int rating, String comment) {
        try {
            if (rating < 1 || rating > 5) {
                plugin.getLogger().warning("Rating must be between 1 and 5");
                return false;
            }
            
            JSONObject reviewData = new JSONObject();
            reviewData.put("author", author);
            reviewData.put("rating", Integer.valueOf(rating));
            if (comment != null && !comment.isEmpty()) {
                reviewData.put("comment", comment);
            }
            
            String response = networkAPI.httpPost(
                browserUrl + "/api/plugins/" + pluginId + "/reviews",
                reviewData.toJSONString()
            );
            
            return response != null && response.contains("success");
        } catch (Exception e) {
            plugin.getLogger().severe("Error submitting review: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Report a plugin
     */
    @SuppressWarnings("unchecked")
    public boolean reportPlugin(int pluginId, String reporter, String reason) {
        try {
            JSONObject reportData = new JSONObject();
            reportData.put("reporter", reporter);
            reportData.put("reason", reason);
            
            String response = networkAPI.httpPost(
                browserUrl + "/api/plugins/" + pluginId + "/report",
                reportData.toJSONString()
            );
            
            return response != null && response.contains("success");
        } catch (Exception e) {
            plugin.getLogger().severe("Error reporting plugin: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get reviews for a plugin
     */
    public List<Map<String, Object>> getPluginReviews(int pluginId) {
        List<Map<String, Object>> reviews = new ArrayList<>();
        
        try {
            String response = networkAPI.httpGet(browserUrl + "/api/plugins/" + pluginId + "/reviews");
            if (response == null || response.isEmpty()) {
                return reviews;
            }
            
            JSONParser parser = new JSONParser();
            JSONArray jsonArray = (JSONArray) parser.parse(response);
            
            for (Object obj : jsonArray) {
                JSONObject reviewObj = (JSONObject) obj;
                Map<String, Object> review = new HashMap<>();
                review.put("id", reviewObj.get("id"));
                review.put("author", reviewObj.get("author"));
                review.put("rating", reviewObj.get("rating"));
                review.put("comment", reviewObj.get("comment"));
                review.put("created_at", reviewObj.get("created_at"));
                reviews.add(review);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting plugin reviews: " + e.getMessage());
            e.printStackTrace();
        }
        
        return reviews;
    }
    
    /**
     * Extract plugin metadata from a JavaScript file
     * Uses a minimal Rhino context with only basic objects to avoid execution errors
     * @param pluginFile Plugin file to read
     * @return Map with extracted metadata (name, version, author, description)
     */
    public Map<String, Object> extractPluginMetadata(File pluginFile) {
        Map<String, Object> metadata = new HashMap<>();
        
        try {
            if (pluginFile == null || !pluginFile.exists()) {
                plugin.getLogger().fine("Plugin file does not exist: " + (pluginFile != null ? pluginFile.getName() : "null"));
                return metadata;
            }
            
            // Read file content
            String script = new String(java.nio.file.Files.readAllBytes(pluginFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            
            if (script == null || script.trim().isEmpty()) {
                plugin.getLogger().fine("Plugin file is empty: " + pluginFile.getName());
                return metadata;
            }
            
            // Create a minimal Rhino context - don't initialize standard objects that might cause issues
            org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
            rhinoContext.setOptimizationLevel(-1);
            rhinoContext.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
            
            try {
                // Create a minimal scope - only basic objects, no Bukkit/API dependencies
                org.mozilla.javascript.Scriptable scope = rhinoContext.initStandardObjects();
                
                // Wrap script execution in try-catch to handle any errors gracefully
                // We only need to execute enough to get pluginInfo defined
                try {
                    // Try to execute the script
                    rhinoContext.evaluateString(scope, script, pluginFile.getName(), 1, null);
                } catch (org.mozilla.javascript.RhinoException e) {
                    // Script execution failed - this is OK, pluginInfo might still be accessible
                    // if it was defined before the error occurred
                    plugin.getLogger().fine("Script execution had errors (this is OK for metadata extraction): " + e.getMessage());
                } catch (Exception e) {
                    // Other errors - log but continue
                    plugin.getLogger().fine("Error during script execution: " + e.getMessage());
                }
                
                // Now try to extract pluginInfo - try multiple methods
                Object pluginInfoObj = null;
                
                // Method 1: Check if pluginInfo exists in scope
                if (scope.has("pluginInfo", scope)) {
                    try {
                        pluginInfoObj = scope.get("pluginInfo", scope);
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                
                // Method 2: Try ScriptableObject.getProperty
                if (pluginInfoObj == null || pluginInfoObj == org.mozilla.javascript.Scriptable.NOT_FOUND) {
                    try {
                        pluginInfoObj = org.mozilla.javascript.ScriptableObject.getProperty(scope, "pluginInfo");
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                
                // Method 3: Try to get from parent scope if it exists
                if ((pluginInfoObj == null || pluginInfoObj == org.mozilla.javascript.Scriptable.NOT_FOUND) && scope.getParentScope() != null) {
                    try {
                        org.mozilla.javascript.Scriptable parent = scope.getParentScope();
                        if (parent.has("pluginInfo", parent)) {
                            pluginInfoObj = parent.get("pluginInfo", parent);
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                
                // Extract properties from pluginInfo if found
                if (pluginInfoObj != null && pluginInfoObj != org.mozilla.javascript.Scriptable.NOT_FOUND && pluginInfoObj instanceof org.mozilla.javascript.Scriptable) {
                    org.mozilla.javascript.Scriptable pluginInfo = (org.mozilla.javascript.Scriptable) pluginInfoObj;
                    
                    // Extract name
                    if (pluginInfo.has("name", pluginInfo)) {
                        Object nameObj = pluginInfo.get("name", pluginInfo);
                        if (nameObj != null && nameObj != org.mozilla.javascript.Scriptable.NOT_FOUND) {
                            metadata.put("name", nameObj.toString());
                        }
                    }
                    
                    // Extract version
                    if (pluginInfo.has("version", pluginInfo)) {
                        Object versionObj = pluginInfo.get("version", pluginInfo);
                        if (versionObj != null && versionObj != org.mozilla.javascript.Scriptable.NOT_FOUND) {
                            metadata.put("version", versionObj.toString());
                        }
                    }
                    
                    // Extract author
                    if (pluginInfo.has("author", pluginInfo)) {
                        Object authorObj = pluginInfo.get("author", pluginInfo);
                        if (authorObj != null && authorObj != org.mozilla.javascript.Scriptable.NOT_FOUND) {
                            metadata.put("author", authorObj.toString());
                        }
                    }
                    
                    // Extract description
                    if (pluginInfo.has("description", pluginInfo)) {
                        Object descObj = pluginInfo.get("description", pluginInfo);
                        if (descObj != null && descObj != org.mozilla.javascript.Scriptable.NOT_FOUND) {
                            metadata.put("description", descObj.toString());
                        }
                    }
                } else {
                    plugin.getLogger().fine("pluginInfo object not found in " + pluginFile.getName() + " (obj=" + pluginInfoObj + ")");
                }
            } finally {
                org.mozilla.javascript.Context.exit();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not extract metadata from plugin file " + pluginFile.getName() + ": " + e.getMessage());
            e.printStackTrace();
            // Return empty map on error - caller can use provided values
        }
        
        return metadata;
    }
    
    /**
     * Upload a plugin file to the browser server
     * Automatically extracts metadata from pluginInfo if available
     * @param pluginFile File to upload
     * @param name Plugin name (will be overridden by pluginInfo.name if present)
     * @param version Plugin version (will be overridden by pluginInfo.version if present)
     * @param author Plugin author (will be overridden by pluginInfo.author if present)
     * @param description Plugin description (will be overridden by pluginInfo.description if present)
     * @param category Plugin category (optional, not in pluginInfo)
     * @return Map with success status and message/error
     */
    public Map<String, Object> uploadPlugin(File pluginFile, String name, String version, String author, String description, String category, String uploader, String uploaderName) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (pluginFile == null || !pluginFile.exists()) {
                result.put("success", false);
                result.put("error", "Plugin file not found");
                return result;
            }
            
            // Extract metadata from plugin file
            Map<String, Object> extractedMetadata = extractPluginMetadata(pluginFile);
            
            // Use extracted metadata if available, otherwise use provided values
            String finalName = extractedMetadata.containsKey("name") && extractedMetadata.get("name") != null 
                ? extractedMetadata.get("name").toString() : name;
            String finalVersion = extractedMetadata.containsKey("version") && extractedMetadata.get("version") != null 
                ? extractedMetadata.get("version").toString() : version;
            String finalAuthor = extractedMetadata.containsKey("author") && extractedMetadata.get("author") != null 
                ? extractedMetadata.get("author").toString() : author;
            String finalDescription = extractedMetadata.containsKey("description") && extractedMetadata.get("description") != null 
                ? extractedMetadata.get("description").toString() : (description != null ? description : "");
            
            // Read file content
            byte[] fileContent = java.nio.file.Files.readAllBytes(pluginFile.toPath());
            
            // Create multipart form data
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            StringBuilder formData = new StringBuilder();
            
            // Add form fields (use extracted metadata)
            formData.append("--").append(boundary).append("\r\n");
            formData.append("Content-Disposition: form-data; name=\"name\"\r\n\r\n");
            formData.append(finalName).append("\r\n");
            
            formData.append("--").append(boundary).append("\r\n");
            formData.append("Content-Disposition: form-data; name=\"version\"\r\n\r\n");
            formData.append(finalVersion).append("\r\n");
            
            formData.append("--").append(boundary).append("\r\n");
            formData.append("Content-Disposition: form-data; name=\"author\"\r\n\r\n");
            formData.append(finalAuthor).append("\r\n");
            
            if (finalDescription != null && !finalDescription.isEmpty()) {
                formData.append("--").append(boundary).append("\r\n");
                formData.append("Content-Disposition: form-data; name=\"description\"\r\n\r\n");
                formData.append(finalDescription).append("\r\n");
            }
            
            if (category != null && !category.isEmpty()) {
                formData.append("--").append(boundary).append("\r\n");
                formData.append("Content-Disposition: form-data; name=\"category\"\r\n\r\n");
                formData.append(category).append("\r\n");
            }
            
            // Add uploader (UUID) and uploaderName (player name for display)
            if (uploader != null && !uploader.isEmpty()) {
                formData.append("--").append(boundary).append("\r\n");
                formData.append("Content-Disposition: form-data; name=\"uploader\"\r\n\r\n");
                formData.append(uploader).append("\r\n");
                
                // Use provided uploaderName, or try to get player name from UUID if not provided
                String finalUploaderName = uploaderName;
                if ((finalUploaderName == null || finalUploaderName.isEmpty()) && uploader != null && !uploader.isEmpty()) {
                    try {
                        org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(uploader));
                        if (offlinePlayer != null && offlinePlayer.getName() != null) {
                            finalUploaderName = offlinePlayer.getName();
                        }
                    } catch (Exception e) {
                        // Ignore - UUID might be invalid or player not found
                    }
                }
                
                // Always send uploaderName if we have it (either provided or looked up)
                if (finalUploaderName != null && !finalUploaderName.isEmpty()) {
                    formData.append("--").append(boundary).append("\r\n");
                    formData.append("Content-Disposition: form-data; name=\"uploaderName\"\r\n\r\n");
                    formData.append(finalUploaderName).append("\r\n");
                }
            }
            
            // Add file
            formData.append("--").append(boundary).append("\r\n");
            formData.append("Content-Disposition: form-data; name=\"plugin\"; filename=\"").append(pluginFile.getName()).append("\"\r\n");
            formData.append("Content-Type: application/javascript\r\n\r\n");
            
            // Convert form data to bytes
            byte[] formDataBytes = formData.toString().getBytes("UTF-8");
            byte[] boundaryBytes = ("\r\n--" + boundary + "--\r\n").getBytes("UTF-8");
            
            // Combine all parts
            byte[] requestBody = new byte[formDataBytes.length + fileContent.length + boundaryBytes.length];
            System.arraycopy(formDataBytes, 0, requestBody, 0, formDataBytes.length);
            System.arraycopy(fileContent, 0, requestBody, formDataBytes.length, fileContent.length);
            System.arraycopy(boundaryBytes, 0, requestBody, formDataBytes.length + fileContent.length, boundaryBytes.length);
            
            // Send POST request
            URI uri = new URI(browserUrl + "/api/plugins/upload");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setRequestProperty("Content-Length", String.valueOf(requestBody.length));
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(requestBody);
            }
            
            // Read response
            int responseCode = conn.getResponseCode();
            String response = "";
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream()))) {
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line).append("\n");
                }
                response = responseBuilder.toString().trim();
            }
            
            plugin.getLogger().fine("Upload response code: " + responseCode);
            plugin.getLogger().fine("Upload response: " + response);
            
            if (responseCode == 200 || responseCode == 201) {
                try {
                    JSONParser parser = new JSONParser();
                    JSONObject jsonResponse = (JSONObject) parser.parse(response);
                    
                    result.put("success", true);
                    if (jsonResponse.containsKey("message")) {
                        result.put("message", jsonResponse.get("message"));
                    }
                    if (jsonResponse.containsKey("pluginId")) {
                        result.put("pluginId", jsonResponse.get("pluginId"));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to parse success response: " + e.getMessage());
                    result.put("success", true);
                    result.put("message", "Upload successful (response code: " + responseCode + ")");
                }
            } else {
                // Try to parse as JSON first
                try {
                    JSONParser parser = new JSONParser();
                    Object parsed = parser.parse(response);
                    if (parsed instanceof JSONObject) {
                        JSONObject jsonResponse = (JSONObject) parsed;
                        result.put("success", false);
                        String errorMsg = jsonResponse.containsKey("error") ? jsonResponse.get("error").toString() : "Server returned error code " + responseCode;
                        result.put("error", errorMsg);
                        if (jsonResponse.containsKey("issues")) {
                            result.put("issues", jsonResponse.get("issues"));
                        }
                    } else {
                        // Not a JSON object, use response as error message
                        result.put("success", false);
                        result.put("error", response.isEmpty() ? "Server returned error code " + responseCode : response);
                    }
                } catch (Exception e) {
                    // Response is not valid JSON, treat it as plain text error message
                    plugin.getLogger().fine("Error response is not JSON, treating as text: " + response);
                    result.put("success", false);
                    // Use the response text directly as error message
                    result.put("error", response.isEmpty() ? "Server returned error code " + responseCode : response);
                }
            }
            
        } catch (java.net.UnknownHostException e) {
            plugin.getLogger().severe("Cannot connect to plugin browser server: " + e.getMessage());
            result.put("success", false);
            result.put("error", "Cannot connect to plugin browser server. Check your internet connection and server URL.");
        } catch (java.net.SocketTimeoutException e) {
            plugin.getLogger().severe("Connection timeout while uploading plugin: " + e.getMessage());
            result.put("success", false);
            result.put("error", "Connection timeout. The server took too long to respond.");
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("IO error while uploading plugin: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("error", "Network error: " + e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().severe("Error uploading plugin: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("error", "Upload failed: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Sanitize file name for safe file system usage
     */
    private String sanitizeFileName(String name) {
        if (name == null) {
            return "plugin";
        }
        // Remove dangerous characters
        return name.replaceAll("[^a-zA-Z0-9\\-_]", "_").replaceAll("_{2,}", "_");
    }
}
