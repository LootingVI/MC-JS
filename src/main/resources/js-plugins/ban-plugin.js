// ========================================
// BAN PLUGIN - Umfassendes Ban-System
// ========================================
// Features:
// - Ban/Unban System mit IDs
// - Temporäre & permanente Bans
// - Ban-History mit Datenbank
// - GUI für Ban-Management
// - IP-Bans
// - Ban-Liste GUI
// - Warn-System
// ========================================

var pluginInfo = {
    name: "Advanced Ban System",
    version: "2.0.0",
    author: "BanPlugin",
    description: "Umfassendes Ban-System mit GUI, IDs und Datenbank"
};

// ========================================
// GLOBALE VARIABLEN
// ========================================
var banReasons = [
    "Hacking/Cheating",
    "Griefing",
    "Spamming",
    "Beleidigung",
    "Werbung",
    "Bugusing",
    "Teambeleidigung",
    "Andere"
];

var banDurations = {
    "1h": 3600000,
    "6h": 21600000,
    "1d": 86400000,
    "3d": 259200000,
    "7d": 604800000,
    "30d": 2592000000,
    "Permanent": -1
};

var activeBanGUIs = {};

// ========================================
// ONNABLE - PLUGIN INITIALISIERUNG
// ========================================
function onEnable() {
    logger.info("=================================");
    logger.info("Advanced Ban System wird geladen...");
    logger.info("=================================");
    
    // Datenbank initialisieren
    initializeDatabase();
    
    // Commands registrieren
    registerCommands();
    
    // Events registrieren
    registerEvents();
    
    logger.info("=================================");
    logger.info("Ban System erfolgreich geladen!");
    logger.info("=================================");
}

// ========================================
// ONDISABLE - PLUGIN DEAKTIVIERUNG
// ========================================
function onDisable() {
    logger.info("Ban System wird deaktiviert...");
    
    // Alle offenen GUIs schließen
    for (var playerName in activeBanGUIs) {
        var player = api.getPlayer(playerName);
        if (player) {
            player.closeInventory();
        }
    }
    
    logger.info("Ban System deaktiviert!");
}

// ========================================
// DATENBANK INITIALISIERUNG
// ========================================
function initializeDatabase() {
    logger.info("Initialisiere Datenbank...");
    
    // Bans Tabelle
    api.createTable("bansystem", "bans", {
        "id": "INTEGER PRIMARY KEY AUTOINCREMENT",
        "player_name": "TEXT NOT NULL",
        "player_uuid": "TEXT NOT NULL",
        "banned_by": "TEXT NOT NULL",
        "reason": "TEXT NOT NULL",
        "ban_time": "INTEGER NOT NULL",
        "expire_time": "INTEGER",
        "is_permanent": "INTEGER DEFAULT 0",
        "is_active": "INTEGER DEFAULT 1",
        "ip_address": "TEXT"
    });
    
    // Warnings Tabelle
    api.createTable("bansystem", "warnings", {
        "id": "INTEGER PRIMARY KEY AUTOINCREMENT",
        "player_name": "TEXT NOT NULL",
        "player_uuid": "TEXT NOT NULL",
        "warned_by": "TEXT NOT NULL",
        "reason": "TEXT NOT NULL",
        "warn_time": "INTEGER NOT NULL"
    });
    
    // IP-Bans Tabelle
    api.createTable("bansystem", "ipbans", {
        "id": "INTEGER PRIMARY KEY AUTOINCREMENT",
        "ip_address": "TEXT NOT NULL UNIQUE",
        "banned_by": "TEXT NOT NULL",
        "reason": "TEXT NOT NULL",
        "ban_time": "INTEGER NOT NULL"
    });
    
    logger.info("Datenbank erfolgreich initialisiert!");
}

