var pluginInfo = {
  "id": "shop",
  "name": "Coins shop",
  "version": "1.0.0",
  "author": "Test"
};

function mcjsScope(player, scope) {
    if (scope === "global") return "global";
    if (!player) throw new Error("Player data requires a player context");
    return "player:" + String(player.getUniqueId());
}
function mcjsRead(player, scope, key, fallback) {
    return JSON.parse(String(api.systems.readData(pluginInfo.id, mcjsScope(player, scope), String(key), JSON.stringify(fallback))));
}
function mcjsWrite(player, scope, key, value) {
    api.systems.writeData(pluginInfo.id, mcjsScope(player, scope), String(key), JSON.stringify(value === undefined ? null : value));
}
function mcjsEventValue(event, getter, fallback) {
    return event && typeof event[getter] === "function" ? event[getter]() : fallback;
}
function mcjsRecordGet(record, key, fallback) {
    return record && Object.prototype.hasOwnProperty.call(record, String(key)) ? record[String(key)] : fallback;
}
function mcjsRecordSet(record, key, value) {
    if (!record || typeof record !== "object") throw new Error("A record is required");
    Object.defineProperty(record, String(key), {value: value, writable: true, configurable: true, enumerable: true});
}
function mcjsNumber(value, fallback) {
    var result = Number(value);
    return String(value).trim() !== "" && isFinite(result) ? result : fallback;
}
function mcjsLocation(world, x, y, z) {
    var found = server.getWorld(String(world));
    return found ? new org.bukkit.Location(found, Number(x), Number(y), Number(z)) : null;
}
function mcjsOffset(location, x, y, z) {
    return location ? location.clone().add(Number(x), Number(y), Number(z)) : null;
}

function mcjsStatePlayer(player, scope) {
    if (scope === "global") return null;
    if (!player) throw new Error("This state machine needs a player");
    return player;
}
function mcjsDebugJson(values) {
    function safe(value, depth) {
        if (value == null || typeof value === "boolean" || typeof value === "number") return value;
        if (typeof value === "string") return value.slice(0, 300);
        if (typeof value === "function") return "[function]";
        if (depth > 2) return "[…]";
        if (Array.isArray(value)) return value.slice(0, 10).map(function(item) { return safe(item, depth + 1); });
        if (Object.prototype.toString.call(value) === "[object Object]") {
            var result = {};
            Object.keys(value).slice(0, 20).forEach(function(key) { result[key] = safe(value[key], depth + 1); });
            return result;
        }
        return String(value).slice(0, 300);
    }
    try { return JSON.stringify(safe(values, 0)).slice(0, 8192); } catch (error) { return "{}"; }
}
var mcjsReportedError, mcjsHasReportedError = false;
function mcjsStep(id, values) {
    mcjsHasReportedError = false;
    if (api.debug && api.debug.isEnabled()) {
        try { api.debug.step(id, mcjsDebugJson(values())); } catch (ignored) {}
    }
}
function mcjsError(id, error, values) {
    if (api.debug && api.debug.isEnabled()) {
        try {
            if (mcjsHasReportedError && mcjsReportedError === error) return;
            mcjsReportedError = error;
            mcjsHasReportedError = true;
            api.debug.error(id, String(error), mcjsDebugJson(values()));
        } catch (ignored) {}
    }
}

