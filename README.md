# 🚀 MC-JS - JavaScript Plugin System for Minecraft

**Write Minecraft Server Plugins in JavaScript - No Java Required!**

MC-JS is a powerful plugin system that allows you to develop server plugins using modern JavaScript (ES6+) instead of Java. Perfect for developers who already know JavaScript or want to quickly create plugins without having to learn Java.

## ✨ Key Features

### 🎯 Core Capabilities

- **📝 Full JavaScript Support** - Develop plugins in modern JavaScript using the Rhino Engine
- **⚡ Hot Reload** - Reload plugins without server restart (`/jsreload`)
- **🔧 Complete API Access** - Access to virtually all Bukkit/Spigot/Paper API functions
- **🎮 Event System** - Register listeners for any Minecraft event with priority support
- **💬 Command System** - Create custom commands with full tab completion support
- **⏰ Task Scheduling** - Synchronous and asynchronous task scheduling
- **📦 Inventory Management** - Create and manage custom GUI menus using standard Bukkit inventories (9-54 slots) with click handlers
- **🗄️ Database Support** - Built-in SQLite database operations (INSERT, UPDATE, DELETE, SELECT)
- **🌐 HTTP Requests** - Make HTTP GET/POST requests (`api.network.httpGet` / `httpPost`)
- **🔐 Encryption** - MD5, SHA256, Base64 encoding/decoding
- **📁 File I/O** - YAML, JSON, and text file support
- **🗂️ Persistent Data** - Per-player storage, prices, kits and cooldowns that survive restarts (`api.systems`)
- **⏱️ Cooldown System** - Built-in per-player cooldowns (`api.systems.claimCooldown`)
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
- **📊 Economy Integration** - Vault economy support for server economies (`api.vault`)

## 📦 Installation

### Requirements

- **Minecraft Server**: Paper 26.1 or higher (recommended: Paper)
- **Java**: Version 25 or higher
- **Minecraft Version**: 26.1

### Steps

1. **Download the latest release** from the [Releases](https://github.com/LootingVI/MC-JS/releases) page
2. **Place the JAR file** in your server's `plugins/` folder
3. **Start or restart** your server
4. **Create JS plugins** in `plugins/MC-JS/js-plugins/` directory

The plugin will automatically create the `js-plugins` directory and copy an example plugin on first run.

## 🚀 Quick Start

### Creating Your First Plugin

1. **Navigate to** `plugins/MC-JS/js-plugins/` directory
2. **Create a new file** with `.js` extension (e.g., `myplugin.js`)
3. **Add the following code**:

```javascript
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


this.onEnable = onEnable;
this.onDisable = onDisable;
this.pluginInfo = pluginInfo;
```

4. **Save the file** - The plugin will auto-load on server start, or use `/jsreload` to reload
5. **Test your command** - Type `/hello` in-game or in console
6. **Check console** - Look for "My plugin is enabled!" message

> 💡 **Tip**: Use `/jslist` to see all loaded JavaScript plugins and `/jsreload <plugin>` to reload a specific plugin.

## 🖥️ Web Dashboard & Block Editor

Run `/mjs dashboard` in-game (permission `mcjs.dashboard`) to receive a one-time login link. The browser shows `/mjs dashboard <verify-code>`; enter that command with the same player to grant access to `http://localhost:8765`.

The built-in Plugin Studio includes a JavaScript editor with API suggestions, a Scratch-style Blockly editor, plugin templates, syntax checking, save/start/stop actions, and JavaScript/block-project import and export. See [dashboard/README.md](dashboard/README.md) for setup, permissions and HTTPS configuration.

## 📚 Available Objects

These objects are automatically available in all JavaScript plugins:

| Object | Description | Usage |
|--------|-------------|-------|
| `api` | Complete JS API wrapper - main interface for all operations | `api.registerCommand(...)` |
| `server` | Minecraft server instance | `server.getOnlinePlayers()` |
| `plugin` | Main plugin instance | `plugin.getName()` |
| `logger` | Plugin logger | `logger.info("Message")` |
| `scheduler` | Server scheduler | `scheduler.runTask(...)` |
| `Bukkit` | Direct Bukkit API access | `Bukkit.getServer()` |

## 🎮 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/mjs dashboard` | Open and verify the MC-JS plugin editor | `mcjs.dashboard` |
| `/jsreload` | Reload all JS plugins | `mcjs.admin` |
| `/jsreload <plugin>` | Reload specific plugin | `mcjs.admin` |
| `/jslist` | List all loaded JS plugins | `mcjs.admin` |
| `/jsconfig` | View or reload plugin configuration | `mcjs.admin` |

## 📖 Documentation

> **📖 Full Documentation**: Visit our [complete documentation website](https://lootingvi.github.io/MC-JS/) for detailed API reference, examples, and guides.

## 💡 Example Applications

- **Welcome Plugins** - Greet players with messages, titles, and sounds
- **Statistics Systems** - Track player statistics with SQLite databases
- **Custom GUI Menus** - Create interactive menus using standard Bukkit inventories with custom layouts and click handlers
- **Mini-Games** - Develop simple games with event handling
- **Utility Plugins** - Create helpful tools and commands
- **Integration Plugins** - Connect your server with external APIs

## 🛠️ Technical Details

- **JavaScript Engine**: Rhino (Mozilla)
- **MC-JS Version**: 2.1.0
- **API Version**: 26.1
- **Compatibility**: Paper (Vault optional, soft-dependency)
- **License**: MIT

## 🤝 Support

- **GitHub**: [LootingVI/MC-JS](https://github.com/LootingVI/MC-JS)
- **Issues**: [GitHub Issues](https://github.com/LootingVI/MC-JS/issues)
- **Documentation**: [lootingvi.github.io/MC-JS](https://lootingvi.github.io/MC-JS/)

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Made with ❤️ for the Minecraft Community**