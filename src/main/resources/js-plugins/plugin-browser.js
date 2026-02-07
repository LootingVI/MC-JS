// MC-JS Plugin Browser - In-Game Plugin Manager
// Browse, search, and install plugins directly from your Minecraft server

var pluginInfo = {
    name: "Plugin Browser",
    version: "1.0.0",
    author: "MC-JS Team",
    description: "In-game plugin browser and installer"
};

var browserState = {
    currentPage: 0,
    pageSize: 9,
    currentPlugins: [],
    currentSearch: "",
    currentCategory: "",
    selectedPlugin: null
};

function onEnable() {
    logger.info("Plugin Browser enabled!");
    
    // Main command
    api.registerCommand("pluginbrowser", "Browse and install plugins from the browser", "/pluginbrowser [search|install|info|reload|upload]", function(sender, args) {
        var player = api.getPlayerFromSender(sender);
        if (!player) {
            api.sendMessage(sender, "&cThis command can only be used by players!");
            return true;
        }
        
        if (!sender.hasPermission("mcjs.admin")) {
            api.sendMessage(player, "&cYou don't have permission to use this command!");
            return true;
        }
        
        if (args.length === 0) {
            openBrowserGUI(player);
            return true;
        }
        
        var action = String(args[0]);
        
        if (action === "search") {
            var query = args.length > 1 ? String(args.slice(1).join(" ")) : "";
            searchPlugins(player, query, "");
            return true;
        }
        
        if (action === "install") {
            if (args.length < 2) {
                api.sendMessage(player, "&cUsage: /pluginbrowser install <id>");
                return true;
            }
            
            var pluginId = parseInt(args[1]);
            if (isNaN(pluginId)) {
                api.sendMessage(player, "&cInvalid plugin ID!");
                return true;
            }
            
            installPlugin(player, pluginId);
            return true;
        }
        
        if (action === "info") {
            if (args.length < 2) {
                api.sendMessage(player, "&cUsage: /pluginbrowser info <id>");
                return true;
            }
            
            var pluginId = parseInt(args[1]);
            if (isNaN(pluginId)) {
                api.sendMessage(player, "&cInvalid plugin ID!");
                return true;
            }
            
            showPluginInfo(player, pluginId);
            return true;
        }
        
        if (action === "reload") {
            api.sendMessage(player, "&eReloading plugin list...");
            browserState.currentPlugins = [];
            browserState.currentPage = 0;
            openBrowserGUI(player);
            return true;
        }
        
        if (action === "upload") {
            if (args.length < 2) {
                api.sendMessage(player, "&cUsage: /pluginbrowser upload <filename> [category]");
                api.sendMessage(player, "&7Example: /pluginbrowser upload myplugin.js utility");
                api.sendMessage(player, "&7Categories: utility, fun, economy, pvp, admin, other");
                api.sendMessage(player, "&7Note: Version, author, and description are read from pluginInfo in the file");
                return true;
            }
            
            var fileName = String(args[1]);
            
            // Parse category (optional)
            var category = "other";
            var validCategories = ["utility", "fun", "economy", "pvp", "admin", "other"];
            
            if (args.length > 2) {
                var categoryArg = String(args[2]).toLowerCase();
                if (validCategories.indexOf(categoryArg) !== -1) {
                    category = categoryArg;
                }
            }
            
            uploadPlugin(player, fileName, category);
            return true;
        }
        
        return false;
    });
    
    // Alias
    api.registerCommand("pb", "Plugin Browser alias", "/pb", function(sender, args) {
        return api.getPlugin("MC-JS").getServer().dispatchCommand(sender, "pluginbrowser " + args.join(" "));
    });
}

function onDisable() {
    logger.info("Plugin Browser disabled!");
}

