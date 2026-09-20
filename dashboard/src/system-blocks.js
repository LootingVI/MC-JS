export const systemRoots = ['mjs_function', 'mjs_observe_event'];

export const systemRuntime = `function mcjsScope(player, scope) {
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
`;

export function registerSystemBlocks(Blockly, generator, Order, toolbox) {
  generator.addReservedWords('mcjsScope,mcjsRead,mcjsWrite,mcjsEventValue,mcjsRecordGet,mcjsRecordSet,mcjsNumber,mcjsLocation,mcjsOffset,data,menu,sidebar,purchaseResult');
  const input = (name, check) => ({type: 'input_value', name, ...(check ? {check} : {})});
  const field = (name, text) => ({type: 'field_input', name, text});
  const select = (name, options) => ({type: 'field_dropdown', name, options});
  const scope = select('SCOPE', [['this player', 'player'], ['the whole plugin', 'global']]);
  const body = name => ({type: 'input_statement', name});
  const row = {type: 'input_dummy'};
  const statement = {previousStatement: null, nextStatement: null};
  const define = (type, message0, args0, options = {}) => ({type, message0, args0, colour: 195, ...options});
  const definitions = [
    define('mjs_observe_event', 'After successful %1 %2 do %3', [select('EVENT', [['block break', 'block.BlockBreakEvent'], ['block place', 'block.BlockPlaceEvent'], ['player death', 'entity.PlayerDeathEvent'], ['entity death', 'entity.EntityDeathEvent'], ['player join', 'player.PlayerJoinEvent']]), row, body('DO')], {colour: 45}),
    define('mjs_current_player', 'current player', [], {output: 'Player'}),
    define('mjs_find_player', 'online player named %1', [input('NAME', 'String')], {output: 'Player'}),
    define('mjs_as_player', 'As player %1 do %2', [input('PLAYER', 'Player'), body('DO')], statement),
    define('mjs_player_id', 'UUID of current player', [], {output: 'String'}),
    define('mjs_is_online', 'player %1 is online', [input('PLAYER', 'Player')], {output: 'Boolean'}),
    define('mjs_arg_count', 'number of command arguments', [], {output: 'Number'}),
    define('mjs_to_number', 'number from %1 or %2', [input('VALUE'), input('DEFAULT', 'Number')], {output: 'Number'}),
    define('mjs_subcommand', 'If command starts with %1 %2 do %3', [field('NAME', 'help'), row, body('DO')], {...statement, colour: 45}),
    define('mjs_event_value', 'event %1', [select('GETTER', [['block', 'getBlock'], ['entity', 'getEntity'], ['attacker', 'getDamager'], ['item', 'getItem'], ['damage', 'getDamage'], ['chat message', 'getMessage'], ['new level', 'getNewLevel'], ['XP amount', 'getAmount'], ['click type', 'getClick'], ['action type', 'getAction'], ['destination', 'getTo'], ['origin', 'getFrom']])], {output: null, colour: 45}),
    define('mjs_object_property', '%1 of %2', [select('GETTER', [['material / type', 'getType'], ['name', 'getName'], ['location', 'getLocation'], ['killer', 'getKiller'], ['UUID', 'getUniqueId'], ['item amount', 'getAmount']]), input('OBJECT')], {output: null, colour: 45}),
    define('mjs_event_set', 'Set event %1 to %2', [select('SETTER', [['damage', 'setDamage'], ['XP amount', 'setAmount'], ['join message', 'setJoinMessage'], ['quit message', 'setQuitMessage']]), input('VALUE')], {...statement, colour: 45}),
    define('mjs_stop_action', 'Stop this action', [], {...statement, colour: 45}),
    define('mjs_function', 'Function %1 with player and input data %2 %3', [field('NAME', 'show_profile'), row, body('DO')], {colour: 275}),
    define('mjs_call', 'Run function %1 for %2 with data %3', [field('NAME', 'show_profile'), input('PLAYER', 'Player'), input('DATA')], {...statement, colour: 275}),
    define('mjs_call_result', 'result of function %1 for %2 with data %3', [field('NAME', 'show_profile'), input('PLAYER', 'Player'), input('DATA')], {output: null, colour: 275}),
    define('mjs_function_data', 'function input data', [], {output: null, colour: 275}),
    define('mjs_return', 'Return %1', [input('VALUE')], {previousStatement: null, colour: 275}),
    define('mjs_data_get', 'Saved data for %1 key %2 default %3', [scope, input('KEY', 'String'), input('DEFAULT')], {output: null, colour: 330}),
    define('mjs_data_set', 'Save data for %1 key %2 value %3', [scope, input('KEY', 'String'), input('VALUE')], {...statement, colour: 330}),
    define('mjs_data_change', 'Change data for %1 key %2 by %3', [scope, input('KEY', 'String'), input('AMOUNT', 'Number')], {...statement, colour: 330}),
    define('mjs_data_delete', 'Delete data for %1 key %2', [scope, input('KEY', 'String')], {...statement, colour: 330}),
    define('mjs_record', 'new empty record', [], {output: 'Record', colour: 330}),
    define('mjs_record_get', 'record %1 key %2 default %3', [input('RECORD'), input('KEY', 'String'), input('DEFAULT')], {output: null, colour: 330}),
    define('mjs_record_set', 'set record %1 key %2 to %3', [input('RECORD'), input('KEY', 'String'), input('VALUE')], {...statement, colour: 330}),
    define('mjs_cooldown', 'Use cooldown %1 for %2 seconds %3 if ready %4 otherwise %5', [input('KEY', 'String'), input('SECONDS', 'Number'), row, body('DO'), body('ELSE')], {...statement, colour: 290}),
    define('mjs_cooldown_remaining', 'seconds left on cooldown %1', [input('KEY', 'String')], {output: 'Number', colour: 290}),
    define('mjs_purchase', 'Buy item %1 for %2 of currency %3 %4 on success %5 otherwise %6', [input('ITEM', 'Item'), input('COST', 'Number'), input('KEY', 'String'), row, body('DO'), body('ELSE')], {...statement, colour: 150}),
    define('mjs_purchase_result', 'purchase result (ok / funds / space)', [], {output: 'String', colour: 150}),
    define('mjs_kit', 'Claim kit %1 every %2 seconds with items %3 %4 on success %5 otherwise %6', [input('KEY', 'String'), input('SECONDS', 'Number'), input('ITEMS', 'Array'), row, body('DO'), body('ELSE')], {...statement, colour: 150}),
    define('mjs_kit_result', 'kit result (ok / cooldown / space)', [], {output: 'String', colour: 150}),
    define('mjs_has_item', 'player has material %1 amount %2', [input('MATERIAL', 'String'), input('AMOUNT', 'Number')], {output: 'Boolean', colour: 150}),
    define('mjs_menu', 'Open menu %1 rows %2 %3 contents %4', [input('TITLE', 'String'), {type: 'field_number', name: 'ROWS', value: 3, min: 1, max: 6, precision: 1}, row, body('DO')], {...statement, colour: 230}),
    define('mjs_menu_button', 'Menu button at slot %1 item %2 %3 when clicked %4', [input('SLOT', 'Number'), input('ITEM', 'Item'), row, body('DO')], {previousStatement: 'MenuContent', nextStatement: 'MenuContent', colour: 230}),
    define('mjs_menu_fill', 'Fill empty menu slots with %1', [input('ITEM', 'Item')], {previousStatement: 'MenuContent', nextStatement: 'MenuContent', colour: 230}),
    define('mjs_menu_close_event', 'When this menu closes %1 %2', [row, body('DO')], {previousStatement: 'MenuContent', nextStatement: 'MenuContent', colour: 230}),
    define('mjs_menu_close', 'Close current menu', [], {...statement, colour: 230}),
    define('mjs_sidebar_board', 'Show sidebar titled %1 %2 lines %3', [input('TITLE', 'String'), row, body('DO')], {...statement, colour: 230}),
    define('mjs_sidebar_line', 'Sidebar line %1 score %2', [input('TEXT', 'String'), input('SCORE', 'Number')], {previousStatement: 'SidebarLine', nextStatement: 'SidebarLine', colour: 230}),
    define('mjs_player_location', 'current player location', [], {output: 'Location', colour: 210}),
    define('mjs_location', 'location in %1 X %2 Y %3 Z %4', [input('WORLD', 'String'), input('X', 'Number'), input('Y', 'Number'), input('Z', 'Number')], {output: 'Location', colour: 210}),
    define('mjs_location_offset', 'location %1 offset X %2 Y %3 Z %4', [input('LOCATION', 'Location'), input('X', 'Number'), input('Y', 'Number'), input('Z', 'Number')], {output: 'Location', colour: 210}),
    define('mjs_location_record', 'saveable record of location %1', [input('LOCATION', 'Location')], {output: 'Record', colour: 210}),
    define('mjs_record_location', 'location from saved record %1', [input('RECORD')], {output: 'Location', colour: 210}),
    define('mjs_teleport_location', 'Teleport current player to %1', [input('LOCATION', 'Location')], {...statement, colour: 210}),
    define('mjs_block_at', 'block at %1', [input('LOCATION', 'Location')], {output: 'Block', colour: 210}),
    define('mjs_set_block', 'Set block %1 to material %2', [input('BLOCK', 'Block'), input('MATERIAL', 'String')], {...statement, colour: 210}),
  ];
  for (const definition of definitions) {
    definition.tooltip = {
      mjs_function: 'Reusable action with its own player and input data. Call it from commands, events, timers or menu buttons.',
      mjs_data_get: 'Saved values survive server restarts. Player data is keyed by UUID and isolated per plugin.',
      mjs_data_set: 'Store text, numbers, booleans, lists or records. Use location records to save a home.',
      mjs_purchase: 'Checks inventory capacity and saved currency before buying. Currency is local to this plugin, not Vault.',
      mjs_menu_button: 'Slots start at 0. Actions run for the player who clicks. Items cannot be taken from this menu.',
      mjs_cooldown: 'Claims the cooldown before running actions and preserves it across restarts.',
    }[definition.type] || definition.message0.replace(/%\d+/g, '…');
  }
  definitions.find(item => item.type === 'mjs_menu').args0.at(-1).check = 'MenuContent';
  definitions.find(item => item.type === 'mjs_sidebar_board').args0.at(-1).check = 'SidebarLine';
  Blockly.defineBlocksWithJsonArray(definitions);
  const q = JSON.stringify;
  const v = (block, gen, name, fallback = 'null') => gen.valueToCode(block, name, Order.NONE) || fallback;
  const b = (block, gen, name = 'DO') => gen.statementToCode(block, name);
  const p = '(typeof player !== "undefined" ? player : null)';
  const s = block => q(block.getFieldValue('SCOPE'));
  const expression = code => [code, Order.FUNCTION_CALL];
  const g = generator.forBlock;
  g.mjs_current_player = () => [p, Order.ATOMIC];
  g.mjs_find_player = (block, gen) => expression(`api.getPlayer(String(${v(block, gen, 'NAME', '""')}))`);
  g.mjs_as_player = (block, gen) => `(function(player) { if (!player) return;\n${b(block, gen)}})(${v(block, gen, 'PLAYER')});\n`;
  g.mjs_player_id = () => [`(${p} ? String(player.getUniqueId()) : "")`, Order.ATOMIC];
  g.mjs_is_online = (block, gen) => expression(`(function(target) { return !!target && target.isOnline(); })(${v(block, gen, 'PLAYER')})`);
  g.mjs_arg_count = () => ['(typeof args !== "undefined" ? args.length : 0)', Order.ATOMIC];
  g.mjs_to_number = (block, gen) => expression(`mcjsNumber(${v(block, gen, 'VALUE')}, ${v(block, gen, 'DEFAULT', '0')})`);
  g.mjs_subcommand = (block, gen) => `if (typeof args !== "undefined" && args.length && String(args[0]).toLowerCase() === ${q(block.getFieldValue('NAME').toLowerCase())}) {\n${b(block, gen)}}\n`;
  g.mjs_event_value = block => expression(`mcjsEventValue(typeof event !== "undefined" ? event : null, ${q(block.getFieldValue('GETTER'))}, null)`);
  g.mjs_object_property = (block, gen) => {
    const getter = block.getFieldValue('GETTER');
    const code = `mcjsEventValue(${v(block, gen, 'OBJECT')}, ${q(getter)}, null)`;
    return expression(['getType', 'getName', 'getUniqueId'].includes(getter) ? `String(${code})` : code);
  };
  g.mjs_event_set = (block, gen) => `if (typeof event !== "undefined" && event && typeof event[${q(block.getFieldValue('SETTER'))}] === "function") event[${q(block.getFieldValue('SETTER'))}](${v(block, gen, 'VALUE')});\n`;
  g.mjs_stop_action = () => 'return;\n';
  g.mjs_function = (block, gen) => {
    const name = block.getFieldValue('NAME');
    gen.definitions_[`system_function_${name}`] = `function mcjs_fn_${name}(player, data) {\n${b(block, gen)}}`;
    return '';
  };
  const call = (block, gen) => `mcjs_fn_${block.getFieldValue('NAME')}(${v(block, gen, 'PLAYER', p)}, ${v(block, gen, 'DATA')})`;
  g.mjs_call = (block, gen) => call(block, gen) + ';\n';
  g.mjs_call_result = (block, gen) => expression(call(block, gen));
  g.mjs_function_data = () => ['(typeof data !== "undefined" ? data : null)', Order.ATOMIC];
  g.mjs_return = (block, gen) => `return ${v(block, gen, 'VALUE')};\n`;
  g.mjs_data_get = (block, gen) => expression(`mcjsRead(${p}, ${s(block)}, ${v(block, gen, 'KEY', '"points"')}, ${v(block, gen, 'DEFAULT', '0')})`);
  g.mjs_data_set = (block, gen) => `mcjsWrite(${p}, ${s(block)}, ${v(block, gen, 'KEY', '"points"')}, ${v(block, gen, 'VALUE')});\n`;
  g.mjs_data_change = (block, gen) => `api.systems.changeNumber(pluginInfo.id, mcjsScope(${p}, ${s(block)}), String(${v(block, gen, 'KEY', '"points"')}), Number(${v(block, gen, 'AMOUNT', '1')}));\n`;
  g.mjs_data_delete = (block, gen) => `api.systems.deleteData(pluginInfo.id, mcjsScope(${p}, ${s(block)}), String(${v(block, gen, 'KEY', '"points"')}));\n`;
  g.mjs_record = () => expression('Object.create(null)');
  g.mjs_record_get = (block, gen) => expression(`mcjsRecordGet(${v(block, gen, 'RECORD')}, ${v(block, gen, 'KEY', '"key"')}, ${v(block, gen, 'DEFAULT')})`);
  g.mjs_record_set = (block, gen) => `mcjsRecordSet(${v(block, gen, 'RECORD')}, ${v(block, gen, 'KEY', '"key"')}, ${v(block, gen, 'VALUE')});\n`;
  g.mjs_cooldown = (block, gen) => `if (api.systems.claimCooldown(pluginInfo.id, mcjsScope(${p}, "player"), String(${v(block, gen, 'KEY', '"kit"')}), Number(${v(block, gen, 'SECONDS', '60')}))) {\n${b(block, gen)}} else {\n${b(block, gen, 'ELSE')}}\n`;
  g.mjs_cooldown_remaining = (block, gen) => expression(`api.systems.cooldownRemaining(pluginInfo.id, mcjsScope(${p}, "player"), String(${v(block, gen, 'KEY', '"kit"')}))`);
  g.mjs_purchase = (block, gen) => `(function(purchaseResult) { if (purchaseResult === "ok") {\n${b(block, gen)}} else {\n${b(block, gen, 'ELSE')}} }) (String(api.systems.purchase(pluginInfo.id, ${p}, String(${v(block, gen, 'KEY', '"coins"')}), Number(${v(block, gen, 'COST', '10')}), ${v(block, gen, 'ITEM')})));\n`;
  g.mjs_purchase_result = () => ['(typeof purchaseResult !== "undefined" ? purchaseResult : "")', Order.ATOMIC];
  generator.addReservedWords('kitResult');
  g.mjs_kit = (block, gen) => `(function(kitResult) { if (kitResult === "ok") {\n${b(block, gen)}} else {\n${b(block, gen, 'ELSE')}} })(String(api.systems.grantKit(pluginInfo.id, ${p}, String(${v(block, gen, 'KEY', '"daily"')}), Number(${v(block, gen, 'SECONDS', '86400')}), ${v(block, gen, 'ITEMS', '[]')})));\n`;
  g.mjs_kit_result = () => ['(typeof kitResult !== "undefined" ? kitResult : "")', Order.ATOMIC];
  g.mjs_has_item = (block, gen) => [`(${p} && player.getInventory().contains(api.getMaterial(String(${v(block, gen, 'MATERIAL', '"STONE"')})), Math.max(1, Math.floor(${v(block, gen, 'AMOUNT', '1')}))))`, Order.ATOMIC];
  g.mjs_menu = (block, gen) => `if (${p}) { (function(menu) {\n${b(block, gen)}menu.open(player);\n})(api.systems.createMenu(String(${v(block, gen, 'TITLE', '"Menu"')}), ${block.getFieldValue('ROWS')})); }\n`;
  g.mjs_menu_button = (block, gen) => `menu.button(Math.floor(${v(block, gen, 'SLOT', '0')}), ${v(block, gen, 'ITEM')}, function(player, event) {\n${b(block, gen)}});\n`;
  g.mjs_menu_fill = (block, gen) => `menu.fill(${v(block, gen, 'ITEM')});\n`;
  g.mjs_menu_close_event = (block, gen) => `menu.onClose(function(player, event) {\n${b(block, gen)}});\n`;
  g.mjs_menu_close = () => `if (${p}) player.closeInventory();\n`;
  g.mjs_sidebar_board = (block, gen) => `if (${p}) { (function(sidebar) {\n${b(block, gen)}player.setScoreboard(sidebar.getScoreboard());\n})(api.scoreboard.createObjective(api.scoreboard.createScoreboard(), "mcjs", "dummy", String(${v(block, gen, 'TITLE', '"Stats"')}))); }\n`;
  g.mjs_sidebar_line = (block, gen) => `sidebar.getScore(String(${v(block, gen, 'TEXT', '"Line"')})).setScore(Math.floor(${v(block, gen, 'SCORE', '1')}));\n`;
  g.mjs_player_location = () => [`(${p} ? player.getLocation() : null)`, Order.ATOMIC];
  g.mjs_location = (block, gen) => expression(`mcjsLocation(${v(block, gen, 'WORLD', '"world"')}, ${v(block, gen, 'X', '0')}, ${v(block, gen, 'Y', '64')}, ${v(block, gen, 'Z', '0')})`);
  g.mjs_location_offset = (block, gen) => expression(`mcjsOffset(${v(block, gen, 'LOCATION')}, ${v(block, gen, 'X', '0')}, ${v(block, gen, 'Y', '0')}, ${v(block, gen, 'Z', '0')})`);
  g.mjs_location_record = (block, gen) => expression(`JSON.parse(String(api.systems.locationToJson(${v(block, gen, 'LOCATION')})))`);
  g.mjs_record_location = (block, gen) => expression(`api.systems.locationFromJson(JSON.stringify(${v(block, gen, 'RECORD')}))`);
  g.mjs_teleport_location = (block, gen) => `(function(destination) { if (${p} && destination) player.teleport(destination); })(${v(block, gen, 'LOCATION')});\n`;
  g.mjs_block_at = (block, gen) => expression(`(function(location) { return location ? location.getBlock() : null; })(${v(block, gen, 'LOCATION')})`);
  g.mjs_set_block = (block, gen) => `(function(target) { if (target) target.setType(api.getMaterial(String(${v(block, gen, 'MATERIAL', '"STONE"')}))); })(${v(block, gen, 'BLOCK')});\n`;
  g.mjs_player_count = () => expression('api.getOnlinePlayers().size()');
  g.mjs_event = (block, gen) => {
    const event = block.getFieldValue('EVENT');
    const code = `var player = api.systems.eventPlayer(event);\n${b(block, gen)}`;
    return `api.systems.registerEvent(${q(event)}, ${block.type === 'mjs_observe_event'}, function(event) {\n`
      + (event.includes('Async') ? `api.runTask(function() {\n${code}});\n` : code) + '});\n';
  };
  g.mjs_observe_event = g.mjs_event;
  const shadow = (type, fields) => ({shadow: {type, fields}});
  const t = text => shadow('text', {TEXT: text});
  const n = number => shadow('math_number', {NUM: number});
  const current = () => ({block: {type: 'mjs_current_player'}});
  const defaults = {
    mjs_find_player: {NAME: t('Alex')}, mjs_as_player: {PLAYER: current()}, mjs_is_online: {PLAYER: current()},
    mjs_to_number: {VALUE: t('10'), DEFAULT: n(0)}, mjs_call: {PLAYER: current()}, mjs_call_result: {PLAYER: current()},
    mjs_data_get: {KEY: t('coins'), DEFAULT: n(0)}, mjs_data_set: {KEY: t('coins'), VALUE: n(0)},
    mjs_data_change: {KEY: t('coins'), AMOUNT: n(1)}, mjs_data_delete: {KEY: t('coins')},
    mjs_record_get: {KEY: t('key'), DEFAULT: t('')}, mjs_record_set: {KEY: t('key'), VALUE: t('value')},
    mjs_cooldown: {KEY: t('daily-kit'), SECONDS: n(86400)}, mjs_cooldown_remaining: {KEY: t('daily-kit')},
    mjs_kit: {KEY: t('daily-kit'), SECONDS: n(86400), ITEMS: {block: {type: 'lists_create_with', extraState: {itemCount: 1}, inputs: {ADD0: {block: {type: 'mjs_item_stack', fields: {MATERIAL: 'BREAD'}, inputs: {AMOUNT: n(16)}}}}}}},
    mjs_purchase: {KEY: t('coins'), COST: n(10), ITEM: {block: {type: 'mjs_item_stack', fields: {MATERIAL: 'DIAMOND'}, inputs: {AMOUNT: n(1)}}}},
    mjs_has_item: {MATERIAL: t('DIAMOND'), AMOUNT: n(1)}, mjs_menu: {TITLE: t('&6Server menu')},
    mjs_menu_button: {SLOT: n(13), ITEM: {block: {type: 'mjs_item_stack', fields: {MATERIAL: 'DIAMOND'}, inputs: {AMOUNT: n(1)}}}},
    mjs_menu_fill: {ITEM: {block: {type: 'mjs_item_stack', fields: {MATERIAL: 'GRAY_STAINED_GLASS_PANE'}, inputs: {AMOUNT: n(1)}}}},
    mjs_sidebar_board: {TITLE: t('&6My server')}, mjs_sidebar_line: {TEXT: t('Coins'), SCORE: n(1)},
    mjs_location: {WORLD: t('world'), X: n(0), Y: n(64), Z: n(0)}, mjs_location_offset: {X: n(0), Y: n(1), Z: n(0)},
    mjs_set_block: {MATERIAL: t('STONE')},
  };
  const category = (name, colour, types) => ({kind: 'category', name, colour, contents: types.map(type => ({kind: 'block', type, ...(defaults[type] ? {inputs: defaults[type]} : {})}))});
  toolbox.contents.splice(toolbox.contents.length - 1, 0,
    category('Player contexts', '#55b5e9', ['mjs_current_player', 'mjs_find_player', 'mjs_as_player', 'mjs_player_id', 'mjs_is_online', 'mjs_arg_count', 'mjs_to_number', 'mjs_subcommand']),
    category('Event data', '#e1aa38', ['mjs_observe_event', 'mjs_event_value', 'mjs_object_property', 'mjs_event_set', 'mjs_stop_action']),
    category('Reusable actions', '#af81e8', ['mjs_function', 'mjs_call', 'mjs_call_result', 'mjs_function_data', 'mjs_return']),
    category('Persistent data', '#c875a4', ['mjs_data_get', 'mjs_data_set', 'mjs_data_change', 'mjs_data_delete', 'mjs_record', 'mjs_record_get', 'mjs_record_set']),
    category('Cooldowns & shop', '#4ebf9a', ['mjs_cooldown', 'mjs_cooldown_remaining', 'mjs_purchase', 'mjs_purchase_result', 'mjs_kit', 'mjs_kit_result', 'mjs_has_item']),
    category('Menu builder', '#7186d0', ['mjs_menu', 'mjs_menu_button', 'mjs_menu_fill', 'mjs_menu_close_event', 'mjs_menu_close']),
    category('Sidebar builder', '#7186d0', ['mjs_sidebar_board', 'mjs_sidebar_line']),
    category('Locations', '#55b5e9', ['mjs_player_location', 'mjs_location', 'mjs_location_offset', 'mjs_location_record', 'mjs_record_location', 'mjs_teleport_location', 'mjs_block_at', 'mjs_set_block']));
}