// ========================================
// COMMANDS REGISTRIEREN
// ========================================
function registerCommands() {
    logger.info("Registriere Commands...");
    
    // /ban Command
    api.registerCommand("ban", "Banne einen Spieler", "/ban <Spieler> [Grund]", 
        function(sender, args) {
            if (!api.hasPermission(sender, "bansystem.ban")) {
                api.sendMessage(sender, "&cDu hast keine Berechtigung für diesen Befehl!");
                return false;
            }
            
            if (args.length < 1) {
                api.sendMessage(sender, "&cNutzung: /ban <Spieler> [Grund]");
                return false;
            }
            
            var targetName = args[0];
            
            // Prüfe ob sender ein Spieler ist
            if (sender instanceof Player) {
                // Ist ein Spieler - GUI öffnen
                openBanGUI(sender, targetName);
            } else {
                // Console Ban (permanent)
                var reason = args.length > 1 ? args.slice(1).join(" ") : "Kein Grund angegeben";
                banPlayer(targetName, "Console", reason, -1, true);
            }
            
            return true;
        },
        function(sender, args) {
            if (args.length === 1) {
                return api.getPlayerNames();
            }
            return [];
        }
    );
    
    // /unban Command
    api.registerCommand("unban", "Entbanne einen Spieler", "/unban <Spieler>",
        function(sender, args) {
            if (!api.hasPermission(sender, "bansystem.unban")) {
                api.sendMessage(sender, "&cDu hast keine Berechtigung für diesen Befehl!");
                return false;
            }
            
            if (args.length < 1) {
                api.sendMessage(sender, "&cNutzung: /unban <Spieler>");
                return false;
            }
            
            var targetName = args[0];
            unbanPlayer(targetName, sender.getName());
            
            return true;
        }
    );
    
    // /banlist Command
    api.registerCommand("banlist", "Zeige alle Bans", "/banlist",
        function(sender, args) {
            if (!api.hasPermission(sender, "bansystem.banlist")) {
                api.sendMessage(sender, "&cDu hast keine Berechtigung für diesen Befehl!");
                return false;
            }
            
            if (sender instanceof Player) {
                // Ist ein Spieler - GUI öffnen
                openBanListGUI(sender);
            } else {
                showBanListConsole(sender);
            }
            
            return true;
        }
    );
    
    // /baninfo Command
    api.registerCommand("baninfo", "Zeige Ban-Informationen", "/baninfo <Spieler>",
        function(sender, args) {
            if (!api.hasPermission(sender, "bansystem.info")) {
                api.sendMessage(sender, "&cDu hast keine Berechtigung für diesen Befehl!");
                return false;
            }
            
            if (args.length < 1) {
                api.sendMessage(sender, "&cNutzung: /baninfo <Spieler>");
                return false;
            }
            
            var targetName = args[0];
            showBanInfo(sender, targetName);
            
            return true;
        }
    );
    
    // /warn Command
    api.registerCommand("warn", "Warne einen Spieler", "/warn <Spieler> <Grund>",
        function(sender, args) {
            if (!api.hasPermission(sender, "bansystem.warn")) {
                api.sendMessage(sender, "&cDu hast keine Berechtigung für diesen Befehl!");
                return false;
            }
            
            if (args.length < 2) {
                api.sendMessage(sender, "&cNutzung: /warn <Spieler> <Grund>");
                return false;
            }
            
            var targetName = args[0];
            var reason = args.slice(1).join(" ");
            warnPlayer(targetName, sender.getName(), reason);
            
            return true;
        },
        function(sender, args) {
            if (args.length === 1) {
                return api.getPlayerNames();
            }
            return [];
        }
    );
    
    // /warnings Command
    api.registerCommand("warnings", "Zeige Warnungen eines Spielers", "/warnings <Spieler>",
        function(sender, args) {
            if (!api.hasPermission(sender, "bansystem.warnings")) {
                api.sendMessage(sender, "&cDu hast keine Berechtigung für diesen Befehl!");
                return false;
            }
            
            if (args.length < 1) {
                api.sendMessage(sender, "&cNutzung: /warnings <Spieler>");
                return false;
            }
            
            var targetName = args[0];
            showWarnings(sender, targetName);
            
            return true;
        }
    );
    
    // /ipban Command
    api.registerCommand("ipban", "Banne eine IP-Adresse", "/ipban <IP> <Grund>",
        function(sender, args) {
            if (!api.hasPermission(sender, "bansystem.ipban")) {
                api.sendMessage(sender, "&cDu hast keine Berechtigung für diesen Befehl!");
                return false;
            }
            
            if (args.length < 2) {
                api.sendMessage(sender, "&cNutzung: /ipban <IP> <Grund>");
                return false;
            }
            
            var ip = args[0];
            var reason = args.slice(1).join(" ");
            ipBanAddress(ip, sender.getName(), reason);
            
            return true;
        }
    );
    
    logger.info("Commands erfolgreich registriert!");
}