function openBrowserGUI(player) {
    api.sendMessage(player, "&eLoading plugins...");
    
    api.runTaskAsync(function() {
        var plugins = api.searchPlugins(browserState.currentSearch, browserState.currentCategory);
        
        api.runTask(function() {
            // Convert Java List to JavaScript Array and convert each Map to JS object
            var pluginsArray = [];
            if (plugins && plugins.length !== undefined) {
                for (var i = 0; i < plugins.length; i++) {
                    var pluginMap = plugins[i];
                    // Convert Java Map to JavaScript object
                    var pluginObj = {};
                    if (pluginMap.get && typeof pluginMap.get === "function") {
                        // It's a Java Map
                        var keys = pluginMap.keySet().toArray();
                        for (var j = 0; j < keys.length; j++) {
                            var key = keys[j];
                            var value = pluginMap.get(key);
                            if (value !== null && value !== undefined) {
                                // Convert numeric values to JavaScript numbers
                                if (key === "rating" || key === "rating_count" || key === "downloads" || key === "id") {
                                    pluginObj[key] = Number(value);
                                } else if (key === "uploader_verified" || key === "uploader_blacklisted") {
                                    // Convert boolean values
                                    pluginObj[key] = value === true || value === 1 || value === "1" || value === "true";
                                } else {
                                    pluginObj[key] = value;
                                }
                            }
                        }
                    } else {
                        // Already a JavaScript object, copy properties
                        pluginObj.id = Number(pluginMap.id) || 0;
                        pluginObj.name = pluginMap.name;
                        pluginObj.version = pluginMap.version;
                        pluginObj.author = pluginMap.author;
                        pluginObj.description = pluginMap.description;
                        pluginObj.category = pluginMap.category;
                        pluginObj.downloads = Number(pluginMap.downloads) || 0;
                        pluginObj.rating = Number(pluginMap.rating) || 0;
                        pluginObj.rating_count = Number(pluginMap.rating_count) || 0;
                        pluginObj.uploader_verified = pluginMap.uploader_verified === true || pluginMap.uploader_verified === 1 || pluginMap.uploader_verified === "1" || pluginMap.uploader_verified === "true";
                        pluginObj.uploader_blacklisted = pluginMap.uploader_blacklisted === true || pluginMap.uploader_blacklisted === 1 || pluginMap.uploader_blacklisted === "1" || pluginMap.uploader_blacklisted === "true";
                        pluginObj.uploader_name = pluginMap.uploader_name || null;
                    }
                    pluginsArray.push(pluginObj);
                }
            }
            browserState.currentPlugins = pluginsArray;
            browserState.currentPage = 0;
            showBrowserGUI(player);
        });
    });
}

