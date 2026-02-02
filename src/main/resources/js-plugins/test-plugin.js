// Test Plugin for MC-JS - Comprehensive API Demonstration
// This plugin demonstrates all available API features in a meaningful way

var pluginInfo = {
    name: "MC-JS Test Plugin",
    version: "1.0.0",
    author: "Test Developer",
    description: "Comprehensive test plugin demonstrating all MC-JS API features"
};

// Plugin state
var pluginState = {
    tasks: [],
    playerStats: {},
    guiOpen: false
};

function onEnable() {
    logger.info("=== MC-JS Test Plugin v" + pluginInfo.version + " enabled! ===");
    
    // ===== 1. COMMAND REGISTRATION =====
    logger.info("Registering commands...");
    
    // Simple command
    api.registerCommand("js-test", "Test command", "/js-test [action]", function(sender, args) {
        try {
            // Debug: Log what we receive
            logger.info("Command executed with args: " + (args ? args.length : "null"));
            if (args && args.length > 0) {
                logger.info("First arg: " + args[0]);
            }
            
            if (!args || args.length === 0) {
                api.sendMessage(sender, "&aTest Plugin Commands:");
                api.sendMessage(sender, "&e/js-test info &7- Show plugin info");
                api.sendMessage(sender, "&e/js-test stats [player] &7- Show player stats");
                api.sendMessage(sender, "&e/js-test item &7- Get a test item");
                api.sendMessage(sender, "&e/js-test gui &7- Open test GUI");
                api.sendMessage(sender, "&e/js-test particle &7- Spawn particles");
                api.sendMessage(sender, "&e/js-test sound &7- Play test sound");
                api.sendMessage(sender, "&e/js-test save &7- Save test data");
                api.sendMessage(sender, "&e/js-test load &7- Load test data");
                api.sendMessage(sender, "&e/js-test db &7- Test database");
                api.sendMessage(sender, "&e/js-test hash [text] &7- Hash text");
                return true;
            }
            
            var action = String(args[0] + "").toLowerCase();
            logger.info("Processing action: " + action);
            
            if (action === "info") {
                api.sendMessage(sender, "&6=== Plugin Info ===");
                api.sendMessage(sender, "&eName: &f" + pluginInfo.name);
                api.sendMessage(sender, "&eVersion: &f" + pluginInfo.version);
                api.sendMessage(sender, "&eAuthor: &f" + pluginInfo.author);
                api.sendMessage(sender, "&eServer Version: &f" + api.getServerVersion());
                api.sendMessage(sender, "&eMax Players: &f" + api.getMaxPlayers());
                api.sendMessage(sender, "&eOnline Players: &f" + api.getOnlinePlayers().size());
            } else if (action === "stats") {
                var target = args.length > 1 ? api.getPlayer(String(args[1] + "")) : api.getPlayerFromSender(sender);
                if (!target) {
                    api.sendMessage(sender, "&cPlayer not found or you must be a player!");
                    return true;
                }
                
                api.sendMessage(sender, "&6=== Player Stats: " + target.getName() + " ===");
                api.sendMessage(sender, "&eHealth: &f" + Math.round(target.getHealth()) + "/" + Math.round(api.getMaxHealth(target)));
                api.sendMessage(sender, "&eFood: &f" + target.getFoodLevel() + "/20");
                api.sendMessage(sender, "&eLevel: &f" + target.getLevel());
                api.sendMessage(sender, "&eWorld: &f" + target.getWorld().getName());
                api.sendMessage(sender, "&eLocation: &f" + Math.round(target.getLocation().getX()) + ", " + 
                               Math.round(target.getLocation().getY()) + ", " + Math.round(target.getLocation().getZ()));
            } else if (action === "item") {
                var player = api.getPlayerFromSender(sender);
                if (!player) {
                    api.sendMessage(sender, "&cYou must be a player!");
                    return true;
                }
                
                // Create a custom item
                var testItem = api.createItemStack(api.getMaterial("DIAMOND_SWORD"), 1);
                testItem = api.setItemDisplayName(testItem, "&b&lTest Sword");
                testItem = api.setItemLore(testItem, [
                    "&7This is a test item",
                    "&7Created by MC-JS Test Plugin",
                    "&eRight-click to use!",
                    "&6Durability: &f" + testItem.getDurability() + "/" + testItem.getType().getMaxDurability()
                ]);
                
                api.giveItem(player, testItem);
                api.sendMessage(player, "&aYou received a test sword!");
                api.playSound(player, "ENTITY_PLAYER_LEVELUP", 1.0, 1.2);
            } else if (action === "gui") {
                var player = api.getPlayerFromSender(sender);
                if (!player) {
                    api.sendMessage(sender, "&cYou must be a player!");
                    return true;
                }
                openTestGUI(player);
            } else if (action === "particle") {
                var player = api.getPlayerFromSender(sender);
                if (!player) {
                    api.sendMessage(sender, "&cYou must be a player!");
                    return true;
                }
                var loc = player.getLocation();
                
                // Spawn different particles
                api.spawnParticle(loc, "HEART", 10, 0.5, 1.0, 0.5, 0.1);
                api.spawnParticle(loc.clone().add(0, 1, 0), "FIREWORK", 5, 0.3, 0.3, 0.3, 0.05);
                api.spawnParticle(loc.clone().add(0, 2, 0), "VILLAGER_HAPPY", 8, 0.4, 0.4, 0.4, 0.1);
                
                api.sendMessage(sender, "&aSpawned particles at your location!");
            } else if (action === "sound") {
                var player = api.getPlayerFromSender(sender);
                if (!player) {
                    api.sendMessage(sender, "&cYou must be a player!");
                    return true;
                }
                api.playSound(player, "ENTITY_PLAYER_LEVELUP", 1.0, 1.0);
                api.sendMessage(sender, "&aPlayed test sound!");
            } else if (action === "save") {
                saveTestData();
                api.sendMessage(sender, "&aTest data saved!");
            } else if (action === "load") {
                var data = loadTestData();
                api.sendMessage(sender, "&aTest data loaded! Keys: " + Object.keys(data).length);
            } else if (action === "db") {
                testDatabase();
                api.sendMessage(sender, "&aDatabase test completed! Check console for details.");
            } else if (action === "hash") {
                var text = args.length > 1 ? args.slice(1).map(function(s) { return String(s + ""); }).join(" ") : "Hello MC-JS!";
                var md5Hash = api.md5(text);
                var sha256Hash = api.sha256(text);
                var base64 = api.base64Encode(text);
                
                api.sendMessage(sender, "&6=== Hash Results ===");
                api.sendMessage(sender, "&eText: &f" + text);
                api.sendMessage(sender, "&eMD5: &f" + md5Hash);
                api.sendMessage(sender, "&eSHA256: &f" + sha256Hash);
                api.sendMessage(sender, "&eBase64: &f" + base64);
                api.sendMessage(sender, "&eDecoded: &f" + api.base64Decode(base64));
            } else {
                api.sendMessage(sender, "&cUnknown action: " + action);
                api.sendMessage(sender, "&7Use /js-test for help");
            }
            
            return true;
        } catch (e) {
            logger.severe("Error in test command: " + e);
            api.sendMessage(sender, "&cAn error occurred: " + e.message);
            return false;
        }
    });
    
    // Command with tab completion
    api.registerCommand("js-testtab", "Test tab completion", "/js-testtab <player>", function(sender, args) {
        if (args.length === 0) {
            api.sendMessage(sender, "&cUsage: /js-testtab <player>");
            return true;
        }
        var target = api.getPlayer(args[0]);
        if (target) {
            api.sendMessage(sender, "&aFound player: " + target.getName());
        } else {
            api.sendMessage(sender, "&cPlayer not found!");
        }
        return true;
    }, function(sender, args) {
        // Tab completer - return online player names
        var players = api.getOnlinePlayers();
        var names = [];
        for (var i = 0; i < players.length; i++) {
            names.push(players[i].getName());
        }
        return names;
    });
    
    // ===== 2. EVENT REGISTRATION =====
    logger.info("Registering events...");
    
    // Player join event
    api.registerEvent("player.PlayerJoinEvent", function(event) {
        var player = event.getPlayer();
        var playerName = player.getName();
        
        // Welcome message
        api.sendTitle(player, "&aWelcome!", "&eTo the Test Server", 10, 70, 20);
        api.sendActionBar(player, "&aWelcome, " + playerName + "!");
        api.broadcast("&6" + playerName + " &ejoined the server!");
        
        // Play sound
        api.playSound(player, "ENTITY_PLAYER_LEVELUP", 1.0, 1.0);
        
        // Spawn welcome particles
        var loc = player.getLocation();
        api.spawnParticle(loc, "FIREWORK", 20, 0.5, 1.0, 0.5, 0.1);
        
        // Track player stats
        if (!pluginState.playerStats[playerName]) {
            pluginState.playerStats[playerName] = {
                joins: 0,
                lastJoin: null
            };
        }
        pluginState.playerStats[playerName].joins++;
        pluginState.playerStats[playerName].lastJoin = api.getCurrentTimeMillis();
        
        logger.info("Player " + playerName + " joined (total joins: " + pluginState.playerStats[playerName].joins + ")");
    });
    
    // Player quit event
    api.registerEvent("player.PlayerQuitEvent", function(event) {
        var player = event.getPlayer();
        api.broadcast("&7" + player.getName() + " left the server");
    });
    
    // Block break event
    api.registerEvent("block.BlockBreakEvent", function(event) {
        var player = event.getPlayer();
        var block = event.getBlock();
        var item = api.getItemInMainHand(player);
        
        // Log block break
        logger.info(player.getName() + " broke " + block.getType().name() + 
                   " with " + (item ? item.getType().name() : "hand"));
        
        // Spawn particles when breaking certain blocks
        if (block.getType().name().includes("ORE")) {
            var loc = block.getLocation().add(0.5, 0.5, 0.5);
            api.spawnParticle(loc, "CRIT", 10, 0.3, 0.3, 0.3, 0.1);
            api.playSound(loc, "BLOCK_STONE_BREAK", 1.0, 1.0);
        }
    });
    
    // Player interact event
    api.registerEvent("player.PlayerInteractEvent", function(event) {
        var player = event.getPlayer();
        var item = event.getItem();
        
        if (item && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            var displayName = item.getItemMeta().getDisplayName();
            if (displayName.includes("Test Sword")) {
                api.sendMessage(player, "&aYou used the Test Sword!");
                api.playSound(player, "ENTITY_EXPERIENCE_ORB_PICKUP", 1.0, 1.5);
                
                // Spawn particles in a circle
                var loc = player.getLocation();
                for (var i = 0; i < 8; i++) {
                    var angle = (i / 8) * Math.PI * 2;
                    var x = Math.cos(angle) * 1.5;
                    var z = Math.sin(angle) * 1.5;
                    api.spawnParticle(loc.clone().add(x, 1, z), "ENCHANTMENT_TABLE", 3, 0.1, 0.1, 0.1, 0.05);
                }
            }
        }
    });
    
    // ===== 3. TASK SCHEDULING =====
    logger.info("Scheduling tasks...");
    
    // Repeating task - update action bar every second
    var actionBarTask = api.runTaskTimer(20, 20, function() {
        var players = api.getOnlinePlayers();
        for (var i = 0; i < players.length; i++) {
            var player = players[i];
            var time = api.formatDate(api.getCurrentTimeMillis(), "HH:mm:ss");
            api.sendActionBar(player, "&6Time: &f" + time + " &7| &ePlayers: &f" + players.length);
        }
    });
    pluginState.tasks.push(actionBarTask);
    
    // Delayed task - welcome message after 5 seconds
    api.runTaskLater(100, function() {
        logger.info("Test Plugin fully loaded after 5 seconds!");
        var players = api.getOnlinePlayers();
        if (players.length > 0) {
            api.broadcast("&aTest Plugin is now fully operational!");
        }
    });
    
    // Async task - simulate API call
    api.runTaskAsync(function() {
        logger.info("Running async task...");
        // Simulate some async work
        var startTime = api.getCurrentTimeMillis();
        
        // In a real scenario, you might do HTTP requests here
        // var response = api.httpGet("https://api.example.com/data");
        
        var endTime = api.getCurrentTimeMillis();
        logger.info("Async task completed in " + (endTime - startTime) + "ms");
    });
    
    // ===== 4. GUI CREATION =====
    function openTestGUI(player) {
        var gui = api.inventory.createGUI("&6Test Plugin GUI", 3);
        
        // Add items to GUI
        var infoItem = api.createItemStack(api.getMaterial("BOOK"), 1);
        infoItem = api.setItemDisplayName(infoItem, "&ePlugin Info");
        infoItem = api.setItemLore(infoItem, [
            "&7Click to see plugin information",
            "&7Version: &f" + pluginInfo.version
        ]);
        gui.setItem(10, infoItem);
        
        var statsItem = api.createItemStack(api.getMaterial("DIAMOND"), 1);
        statsItem = api.setItemDisplayName(statsItem, "&bPlayer Stats");
        statsItem = api.setItemLore(statsItem, [
            "&7Click to see your stats",
            "&7Health: &f" + Math.round(player.getHealth()) + "/" + Math.round(api.getMaxHealth(player))
        ]);
        gui.setItem(12, statsItem);
        
        var particleItem = api.createItemStack(api.getMaterial("FIREWORK_ROCKET"), 1);
        particleItem = api.setItemDisplayName(particleItem, "&cSpawn Particles");
        particleItem = api.setItemLore(particleItem, [
            "&7Click to spawn particles",
            "&7at your location"
        ]);
        gui.setItem(14, particleItem);
        
        var closeItem = api.createItemStack(api.getMaterial("BARRIER"), 1);
        closeItem = api.setItemDisplayName(closeItem, "&cClose");
        gui.setItem(22, closeItem);
        
        // Set click handlers
        gui.onClick(function(event) {
            var slot = event.getSlot();
            var clickedItem = event.getCurrentItem();
            
            if (!clickedItem || clickedItem.getType().name() === "AIR") {
                return;
            }
            
            event.setCancelled(true);
            
            if (slot === 10) {
                api.sendMessage(player, "&6=== Plugin Info ===");
                api.sendMessage(player, "&eName: &f" + pluginInfo.name);
                api.sendMessage(player, "&eVersion: &f" + pluginInfo.version);
            } else if (slot === 12) {
                api.sendMessage(player, "&6=== Your Stats ===");
                api.sendMessage(player, "&eHealth: &f" + Math.round(player.getHealth()) + "/" + Math.round(api.getMaxHealth(player)));
                api.sendMessage(player, "&eLevel: &f" + player.getLevel());
            } else if (slot === 14) {
                var loc = player.getLocation();
                api.spawnParticle(loc, "FIREWORK", 30, 0.5, 1.0, 0.5, 0.1);
                api.sendMessage(player, "&aSpawned particles!");
            } else if (slot === 22) {
                player.closeInventory();
            }
        });
        
        gui.onClose(function(event) {
            api.sendMessage(player, "&7GUI closed!");
            pluginState.guiOpen = false;
        });
        
        gui.open(player);
        pluginState.guiOpen = true;
    }
    
    // ===== 5. FILE OPERATIONS =====
    function saveTestData() {
        var data = {
            plugin: {
                name: pluginInfo.name,
                version: pluginInfo.version,
                enabledAt: api.getCurrentTimeMillis()
            },
            stats: pluginState.playerStats,
            config: {
                actionBarEnabled: true,
                particlesEnabled: true
            }
        };
        
        api.saveYamlFile("test_plugin_data", data);
        logger.info("Test data saved to test_plugin_data.yml");
    }
    
    function loadTestData() {
        var data = api.loadYamlFile("test_plugin_data");
        if (data && data.stats) {
            pluginState.playerStats = data.stats;
            logger.info("Test data loaded from test_plugin_data.yml");
        }
        return data || {};
    }
    
    // Load data on startup
    loadTestData();
    
    // Save JSON example
    var jsonData = {
        timestamp: api.getCurrentTimeMillis(),
        formattedDate: api.formatDate(api.getCurrentTimeMillis()),
        serverVersion: api.getServerVersion(),
        maxPlayers: api.getMaxPlayers()
    };
    api.saveJsonFile("test_server_info", JSON.stringify(jsonData, null, 2));
    
    // ===== 6. DATABASE OPERATIONS =====
    function testDatabase() {
        // Create table
        api.createTable("test_db", "player_data", {
            "player_name": "TEXT PRIMARY KEY",
            "joins": "INTEGER DEFAULT 0",
            "last_join": "INTEGER",
            "playtime": "INTEGER DEFAULT 0"
        });
        logger.info("Created database table: player_data");
        
        // Insert test data
        var players = api.getOnlinePlayers();
        for (var i = 0; i < players.length; i++) {
            var player = players[i];
            var playerName = player.getName();
            
            api.insertData("test_db", "player_data", {
                "player_name": playerName,
                "joins": pluginState.playerStats[playerName] ? pluginState.playerStats[playerName].joins : 1,
                "last_join": api.getCurrentTimeMillis(),
                "playtime": 0
            });
        }
        logger.info("Inserted data for " + players.length + " player(s)");
        
        // Query data
        var results = api.querySQL("test_db", "SELECT * FROM player_data");
        logger.info("Database query returned " + results.length + " row(s)");
        for (var i = 0; i < results.length; i++) {
            var row = results[i];
            logger.info("Player: " + row.player_name + ", Joins: " + row.joins);
        }
    }
    
    // ===== 7. UTILITY DEMONSTRATIONS =====
    logger.info("Demonstrating utilities...");
    
    // Hash demonstration
    var testString = "MC-JS Test Plugin";
    var md5Hash = api.md5(testString);
    var sha256Hash = api.sha256(testString);
    logger.info("MD5 of '" + testString + "': " + md5Hash);
    logger.info("SHA256 of '" + testString + "': " + sha256Hash);
    
    // Base64 encoding
    var encoded = api.base64Encode("Hello from MC-JS!");
    var decoded = api.base64Decode(encoded);
    logger.info("Base64 encoded: " + encoded);
    logger.info("Base64 decoded: " + decoded);
    
    // Date formatting
    var now = api.getCurrentTimeMillis();
    logger.info("Current time: " + api.formatDate(now));
    logger.info("Formatted: " + api.formatDate(now, "yyyy-MM-dd HH:mm:ss"));
    
    logger.info("=== Test Plugin fully enabled! ===");
}

function onDisable() {
    logger.info("=== MC-JS Test Plugin disabling... ===");
    
    // Cancel all tasks
    for (var i = 0; i < pluginState.tasks.length; i++) {
        api.cancelTask(pluginState.tasks[i]);
    }
    pluginState.tasks = [];
    
    // Save data
    saveTestData();
    
    logger.info("=== Test Plugin disabled! ===");
}

// Export functions
this.onEnable = onEnable;
this.onDisable = onDisable;
this.pluginInfo = pluginInfo;