// ========================================
// EVENTS REGISTRIEREN
// ========================================
function registerEvents() {
    logger.info("Registriere Events...");
    
    // Player Join Event - Ban Check
    api.registerEvent("player.PlayerJoinEvent", function(event) {
        var player = event.getPlayer();
        var playerName = player.getName();
        var uuid = player.getUniqueId().toString();
        var ip = "";
        try {
            if (player.getAddress() && player.getAddress().getAddress()) {
                ip = player.getAddress().getAddress().getHostAddress();
            }
        } catch (e) {
            // IP nicht verfügbar
            ip = "";
        }
        
        // IP-Ban Check
        var ipBanResults = api.querySQL("bansystem", 
            "SELECT * FROM ipbans WHERE ip_address = '" + ip + "'");
        
        if (ipBanResults.length > 0) {
            var ipBan = ipBanResults[0];
            var kickMessage = api.colorize("&c&l╔═══════════════════════════╗\n") +
                api.colorize("&c&l║   DU WURDEST GEBANNT!   ║\n") +
                api.colorize("&c&l╚═══════════════════════════╝\n\n") +
                api.colorize("&7Grund: &f" + ipBan.reason + "\n") +
                api.colorize("&7Typ: &cIP-Ban\n") +
                api.colorize("&7Von: &f" + ipBan.banned_by + "\n\n") +
                api.colorize("&cDiese IP-Adresse wurde gebannt!");
            api.runTaskLater(5, function(p) {
                return function() {
                    p.kickPlayer(kickMessage);
                };
            }(player));
            return;
        }
        
        // Player Ban Check
        var banResults = api.querySQL("bansystem", 
            "SELECT * FROM bans WHERE player_uuid = '" + uuid + "' AND is_active = 1");
        
        if (banResults.length > 0) {
            var ban = banResults[0];
            var currentTime = api.getCurrentTimeMillis();
            
            // Temporären Ban checken
            if (ban.is_permanent === 0 && ban.expire_time < currentTime) {
                // Ban abgelaufen
                api.updateData("bansystem", "bans",
                    { "is_active": 0 },
                    "id = " + ban.id);
                return;
            }
            
            // Spieler kicken
            var kickMessage = formatBanMessage(ban);
            api.runTaskLater(5, function(p) {
                return function() {
                    p.kickPlayer(kickMessage);
                };
            }(player));
        }
    });
    
    logger.info("Events erfolgreich registriert!");
}

// ========================================
// BAN GUI ÖFFNEN
// ========================================
function openBanGUI(sender, targetName) {
    var inv = api.createInventory(null, 54, "&c&lBan: &f" + targetName);
    activeBanGUIs[sender.getName()] = {
        inventory: inv,
        target: targetName
    };
    
    // Hintergrund füllen
    var glass = api.createItemStack(api.getMaterial("BLACK_STAINED_GLASS_PANE"), 1);
    glass = api.setItemDisplayName(glass, " ");
    for (var i = 0; i < 54; i++) {
        api.setInventoryItem(inv, i, glass);
    }
    
    // Ban-Gründe
    for (var i = 0; i < banReasons.length && i < 7; i++) {
        var reason = banReasons[i];
        var item = api.createItemStack(api.getMaterial("PAPER"), 1);
        item = api.setItemDisplayName(item, "&e" + reason);
        item = api.setItemLore(item, [
            "&7Klicke, um diesen Grund",
            "&7als Ban-Grund zu wählen"
        ]);
        api.setInventoryItem(inv, 10 + i, item);
    }
    
    // Ban-Dauern
    var durations = ["1h", "6h", "1d", "3d", "7d", "30d", "Permanent"];
    var materials = ["IRON_INGOT", "GOLD_INGOT", "DIAMOND", "EMERALD", "NETHERITE_INGOT", "NETHER_STAR", "BEDROCK"];
    
    for (var i = 0; i < durations.length; i++) {
        var duration = durations[i];
        var material = materials[i];
        var item = api.createItemStack(api.getMaterial(material), 1);
        item = api.setItemDisplayName(item, "&6" + duration);
        item = api.setItemLore(item, [
            "&7Klicke, um diese Dauer",
            "&7für den Ban zu wählen"
        ]);
        api.setInventoryItem(inv, 28 + i, item);
    }
    
    // Info
    var info = api.createItemStack(api.getMaterial("BOOK"), 1);
    info = api.setItemDisplayName(info, "&b&lAnleitung");
    info = api.setItemLore(info, [
        "&7",
        "&e1. &7Wähle einen Grund",
        "&e2. &7Wähle eine Dauer",
        "&e3. &7Bestätige den Ban",
        "&7",
        "&cVorsicht: &7Diese Aktion kann",
        "&7nicht rückgängig gemacht werden!"
    ]);
    api.setInventoryItem(inv, 49, info);
    
    // Abbrechen
    var cancel = api.createItemStack(api.getMaterial("BARRIER"), 1);
    cancel = api.setItemDisplayName(cancel, "&c&lAbbrechen");
    cancel = api.setItemLore(cancel, ["&7Klicke zum Schließen"]);
    api.setInventoryItem(inv, 53, cancel);
    
    // Click Handler
    api.registerInventoryClick(inv, function(event) {
        event.setCancelled(true);
        
        var clickedSlot = event.getSlot();
        var player = event.getWhoClicked();
        var guiData = activeBanGUIs[player.getName()];
        
        if (!guiData) return;
        
        // Abbrechen
        if (clickedSlot === 53) {
            player.closeInventory();
            delete activeBanGUIs[player.getName()];
            api.sendMessage(player, "&cBan abgebrochen.");
            return;
        }
        
        // Grund auswählen
        if (clickedSlot >= 10 && clickedSlot <= 16) {
            var reasonIndex = clickedSlot - 10;
            if (reasonIndex < banReasons.length) {
                guiData.reason = banReasons[reasonIndex];
                api.sendMessage(player, "&aGrund ausgewählt: &e" + guiData.reason);
                
                // Bestätigungs-Button anzeigen, wenn Grund UND Dauer gewählt
                if (guiData.reason && guiData.duration) {
                    showConfirmButton(inv, guiData);
                }
            }
        }
        
        // Dauer auswählen
        if (clickedSlot >= 28 && clickedSlot <= 34) {
            var durationIndex = clickedSlot - 28;
            var durations = ["1h", "6h", "1d", "3d", "7d", "30d", "Permanent"];
            if (durationIndex < durations.length) {
                guiData.duration = durations[durationIndex];
                api.sendMessage(player, "&aDauer ausgewählt: &e" + guiData.duration);
                
                // Bestätigungs-Button anzeigen, wenn Grund UND Dauer gewählt
                if (guiData.reason && guiData.duration) {
                    showConfirmButton(inv, guiData);
                }
            }
        }
        
        // Bestätigen
        if (clickedSlot === 40 && guiData.reason && guiData.duration) {
            player.closeInventory();
            
            var isPermanent = guiData.duration === "Permanent";
            var duration = isPermanent ? -1 : banDurations[guiData.duration];
            
            banPlayer(guiData.target, player.getName(), guiData.reason, duration, isPermanent);
            
            delete activeBanGUIs[player.getName()];
        }
    });
    
    // Close Handler
    api.registerInventoryClose(inv, function(event) {
        var player = event.getPlayer();
        if (activeBanGUIs[player.getName()]) {
            delete activeBanGUIs[player.getName()];
        }
    });
    
    sender.openInventory(inv);
}