function showBrowserGUI(player) {
    // Use already converted plugins array
    var pluginsArray = browserState.currentPlugins || [];
    
    var startIdx = browserState.currentPage * browserState.pageSize;
    var endIdx = Math.min(startIdx + browserState.pageSize, pluginsArray.length);
    var pagePlugins = pluginsArray.slice(startIdx, endIdx);
    
    var gui = api.createGUI("&8Plugin Browser", 54);
    
    // Header
    gui.setItem(4, createItem("BOOK", 1, "&ePlugin Browser", [
        "&7Total plugins: &b" + pluginsArray.length,
        "&7Page: &b" + (browserState.currentPage + 1) + " &7/ &b" + Math.ceil(pluginsArray.length / browserState.pageSize),
        "",
        browserState.currentSearch ? "&7Search: &e" + browserState.currentSearch : "&7No search filter",
        browserState.currentCategory ? "&7Category: &e" + browserState.currentCategory : "&7All categories"
    ]));
    
    // Search button
    gui.setItem(45, createItem("COMPASS", 1, "&eSearch Plugins", [
        "&7Click to search plugins",
        "",
        "&7Current: &e" + (browserState.currentSearch || "None")
    ]), function(event) {
        event.setCancelled(true);
        var clicker = event.getWhoClicked();
        if (api.isPlayer(clicker)) {
            var player = api.getPlayerFromSender(clicker);
            player.closeInventory();
            api.sendMessage(player, "&eType your search query in chat:");
            // Note: In a real implementation, you'd need a chat input handler
            api.sendMessage(player, "&7Use &e/pluginbrowser search <query> &7instead");
        }
    });
    
    // Category filter
    gui.setItem(46, createItem("HOPPER", 1, "&eFilter by Category", [
        "&7Click to change category",
        "",
        "&7Current: &e" + (browserState.currentCategory || "All")
    ]), function(event) {
        event.setCancelled(true);
        var clicker = event.getWhoClicked();
        if (api.isPlayer(clicker)) {
            var player = api.getPlayerFromSender(clicker);
            openCategoryGUI(player);
        }
    });
    
    // Plugin slots (9-44)
    var slot = 9;
    for (var i = 0; i < pagePlugins.length && slot < 45; i++) {
        var plugin = pagePlugins[i];
        var pluginSlot = slot++;
        
        // Get verification status
        var uploaderVerified = plugin.uploader_verified || false;
        var uploaderBlacklisted = plugin.uploader_blacklisted || false;
        var uploaderName = plugin.uploader_name || "Unknown";
        
        var verificationIcon = "";
        if (uploaderBlacklisted) {
            verificationIcon = "&8[&4✖&8]"; // Blacklisted symbol
        } else if (uploaderVerified) {
            verificationIcon = "&a[&2✓&a]"; // Verified checkmark
        } else {
            verificationIcon = "&7[&8?&7]"; // Unverified/question mark
        }
        
        var lore = [
            "&7Version: &a" + plugin.version,
            "&7Author: &e" + plugin.author + " " + verificationIcon,
            "&7Category: &b" + (plugin.category || "other"),
            "",
            "&7Downloads: &b" + (plugin.downloads || 0),
            "&7Rating: &e" + (plugin.rating ? Number(plugin.rating).toFixed(1) : "0.0") + " &7(" + (plugin.rating_count || 0) + ")",
            "",
            "&eClick for details",
            "&aRight-click to install"
        ];
        
        if (plugin.description) {
            var desc = plugin.description;
            if (desc.length > 40) {
                desc = desc.substring(0, 37) + "...";
            }
            lore.splice(3, 0, "&7" + desc);
        }
        
        var material = getCategoryMaterial(plugin.category);
        // Use closure to capture plugin object
        (function(pluginObj) {
            gui.setItem(pluginSlot, createItem(material, 1, "&e" + pluginObj.name, lore), function(event) {
                event.setCancelled(true);
                var clicker = event.getWhoClicked();
                if (api.isPlayer(clicker)) {
                    var player = api.getPlayerFromSender(clicker);
                    if (event.isRightClick()) {
                        installPlugin(player, pluginObj.id);
                    } else {
                        showPluginDetailsGUI(player, pluginObj.id);
                    }
                }
            });
        })(plugin);
    }
    
    // Navigation buttons
    if (browserState.currentPage > 0) {
        gui.setItem(48, createItem("ARROW", 1, "&ePrevious Page", [
            "&7Go to page " + browserState.currentPage
        ]), function(event) {
            event.setCancelled(true);
            var clicker = event.getWhoClicked();
            if (api.isPlayer(clicker)) {
                var player = api.getPlayerFromSender(clicker);
                browserState.currentPage--;
                showBrowserGUI(player);
            }
        });
    }
    
    if (endIdx < pluginsArray.length) {
        gui.setItem(50, createItem("ARROW", 1, "&eNext Page", [
            "&7Go to page " + (browserState.currentPage + 2)
        ]), function(event) {
            event.setCancelled(true);
            var clicker = event.getWhoClicked();
            if (api.isPlayer(clicker)) {
                var player = api.getPlayerFromSender(clicker);
                browserState.currentPage++;
                showBrowserGUI(player);
            }
        });
    }
    
    // Close button
    gui.setItem(49, createItem("BARRIER", 1, "&cClose", []), function(event) {
        event.setCancelled(true);
        var clicker = event.getWhoClicked();
        if (api.isPlayer(clicker)) {
            var player = api.getPlayerFromSender(clicker);
            player.closeInventory();
        }
    });
    
    gui.open(player);
}