export function diagnose(workspace) {
  const problems = [];
  const functions = new Set();
  const commands = new Set();
  const blocks = workspace.getAllBlocks(false).filter(block => block.isEnabled() && !block.getInheritedDisabled());
  const error = (block, message) => problems.push({id: block.id, message});
  const rootTypes = ['mjs_enable', 'mjs_disable', 'mjs_event', 'mjs_command', 'mjs_timer', 'mjs_machine', 'procedures_defnoreturn', 'procedures_defreturn', ...systemRoots];
  for (const block of workspace.getTopBlocks(false)) {
    if (block.isEnabled() && !rootTypes.includes(block.type)) error(block, 'Connect free blocks to an event, command, function or startup block.');
  }
  const ancestor = (block, types) => {
    for (let parent = block.getSurroundParent(); parent; parent = parent.getSurroundParent()) if (types.includes(parent.type)) return parent;
    return null;
  };
  for (const block of blocks.filter(block => block.type === 'mjs_function')) {
    const name = block.getFieldValue('NAME');
    if (!/^[a-zA-Z][a-zA-Z0-9_]{0,47}$/.test(name)) error(block, 'Action names require 1–48 letters, numbers or underscores and must start with a letter.');
    else if (functions.has(name)) error(block, `Duplicate reusable action: ${name}.`);
    else functions.add(name);
  }
  for (const block of blocks) {
    if (block.type === 'mjs_command') {
      const name = block.getFieldValue('NAME');
      if (!/^[a-z][a-z0-9_-]{0,31}$/.test(name) || ['mjs', 'jsreload', 'jslist', 'jsconfig'].includes(name)) error(block, 'Command names require lowercase letters, numbers, _ or -. MC-JS commands are reserved.');
      else if (commands.has(name)) error(block, `Duplicate command: /${name}. Use subcommand blocks in one command instead.`);
      commands.add(name);
    }
    if (['mjs_call', 'mjs_call_result'].includes(block.type) && !functions.has(block.getFieldValue('NAME'))) error(block, `Unknown reusable action: ${block.getFieldValue('NAME')}. Create a matching Function block.`);
    if (block.type === 'mjs_function_data' && !ancestor(block, ['mjs_function', 'mjs_state', 'mjs_transition'])) error(block, 'This block belongs inside a reusable Function or state action.');
    if (block.type === 'mjs_return' && !ancestor(block, ['mjs_function'])) error(block, 'This block belongs inside a reusable Function.');
    if (['mjs_menu_button', 'mjs_menu_fill', 'mjs_menu_close_event'].includes(block.type) && !ancestor(block, ['mjs_menu'])) error(block, 'Put this block in the contents of an Open menu block.');
    if (block.type === 'mjs_sidebar_line' && !ancestor(block, ['mjs_sidebar_board'])) error(block, 'Put sidebar lines inside a Show sidebar block.');
    if (block.type === 'mjs_purchase_result' && !ancestor(block, ['mjs_purchase'])) error(block, 'Purchase result belongs inside a Buy item block.');
    if (block.type === 'mjs_kit_result' && !ancestor(block, ['mjs_kit'])) error(block, 'Kit result belongs inside a Claim kit block.');
    const event = ancestor(block, ['mjs_event', 'mjs_observe_event']);
    if (event?.type === 'mjs_observe_event' && ['mjs_cancel', 'mjs_event_set'].includes(block.type)) error(block, 'Observation events run after other plugins. Use a regular event to cancel or modify an event.');
    if (event?.getFieldValue('EVENT')?.includes('Async') && ['mjs_cancel', 'mjs_event_set'].includes(block.type)) error(block, 'Asynchronous events run actions on the next server tick. They cannot be cancelled or modified here.');
    const contextRoot = ancestor(block, ['mjs_command', 'mjs_event', 'mjs_observe_event', 'mjs_function', 'mjs_state', 'mjs_transition', 'mjs_as_player', 'mjs_each_player', 'mjs_menu_button', 'mjs_menu_close_event']);
    if ((['mjs_cooldown', 'mjs_cooldown_remaining', 'mjs_purchase', 'mjs_kit'].includes(block.type)
        || block.type.startsWith('mjs_data_') && block.getFieldValue('SCOPE') === 'player') && !contextRoot) error(block, 'Player data needs a player context. Use a command, player event, reusable action or For each online player block.');
  }
  return problems;
}