// ========================================
// BESTÄTIGUNGS-BUTTON ANZEIGEN
// ========================================
function showConfirmButton(inv, guiData) {
    var confirm = api.createItemStack(api.getMaterial("LIME_CONCRETE"), 1);
    confirm = api.setItemDisplayName(confirm, "&a&lBan bestätigen");
    confirm = api.setItemLore(confirm, [
        "&7",
        "&eSpieler: &f" + guiData.target,
        "&eGrund: &f" + guiData.reason,
        "&eDauer: &f" + guiData.duration,
        "&7",
        "&cKlicke zum Bestätigen!"
    ]);
    api.setInventoryItem(inv, 40, confirm);
}

// ========================================
// SPIELER BANNEN
// ========================================
function banPlayer(targetName, bannedBy, reason, duration, isPermanent) {
    var target = api.getPlayer(targetName);
    var uuid = null;
    var ip = "";
    
    if (target) {
        uuid = target.getUniqueId().toString();
        try {
            if (target.getAddress() && target.getAddress().getAddress()) {
                ip = target.getAddress().getAddress().getHostAddress();
            }
        } catch (e) {
            // IP nicht verfügbar
            ip = "";
        }
    } else {
        // Versuche UUID aus Datenbank zu holen
        var oldBans = api.querySQL("bansystem", 
            "SELECT player_uuid FROM bans WHERE player_name = '" + targetName + "' LIMIT 1");
        
        if (oldBans.length > 0 && oldBans[0].player_uuid && oldBans[0].player_uuid !== "") {
            uuid = oldBans[0].player_uuid;
        } else {
            // Versuche UUID vom OfflinePlayer zu holen
            try {
                var offlinePlayer = server.getOfflinePlayer(targetName);
                if (offlinePlayer && offlinePlayer.getUniqueId) {
                    var offlineUuid = offlinePlayer.getUniqueId();
                    if (offlineUuid) {
                        uuid = offlineUuid.toString();
                    }
                }
            } catch (e) {
                // OfflinePlayer nicht verfügbar
                logger.warning("Konnte OfflinePlayer für " + targetName + " nicht abrufen: " + e);
            }
            
            // Fallback: Generiere eine deterministische UUID basierend auf dem Namen
            if (!uuid || uuid === "" || uuid === null) {
                // Verwende eine deterministische UUID basierend auf dem Namen
                // Format: 00000000-0000-0000-0000-XXXXXXXXXXXX (X = Hash des Namens)
                var nameHash = 0;
                for (var i = 0; i < targetName.length; i++) {
                    nameHash = ((nameHash << 5) - nameHash) + targetName.charCodeAt(i);
                    nameHash = nameHash & nameHash; // Convert to 32bit integer
                }
                // Konvertiere zu positiver Zahl und zu Hex (12 Zeichen)
                var hexHash = Math.abs(nameHash).toString(16);
                while (hexHash.length < 12) {
                    hexHash = "0" + hexHash;
                }
                uuid = "00000000-0000-0000-0000-" + hexHash;
            }
        }
    }
    
    // Sicherstellen, dass UUID nicht leer ist
    if (!uuid || uuid === "" || uuid === null) {
        logger.severe("Konnte keine UUID für Spieler " + targetName + " ermitteln!");
        var banner = api.getPlayer(bannedBy);
        if (banner) {
            api.sendMessage(banner, "&cFehler: Konnte keine UUID für " + targetName + " ermitteln!");
        }
        return;
    }
    
    var currentTime = api.getCurrentTimeMillis();
    var expireTime = isPermanent ? 0 : currentTime + duration;
    
    // In Datenbank speichern
    api.insertData("bansystem", "bans", {
        "player_name": targetName,
        "player_uuid": uuid,
        "banned_by": bannedBy,
        "reason": reason,
        "ban_time": currentTime,
        "expire_time": expireTime,
        "is_permanent": isPermanent ? 1 : 0,
        "is_active": 1,
        "ip_address": ip
    });
    
    // Ban-ID abrufen
    var banResults = api.querySQL("bansystem", 
        "SELECT id FROM bans WHERE player_uuid = '" + uuid + "' AND is_active = 1 ORDER BY id DESC LIMIT 1");
    
    var banId = banResults.length > 0 ? banResults[0].id : "N/A";
    
    // Spieler kicken, falls online
    if (target) {
        var ban = {
            id: banId,
            reason: reason,
            banned_by: bannedBy,
            is_permanent: isPermanent ? 1 : 0,
            expire_time: expireTime
        };
        
        var kickMessage = formatBanMessage(ban);
        api.runTaskLater(5, function(t) {
            return function() {
                t.kickPlayer(kickMessage);
            };
        }(target));
    }
    
    // Broadcast an Team
    var durationText = isPermanent ? "Permanent" : formatDuration(duration);
    api.broadcast("&c&l[BAN] &r&7" + targetName + " &cwurde von &7" + bannedBy + " &cgebannt!", "bansystem.notify");
    api.broadcast("&7Grund: &f" + reason, "bansystem.notify");
    api.broadcast("&7Dauer: &f" + durationText, "bansystem.notify");
    api.broadcast("&7Ban-ID: &f#" + banId, "bansystem.notify");
}