function openCategoryGUI(player) {
    var categories = [
        { name: "All", value: "" },
        { name: "Utility", value: "utility" },
        { name: "Fun", value: "fun" },
        { name: "Economy", value: "economy" },
        { name: "PvP", value: "pvp" },
        { name: "Admin", value: "admin" },
        { name: "Other", value: "other" }
    ];
    
    var gui = api.createGUI("&8Select Category", 27);
    
    for (var i = 0; i < categories.length; i++) {
        var cat = categories[i];
        var isSelected = browserState.currentCategory === cat.value;
        
        (function(categoryValue) {
            gui.setItem(i + 10, createItem(
                isSelected ? "LIME_DYE" : "GRAY_DYE",
                1,
                (isSelected ? "&a" : "&7") + cat.name,
                isSelected ? ["&aSelected"] : []
            ), function(event) {
                event.setCancelled(true);
                var clicker = event.getWhoClicked();
                if (api.isPlayer(clicker)) {
                    var player = api.getPlayerFromSender(clicker);
                    browserState.currentCategory = categoryValue;
                    browserState.currentPage = 0;
                    openBrowserGUI(player);
                }
            });
        })(cat.value);
    }
    
    gui.setItem(22, createItem("BARRIER", 1, "&cBack", []), function(event) {
        event.setCancelled(true);
        var clicker = event.getWhoClicked();
        if (api.isPlayer(clicker)) {
            var player = api.getPlayerFromSender(clicker);
            openBrowserGUI(player);
        }
    });
    
    gui.open(player);
}

function showPluginDetailsGUI(player, pluginId) {
    api.sendMessage(player, "&eLoading plugin details...");
    
    api.runTaskAsync(function() {
        var details = api.getPluginDetails(pluginId);
        var reviewsRaw = api.getPluginReviews(pluginId);
        
        api.runTask(function() {
            // Convert reviews array
            var reviews = [];
            if (reviewsRaw && reviewsRaw.length !== undefined) {
                for (var r = 0; r < reviewsRaw.length; r++) {
                    var reviewRaw = reviewsRaw[r];
                    var reviewObj = {};
                    if (reviewRaw.get && typeof reviewRaw.get === "function") {
                        // It's a Java Map
                        var keys = reviewRaw.keySet().toArray();
                        for (var k = 0; k < keys.length; k++) {
                            var key = keys[k];
                            var value = reviewRaw.get(key);
                            if (value !== null && value !== undefined) {
                                if (key === "rating" || key === "id") {
                                    reviewObj[key] = Number(value);
                                } else {
                                    reviewObj[key] = value;
                                }
                            }
                        }
                    } else {
                        reviewObj.rating = Number(reviewRaw.rating) || 0;
                        reviewObj.author = reviewRaw.author;
                        reviewObj.comment = reviewRaw.comment;
                        reviewObj.id = Number(reviewRaw.id) || 0;
                    }
                    reviews.push(reviewObj);
                }
            }
            
            // Convert Java Map to JavaScript object if needed
            var detailsObj = {};
            if (details.get && typeof details.get === "function") {
                // It's a Java Map
                var keys = details.keySet().toArray();
                for (var i = 0; i < keys.length; i++) {
                    var key = keys[i];
                    var value = details.get(key);
                    if (value !== null && value !== undefined) {
                        // Convert numeric values to JavaScript numbers
                        if (key === "rating" || key === "rating_count" || key === "downloads" || key === "id") {
                            detailsObj[key] = Number(value);
                        } else if (key === "uploader_verified" || key === "uploader_blacklisted") {
                            // Convert boolean values
                            detailsObj[key] = value === true || value === 1 || value === "1" || value === "true";
                        } else {
                            detailsObj[key] = value;
                        }
                    }
                }
            } else {
                detailsObj = details;
                // Ensure numeric fields are numbers
                if (detailsObj.rating !== undefined) detailsObj.rating = Number(detailsObj.rating) || 0;
                if (detailsObj.rating_count !== undefined) detailsObj.rating_count = Number(detailsObj.rating_count) || 0;
                if (detailsObj.downloads !== undefined) detailsObj.downloads = Number(detailsObj.downloads) || 0;
                if (detailsObj.id !== undefined) detailsObj.id = Number(detailsObj.id) || 0;
            }
            
            if (!detailsObj || Object.keys(detailsObj).length === 0) {
                api.sendMessage(player, "&cPlugin not found!");
                return;
            }
            
            var gui = api.createGUI("&8" + detailsObj.name, 54);
            
            // Get verification status
            var uploaderVerified = detailsObj.uploader_verified || false;
            var uploaderBlacklisted = detailsObj.uploader_blacklisted || false;
            var uploaderName = detailsObj.uploader_name || "Unknown";
            
            var verificationStatus = "";
            if (uploaderBlacklisted) {
                verificationStatus = "&8[&4✖ Blacklisted&8]";
            } else if (uploaderVerified) {
                verificationStatus = "&a[&2✓ Verified&a]";
            } else {
                verificationStatus = "&7[&8Unverified&7]";
            }
            
            // Plugin info
            var infoLore = [
                "&7Version: &a" + detailsObj.version,
                "&7Author: &e" + detailsObj.author + " " + (uploaderVerified ? "&a[&2✓&a]" : uploaderBlacklisted ? "&8[&4✖&8]" : ""),
                "&7Uploader: &b" + uploaderName + " " + verificationStatus,
                "&7Category: &b" + (detailsObj.category || "other"),
                "",
                "&7Downloads: &b" + (detailsObj.downloads || 0),
                "&7Rating: &e" + (detailsObj.rating ? Number(detailsObj.rating).toFixed(1) : "0.0") + " &7(" + (detailsObj.rating_count || 0) + " reviews)",
                "",
                "&7Description:",
                "&f" + (detailsObj.description || "No description")
            ];
            
            gui.setItem(4, createItem("BOOK", 1, "&e" + detailsObj.name, infoLore));
            
            // Install button
            (function(pid) {
                gui.setItem(49, createItem("EMERALD", 1, "&aInstall Plugin", [
                    "&7Click to install this plugin",
                    "",
                    "&7After installation, use",
                    "&e/jsreload &7to reload plugins"
                ]), function(event) {
                    event.setCancelled(true);
                    var clicker = event.getWhoClicked();
                    if (api.isPlayer(clicker)) {
                        var player = api.getPlayerFromSender(clicker);
                        player.closeInventory();
                        installPlugin(player, pid);
                    }
                });
            })(pluginId);
            
            // Reviews section
            var reviewSlot = 18;
            for (var i = 0; i < Math.min(reviews.length, 9); i++) {
                var review = reviews[i];
                var rating = Number(review.rating) || 0;
                var stars = "";
                for (var j = 0; j < rating; j++) {
                    stars += "&e★";
                }
                for (var j = rating; j < 5; j++) {
                    stars += "&7★";
                }
                
                var reviewLore = [
                    stars,
                    "",
                    "&7" + (review.comment || "No comment")
                ];
                
                gui.setItem(reviewSlot++, createItem(
                    "PAPER",
                    1,
                    "&7Review by &e" + review.author,
                    reviewLore
                ));
            }
            
            // Back button
            gui.setItem(45, createItem("ARROW", 1, "&eBack to Browser", []), function(event) {
                event.setCancelled(true);
                var clicker = event.getWhoClicked();
                if (api.isPlayer(clicker)) {
                    var player = api.getPlayerFromSender(clicker);
                    openBrowserGUI(player);
                }
            });
            
            gui.open(player);
        });
    });
}

