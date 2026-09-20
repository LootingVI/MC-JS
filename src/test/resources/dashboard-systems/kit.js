var pluginInfo = {
  "id": "kit",
  "name": "Daily kit",
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

function mcjs_fn_claim_daily_kit(player, data) {
  mcjsStep("3_bvp,*Ys^K]0FL32p*6", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
  try {
  (function(kitResult) { if (kitResult === "ok") {
    mcjsStep("JvgB|ZGT]ti_f4$}3ka(", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&aYour daily kit is ready!'); }
    } catch (mcjsCaught) { mcjsError("JvgB|ZGT]ti_f4$}3ka(", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
  } else {
    mcjsStep("4nb-V5cTjhuuuU)`)fSA", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    if ((typeof kitResult !== "undefined" ? kitResult : "") == 'space') {
      mcjsStep("t)0BRRJt}SbooX#=Px;P", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&cMake room in your inventory and try again.'); }
      } catch (mcjsCaught) { mcjsError("t)0BRRJt}SbooX#=Px;P", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
    } else {
      mcjsStep("zu#qda1|k!c21dmX-frd", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, ['&eTry again in ',api.systems.cooldownRemaining(pluginInfo.id, mcjsScope((typeof player !== "undefined" ? player : null), "player"), String('daily-kit')),' seconds.'].join('')); }
      } catch (mcjsCaught) { mcjsError("zu#qda1|k!c21dmX-frd", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
    }
    } catch (mcjsCaught) { mcjsError("4nb-V5cTjhuuuU)`)fSA", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
  } })(String(api.systems.grantKit(pluginInfo.id, (typeof player !== "undefined" ? player : null), String('daily-kit'), Number(86400), [api.createItemStack(api.getMaterial("BREAD"), Math.max(1, Math.min(64, Math.floor(16)))), api.createItemStack(api.getMaterial("IRON_SWORD"), Math.max(1, Math.min(64, Math.floor(1)))), api.createItemStack(api.getMaterial("TORCH"), Math.max(1, Math.min(64, Math.floor(32))))])));
  } catch (mcjsCaught) { mcjsError("3_bvp,*Ys^K]0FL32p*6", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
}

function onEnable() {
    mcjsStep("`OpSR.;k:MHpL{9|U6mt", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    api.registerCommand("kit", function(sender, args) {
      var player = api.getPlayerFromSender(sender);
      if (!player) { api.sendMessage(sender, "Only available in-game."); return true; }
      mcjsStep("|Zi{3H4O5UNbk!tu61k=", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      mcjs_fn_claim_daily_kit((typeof player !== "undefined" ? player : null), null);
      } catch (mcjsCaught) { mcjsError("|Zi{3H4O5UNbk!tu61k=", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      return true;
    });
    } catch (mcjsCaught) { mcjsError("`OpSR.;k:MHpL{9|U6mt", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }

    mcjsStep("D#0AMRkFaeqvK+aMl!l`", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    api.registerCommand("kits", function(sender, args) {
      var player = api.getPlayerFromSender(sender);
      if (!player) { api.sendMessage(sender, "Only available in-game."); return true; }
      mcjsStep("NWE#)mlnH`[YYESa0LQY", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      if ((typeof player !== "undefined" ? player : null)) { (function(menu) {
        mcjsStep("$pAJRUE8PP$m/U!39Vg=", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
        try {
        menu.button(Math.floor(13), api.setItemDisplayName(api.createItemStack(api.getMaterial("CHEST"), Math.max(1, Math.min(64, Math.floor(1)))), "&aClaim daily kit"), function(player, event) {
          mcjsStep("(eRK1*GckXvD1eV8;b9u", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
          try {
          mcjs_fn_claim_daily_kit((typeof player !== "undefined" ? player : null), null);
          } catch (mcjsCaught) { mcjsError("(eRK1*GckXvD1eV8;b9u", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
        });
        } catch (mcjsCaught) { mcjsError("$pAJRUE8PP$m/U!39Vg=", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      menu.open(player);
      })(api.systems.createMenu(String('&6Daily rewards'), 3)); }
      } catch (mcjsCaught) { mcjsError("NWE#)mlnH`[YYESa0LQY", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      return true;
    });
    } catch (mcjsCaught) { mcjsError("D#0AMRkFaeqvK+aMl!l`", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
}

function onDisable() {

}