// ========================================
// SPIELER ENTBANNEN
// ========================================
function unbanPlayer(targetName, unbannedBy) {
    var banResults = api.querySQL("bansystem", 
        "SELECT * FROM bans WHERE player_name = '" + targetName + "' AND is_active = 1");
    
    if (banResults.length === 0) {
        var unbanner = api.getPlayer(unbannedBy);
        if (unbanner) {
            api.sendMessage(unbanner, "&cDieser Spieler ist nicht gebannt!");
        } else {
            logger.info("Spieler " + targetName + " ist nicht gebannt (unban von " + unbannedBy + ")");
        }
        return;
    }
    
    var ban = banResults[0];
    
    // Ban deaktivieren
    api.updateData("bansystem", "bans",
        { "is_active": 0 },
        "id = " + ban.id);
    
    // Broadcast
    api.broadcast("&a&l[UNBAN] &r&7" + targetName + " &awurde von &7" + unbannedBy + " &aentbannt!", "bansystem.notify");
    api.broadcast("&7Ban-ID: &f#" + ban.id, "bansystem.notify");
    
    var unbanner = api.getPlayer(unbannedBy);
    if (unbanner) {
        api.sendMessage(unbanner, "&aSpieler erfolgreich entbannt!");
    }
}

// ========================================
// BAN-LISTE GUI
// ========================================
function openBanListGUI(sender) {
    var banResults = api.querySQL("bansystem", 
        "SELECT * FROM bans WHERE is_active = 1 ORDER BY ban_time DESC LIMIT 45");
    
    var inv = api.createInventory(null, 54, "&c&lAktive Bans");
    
    // Hintergrund
    var glass = api.createItemStack(api.getMaterial("GRAY_STAINED_GLASS_PANE"), 1);
    glass = api.setItemDisplayName(glass, " ");
    for (var i = 0; i < 54; i++) {
        api.setInventoryItem(inv, i, glass);
    }
    
    // Bans anzeigen
    for (var i = 0; i < banResults.length && i < 45; i++) {
        var ban = banResults[i];
        var item = api.createItemStack(api.getMaterial("PLAYER_HEAD"), 1);
        item = api.setItemDisplayName(item, "&c" + ban.player_name);
        
        var lore = [
            "&7",
            "&eGrund: &f" + ban.reason,
            "&eVon: &f" + ban.banned_by,
            "&eZeit: &f" + (ban.ban_time ? api.formatDate(ban.ban_time) : "N/A"),
            "&7"
        ];
        
        if (ban.is_permanent === 1) {
            lore.push("&cPermanent gebannt");
        } else {
            if (ban.expire_time && ban.expire_time > 0) {
                var remaining = ban.expire_time - api.getCurrentTimeMillis();
                lore.push("&eNoch: &f" + formatDuration(remaining));
            } else {
                lore.push("&eNoch: &fUnbekannt");
            }
        }
        
        lore.push("&7");
        lore.push("&7Ban-ID: &f#" + ban.id);
        
        item = api.setItemLore(item, lore);
        api.setInventoryItem(inv, i, item);
    }
    
    // Info
    var info = api.createItemStack(api.getMaterial("BOOK"), 1);
    info = api.setItemDisplayName(info, "&b&lBan-Statistik");
    info = api.setItemLore(info, [
        "&7",
        "&eAktive Bans: &f" + banResults.length,
        "&7",
        "&7Nutze /baninfo <Spieler>",
        "&7für mehr Informationen"
    ]);
    api.setInventoryItem(inv, 49, info);
    
    // Close
    var close = api.createItemStack(api.getMaterial("BARRIER"), 1);
    close = api.setItemDisplayName(close, "&c&lSchließen");
    api.setInventoryItem(inv, 53, close);
    
    // Click Handler
    api.registerInventoryClick(inv, function(event) {
        event.setCancelled(true);
        
        if (event.getSlot() === 53) {
            event.getWhoClicked().closeInventory();
        }
    });
    
    sender.openInventory(inv);
}

