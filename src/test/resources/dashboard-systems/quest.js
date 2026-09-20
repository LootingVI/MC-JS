var pluginInfo = {
  "id": "quest",
  "name": "Mining quest",
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

function mcjs_fn_show_quest(player, data) {
  mcjsStep("{J^eG6X3r6Qt])3s^eOW", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
  try {
  if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, ['&eStone mined: ',mcjsRead((typeof player !== "undefined" ? player : null), "player", 'stone-mined', 0),'/10'].join('')); }
  } catch (mcjsCaught) { mcjsError("{J^eG6X3r6Qt])3s^eOW", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
  mcjsStep("5tBe=N11}Id+W*Xi8Ol!", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
  try {
  if ((typeof player !== "undefined" ? player : null)) { (function(sidebar) {
    mcjsStep("f`Yl{c/pntx$bjdH9Uic", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    sidebar.getScore(String(['Stone: ',mcjsRead((typeof player !== "undefined" ? player : null), "player", 'stone-mined', 0),'/10'].join(''))).setScore(Math.floor(3));
    } catch (mcjsCaught) { mcjsError("f`Yl{c/pntx$bjdH9Uic", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
    mcjsStep("!tWqFlpcCuZ(`oYfXF,a", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    sidebar.getScore(String('Completed: ' + String(mcjsRead((typeof player !== "undefined" ? player : null), "player", 'quest-complete', false)))).setScore(Math.floor(2));
    } catch (mcjsCaught) { mcjsError("!tWqFlpcCuZ(`oYfXF,a", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
    mcjsStep(",:!#doZr|}cY=HF=:wdI", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    sidebar.getScore(String('Quest coins: ' + String(mcjsRead((typeof player !== "undefined" ? player : null), "player", 'coins', 0)))).setScore(Math.floor(1));
    } catch (mcjsCaught) { mcjsError(",:!#doZr|}cY=HF=:wdI", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
  player.setScoreboard(sidebar.getScoreboard());
  })(api.scoreboard.createObjective(api.scoreboard.createScoreboard(), "mcjs", "dummy", String('&6Stone Miner'))); }
  } catch (mcjsCaught) { mcjsError("5tBe=N11}Id+W*Xi8Ol!", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
}

function onEnable() {
    mcjsStep("e(CaPw*C``oj%2!`eqqt", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    api.systems.registerEvent("block.BlockBreakEvent", true, function(event) {
    var player = api.systems.eventPlayer(event);
      mcjsStep("lmXlSQPraBGHtY@(CbN#", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      if (String(mcjsEventValue(mcjsEventValue(typeof event !== "undefined" ? event : null, "getBlock", null), "getType", null)) == 'STONE' && !mcjsRead((typeof player !== "undefined" ? player : null), "player", 'quest-complete', false)) {
        mcjsStep("9$$n$`ACH0N:{J#L/q8M", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
        try {
        api.systems.changeNumber(pluginInfo.id, mcjsScope((typeof player !== "undefined" ? player : null), "player"), String('stone-mined'), Number(1));
        } catch (mcjsCaught) { mcjsError("9$$n$`ACH0N:{J#L/q8M", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
        mcjsStep("EH(]Nbn2TsCcY@Mzy4Fg", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
        try {
        if (mcjsRead((typeof player !== "undefined" ? player : null), "player", 'stone-mined', 0) >= 10) {
          mcjsStep("-KeQwn}9L*bc1kw%a:NW", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
          try {
          (function(kitResult) { if (kitResult === "ok") {
            mcjsStep("cY9g-Wxk*#z:[wRI+:!P", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
            try {
            mcjsWrite((typeof player !== "undefined" ? player : null), "player", 'quest-complete', true);
            } catch (mcjsCaught) { mcjsError("cY9g-Wxk*#z:[wRI+:!P", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
            mcjsStep("|nn^Ep-#GlFTtN,a-CHg", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
            try {
            api.systems.changeNumber(pluginInfo.id, mcjsScope((typeof player !== "undefined" ? player : null), "player"), String('coins'), Number(50));
            } catch (mcjsCaught) { mcjsError("|nn^Ep-#GlFTtN,a-CHg", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
            mcjsStep("VyV2Z2o%X~Ek%6@FEWGh", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
            try {
            if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&aQuest complete! You earned a diamond and 50 quest coins.'); }
            } catch (mcjsCaught) { mcjsError("VyV2Z2o%X~Ek%6@FEWGh", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
          } else {
            mcjsStep("B63^e;YqO]4ryC9UAk85", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
            try {
            if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&eMake inventory space, then mine one more stone to claim your reward.'); }
            } catch (mcjsCaught) { mcjsError("B63^e;YqO]4ryC9UAk85", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
          } })(String(api.systems.grantKit(pluginInfo.id, (typeof player !== "undefined" ? player : null), String('quest-reward'), Number(0), [api.createItemStack(api.getMaterial("DIAMOND"), Math.max(1, Math.min(64, Math.floor(1))))])));
          } catch (mcjsCaught) { mcjsError("-KeQwn}9L*bc1kw%a:NW", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
        }
        } catch (mcjsCaught) { mcjsError("EH(]Nbn2TsCcY@Mzy4Fg", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      }
      } catch (mcjsCaught) { mcjsError("lmXlSQPraBGHtY@(CbN#", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
    });
    } catch (mcjsCaught) { mcjsError("e(CaPw*C``oj%2!`eqqt", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }


    mcjsStep("63A$KE2n.VeqKP@mN@W*", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    api.registerCommand("quest", function(sender, args) {
      var player = api.getPlayerFromSender(sender);
      if (!player) { api.sendMessage(sender, "Only available in-game."); return true; }
      mcjsStep("n|9|d9YVAGiT?XG8qG(f", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      mcjs_fn_show_quest((typeof player !== "undefined" ? player : null), null);
      } catch (mcjsCaught) { mcjsError("n|9|d9YVAGiT?XG8qG(f", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      return true;
    });
    } catch (mcjsCaught) { mcjsError("63A$KE2n.VeqKP@mN@W*", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
}

function onDisable() {

}