function searchPlugins(player, query, category) {
    api.sendMessage(player, "&eSearching plugins...");
    
    api.runTaskAsync(function() {
        var plugins = api.searchPlugins(query, category);
        
        api.runTask(function() {
            // Convert Java List to JavaScript Array and convert each Map to JS object
            var pluginsArray = [];
            if (plugins && plugins.length !== undefined) {
                for (var i = 0; i < plugins.length; i++) {
                    var pluginMap = plugins[i];
                    // Convert Java Map to JavaScript object
                    var pluginObj = {};
                    if (pluginMap.get && typeof pluginMap.get === "function") {
                        // It's a Java Map
                        var keys = pluginMap.keySet().toArray();
                        for (var j = 0; j < keys.length; j++) {
                            var key = keys[j];
                            var value = pluginMap.get(key);
                            if (value !== null && value !== undefined) {
                                // Convert numeric values to JavaScript numbers
                                if (key === "rating" || key === "rating_count" || key === "downloads" || key === "id") {
                                    pluginObj[key] = Number(value);
                                } else if (key === "uploader_verified" || key === "uploader_blacklisted") {
                                    // Convert boolean values
                                    pluginObj[key] = value === true || value === 1 || value === "1" || value === "true";
                                } else {
                                    pluginObj[key] = value;
                                }
                            }
                        }
                    } else {
                        // Already a JavaScript object, copy properties
                        pluginObj.id = Number(pluginMap.id) || 0;
                        pluginObj.name = pluginMap.name;
                        pluginObj.version = pluginMap.version;
                        pluginObj.author = pluginMap.author;
                        pluginObj.description = pluginMap.description;
                        pluginObj.category = pluginMap.category;
                        pluginObj.downloads = Number(pluginMap.downloads) || 0;
                        pluginObj.rating = Number(pluginMap.rating) || 0;
                        pluginObj.rating_count = Number(pluginMap.rating_count) || 0;
                        pluginObj.uploader_verified = pluginMap.uploader_verified === true || pluginMap.uploader_verified === 1 || pluginMap.uploader_verified === "1" || pluginMap.uploader_verified === "true";
                        pluginObj.uploader_blacklisted = pluginMap.uploader_blacklisted === true || pluginMap.uploader_blacklisted === 1 || pluginMap.uploader_blacklisted === "1" || pluginMap.uploader_blacklisted === "true";
                        pluginObj.uploader_name = pluginMap.uploader_name || null;
                    }
                    pluginsArray.push(pluginObj);
                }
            }
            
            browserState.currentPlugins = pluginsArray;
            browserState.currentSearch = query;
            browserState.currentCategory = category;
            browserState.currentPage = 0;
            
            if (pluginsArray.length === 0) {
                api.sendMessage(player, "&cNo plugins found!");
                return;
            }
            
            api.sendMessage(player, "&aFound &e" + pluginsArray.length + " &aplugins!");
            api.sendMessage(player, "&7Opening browser GUI...");
            showBrowserGUI(player);
        });
    });
}