// ========================================
// BAN-LISTE CONSOLE
// ========================================
function showBanListConsole(sender) {
    var banResults = api.querySQL("bansystem", 
        "SELECT * FROM bans WHERE is_active = 1 ORDER BY ban_time DESC");
    
    api.sendMessage(sender, "&c&l=== AKTIVE BANS ===");
    api.sendMessage(sender, "");
    
    if (banResults.length === 0) {
        api.sendMessage(sender, "&7Keine aktiven Bans vorhanden.");
        return;
    }
    
    for (var i = 0; i < banResults.length; i++) {
        var ban = banResults[i];
        api.sendMessage(sender, "&c#" + ban.id + " &7- &f" + ban.player_name);
        api.sendMessage(sender, "&7  Grund: &f" + ban.reason);
        api.sendMessage(sender, "&7  Von: &f" + ban.banned_by);
        
        if (ban.is_permanent === 1) {
            api.sendMessage(sender, "&7  Dauer: &cPermanent");
        } else {
            var remaining = ban.expire_time - api.getCurrentTimeMillis();
            api.sendMessage(sender, "&7  Noch: &f" + formatDuration(remaining));
        }
        
        api.sendMessage(sender, "");
    }
}

// ========================================
// BAN-INFO ANZEIGEN
// ========================================
function showBanInfo(sender, targetName) {
    var banResults = api.querySQL("bansystem", 
        "SELECT * FROM bans WHERE player_name = '" + targetName + "' ORDER BY ban_time DESC");
    
    api.sendMessage(sender, "&c&l=== BAN-INFO: " + targetName + " ===");
    api.sendMessage(sender, "");
    
    if (banResults.length === 0) {
        api.sendMessage(sender, "&7Keine Bans gefunden für diesen Spieler.");
        return;
    }
    
    for (var i = 0; i < banResults.length; i++) {
        var ban = banResults[i];
        var status = ban.is_active === 1 ? "&c[AKTIV]" : "&a[ABGELAUFEN]";
        
        api.sendMessage(sender, status + " &7Ban-ID: &f#" + ban.id);
        api.sendMessage(sender, "&7  Grund: &f" + ban.reason);
        api.sendMessage(sender, "&7  Von: &f" + ban.banned_by);
        api.sendMessage(sender, "&7  Zeit: &f" + (ban.ban_time ? api.formatDate(ban.ban_time) : "N/A"));
        
        if (ban.is_permanent === 1) {
            api.sendMessage(sender, "&7  Typ: &cPermanent");
        } else {
            api.sendMessage(sender, "&7  Ablauf: &f" + (ban.expire_time ? api.formatDate(ban.expire_time) : "N/A"));
        }
        
        api.sendMessage(sender, "");
    }
}

