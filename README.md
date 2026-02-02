<div align="center">

# 🚀 MC-JS

**Write Minecraft Plugins in JavaScript - No Java Required!**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20+-green.svg)](https://www.minecraft.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-2.0.0-brightgreen.svg)](https://github.com/LootingVI/MC-JS/releases)
[![GitHub](https://img.shields.io/github/stars/LootingVI/MC-JS?style=social)](https://github.com/LootingVI/MC-JS)

**A powerful Minecraft plugin that allows you to create server plugins using JavaScript instead of Java.**

[Features](#-features) • [Installation](#-installation) • [Quick Start](#-quick-start) • [Documentation](#-documentation) • [Examples](#-examples) • [API Reference](#-complete-api-reference)

---

> 💡 **New to MC-JS?** Start with the [Quick Start](#-quick-start) guide or check out our [complete documentation](https://lootingvi.github.io/MC-JS/).

</div>

---

## ✨ Features

### 🎯 Core Capabilities

- **📝 Full JavaScript Support** - Write plugins in modern JavaScript (ES6+) using Rhino Engine
- **⚡ Hot Reload** - Reload plugins without restarting the server (`/jsreload`)
- **🔧 Complete API Access** - Access to virtually all Bukkit/Spigot/Paper API functions
- **🎮 Event System** - Register listeners for any Minecraft event with priority support
- **💬 Command System** - Create custom commands with full tab completion support
- **⏰ Task Scheduling** - Synchronous and asynchronous task scheduling
- **📦 Inventory Management** - Create and manage custom GUIs with click handlers
- **🗄️ Database Support** - Built-in SQLite database operations (INSERT, UPDATE, DELETE, SELECT)
- **🌐 HTTP Requests** - Make HTTP GET/POST requests asynchronously
- **🔐 Encryption** - MD5, SHA256, Base64 encoding/decoding
- **📁 File I/O** - YAML, JSON, and text file support
- **🎨 BossBar Support** - Create and manage boss bars with progress tracking
- **⏱️ Cooldown System** - Built-in cooldown management per player
- **⚙️ Config System** - Per-plugin configuration files (YAML)
- **🌍 World Management** - Control weather, time, world border, explosions
- **🎯 Entity Management** - Spawn, control, and customize entities
- **🔒 Player Management** - Ban, kick, teleport, health, food, gamemode

### 🛠️ Advanced Features

- **🎨 Particle Effects** - Spawn particles with string or enum support
- **🔊 Sound System** - Play sounds at locations or for players
- **📊 Scoreboard System** - Create and manage scoreboards, teams, objectives
- **📝 Item Manipulation** - Create, modify, and manage items with custom names and lore
- **🔒 Permission System** - Check and manage player permissions
- **🎨 Color Support** - Minecraft color codes and formatting utilities
- **🌐 HTTP Integration** - Make external API calls and web requests
- **📊 Economy Integration** - Vault economy support for server economies

---

## 📦 Installation

### Requirements

- **Minecraft Server**: Paper/Spigot 1.20+ (recommended: Paper)
- **Java**: Version 21 or higher
- **Minecraft Version**: 1.20+

### Steps

1. **Download the latest release** from the [Releases](https://github.com/LootingVI/MC-JS/releases) page
2. **Place the JAR file** in your server's `plugins/` folder
3. **Start or restart** your server
4. **Create JS plugins** in `plugins/MC-JS/js-plugins/`

The plugin will automatically create the `js-plugins` directory and copy an example plugin on first run.

---

## 🚀 Quick Start

### Prerequisites

- Minecraft server running Paper/Spigot 1.20+
- MC-JS plugin installed in `plugins/` folder
- Basic JavaScript knowledge

### Creating Your First Plugin

1. **Navigate to** `plugins/MC-JS/js-plugins/` directory
2. **Create a new file** with a `.js` extension (e.g., `myplugin.js`)
3. **Add the following code**:

```javascript
// Plugin metadata
var pluginInfo = {
    name: "My First Plugin",
    version: "1.0.0",
    author: "YourName",
    description: "My awesome plugin!"
};

function onEnable() {
    logger.info("My plugin is enabled!");
    
    // Register a command
    api.registerCommand("hello", "Say hello", "/hello", function(sender, args) {
        api.sendMessage(sender, "&aHello from JavaScript!");
        return true;
    });
    
    // Register an event
    api.registerEvent("player.PlayerJoinEvent", function(event) {
        var player = event.getPlayer();
        api.sendMessage(player, "&6Welcome to the server!");
    });
}

function onDisable() {
    logger.info("My plugin is disabled!");
}

// Export functions
this.onEnable = onEnable;
this.onDisable = onDisable;
this.pluginInfo = pluginInfo;
```

3. **Save the file** - The plugin will auto-load on server start, or use `/jsreload` to reload
4. **Test your command** - Type `/hello` in-game or in console
5. **Check console** - Look for "My plugin is enabled!" message

> 💡 **Tip**: Use `/jslist` to see all loaded JavaScript plugins and `/jsreload <plugin>` to reload a specific plugin.

---

## 📚 Documentation

> **📖 Full Documentation**: Visit our [complete documentation website](https://lootingvi.github.io/MC-JS/) for detailed API reference, examples, and guides.

### Available Objects

These objects are automatically available in all JavaScript plugins:

These objects are available in all JavaScript plugins:

| Object | Description | Usage |
|--------|-------------|-------|
| `api` | Complete JS API wrapper - main interface for all operations | `api.registerCommand(...)` |
| `server` | Minecraft server instance | `server.getOnlinePlayers()` |
| `plugin` | Main plugin instance | `plugin.getName()` |
| `logger` | Plugin logger | `logger.info("Message")` |
| `scheduler` | Server scheduler | `scheduler.runTask(...)` |
| `Bukkit` | Direct Bukkit API access | `Bukkit.getServer()` |

### Command Registration

```javascript
// Simple command
api.registerCommand("command", function(sender, args) {
    api.sendMessage(sender, "Command executed!");
    return true;
});

// Command with description and usage
api.registerCommand("command", "Description", "/command [args]", function(sender, args) {
    // Your code here
    return true;
});

// Command with tab completion
api.registerCommand("command", "Description", "/command [args]", 
    function(sender, args) {
        // Command executor
        return true;
    },
    function(sender, args) {
        // Tab completer - return array of strings
        return ["option1", "option2", "option3"];
    }
);
```

### Event Registration

```javascript
// Register event by string name
api.registerEvent("player.PlayerJoinEvent", function(event) {
    var player = event.getPlayer();
    api.sendMessage(player, "Welcome!");
});

// Register with priority
api.registerEvent("block.BlockBreakEvent", function(event) {
    // HIGH priority - runs before NORMAL
}, "HIGH");

// Available priorities: LOWEST, LOW, NORMAL, HIGH, HIGHEST, MONITOR
```

**Available Event Packages:**
- `player.*` - Player events (PlayerJoinEvent, PlayerQuitEvent, etc.)
- `block.*` - Block events (BlockBreakEvent, BlockPlaceEvent, etc.)
- `entity.*` - Entity events
- `inventory.*` - Inventory events
- `server.*` - Server events

### Task Scheduling

```javascript
// Run task after delay (in ticks, 20 ticks = 1 second)
api.runTaskLater(100, function() {
    logger.info("This runs after 5 seconds");
});

// Run repeating task
var task = api.runTaskTimer(0, 1200, function() {
    logger.info("This runs every minute");
});

// Cancel task
api.cancelTask(task);

// Run async task (for non-blocking operations)
api.runTaskAsync(function() {
    // This runs in a separate thread
    var data = api.httpGet("https://api.example.com/data");
    logger.info("Fetched: " + data);
});
```

### Player Management

```javascript
// Get players
var player = api.getPlayer("PlayerName");
var allPlayers = api.getOnlinePlayers();

// Send messages
api.sendMessage(player, "&aHello!");
api.broadcast("&6Server announcement!");
api.sendTitle(player, "&aTitle", "&eSubtitle");
api.sendActionBar(player, "&bAction bar message");

// Player properties
api.setHealth(player, 20.0);
api.setFoodLevel(player, 20);
api.setSaturation(player, 20.0); // Set saturation level
api.setGameMode(player, api.getMaterial("CREATIVE")); // Use GameMode enum
api.teleport(player, location);

// Clear player inventory
api.clearInventory(player);
```

### Inventory & Items

```javascript
// Method 1: Create inventory manually
var inv = api.createInventory(null, 27, "&6My GUI");

// Method 2: Use GUI Builder (recommended - easier and more powerful)
var gui = api.inventory.createGUI("&6My GUI", 3); // 3 rows = 27 slots
gui.setItem(10, item1)
   .setItem(12, item2)
   .setItem(14, item3)
   .onClick(function(event) {
       event.setCancelled(true);
       var slot = event.getSlot();
       if (slot === 10) {
           api.sendMessage(event.getWhoClicked(), "&aYou clicked item 1!");
       }
   })
   .onClose(function(event) {
       api.sendMessage(event.getPlayer(), "&7GUI closed!");
   });
gui.open(player);

// Create item
var item = api.createItemStack(api.getMaterial("DIAMOND"), 1);
item = api.setItemDisplayName(item, "&b&lSpecial Diamond");
item = api.setItemLore(item, [
    "&7Line 1",
    "&7Line 2"
]);

// Set item in inventory
api.setInventoryItem(inv, 0, item);

// Register inventory click handler
api.registerInventoryClick(inv, function(event) {
    event.setCancelled(true);
    api.sendMessage(event.getWhoClicked(), "You clicked slot " + event.getSlot());
});
```

### Database Operations

```javascript
// Create table
api.createTable("mydb", "players", {
    "id": "INTEGER PRIMARY KEY",
    "name": "TEXT",
    "level": "INTEGER"
});

// Insert data
api.insertData("mydb", "players", {
    "name": "PlayerName",
    "level": 10
});

// Update data
api.updateData("mydb", "players", 
    { "level": 20 }, 
    "name = 'PlayerName'"
);

// Query data
var results = api.querySQL("mydb", "SELECT * FROM players WHERE level > 5");
for (var i = 0; i < results.length; i++) {
    logger.info("Player: " + results[i].name + ", Level: " + results[i].level);
}

// Delete data
api.deleteData("mydb", "players", "level < 5");

// Count rows
var count = api.countRows("mydb", "players");
```

### File Operations

```javascript
// YAML files
var data = { "key": "value", "number": 42 };
api.saveYamlFile("config", data);
var loaded = api.loadYamlFile("config");

// JSON files
api.saveJsonFile("data", JSON.stringify({ "key": "value" }));
var json = api.loadJsonFile("data");
var obj = JSON.parse(json);

// Text files
api.saveTextFile("log", "Some text content");
var text = api.loadTextFile("log");
```

### Config System

```javascript
// Get plugin config (creates if doesn't exist)
var config = api.getPluginConfig("myplugin");

// Get config value with default
var setting = api.getPluginConfigValue("myplugin", "setting", "default");

// Set config value
api.setPluginConfigValue("myplugin", "setting", "new value");

// Save entire config
api.savePluginConfig("myplugin", { "setting1": "value1", "setting2": "value2" });
```

### Cooldown System

```javascript
// Check cooldown
if (!api.hasCooldown(player.getName(), "command")) {
    // Execute command
    api.setCooldown(player.getName(), "command", 5000); // 5 seconds
} else {
    var remaining = api.getCooldownRemaining(player.getName(), "command");
    api.sendMessage(player, "&cCooldown: " + (remaining / 1000) + " seconds");
}

// Remove cooldown
api.removeCooldown(player.getName(), "command");
```

### BossBar

```javascript
// Create boss bar
var bossBar = api.createBossBar("&cBoss Fight!", "RED", "SOLID");
bossBar.addPlayer(player);
bossBar.setProgress(0.5); // 50%

// Update boss bar
bossBar.setTitle("&6New Title");
bossBar.setProgress(0.75);

// Remove boss bar
bossBar.removePlayer(player);
bossBar.removeAll();
```

### Particles & Sounds

```javascript
// Spawn particles (string or enum)
api.spawnParticle(location, "FIREWORK", 10, 0.5, 0.5, 0.5, 0.1);
api.spawnParticle(location, Particle.FLAME, 5, 0, 0, 0);

// Play sounds
api.playSound(location, "ENTITY_PLAYER_LEVELUP", 1.0, 1.0);
api.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5, 1.5);
```

### Utility Methods

```javascript
// String utilities
var colored = api.colorize("&aHello &bWorld");
var stripped = api.stripColor(colored);
var formatted = api.format("Hello %s!", "World");

// Math utilities
var rounded = api.round(3.14159, 2); // 3.14
var clamped = api.clamp(150, 0, 100); // 100

// Encoding
var encoded = api.base64Encode("Hello");
var decoded = api.base64Decode(encoded);
var md5 = api.md5("text");
var sha256 = api.sha256("text");

// Date/Time
var now = api.getCurrentTimeMillis();
var formatted = api.formatDate(now);
var parsed = api.parseDate("2024-01-01 12:00:00");
```

---

## 💡 Examples

### Example 1: Welcome Plugin

```javascript
var pluginInfo = {
    name: "Welcome Plugin",
    version: "1.0.0",
    author: "YourName"
};

function onEnable() {
    api.registerEvent("player.PlayerJoinEvent", function(event) {
        var player = event.getPlayer();
        
        // Welcome message
        api.sendMessage(player, "&6Welcome to the server, &b" + player.getName() + "&6!");
        
        // Title
        api.sendTitle(player, "&aWelcome!", "&eEnjoy your stay!", 10, 70, 20);
        
        // Sound
        api.playSound(player, "ENTITY_PLAYER_LEVELUP", 1.0, 1.0);
        
        // Give welcome kit
        api.runTaskLater(20, function() {
            var bread = api.createItemStack(api.getMaterial("BREAD"), 5);
            api.giveItem(player, bread);
        });
    });
}

this.onEnable = onEnable;
this.pluginInfo = pluginInfo;
```

### Example 2: Custom Command with Database

```javascript
var pluginInfo = {
    name: "Stats Plugin",
    version: "1.0.0"
};

function onEnable() {
    // Create database table
    api.createTable("stats", "player_stats", {
        "player_name": "TEXT PRIMARY KEY",
        "kills": "INTEGER DEFAULT 0",
        "deaths": "INTEGER DEFAULT 0"
    });
    
    // Register command
    api.registerCommand("stats", "View your stats", "/stats [player]", 
        function(sender, args) {
            var targetName = args.length > 0 ? args[0] : sender.getName();
            
            var results = api.querySQL("stats", 
                "SELECT * FROM player_stats WHERE player_name = '" + targetName + "'");
            
            if (results.length > 0) {
                var stats = results[0];
                api.sendMessage(sender, "&6=== Stats for " + targetName + " ===");
                api.sendMessage(sender, "&eKills: &a" + stats.kills);
                api.sendMessage(sender, "&eDeaths: &c" + stats.deaths);
            } else {
                api.sendMessage(sender, "&cNo stats found for " + targetName);
            }
            
            return true;
        },
        function(sender, args) {
            // Tab completion
            var players = api.getPlayerNames();
            return players;
        }
    );
    
    // Track kills
    api.registerEvent("entity.PlayerDeathEvent", function(event) {
        var killer = event.getEntity().getKiller();
        var victim = event.getEntity();
        
        if (killer) {
            // Update killer stats
            var killerStats = api.querySQL("stats", 
                "SELECT kills FROM player_stats WHERE player_name = '" + killer.getName() + "'");
            
            if (killerStats.length > 0) {
                api.updateData("stats", "player_stats", 
                    { "kills": killerStats[0].kills + 1 },
                    "player_name = '" + killer.getName() + "'");
            } else {
                api.insertData("stats", "player_stats", {
                    "player_name": killer.getName(),
                    "kills": 1,
                    "deaths": 0
                });
            }
        }
        
        // Update victim stats
        var victimStats = api.querySQL("stats",
            "SELECT deaths FROM player_stats WHERE player_name = '" + victim.getName() + "'");
        
        if (victimStats.length > 0) {
            api.updateData("stats", "player_stats",
                { "deaths": victimStats[0].deaths + 1 },
                "player_name = '" + victim.getName() + "'");
        } else {
            api.insertData("stats", "player_stats", {
                "player_name": victim.getName(),
                "kills": 0,
                "deaths": 1
            });
        }
    });
}

this.onEnable = onEnable;
this.pluginInfo = pluginInfo;
```

### Example 3: Custom GUI Menu

```javascript
var pluginInfo = {
    name: "Menu Plugin",
    version: "1.0.0"
};

function onEnable() {
    api.registerCommand("menu", "Open custom menu", "/menu", function(sender, args) {
        var player = api.getPlayerFromSender(sender);
        if (!player) {
            api.sendMessage(sender, "&cOnly players can use this command!");
            return false;
        }
        var inv = api.createInventory(null, 27, "&6Custom Menu");
        
        // Fill with items
        var item1 = api.createItemStack(api.getMaterial("DIAMOND"), 1);
        item1 = api.setItemDisplayName(item1, "&bOption 1");
        item1 = api.setItemLore(item1, ["&7Click me!"]);
        api.setInventoryItem(inv, 10, item1);
        
        var item2 = api.createItemStack(api.getMaterial("EMERALD"), 1);
        item2 = api.setItemDisplayName(item2, "&aOption 2");
        item2 = api.setItemLore(item2, ["&7Click me too!"]);
        api.setInventoryItem(inv, 16, item2);
        
        // Register click handler
        api.registerInventoryClick(inv, function(event) {
            event.setCancelled(true);
            var slot = event.getSlot();
            
            if (slot == 10) {
                api.sendMessage(player, "&aYou clicked Option 1!");
                api.giveItem(player, api.createItemStack(api.getMaterial("DIAMOND"), 5));
            } else if (slot == 16) {
                api.sendMessage(player, "&aYou clicked Option 2!");
                api.giveItem(player, api.createItemStack(api.getMaterial("EMERALD"), 5));
            }
        });
        
        player.openInventory(inv);
        return true;
    });
}

this.onEnable = onEnable;
this.pluginInfo = pluginInfo;
```

---

## 🎮 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/jsreload` | Reload all JS plugins | `mcjs.admin` |
| `/jsreload <plugin>` | Reload specific plugin | `mcjs.admin` |
| `/jslist` | List all loaded JS plugins | `mcjs.admin` |

---

## 📖 Complete API Reference

### Command Methods
- `api.registerCommand(name, executor)`
- `api.registerCommand(name, description, usage, executor)`
- `api.registerCommand(name, description, usage, executor, tabCompleter)`

### Event Methods
- `api.registerEvent(eventClassName, handler)`
- `api.registerEvent(eventClass, handler, priority)`

### Scheduler Methods
- `api.runTask(task)`
- `api.runTaskLater(delay, task)`
- `api.runTaskTimer(delay, period, task)`
- `api.runTaskAsync(task)`
- `api.runTaskLaterAsync(delay, task)`
- `api.cancelTask(task)`

### Player Methods
- `api.getPlayer(name)` - Get player by name (accepts string or object)
- `api.getPlayerExact(name)` - Get exact player match (accepts string or object)
- `api.getPlayerFromSender(sender)` - Convert CommandSender to Player (returns null if not a player)
- `api.isPlayer(sender)` - Check if CommandSender is a Player
- `api.getOnlinePlayers()` - Get all online players
- `api.sendMessage(sender, message)` - Send message to CommandSender
- `api.sendTitle(player, title, subtitle)` - Send title to player
- `api.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut)` - Send title with custom timings
- `api.sendActionBar(player, message)` - Send action bar message
- `api.broadcast(message)` - Broadcast message to all players
- `api.broadcast(message, permission)` - Broadcast message to players with permission
- `api.setHealth(player, health)` - Set player health
- `api.setFoodLevel(player, level)` - Set player food level
- `api.setSaturation(player, saturation)` - Set player saturation level (float)
- `api.getMaxHealth(player)` - Get player's maximum health
- `api.clearInventory(player)` - Clear player's inventory

### Inventory Methods
- `api.createInventory(holder, size, title)` - Create inventory with custom holder
- `api.inventory.createGUI(title, rows)` - Create GUI using builder pattern (recommended)
- `api.inventory.createInventoryWithHolder(holder, size, title)` - Create inventory with custom holder
- `api.setInventoryItem(inventory, slot, item)` - Set item in inventory slot
- `api.getInventoryItem(inventory, slot)` - Get item from inventory slot
- `api.fillInventory(inventory, item)` - Fill inventory with item
- `api.registerInventoryClick(inventory, handler)` - Register click handler for inventory
- `api.registerInventoryClose(inventory, handler)` - Register close handler for inventory

**GUI Builder Example:**
```javascript
var gui = api.inventory.createGUI("&6My Menu", 3);
gui.setItem(10, item1).setItem(12, item2);
gui.onClick(function(event) { /* handler */ });
gui.onClose(function(event) { /* handler */ });
gui.open(player);
```

### Item Methods
- `api.createItemStack(material, amount)`
- `api.setItemDisplayName(item, name)`
- `api.setItemLore(item, lore)`
- `api.giveItem(player, item)`
- `api.getItemInMainHand(player)`

### Database Methods
- `api.createTable(dbName, tableName, columns)`
- `api.insertData(dbName, tableName, data)`
- `api.updateData(dbName, tableName, data, whereClause)`
- `api.deleteData(dbName, tableName, whereClause)`
- `api.querySQL(dbName, sql)`
- `api.countRows(dbName, tableName)`
- `api.countRows(dbName, tableName, whereClause)`

### File Methods
- `api.saveYamlFile(fileName, data)`
- `api.loadYamlFile(fileName)`
- `api.saveJsonFile(fileName, content)`
- `api.loadJsonFile(fileName)`
- `api.saveTextFile(fileName, content)`
- `api.loadTextFile(fileName)`

### Config Methods
- `api.getPluginConfig(pluginName)`
- `api.savePluginConfig(pluginName, data)`
- `api.getPluginConfigValue(pluginName, path)`
- `api.getPluginConfigValue(pluginName, path, defaultValue)`
- `api.setPluginConfigValue(pluginName, path, value)`

### Cooldown Methods
- `api.hasCooldown(playerName, key)`
- `api.setCooldown(playerName, key, durationMillis)`
- `api.getCooldownRemaining(playerName, key)`
- `api.removeCooldown(playerName, key)`
- `api.clearCooldowns(playerName)`

### BossBar Methods
- `api.createBossBar(title, color, style)`
- `api.createBossBar(title, colorName, styleName)`

### Particle & Sound Methods
- `api.spawnParticle(location, particle, count)`
- `api.spawnParticle(location, particleName, count, offsetX, offsetY, offsetZ, extra)`
- `api.playSound(location, sound, volume, pitch)`
- `api.playSound(location, soundName, volume, pitch)`

### Utility Methods
- `api.colorize(text)` - Convert color codes to formatted text
- `api.stripColor(text)` - Remove color codes from text
- `api.format(format, ...args)` - Format string with arguments
- `api.round(value, places)` - Round number to decimal places
- `api.clamp(value, min, max)` - Clamp value between min and max
- `api.getMaterial(materialName)` - Get Material enum from string
- `api.md5(input)` - Generate MD5 hash
- `api.sha256(input)` - Generate SHA-256 hash
- `api.base64Encode(input)` - Encode string to Base64
- `api.base64Decode(input)` - Decode Base64 string
- `api.getCurrentTimeMillis()` - Get current timestamp in milliseconds
- `api.formatDate(timestamp)` - Format timestamp to date string
- `api.formatDate(timestamp, format)` - Format timestamp with custom format
- `api.getServerVersion()` - Get server version string
- `api.getMaxPlayers()` - Get maximum player count

*And many more! Check the source code for the complete list.*

---

## 🐛 Troubleshooting

### Plugin not loading?
- Check the server console for errors
- Ensure the file has a `.js` extension
- Verify the JavaScript syntax is correct
- Check that `onEnable` function is defined

### Events not firing?
- Make sure you're using the correct event class name
- Check the event package (e.g., `player.PlayerJoinEvent`)
- Verify the event handler function is correct

### Commands not working?
- Ensure the command is registered in `onEnable`
- Check for JavaScript errors in console
- Verify the command executor returns `true` or `false`

### Need help?
- Check the [Issues](https://github.com/LootingVI/MC-JS/issues) page
- Create a new issue with your problem
- Include error logs and your plugin code
- Visit the [Documentation Website](https://lootingvi.github.io/MC-JS/) for complete API reference

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 🛠️ Development Tools

- **📝 Commit Generator**: Use `python scripts/commit-generator.py` for automatic commit messages
- **📋 Issue Templates**: Bug reports and feature requests templates included
- **🤝 Contributing**: See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines
- **📖 Documentation**: Complete API reference available at [lootingvi.github.io/MC-JS](https://lootingvi.github.io/MC-JS/)

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built with [Rhino JavaScript Engine](https://github.com/mozilla/rhino)
- Compatible with [Paper](https://papermc.io/), [Spigot](https://www.spigotmc.org/), and [Bukkit](https://bukkit.org/)
- Inspired by the need for easier plugin development
- Special thanks to the Minecraft plugin development community

---

<div align="center">

**Made with ❤️ for the Minecraft Community**

[⭐ Star this repo](https://github.com/LootingVI/MC-JS) • [🐛 Report Bug](https://github.com/LootingVI/MC-JS/issues) • [💡 Request Feature](https://github.com/LootingVI/MC-JS/issues) • [📚 Documentation](https://lootingvi.github.io/MC-JS/)

</div>