function installPlugin(player, pluginId) {
    api.sendMessage(player, "&eInstalling plugin...");
    
    api.runTaskAsync(function() {
        var success = api.installPlugin(pluginId, null);
        
        api.runTask(function() {
            if (success) {
                api.sendMessage(player, "&aPlugin installed successfully!");
                api.sendMessage(player, "&7Use &e/jsreload &7to reload plugins");
                api.playSound(player, "ENTITY_PLAYER_LEVELUP", 1.0, 1.0);
            } else {
                api.sendMessage(player, "&cFailed to install plugin!");
                api.sendMessage(player, "&7Check console for errors");
                api.playSound(player, "ENTITY_VILLAGER_NO", 1.0, 1.0);
            }
        });
    });
}

function showPluginInfo(player, pluginId) {
    api.sendMessage(player, "&eLoading plugin info...");
    
    api.runTaskAsync(function() {
        var details = api.getPluginDetails(pluginId);
        var reviews = api.getPluginReviews(pluginId);
        
        api.runTask(function() {
            if (!details || Object.keys(details).length === 0) {
                api.sendMessage(player, "&cPlugin not found!");
                return;
            }
            
            // Convert details if needed
            var detailsObj = {};
            if (details.get && typeof details.get === "function") {
                // It's a Java Map
                var keys = details.keySet().toArray();
                for (var i = 0; i < keys.length; i++) {
                    var key = keys[i];
                    var value = details.get(key);
                    if (value !== null && value !== undefined) {
                        if (key === "rating" || key === "rating_count" || key === "downloads" || key === "id") {
                            detailsObj[key] = Number(value);
                        } else if (key === "uploader_verified" || key === "uploader_blacklisted") {
                            // Convert boolean values
                            detailsObj[key] = value === true || value === 1 || value === "1" || value === "true";
                        } else {
                            detailsObj[key] = value;
                        }
                    }
                }
            } else {
                detailsObj = details;
                if (detailsObj.rating !== undefined) detailsObj.rating = Number(detailsObj.rating) || 0;
                if (detailsObj.rating_count !== undefined) detailsObj.rating_count = Number(detailsObj.rating_count) || 0;
                if (detailsObj.downloads !== undefined) detailsObj.downloads = Number(detailsObj.downloads) || 0;
                // Ensure boolean fields are booleans
                if (detailsObj.uploader_verified !== undefined) detailsObj.uploader_verified = detailsObj.uploader_verified === true || detailsObj.uploader_verified === 1 || detailsObj.uploader_verified === "1" || detailsObj.uploader_verified === "true";
                if (detailsObj.uploader_blacklisted !== undefined) detailsObj.uploader_blacklisted = detailsObj.uploader_blacklisted === true || detailsObj.uploader_blacklisted === 1 || detailsObj.uploader_blacklisted === "1" || detailsObj.uploader_blacklisted === "true";
            }
            
            // Get verification status
            var uploaderVerified = detailsObj.uploader_verified || false;
            var uploaderBlacklisted = detailsObj.uploader_blacklisted || false;
            var uploaderName = detailsObj.uploader_name || "Unknown";
            
            var verificationStatus = "";
            if (uploaderBlacklisted) {
                verificationStatus = " &8[&4✖ Blacklisted&8]";
            } else if (uploaderVerified) {
                verificationStatus = " &a[&2✓ Verified&a]";
            } else {
                verificationStatus = " &7[&8Unverified&7]";
            }
            
            api.sendMessage(player, "&e=== &6" + detailsObj.name + " &e===");
            api.sendMessage(player, "&7Version: &a" + detailsObj.version);
            api.sendMessage(player, "&7Author: &e" + detailsObj.author + (uploaderVerified ? " &a[&2✓&a]" : uploaderBlacklisted ? " &8[&4✖&8]" : ""));
            api.sendMessage(player, "&7Uploader: &b" + uploaderName + verificationStatus);
            api.sendMessage(player, "&7Category: &b" + (detailsObj.category || "other"));
            api.sendMessage(player, "&7Downloads: &b" + (detailsObj.downloads || 0));
            api.sendMessage(player, "&7Rating: &e" + (detailsObj.rating ? Number(detailsObj.rating).toFixed(1) : "0.0") + " &7(" + (detailsObj.rating_count || 0) + " reviews)");
            
            if (detailsObj.description) {
                api.sendMessage(player, "");
                api.sendMessage(player, "&7Description:");
                api.sendMessage(player, "&f" + detailsObj.description);
            }
            
            if (reviews.length > 0) {
                api.sendMessage(player, "");
                api.sendMessage(player, "&7Recent Reviews:");
                for (var i = 0; i < Math.min(reviews.length, 3); i++) {
                    var review = reviews[i];
                    var rating = Number(review.rating) || 0;
                    var stars = "";
                    for (var j = 0; j < rating; j++) {
                        stars += "&e★";
                    }
                    api.sendMessage(player, "&7- &e" + review.author + " &7" + stars + " &7" + (review.comment || ""));
                }
            }
        });
    });
}