// ========================================
// SPIELER WARNEN
// ========================================
function warnPlayer(targetName, warnedBy, reason) {
    var target = api.getPlayer(targetName);
    
    if (!target) {
        var warner = api.getPlayer(warnedBy);
        if (warner) {
            api.sendMessage(warner, "&cDieser Spieler ist nicht online!");
        } else {
            logger.warning("Spieler " + targetName + " ist nicht online (warn von " + warnedBy + ")");
        }
        return;
    }
    
    var uuid = target.getUniqueId().toString();
    var currentTime = api.getCurrentTimeMillis();
    
    // Warnung in Datenbank speichern
    api.insertData("bansystem", "warnings", {
        "player_name": targetName,
        "player_uuid": uuid,
        "warned_by": warnedBy,
        "reason": reason,
        "warn_time": currentTime
    });
    
    // Warnung-ID abrufen
    var warnResults = api.querySQL("bansystem", 
        "SELECT id FROM warnings WHERE player_uuid = '" + uuid + "' ORDER BY id DESC LIMIT 1");
    
    var warnId = warnResults.length > 0 ? warnResults[0].id : "N/A";
    
    // Anzahl Warnungen zählen
    var warnCount = api.countRows("bansystem", "warnings", "player_uuid = '" + uuid + "'");
    
    // Spieler informieren
    api.sendMessage(target, "&c&l╔═══════════════════════════╗");
    api.sendMessage(target, "&c&l║      DU WURDEST VERWARNT!     ║");
    api.sendMessage(target, "&c&l╚═══════════════════════════╝");
    api.sendMessage(target, "");
    api.sendMessage(target, "&7Grund: &f" + reason);
    api.sendMessage(target, "&7Von: &f" + warnedBy);
    api.sendMessage(target, "&7Warn-ID: &f#" + warnId);
    api.sendMessage(target, "");
    api.sendMessage(target, "&cDu hast nun &e" + warnCount + " &cWarnung(en)!");
    api.sendMessage(target, "&7Bei &e3 &7Warnungen erfolgt ein automatischer Ban!");
    
    // Sound abspielen
    api.playSound(target, "ENTITY_ENDER_DRAGON_GROWL", 1.0, 1.0);
    
    // Broadcast an Team
    api.broadcast("&6&l[WARN] &r&7" + targetName + " &6wurde von &7" + warnedBy + " &6verwarnt!", "bansystem.notify");
    api.broadcast("&7Grund: &f" + reason, "bansystem.notify");
    api.broadcast("&7Warnungen gesamt: &e" + warnCount, "bansystem.notify");
    
    // Auto-Ban bei 3 Warnungen
    if (warnCount >= 3) {
        var warner = api.getPlayer(warnedBy);
        if (warner) {
            api.sendMessage(warner, "&c" + targetName + " hat 3 Warnungen erreicht und wird automatisch gebannt!");
        }
        banPlayer(targetName, "System", "3 Warnungen erreicht", banDurations["7d"], false);
    }
}

// ========================================
// WARNUNGEN ANZEIGEN
// ========================================
function showWarnings(sender, targetName) {
    var warnResults = api.querySQL("bansystem", 
        "SELECT * FROM warnings WHERE player_name = '" + targetName + "' ORDER BY warn_time DESC");
    
    api.sendMessage(sender, "&6&l=== WARNUNGEN: " + targetName + " ===");
    api.sendMessage(sender, "");
    
    if (warnResults.length === 0) {
        api.sendMessage(sender, "&7Keine Warnungen gefunden für diesen Spieler.");
        return;
    }
    
    api.sendMessage(sender, "&7Gesamt: &e" + warnResults.length + " &7Warnung(en)");
    api.sendMessage(sender, "");
    
    for (var i = 0; i < warnResults.length; i++) {
        var warn = warnResults[i];
        api.sendMessage(sender, "&6#" + warn.id + " &7- &f" + (warn.warn_time ? api.formatDate(warn.warn_time) : "N/A"));
        api.sendMessage(sender, "&7  Grund: &f" + warn.reason);
        api.sendMessage(sender, "&7  Von: &f" + warn.warned_by);
        api.sendMessage(sender, "");
    }
}