function mcjs_fn_open_shop(player, data) {
  mcjsStep("oz:vi1x7%CEO!DDBp`aj", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
  try {
  if ((typeof player !== "undefined" ? player : null)) { (function(menu) {
    mcjsStep("MSt%%9L9gDAx{|,Mv?^w", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    menu.button(Math.floor(11), api.setItemDisplayName(api.createItemStack(api.getMaterial("DIAMOND"), Math.max(1, Math.min(64, Math.floor(1)))), "&bDiamond &7— 10 coins"), function(player, event) {
      mcjsStep("M6ybafPsnBWy1VxQl?Or", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      (function(purchaseResult) { if (purchaseResult === "ok") {
        mcjsStep("~0~+of=LU/LN@%af|fNM", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
        try {
        if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&aPurchased one diamond!'); }
        } catch (mcjsCaught) { mcjsError("~0~+of=LU/LN@%af|fNM", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      } else {
        mcjsStep("=idKkmz8LScxp=aCW1U(", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
        try {
        if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&cPurchase failed: ' + String((typeof purchaseResult !== "undefined" ? purchaseResult : ""))); }
        } catch (mcjsCaught) { mcjsError("=idKkmz8LScxp=aCW1U(", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      } }) (String(api.systems.purchase(pluginInfo.id, (typeof player !== "undefined" ? player : null), String('coins'), Number(10), api.createItemStack(api.getMaterial("DIAMOND"), Math.max(1, Math.min(64, Math.floor(1)))))));
      } catch (mcjsCaught) { mcjsError("M6ybafPsnBWy1VxQl?Or", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
    });
    } catch (mcjsCaught) { mcjsError("MSt%%9L9gDAx{|,Mv?^w", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
    mcjsStep("y6gHT$ERe{|IEjZ41L8m", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    menu.button(Math.floor(15), api.setItemDisplayName(api.createItemStack(api.getMaterial("GOLD_INGOT"), Math.max(1, Math.min(64, Math.floor(1)))), "&eYour balance"), function(player, event) {
      mcjsStep("[npl]}_,ybBN6lZ_zhi0", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&eCoins: ' + String(mcjsRead((typeof player !== "undefined" ? player : null), "player", 'coins', 0))); }
      } catch (mcjsCaught) { mcjsError("[npl]}_,ybBN6lZ_zhi0", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
    });
    } catch (mcjsCaught) { mcjsError("y6gHT$ERe{|IEjZ41L8m", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
    mcjsStep("nT4a![mn}h6]i1|p$QV~", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    menu.fill(api.setItemDisplayName(api.createItemStack(api.getMaterial("GRAY_STAINED_GLASS_PANE"), Math.max(1, Math.min(64, Math.floor(1)))), " "));
    } catch (mcjsCaught) { mcjsError("nT4a![mn}h6]i1|p$QV~", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
  menu.open(player);
  })(api.systems.createMenu(String('&6Coin Shop'), 3)); }
  } catch (mcjsCaught) { mcjsError("oz:vi1x7%CEO!DDBp`aj", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
}

function onEnable() {
    mcjsStep("MA:KD7lJ+4hE$Z!MJ{SN", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    api.registerCommand("shop", function(sender, args) {
      var player = api.getPlayerFromSender(sender);
      if (!player) { api.sendMessage(sender, "Only available in-game."); return true; }
      mcjsStep("0;!iG;?yHOvgr4W(d.%q", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      mcjs_fn_open_shop((typeof player !== "undefined" ? player : null), null);
      } catch (mcjsCaught) { mcjsError("0;!iG;?yHOvgr4W(d.%q", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      return true;
    });
    } catch (mcjsCaught) { mcjsError("MA:KD7lJ+4hE$Z!MJ{SN", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }

    mcjsStep("i98=wF)jEeFm+hJ-Nq;,", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    api.registerCommand("coins", function(sender, args) {
      var player = api.getPlayerFromSender(sender);
      if (!player) { api.sendMessage(sender, "Only available in-game."); return true; }
      mcjsStep("o$+lZSV#/PteJl6,Y!d}", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&eCoins: ' + String(mcjsRead((typeof player !== "undefined" ? player : null), "player", 'coins', 0))); }
      } catch (mcjsCaught) { mcjsError("o$+lZSV#/PteJl6,Y!d}", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      return true;
    });
    } catch (mcjsCaught) { mcjsError("i98=wF)jEeFm+hJ-Nq;,", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }

    mcjsStep("`lf][dRSIZ($^Thp)u:F", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    api.systems.registerEvent("player.PlayerJoinEvent", false, function(event) {
    var player = api.systems.eventPlayer(event);
      mcjsStep("a-q6[3}(L.7Ot*oUJb#L", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      if (mcjsRead((typeof player !== "undefined" ? player : null), "player", 'coins', -1) == -1) {
        mcjsStep("3M}[WQ:PV%G+BbPW$XI#", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
        try {
        mcjsWrite((typeof player !== "undefined" ? player : null), "player", 'coins', 100);
        } catch (mcjsCaught) { mcjsError("3M}[WQ:PV%G+BbPW$XI#", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
        mcjsStep("Q2GNYtL@@_Na`twYr!Gh", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
        try {
        if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&6Welcome! You received 100 coins. Use /shop.'); }
        } catch (mcjsCaught) { mcjsError("Q2GNYtL@@_Na`twYr!Gh", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      }
      } catch (mcjsCaught) { mcjsError("a-q6[3}(L.7Ot*oUJb#L", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
    });
    } catch (mcjsCaught) { mcjsError("`lf][dRSIZ($^Thp)u:F", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
}

function onDisable() {

}
