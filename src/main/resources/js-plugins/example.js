// Example JS Plugin for MC-JS v2.0
// This demonstrates how to create modern plugins using JavaScript with GraalVM

// Plugin metadata (optional but recommended)
var pluginInfo = {
    name: "Example Plugin",
    version: "1.0.0",
    author: "MC-JS Team",
    description: "A comprehensive example plugin demonstrating MC-JS capabilities"
};

function onEnable() {
    logger.info("Example JS Plugin v" + pluginInfo.version + " enabled!");

    // Register a simple command with proper error handling
    api.registerCommand("hello", "Say hello to players", "/hello [player]", function(sender, args) {
        try {
            if (args.length > 0) {
                var target = api.getPlayer(args[0]);
                if (target) {
                    api.sendMessage(target, "&aHello from JavaScript!");
                    api.sendMessage(sender, "&eSent hello to " + target.getName());
                } else {
                    api.sendMessage(sender, "&cPlayer not found!");
                }
            } else {
                api.sendMessage(sender, "&aHello from JavaScript plugin!");
            }
            return true;
        } catch (e) {
            logger.severe("Error in hello command: " + e);
            return false;
        }
    });

    // Register an event listener for player join with modern API
    api.registerEvent("player.PlayerJoinEvent", function(event) {
        var player = event.getPlayer();
        api.broadcast("&6Welcome &b" + player.getName() + " &6to the server!");
        
        // Send title and action bar
        api.sendTitle(player, "&aWelcome!", "&eEnjoy your stay!", 10, 70, 20);
        api.sendActionBar(player, "&aYou joined the server!");
        
        // Play sound
        api.playSound(player, "ENTITY_PLAYER_LEVELUP", 1.0, 1.0);
    });

    // Register player quit event
    api.registerEvent("player.PlayerQuitEvent", function(event) {
        var player = event.getPlayer();
        api.broadcast("&7" + player.getName() + " left the server");
    });

    // Register block break event with modern item handling
    api.registerEvent("block.BlockBreakEvent", function(event) {
        var player = event.getPlayer();
        var block = event.getBlock();
        var item = api.getItemInMainHand(player);
        
        logger.info(player.getName() + " broke " + block.getType().name() + 
                   " with " + (item ? item.getType().name() : "hand"));
    });

    // Schedule a repeating task (every minute = 1200 ticks)
    var heartbeatTask = api.runTaskTimer(0, 1200, function() {
        var playerCount = api.getOnlinePlayers().size();
        logger.info("JS Plugin heartbeat - Server has " + playerCount + " player(s)");
    });

    // Create a custom item with display name and lore
    api.runTaskLater(100, function() { // After 5 seconds
        var diamond = api.createItemStack(api.getMaterial("DIAMOND"), 1);
        diamond = api.setItemDisplayName(diamond, "&b&lSpecial Diamond");
        diamond = api.setItemLore(diamond, [
            "&7This is a special diamond",
            "&7given by the JS plugin!",
            "&eRight-click to use"
        ]);
        
        var players = api.getOnlinePlayers();
        for (var i = 0; i < players.length; i++) {
            var player = players[i];
            api.giveItem(player, diamond);
            api.sendMessage(player, "&bYou received a special diamond from JS!");
        }
    });

    // Spawn particles at spawn location
    api.runTaskTimer(0, 40, function() { // Every 2 seconds
        var worlds = api.getWorlds();
        if (worlds.length > 0) {
            var world = worlds[0];
            var spawn = world.getSpawnLocation();
            api.spawnParticle(spawn, "FIREWORK", 10, 0.5, 0.5, 0.5, 0.1);
        }
    });

    // Demonstrate file operations
    var playerData = {};
    var players = api.getOnlinePlayers();
    for (var i = 0; i < players.length; i++) {
        var player = players[i];
        playerData[player.getName()] = {
            "health": player.getHealth(),
            "maxHealth": api.getMaxHealth(player),
            "food": player.getFoodLevel(),
            "level": player.getLevel(),
            "location": {
                "x": player.getLocation().getX(),
                "y": player.getLocation().getY(),
                "z": player.getLocation().getZ(),
                "world": player.getWorld().getName()
            },
            "lastSeen": api.getCurrentTimeMillis()
        };
    }

    // Save player data to YAML
    api.saveYamlFile("players", playerData);
    logger.info("Saved player data to players.yml");

    // Load and display saved data
    api.runTaskLater(200, function() { // After 10 seconds
        var loadedData = api.loadYamlFile("players");
        var keys = Object.keys(loadedData);
        logger.info("Loaded player data: " + keys.length + " player(s)");

        // Save some JSON data
        var config = {
            "serverName": api.getServerVersion(),
            "maxPlayers": api.getMaxPlayers(),
            "onlinePlayers": api.getOnlinePlayers().size(),
            "timestamp": api.getCurrentTimeMillis(),
            "formattedDate": api.formatDate(api.getCurrentTimeMillis())
        };
        api.saveJsonFile("server_stats", JSON.stringify(config, null, 2));
        logger.info("Saved server stats to server_stats.json");

        // Demonstrate database operations
        api.createTable("player_stats", "player_stats", {
            "player_name": "TEXT PRIMARY KEY",
            "joins": "INTEGER DEFAULT 0",
            "playtime": "INTEGER DEFAULT 0",
            "last_join": "INTEGER"
        });

        // Insert some test data
        api.insertData("player_stats", "player_stats", {
            "player_name": "TestPlayer",
            "joins": 1,
            "playtime": 1000,
            "last_join": api.getCurrentTimeMillis()
        });

        // Query the data
        var results = api.querySQL("player_stats", "SELECT * FROM player_stats");
        logger.info("Database query returned " + results.length + " row(s)");
    });

    // Demonstrate HTTP requests (async)
    api.runTaskAsync(function() {
        try {
            // Example: Fetch data from an API
            // var response = api.httpGet("https://api.example.com/data");
            // logger.info("HTTP Response: " + response);
            logger.info("HTTP functionality is available for external API calls");
        } catch (e) {
            logger.warning("HTTP demo: " + e);
        }
    });

    // Demonstrate encryption/hashing
    var testString = "Hello MC-JS!";
    var md5Hash = api.md5(testString);
    var sha256Hash = api.sha256(testString);
    logger.info("MD5 of '" + testString + "': " + md5Hash);
    logger.info("SHA256 of '" + testString + "': " + sha256Hash);

    // Demonstrate encoding
    var encoded = api.base64Encode("Hello World!");
    var decoded = api.base64Decode(encoded);
    logger.info("Base64 encoded: " + encoded);
    logger.info("Base64 decoded: " + decoded);
}

function onDisable() {
    logger.info("Example JS Plugin disabled!");
    
    // Cleanup tasks if needed
    // Cancel tasks, save data, etc.
}

// Export functions and metadata for the plugin manager
this.onEnable = onEnable;
this.onDisable = onDisable;
this.pluginInfo = pluginInfo;