// ========================================
// IP-BAN
// ========================================
function ipBanAddress(ip, bannedBy, reason) {
    try {
        api.insertData("bansystem", "ipbans", {
            "ip_address": ip,
            "banned_by": bannedBy,
            "reason": reason,
            "ban_time": api.getCurrentTimeMillis()
        });
        
        var banner = api.getPlayer(bannedBy);
        if (banner) {
            api.sendMessage(banner, "&aIP-Adresse erfolgreich gebannt!");
        }
        api.broadcast("&c&l[IP-BAN] &r&7IP &c" + ip + " &7wurde von &f" + bannedBy + " &7gebannt!", "bansystem.notify");
        api.broadcast("&7Grund: &f" + reason, "bansystem.notify");
        
        // Alle Spieler mit dieser IP kicken
        var onlinePlayers = api.getOnlinePlayers();
        var playersToKick = [];
        for (var i = 0; i < onlinePlayers.length; i++) {
            var player = onlinePlayers[i];
            var playerIp = "";
            try {
                if (player.getAddress() && player.getAddress().getAddress()) {
                    playerIp = player.getAddress().getAddress().getHostAddress();
                }
            } catch (e) {
                // IP nicht verfügbar
                continue;
            }
            
            if (playerIp === ip) {
                playersToKick.push(player);
            }
        }
        
        // Spieler kicken
        var kickMsg = api.colorize("&c&l╔═══════════════════════════╗\n") +
            api.colorize("&c&l║   DU WURDEST GEBANNT!   ║\n") +
            api.colorize("&c&l╚═══════════════════════════╝\n\n") +
            api.colorize("&7Grund: &f" + reason + "\n") +
            api.colorize("&7Typ: &cIP-Ban\n") +
            api.colorize("&7Von: &f" + bannedBy);
        
        for (var j = 0; j < playersToKick.length; j++) {
            var playerToKick = playersToKick[j];
            api.runTaskLater(5, function(p) {
                return function() {
                    p.kickPlayer(kickMsg);
                };
            }(playerToKick));
        }
        
    } catch (e) {
        var banner = api.getPlayer(bannedBy);
        if (banner) {
            api.sendMessage(banner, "&cFehler beim Bannen der IP-Adresse!");
        }
        logger.warning("IP-Ban Error: " + e);
    }
}

// ========================================
// HILFSFUNKTIONEN
// ========================================

// Ban-Nachricht formatieren
function formatBanMessage(ban) {
    var msg = api.colorize("&c&l╔═══════════════════════════╗\n");
    msg += api.colorize("&c&l║   DU WURDEST GEBANNT!   ║\n");
    msg += api.colorize("&c&l╚═══════════════════════════╝\n\n");
    msg += api.colorize("&7Grund: &f" + ban.reason + "\n");
    msg += api.colorize("&7Von: &f" + ban.banned_by + "\n");
    
    if (ban.is_permanent === 1) {
        msg += api.colorize("&7Dauer: &cPERMANENT\n");
    } else {
        if (ban.expire_time && ban.expire_time > 0) {
            var remaining = ban.expire_time - api.getCurrentTimeMillis();
            msg += api.colorize("&7Verbleibend: &f" + formatDuration(remaining) + "\n");
            msg += api.colorize("&7Ablauf: &f" + api.formatDate(ban.expire_time) + "\n");
        } else {
            msg += api.colorize("&7Dauer: &cUnbekannt\n");
        }
    }
    
    msg += api.colorize("\n&7Ban-ID: &f#" + ban.id + "\n");
    msg += api.colorize("\n&7Einspruch? &fwww.dein-server.de/unban");
    
    return msg;
}

// Dauer formatieren
function formatDuration(milliseconds) {
    if (milliseconds < 0) {
        return "Permanent";
    }
    
    var seconds = Math.floor(milliseconds / 1000);
    var minutes = Math.floor(seconds / 60);
    var hours = Math.floor(minutes / 60);
    var days = Math.floor(hours / 24);
    
    if (days > 0) {
        return days + " Tag(e), " + (hours % 24) + " Stunde(n)";
    } else if (hours > 0) {
        return hours + " Stunde(n), " + (minutes % 60) + " Minute(n)";
    } else if (minutes > 0) {
        return minutes + " Minute(n)";
    } else {
        return seconds + " Sekunde(n)";
    }
}

// ========================================
// EXPORTS
// ========================================
this.onEnable = onEnable;
this.onDisable = onDisable;
this.pluginInfo = pluginInfo;