function getCategoryMaterial(category) {
    var materials = {
        "utility": "REDSTONE",
        "fun": "FIREWORK_ROCKET",
        "economy": "GOLD_INGOT",
        "pvp": "DIAMOND_SWORD",
        "admin": "COMMAND_BLOCK",
        "other": "BOOK"
    };
    return api.getMaterial(materials[category] || "BOOK");
}

// Helper function to create an item with name and lore
function createItem(material, amount, name, lore) {
    var item = api.createItemStack(api.getMaterial(material), amount || 1);
    if (name) {
        item = api.setItemDisplayName(item, name);
    }
    if (lore && lore.length > 0) {
        item = api.setItemLore(item, lore);
    }
    return item;
}

function uploadPlugin(player, fileName, category) {
    api.sendMessage(player, "&eReading plugin file...");
    
    // Check if file exists
    if (!api.pluginFileExists(fileName)) {
        api.sendMessage(player, "&cPlugin file not found: " + fileName);
        api.sendMessage(player, "&7Make sure the file is in the js-plugins directory");
        return;
    }
    
    // Get file
    var pluginFile = api.getPluginFile(fileName);
    
    api.runTaskAsync(function() {
        // Extract metadata from plugin file
        var metadataObj = api.extractPluginMetadata(pluginFile);
        
        // Convert Java Map to JavaScript object
        var metadata = {};
        if (metadataObj) {
            // Handle Java Map/Map-like object
            if (metadataObj.get && typeof metadataObj.get === "function") {
                // It's a Java Map
                var keys = metadataObj.keySet().toArray();
                for (var i = 0; i < keys.length; i++) {
                    var key = keys[i];
                    var value = metadataObj.get(key);
                    if (value !== null && value !== undefined) {
                        metadata[key] = String(value);
                    }
                }
            } else {
                // Try direct property access
                metadata.name = metadataObj.name || null;
                metadata.version = metadataObj.version || null;
                metadata.author = metadataObj.author || null;
                metadata.description = metadataObj.description || null;
            }
        }
        
        api.runTask(function() {
            // Check if required metadata is present
            var name = metadata.name || fileName.replace(".js", "");
            var version = metadata.version;
            var author = metadata.author || player.getName();
            var description = metadata.description || "";
            
            if (!version) {
                api.sendMessage(player, "&cError: Plugin file must contain a pluginInfo object with version!");
                api.sendMessage(player, "&7Example:");
                api.sendMessage(player, "&7var pluginInfo = {");
                api.sendMessage(player, "&7    name: \"My Plugin\",");
                api.sendMessage(player, "&7    version: \"1.0.0\",");
                api.sendMessage(player, "&7    author: \"YourName\",");
                api.sendMessage(player, "&7    description: \"Description\"");
                api.sendMessage(player, "&7};");
                return;
            }
            
            // Show extracted info
            api.sendMessage(player, "&ePlugin Info:");
            api.sendMessage(player, "&7Name: &a" + name);
            api.sendMessage(player, "&7Version: &a" + version);
            api.sendMessage(player, "&7Author: &a" + author);
            if (description) {
                api.sendMessage(player, "&7Description: &f" + description);
            }
            api.sendMessage(player, "&7Category: &b" + (category || "other"));
            api.sendMessage(player, "&eUploading...");
            
            api.runTaskAsync(function() {
                var playerUUID = player.getUniqueId().toString();
                var playerName = player.getName(); // Get Minecraft player name
                var resultObj = api.uploadPlugin(pluginFile, name, version, author, description, category || "other", playerUUID, playerName);
                
                api.runTask(function() {
                    // Convert Java Map to JavaScript object
                    var result = {};
                    if (resultObj.get && typeof resultObj.get === "function") {
                        // It's a Java Map
                        var keys = resultObj.keySet().toArray();
                        for (var i = 0; i < keys.length; i++) {
                            var key = keys[i];
                            var value = resultObj.get(key);
                            if (value !== null && value !== undefined) {
                                result[key] = value;
                            }
                        }
                    } else {
                        // Already a JavaScript object, copy properties
                        result.success = resultObj.success;
                        result.message = resultObj.message;
                        result.error = resultObj.error;
                        result.pluginId = resultObj.pluginId;
                        result.issues = resultObj.issues;
                    }
                    
                    // Check success (handle both boolean and string "true")
                    var isSuccess = result.success === true || result.success === "true" || String(result.success).toLowerCase() === "true";
                    
                    if (isSuccess) {
                        api.sendMessage(player, "&aPlugin uploaded successfully!");
                        api.sendMessage(player, "&7" + (result.message || "Your plugin will be reviewed before being published"));
                        if (result.pluginId) {
                            api.sendMessage(player, "&7Plugin ID: &e" + result.pluginId);
                        }
                        api.playSound(player, "ENTITY_PLAYER_LEVELUP", 1.0, 1.0);
                    } else {
                        api.sendMessage(player, "&cUpload failed!");
                        api.sendMessage(player, "&7" + (result.error || "Unknown error"));
                        
                        if (result.issues) {
                            api.sendMessage(player, "&7Security issues found:");
                            // Convert Java List to JavaScript Array
                            var issuesArray = [];
                            if (result.issues && result.issues.length !== undefined) {
                                for (var i = 0; i < result.issues.length; i++) {
                                    issuesArray.push(result.issues[i]);
                                }
                            }
                            for (var i = 0; i < Math.min(issuesArray.length, 5); i++) {
                                var issue = issuesArray[i];
                                api.sendMessage(player, "&c- " + (issue.message || issue.pattern || "Unknown issue"));
                            }
                        }
                        
                        api.playSound(player, "ENTITY_VILLAGER_NO", 1.0, 1.0);
                    }
                });
            });
        });
    });
}
