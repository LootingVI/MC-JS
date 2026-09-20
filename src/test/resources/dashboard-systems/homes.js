var pluginInfo = {
  "id": "homes",
  "name": "Player homes",
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

function mcjs_fn_go_home(player, data) {
  mcjsStep("%E?hz;m~wyTzi!N0wn@,", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
  try {
  if (api.systems.locationFromJson(JSON.stringify(mcjsRead((typeof player !== "undefined" ? player : null), "player", 'home', null))) != null) {
    mcjsStep("}l4z/q^.~jY`*iaa;hSI", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    if (api.systems.claimCooldown(pluginInfo.id, mcjsScope((typeof player !== "undefined" ? player : null), "player"), String('home'), Number(10))) {
      mcjsStep("bKf,0/.8V{_?[S?_:DM/", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      (function(destination) { if ((typeof player !== "undefined" ? player : null) && destination) player.teleport(destination); })(api.systems.locationFromJson(JSON.stringify(mcjsRead((typeof player !== "undefined" ? player : null), "player", 'home', null))));
      } catch (mcjsCaught) { mcjsError("bKf,0/.8V{_?[S?_:DM/", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      mcjsStep("=|j5Bh-W`F[jW;i8[W$L", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&aWelcome home.'); }
      } catch (mcjsCaught) { mcjsError("=|j5Bh-W`F[jW;i8[W$L", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
    } else {
      mcjsStep("I^[q32Lf9e%o6!}wBgaD", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&ePlease wait before teleporting again.'); }
      } catch (mcjsCaught) { mcjsError("I^[q32Lf9e%o6!}wBgaD", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
    }
    } catch (mcjsCaught) { mcjsError("}l4z/q^.~jY`*iaa;hSI", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
  } else {
    mcjsStep("N}eCNb|i|u)O7sY?@4-M", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&cNo available home. Use /sethome first.'); }
    } catch (mcjsCaught) { mcjsError("N}eCNb|i|u)O7sY?@4-M", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
  }
  } catch (mcjsCaught) { mcjsError("%E?hz;m~wyTzi!N0wn@,", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
}

function onEnable() {
    mcjsStep("GrV*{4~BHFdVL8M03~@J", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    api.registerCommand("sethome", function(sender, args) {
      var player = api.getPlayerFromSender(sender);
      if (!player) { api.sendMessage(sender, "Only available in-game."); return true; }
      mcjsStep("hoBh7Sgc9ZW,yT28R%af", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      mcjsWrite((typeof player !== "undefined" ? player : null), "player", 'home', JSON.parse(String(api.systems.locationToJson(((typeof player !== "undefined" ? player : null) ? player.getLocation() : null)))));
      } catch (mcjsCaught) { mcjsError("hoBh7Sgc9ZW,yT28R%af", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      mcjsStep("5eZv`i))X`;[o9F`6U@H", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&aHome saved. Use /home to return.'); }
      } catch (mcjsCaught) { mcjsError("5eZv`i))X`;[o9F`6U@H", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      return true;
    });
    } catch (mcjsCaught) { mcjsError("GrV*{4~BHFdVL8M03~@J", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }


    mcjsStep("rxTX%ezT%._?~X7/.$1d", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    api.registerCommand("home", function(sender, args) {
      var player = api.getPlayerFromSender(sender);
      if (!player) { api.sendMessage(sender, "Only available in-game."); return true; }
      mcjsStep("k8e{V[Il(HA%D.m7+`-^", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      mcjs_fn_go_home((typeof player !== "undefined" ? player : null), null);
      } catch (mcjsCaught) { mcjsError("k8e{V[Il(HA%D.m7+`-^", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      return true;
    });
    } catch (mcjsCaught) { mcjsError("rxTX%ezT%._?~X7/.$1d", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
}

function onDisable() {

}
