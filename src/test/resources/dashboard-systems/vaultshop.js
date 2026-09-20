var pluginInfo = {
  "id": "vaultshop",
  "name": "Vault economy shop",
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

function mcjs_fn_open_vault_shop(player, data) {
  mcjsStep(".|r8ik}]F1Ox{VaDAaq`", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
  try {
  if (api.vault.isAvailable()) {
    mcjsStep(",XND?!]C+Tcy.DZvSP*|", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    if ((typeof player !== "undefined" ? player : null)) { (function(menu) {
      mcjsStep("0_cCxC*wt0rQRD$;kD_w", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      menu.button(Math.floor(11), api.setItemDisplayName(api.createItemStack(api.getMaterial("DIAMOND"), Math.max(1, Math.min(64, Math.floor(1)))), "&bBuy a diamond"), function(player, event) {
        mcjsStep("ki?{k.$ol5c.-3._dc=p", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
        try {
        (function(vaultResult) { if (vaultResult.isSuccess()) {
          mcjsStep("$q$(cs*,|=gc}lqgQngu", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
          try {
          if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&aPurchased one diamond!'); }
          } catch (mcjsCaught) { mcjsError("$q$(cs*,|=gc}lqgQngu", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
        } else {
          mcjsStep("LsH``KNiII06}E!NQC6s", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
          try {
          if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&cPurchase failed: ' + String((typeof vaultResult !== "undefined" ? String(vaultResult.getError()) : null))); }
          } catch (mcjsCaught) { mcjsError("LsH``KNiII06}E!NQC6s", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
        } })(api.vault.purchase((typeof player !== "undefined" ? player : null), Number(10), api.createItemStack(api.getMaterial("DIAMOND"), Math.max(1, Math.min(64, Math.floor(1))))));
        } catch (mcjsCaught) { mcjsError("ki?{k.$ol5c.-3._dc=p", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      });
      } catch (mcjsCaught) { mcjsError("0_cCxC*wt0rQRD$;kD_w", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      mcjsStep("hiUOh1DGm6VV8Hhe~~EY", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      menu.button(Math.floor(15), api.setItemDisplayName(api.createItemStack(api.getMaterial("GOLD_INGOT"), Math.max(1, Math.min(64, Math.floor(1)))), "&eYour balance"), function(player, event) {
        mcjsStep("KH*vF{BmMjkTpwX_E8*,", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
        try {
        if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&eBalance: ' + String(String(api.vault.format(Number(api.vault.balance((typeof player !== "undefined" ? player : null))))))); }
        } catch (mcjsCaught) { mcjsError("KH*vF{BmMjkTpwX_E8*,", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      });
      } catch (mcjsCaught) { mcjsError("hiUOh1DGm6VV8Hhe~~EY", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      mcjsStep(",EhI`DOXZhHiUh|dg`MO", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      menu.fill(api.setItemDisplayName(api.createItemStack(api.getMaterial("GRAY_STAINED_GLASS_PANE"), Math.max(1, Math.min(64, Math.floor(1)))), " "));
      } catch (mcjsCaught) { mcjsError(",EhI`DOXZhHiUh|dg`MO", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
    menu.open(player);
    })(api.systems.createMenu(String('&6Economy Shop'), 3)); }
    } catch (mcjsCaught) { mcjsError(",XND?!]C+Tcy.DZvSP*|", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
  } else {
    mcjsStep("WlYf=)agk3U?keGsp!K#", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&cVault economy is unavailable. Ask an administrator to install Vault and an economy provider.'); }
    } catch (mcjsCaught) { mcjsError("WlYf=)agk3U?keGsp!K#", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
  }
  } catch (mcjsCaught) { mcjsError(".|r8ik}]F1Ox{VaDAaq`", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
}

function onEnable() {
    mcjsStep("b0FqWlWJ~l(g-;q-YA;p", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    api.registerCommand("vshop", function(sender, args) {
      var player = api.getPlayerFromSender(sender);
      if (!player) { api.sendMessage(sender, "Only available in-game."); return true; }
      mcjsStep("(rf/3Yg?WyU]-h5F5i@i", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      mcjs_fn_open_vault_shop((typeof player !== "undefined" ? player : null), null);
      } catch (mcjsCaught) { mcjsError("(rf/3Yg?WyU]-h5F5i@i", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      return true;
    });
    } catch (mcjsCaught) { mcjsError("b0FqWlWJ~l(g-;q-YA;p", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }

    mcjsStep("r?qxq=m3dg:JB~Y)6_Vj", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
    try {
    api.registerCommand("money", function(sender, args) {
      var player = api.getPlayerFromSender(sender);
      if (!player) { api.sendMessage(sender, "Only available in-game."); return true; }
      mcjsStep("+uIMJ@ihm}Za@`O%d0zw", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
      try {
      if (api.vault.isAvailable()) {
        mcjsStep("riEWoSMqp-YQ1#0cLpYW", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
        try {
        if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&eBalance: ' + String(String(api.vault.format(Number(api.vault.balance((typeof player !== "undefined" ? player : null))))))); }
        } catch (mcjsCaught) { mcjsError("riEWoSMqp-YQ1#0cLpYW", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      } else {
        mcjsStep("Af_j/DEzS[LkmFj#p5F}", function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; });
        try {
        if (typeof player !== "undefined" && player !== null) { api.sendMessage(player, '&cVault economy is unavailable. Ask an administrator to install Vault and an economy provider.'); }
        } catch (mcjsCaught) { mcjsError("Af_j/DEzS[LkmFj#p5F}", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      }
      } catch (mcjsCaught) { mcjsError("+uIMJ@ihm}Za@`O%d0zw", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
      return true;
    });
    } catch (mcjsCaught) { mcjsError("r?qxq=m3dg:JB~Y)6_Vj", mcjsCaught, function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {}}; }); throw mcjsCaught; }
}

function onDisable() {

}
