import * as BlocklyModule from 'blockly/core';
import 'blockly/blocks';
import * as English from 'blockly/msg/en';
import {javascriptGenerator as generator, Order} from 'blockly/javascript';
import {registerSystemBlocks, diagnose as diagnoseSystems, systemRoots, systemRuntime} from './system-blocks.js';
import {registerStudioBlocks, diagnoseStudio, studioRoots, studioRuntime, instrumentBlocks} from './studio-blocks.js';
import {systemTemplate} from './system-templates.js';

const unwrap = module => module.default || module;
const Blockly = unwrap(BlocklyModule);
Blockly.setLocale(unwrap(English));
generator.addReservedWords('api,server,plugin,logger,player,sender,args,event,pluginInfo,onEnable,onDisable,gui,obj,sb,destinationWorld,world');
const text = (name, value) => ({type: 'field_input', name, text: value});
const value = (name, check) => ({type: 'input_value', name, ...(check ? {check} : {})});
const body = {type: 'input_statement', name: 'DO'};
const statement = {previousStatement: null, nextStatement: null};
const dropdown = (name, options) => ({type: 'field_dropdown', name, options});
const definitions = [
  {type: 'mjs_enable', message0: 'When the plugin starts %1 %2', args0: [{type: 'input_dummy'}, body], colour: 45},
  {type: 'mjs_disable', message0: 'When the plugin stops %1 %2', args0: [{type: 'input_dummy'}, body], colour: 45},
  {type: 'mjs_event', message0: 'When %1 %2 %3', args0: [dropdown('EVENT', [
    ['someone joins', 'player.PlayerJoinEvent'], ['someone quits', 'player.PlayerQuitEvent'],
    ['someone respawns', 'player.PlayerRespawnEvent'], ['someone chats', 'player.AsyncPlayerChatEvent'],
    ['someone moves', 'player.PlayerMoveEvent'], ['someone interacts', 'player.PlayerInteractEvent'],
    ['someone drops an item', 'player.PlayerDropItemEvent'], ['someone empties a bucket', 'player.PlayerBucketEmptyEvent'],
    ['someone fills a bucket', 'player.PlayerBucketFillEvent'], ['someone toggles sneaking', 'player.PlayerToggleSneakEvent'],
    ['someone toggles sprinting', 'player.PlayerToggleSprintEvent'], ['someone changes level', 'player.PlayerLevelChangeEvent'],
    ['someone gains XP', 'player.PlayerExpChangeEvent'], ['someone swaps hands', 'player.PlayerSwapHandItemsEvent'],
    ['a block is broken', 'block.BlockBreakEvent'], ['a block is placed', 'block.BlockPlaceEvent'],
    ['a block ignites', 'block.BlockIgniteEvent'], ['someone dies', 'entity.PlayerDeathEvent'],
    ['an entity is damaged', 'entity.EntityDamageEvent'], ['an entity dies', 'entity.EntityDeathEvent'],
    ['someone clicks an inventory', 'inventory.InventoryClickEvent'], ['an inventory closes', 'inventory.InventoryCloseEvent']]), {type: 'input_dummy'}, body], colour: 45},
  {type: 'mjs_command', message0: 'Command / %1 Permission %2 %3 %4', args0: [text('NAME', 'hello'), text('PERMISSION', ''), {type: 'input_dummy'}, body], colour: 45},
  {type: 'mjs_message', message0: 'Send player %1', args0: [value('TEXT')], colour: 160, ...statement},
  {type: 'mjs_broadcast', message0: 'Broadcast %1', args0: [value('TEXT')], colour: 160, ...statement},
  {type: 'mjs_title', message0: 'Show title %1 subtitle %2', args0: [value('TITLE'), value('SUBTITLE')], colour: 160, ...statement},
  {type: 'mjs_actionbar', message0: 'Show action bar %1', args0: [value('TEXT')], colour: 160, ...statement},
  {type: 'mjs_log', message0: 'Log to console %1', args0: [value('TEXT')], colour: 160, ...statement},
  {type: 'mjs_name', message0: 'Player name', output: 'String', colour: 195},
  {type: 'mjs_permission', message0: 'Player has permission %1', args0: [text('PERMISSION', 'myplugin.use')], output: 'Boolean', colour: 195},
  {type: 'mjs_argument', message0: 'Command argument # %1', args0: [{type: 'field_number', name: 'INDEX', value: 1, min: 1, max: 50, precision: 1}], output: 'String', colour: 195},
  {type: 'mjs_give', message0: 'Give player %1 × %2', args0: [value('AMOUNT', 'Number'), text('MATERIAL', 'DIAMOND')], colour: 195, ...statement},
  {type: 'mjs_health', message0: 'Set %1 to %2', args0: [dropdown('STAT', [
    ['health', 'setHealth'], ['hunger', 'setFoodLevel'], ['level', 'setLevel'],
    ['XP', 'setExp'], ['saturation', 'setSaturation']]), value('AMOUNT', 'Number')], colour: 195, ...statement},
  {type: 'mjs_damage', message0: 'Damage player by %1', args0: [value('AMOUNT', 'Number')], colour: 195, ...statement},
  {type: 'mjs_gamemode', message0: 'Set game mode to %1', args0: [dropdown('MODE', [
    ['Survival', 'SURVIVAL'], ['Creative', 'CREATIVE'], ['Adventure', 'ADVENTURE'], ['Spectator', 'SPECTATOR']])], colour: 195, ...statement},
  {type: 'mjs_give_exp', message0: 'Give player %1 experience', args0: [value('AMOUNT', 'Number')], colour: 195, ...statement},
  {type: 'mjs_kick', message0: 'Kick player: %1', args0: [value('REASON')], colour: 195, ...statement},
  {type: 'mjs_health_get', message0: 'Health of player', output: 'Number', colour: 195},
  {type: 'mjs_level_get', message0: 'Level of player', output: 'Number', colour: 195},
  {type: 'mjs_food_get', message0: 'Food level of player', output: 'Number', colour: 195},
  {type: 'mjs_get_player', message0: 'Player named %1', args0: [text('NAME', 'Alex')], output: 'Player', colour: 195},
  {type: 'mjs_player_count', message0: 'Online players', output: 'Number', colour: 195},
  {type: 'mjs_world_time', message0: 'Set time of %1 to %2', args0: [text('WORLD', 'world'), dropdown('TIME', [
    ['sunrise', '0'], ['noon', '6000'], ['sunset', '12000'], ['midnight', '18000']])], colour: 210, ...statement},
  {type: 'mjs_world_weather', message0: 'Set weather of %1 to %2', args0: [text('WORLD', 'world'), dropdown('WEATHER', [
    ['clear', 'clear'], ['rain', 'rain'], ['thunder', 'thunder']])], colour: 210, ...statement},
  {type: 'mjs_world_explosion', message0: 'Explosion at player (%1 power)', args0: [value('POWER', 'Number')], colour: 210, ...statement},
  {type: 'mjs_world_lightning', message0: 'Strike lightning at player', colour: 210, ...statement},
  {type: 'mjs_block_break', message0: 'Break block at player (drop items)', colour: 210, ...statement},
  {type: 'mjs_block_set', message0: 'Set block at player to %1', args0: [text('MATERIAL', 'STONE')], colour: 210, ...statement},
  {type: 'mjs_sound', message0: 'Play sound %1', args0: [text('SOUND', 'ENTITY_PLAYER_LEVELUP')], colour: 295, ...statement},
  {type: 'mjs_particles', message0: 'Spawn %1 particles (%2) at player', args0: [text('PARTICLE', 'FLAME'), value('COUNT', 'Number')], colour: 295, ...statement},
  {type: 'mjs_teleport', message0: 'Teleport to world %1 X %2 Y %3 Z %4', args0: [text('WORLD', 'world'), value('X', 'Number'), value('Y', 'Number'), value('Z', 'Number')], colour: 195, ...statement},
  {type: 'mjs_item_stack', message0: 'Create item %1 × %2', args0: [text('MATERIAL', 'DIAMOND'), value('AMOUNT', 'Number')], output: 'Item', colour: 295},
  {type: 'mjs_item_name', message0: 'Name item %1 with %2', args0: [value('ITEM', 'Item'), text('NAME', 'My item')], output: 'Item', colour: 295},
  {type: 'mjs_item_lore', message0: 'Lore of item %1: %2', args0: [value('ITEM', 'Item'), text('LORE', 'Line 1|Line 2')], output: 'Item', colour: 295},
  {type: 'mjs_give_custom', message0: 'Give player %1', args0: [value('ITEM', 'Item')], colour: 295, ...statement},
  {type: 'mjs_spawn_entity', message0: 'Spawn entity %1 named %2 at player', args0: [text('TYPE', 'ZOMBIE'), text('NAME', 'Bob')], colour: 210, ...statement},
  {type: 'mjs_gui_open', message0: 'Open GUI %1 with %2 rows %3 %4', args0: [value('TITLE'), {type: 'field_number', name: 'ROWS', value: 3, min: 1, max: 6, precision: 1}, {type: 'input_dummy'}, body], colour: 230, ...statement},
  {type: 'mjs_event_slot', message0: 'Clicked slot of inventory event', output: 'Number', colour: 290},
  {type: 'mjs_sidebar', message0: 'Sidebar %1: %2 = %3', args0: [text('NAME', 'stats'), text('LABEL', 'Score'), value('SCORE', 'Number')], colour: 290, ...statement},
  {type: 'mjs_exec_cmd', message0: 'Execute command %1', args0: [value('CMD')], colour: 160, ...statement},
  {type: 'mjs_cancel', message0: 'Cancel this event', colour: 45, ...statement},
  {type: 'mjs_later', message0: 'After %1 seconds %2', args0: [value('SECONDS', 'Number'), body], colour: 290, ...statement},
  {type: 'mjs_timer', message0: 'Every %1 seconds %2 %3', args0: [{type: 'field_number', name: 'SECONDS', value: 10, min: 0.05}, {type: 'input_dummy'}, body], colour: 290},
  {type: 'mjs_each_player', message0: 'For each online player %1 %2', args0: [{type: 'input_dummy'}, body], colour: 290, ...statement},
  {type: 'mjs_config_get', message0: 'Saved value %1 default %2', args0: [text('KEY', 'points'), value('DEFAULT')], output: null, colour: 330},
  {type: 'mjs_config_set', message0: 'Save value %1 as %2', args0: [text('KEY', 'points'), value('VALUE')], colour: 330, ...statement},
  {type: 'mjs_raw', message0: 'JavaScript %1', args0: [text('CODE', 'api.broadcast("Hello!");')], colour: 230, ...statement},
];
Blockly.defineBlocksWithJsonArray(definitions);
const q = JSON.stringify;
const v = (block, gen, name, fallback = '""') => gen.valueToCode(block, name, Order.NONE) || fallback;
const b = (block, gen) => gen.statementToCode(block, 'DO');
const guard = code => `if (typeof player !== "undefined" && player !== null) { ${code} }\n`;
const forBlock = generator.forBlock;
forBlock.mjs_enable = b;
forBlock.mjs_disable = b;
forBlock.mjs_event = (block, gen) => {
  const event = block.getFieldValue('EVENT');
  const getter = event === 'entity.PlayerDeathEvent' || event === 'entity.EntityDamageEvent' || event === 'entity.EntityDeathEvent'
    ? 'getEntity' : 'getPlayer';
  const playerCode = getter === 'getPlayer'
    ? `  var player = typeof event.getPlayer === "function" ? event.getPlayer() : (typeof event.getEntity === "function" ? event.getEntity() : null);\n`
    : `  var player = event.getEntity();\n`;
  return `api.registerEvent(${q(event)}, function(event) {\n${playerCode}${b(block, gen)}});\n`;
};
forBlock.mjs_command = (block, gen) => {
  const name = block.getFieldValue('NAME');
  if (!/^[a-z][a-z0-9_-]{0,31}$/.test(name) || ['mjs', 'jsreload', 'jslist', 'jsconfig'].includes(name)) {
    throw new Error('Command names: lowercase letters, numbers, _ or -. MC-JS commands are reserved.');
  }
  const permission = block.getFieldValue('PERMISSION').trim();
  return `api.registerCommand(${q(name)}, function(sender, args) {\n`
    + (permission ? `  if (!sender.hasPermission(${q(permission)})) { api.sendMessage(sender, "&cNo permission."); return true; }\n` : '')
    + `  var player = api.getPlayerFromSender(sender);\n  if (!player) { api.sendMessage(sender, "Only available in-game."); return true; }\n${b(block, gen)}  return true;\n});\n`;
};
forBlock.mjs_message = (block, gen) => guard(`api.sendMessage(player, ${v(block, gen, 'TEXT')});`);
forBlock.mjs_broadcast = (block, gen) => `api.broadcast(${v(block, gen, 'TEXT')});\n`;
forBlock.mjs_log = (block, gen) => `logger.info(String(${v(block, gen, 'TEXT')}));\n`;
forBlock.mjs_title = (block, gen) => guard(`api.sendTitle(player, ${v(block, gen, 'TITLE')}, ${v(block, gen, 'SUBTITLE')});`);
forBlock.mjs_actionbar = (block, gen) => guard(`api.sendActionBar(player, ${v(block, gen, 'TEXT')});`);
forBlock.mjs_name = () => ['(typeof player !== "undefined" && player !== null ? String(player.getName()) : "")', Order.ATOMIC];
forBlock.mjs_permission = block => [`(typeof player !== "undefined" && player !== null && player.hasPermission(${q(block.getFieldValue('PERMISSION'))}))`, Order.ATOMIC];
forBlock.mjs_argument = block => [`(typeof args !== "undefined" && args.length >= ${block.getFieldValue('INDEX')} ? String(args[${block.getFieldValue('INDEX') - 1}]) : "")`, Order.ATOMIC];
forBlock.mjs_give = (block, gen) => guard(`api.giveItem(player, api.createItemStack(api.getMaterial(${q(block.getFieldValue('MATERIAL').toUpperCase())}), Math.max(1, Math.min(64, Math.floor(${v(block, gen, 'AMOUNT', '1')})))));`);
forBlock.mjs_health = (block, gen) => guard(`api.player.${block.getFieldValue('STAT')}(player, ${v(block, gen, 'AMOUNT', '20')});`);
forBlock.mjs_damage = (block, gen) => guard(`api.player.damage(player, ${v(block, gen, 'AMOUNT', '1')});`);
forBlock.mjs_gamemode = block => guard(`api.player.setGameMode(player, GameMode.${block.getFieldValue('MODE')});`);
forBlock.mjs_give_exp = (block, gen) => guard(`api.player.giveExp(player, Math.max(0, Math.floor(${v(block, gen, 'AMOUNT', '10')})));`);
forBlock.mjs_kick = (block, gen) => guard(`api.player.kickPlayer(player, ${v(block, gen, 'REASON', q('Leaving.'))});`);
forBlock.mjs_health_get = () => ['(typeof player !== "undefined" && player !== null ? api.player.getHealth(player) : 0)', Order.FUNCTION_CALL];
forBlock.mjs_level_get = () => ['(typeof player !== "undefined" && player !== null ? api.player.getLevel(player) : 0)', Order.FUNCTION_CALL];
forBlock.mjs_food_get = () => ['(typeof player !== "undefined" && player !== null ? api.player.getFoodLevel(player) : 0)', Order.FUNCTION_CALL];
forBlock.mjs_get_player = block => [`api.getPlayer(${q(block.getFieldValue('NAME'))})`, Order.FUNCTION_CALL];
forBlock.mjs_player_count = () => ['api.getOnlinePlayers().length', Order.FUNCTION_CALL];
forBlock.mjs_sound = block => guard(`api.playSound(player.getLocation(), ${q(block.getFieldValue('SOUND'))}, 1, 1);`);
forBlock.mjs_particles = (block, gen) => guard(`api.spawnParticle(player.getLocation(), ${q(block.getFieldValue('PARTICLE'))}, Math.max(1, Math.floor(${v(block, gen, 'COUNT', '1')})));`);
forBlock.mjs_world_time = block => `api.world.setWorldTime(server.getWorld(${q(block.getFieldValue('WORLD'))}), ${block.getFieldValue('TIME')});\n`;
forBlock.mjs_world_weather = block => {
  const world = `server.getWorld(${q(block.getFieldValue('WORLD'))})`;
  const weather = block.getFieldValue('WEATHER');
  if (weather === 'clear') return `api.world.setStorm(${world}, false); api.world.setThundering(${world}, false);\n`;
  if (weather === 'thunder') return `api.world.setStorm(${world}, true); api.world.setThundering(${world}, true);\n`;
  return `api.world.setStorm(${world}, true); api.world.setThundering(${world}, false);\n`;
};
forBlock.mjs_world_explosion = (block, gen) => guard(`api.world.createExplosion(player.getLocation(), ${v(block, gen, 'POWER', '4')}, true);`);
forBlock.mjs_world_lightning = () => guard('api.world.strikeLightning(player.getLocation());');
forBlock.mjs_block_break = () => guard('api.block.breakBlock(player.getLocation(), true);');
forBlock.mjs_block_set = block => guard(`api.block.setBlockType(player.getLocation(), api.getMaterial(${q(block.getFieldValue('MATERIAL').toUpperCase())}));`);
forBlock.mjs_teleport = (block, gen) => guard(`var destinationWorld = server.getWorld(${q(block.getFieldValue('WORLD'))}); if (destinationWorld) player.teleport(new org.bukkit.Location(destinationWorld, ${v(block, gen, 'X', '0')}, ${v(block, gen, 'Y', '64')}, ${v(block, gen, 'Z', '0')}));`);
forBlock.mjs_cancel = () => 'if (typeof event !== "undefined" && typeof event.setCancelled === "function") event.setCancelled(true);\n';
forBlock.mjs_later = (block, gen) => `api.runTaskLater(Math.max(1, Math.round((${v(block, gen, 'SECONDS', '1')}) * 20)), function() {\n${b(block, gen)}});\n`;
forBlock.mjs_timer = (block, gen) => `api.runTaskTimer(0, ${Math.max(1, Math.round(block.getFieldValue('SECONDS') * 20))}, function() {\n${b(block, gen)}});\n`;
forBlock.mjs_each_player = (block, gen) => `api.getOnlinePlayers().forEach(function(player) {\n${b(block, gen)}});\n`;
forBlock.mjs_config_get = (block, gen) => [`api.getPluginConfigValue(pluginInfo.id, ${q(block.getFieldValue('KEY'))}, ${v(block, gen, 'DEFAULT', '0')})`, Order.FUNCTION_CALL];
forBlock.mjs_config_set = (block, gen) => `api.setPluginConfigValue(pluginInfo.id, ${q(block.getFieldValue('KEY'))}, ${v(block, gen, 'VALUE', '0')});\n`;
forBlock.mjs_raw = block => `${block.getFieldValue('CODE')}\n`;
forBlock.mjs_item_stack = (block, gen) => [`api.createItemStack(api.getMaterial(${q(block.getFieldValue('MATERIAL').toUpperCase())}), Math.max(1, Math.min(64, Math.floor(${v(block, gen, 'AMOUNT', '1')}))))`, Order.FUNCTION_CALL];
forBlock.mjs_item_name = (block, gen) => {
  const item = v(block, gen, 'ITEM', 'api.createItemStack(api.getMaterial("STICK"), 1)');
  return [`api.setItemDisplayName(${item}, ${q(block.getFieldValue('NAME'))})`, Order.FUNCTION_CALL];
};
forBlock.mjs_item_lore = (block, gen) => {
  const item = v(block, gen, 'ITEM', 'api.createItemStack(api.getMaterial("STICK"), 1)');
  const lines = block.getFieldValue('LORE').split('|');
  return [`api.setItemLore(${item}, [${lines.map(line => q(line)).join(', ')}])`, Order.FUNCTION_CALL];
};
forBlock.mjs_give_custom = (block, gen) => guard(`api.giveItem(player, ${v(block, gen, 'ITEM', 'api.createItemStack(api.getMaterial("STICK"), 1)')});`);
forBlock.mjs_spawn_entity = block => guard(`var spawned = api.entity.spawnEntity(player.getLocation(), ${q(block.getFieldValue('TYPE').toUpperCase())}); api.entity.setEntityCustomName(spawned, ${q(block.getFieldValue('NAME'))});`);
forBlock.mjs_gui_open = (block, gen) => {
  const rows = Math.max(1, Math.min(6, Math.round(Number(block.getFieldValue('ROWS')) || 3)));
  const doCode = b(block, gen);
  const handler = doCode
    ? `  gui.onClick(function(event) {\n${doCode}  });\n`
    : '';
  return `var gui = api.createGUI(${v(block, gen, 'TITLE', '""')}, ${rows});\n`
    + (handler || `  gui.onClick(function(event) {});\n`)
    + guard(`gui.open(player);`);
};
forBlock.mjs_event_slot = () => ['(typeof event !== "undefined" && typeof event.getSlot === "function" ? event.getSlot() : -1)', Order.ATOMIC];
forBlock.mjs_sidebar = (block, gen) => {
  const name = q(block.getFieldValue('NAME') || 'stats');
  const label = q(block.getFieldValue('LABEL') || 'Score');
  return `var sb = api.scoreboard.createScoreboard();\n`
    + `var obj = api.scoreboard.createObjective(sb, ${name}, "dummy", ${name});\n`
    + `obj.getScore(${label}).setScore(${v(block, gen, 'SCORE', '0')});\n`
    + guard(`player.setScoreboard(sb);`);
};
forBlock.mjs_exec_cmd = (block, gen) => `api.utility.executeCommand(String(${v(block, gen, 'CMD', '""')}));\n`;

const shadow = (type, fields) => ({shadow: {type, fields}});
const stringShadow = text => shadow('text', {TEXT: text});
const numberShadow = num => shadow('math_number', {NUM: num});
const entry = (type, inputs, fields) => ({kind: 'block', type, ...(inputs ? {inputs} : {}), ...(fields ? {fields} : {})});
const category = (name, colour, contents) => ({kind: 'category', name, colour, contents});
export const toolbox = {
  kind: 'categoryToolbox', contents: [
    category('Events', '#e1aa38', ['mjs_enable', 'mjs_disable', 'mjs_event', 'mjs_command', 'mjs_cancel'].map(type => entry(type))),
    category('Messages', '#4ebf9a', [entry('mjs_message', {TEXT: stringShadow('&aHello!')}), entry('mjs_broadcast', {TEXT: stringShadow('&6Welcome!')}), entry('mjs_title', {TITLE: stringShadow('Welcome'), SUBTITLE: stringShadow('Have fun!')}), entry('mjs_actionbar', {TEXT: stringShadow('Let\'s go!')}), entry('mjs_log', {TEXT: stringShadow('Plugin started')}), entry('mjs_exec_cmd', {CMD: stringShadow('say Hello world!')})]),
    category('Player & Stats', '#55b5e9', [entry('mjs_name'), entry('mjs_permission'), entry('mjs_argument'), entry('mjs_get_player', null, {NAME: 'Alex'}), entry('mjs_player_count'), entry('mjs_health_get'), entry('mjs_level_get'), entry('mjs_food_get'), entry('mjs_health', {AMOUNT: numberShadow(20)}), entry('mjs_damage', {AMOUNT: numberShadow(2)}), entry('mjs_give_exp', {AMOUNT: numberShadow(10)}), entry('mjs_gamemode'), entry('mjs_kick', {REASON: stringShadow('Goodbye!')}), entry('mjs_teleport', {X: numberShadow(0), Y: numberShadow(64), Z: numberShadow(0)})]),
    category('World', '#5b80a5', [entry('mjs_world_time'), entry('mjs_world_weather'), entry('mjs_world_explosion', {POWER: numberShadow(4)}), entry('mjs_world_lightning'), entry('mjs_block_break'), entry('mjs_block_set', null, {MATERIAL: 'STONE'})]),
    category('Items & Effects', '#55b5e9', [entry('mjs_item_stack', {AMOUNT: numberShadow(1)}), entry('mjs_item_name', null, {NAME: 'My item'}), entry('mjs_item_lore', null, {LORE: 'Line 1|Line 2'}), entry('mjs_give_custom'), entry('mjs_give', {AMOUNT: numberShadow(1)}), entry('mjs_sound'), entry('mjs_particles', {COUNT: numberShadow(1)})]),
    category('Entities', '#af81e8', [entry('mjs_spawn_entity', null, {TYPE: 'ZOMBIE', NAME: 'Bob'})]),
    category('Time & Players', '#af81e8', [entry('mjs_later', {SECONDS: numberShadow(1)}), entry('mjs_timer'), entry('mjs_each_player')]),
    category('Conditions', '#5b80a5', ['controls_if', 'logic_compare', 'logic_operation', 'logic_negate', 'logic_boolean', 'logic_null', 'logic_ternary'].map(type => entry(type))),
    category('Loops', '#5ba55b', ['controls_repeat_ext', 'controls_whileUntil', 'controls_for', 'controls_forEach', 'controls_flow_statements'].map(type => entry(type))),
    category('Math', '#5b67a5', ['math_number', 'math_arithmetic', 'math_random_int', 'math_round', 'math_modulo', 'math_constrain'].map(type => entry(type))),
    category('Text & Lists', '#5ba58c', ['text', 'text_join', 'text_length', 'text_isEmpty', 'lists_create_with', 'lists_length', 'lists_getIndex', 'lists_setIndex'].map(type => entry(type))),
    {kind: 'category', name: 'Variables', colour: '#bf8055', custom: 'VARIABLE'},
    {kind: 'category', name: 'Functions', colour: '#995ba5', custom: 'PROCEDURE'},
    category('Storage', '#c875a4', [entry('mjs_config_get'), entry('mjs_config_set')]),
    category('GUI & Scoreboard', '#c875a4', [entry('mjs_gui_open', null, {ROWS: 3}), entry('mjs_event_slot'), entry('mjs_sidebar', {SCORE: numberShadow(1)}, {NAME: 'stats', LABEL: 'Score'})]),
    category('Custom Code', '#7186d0', [entry('mjs_raw')]),
  ],
};

registerSystemBlocks(Blockly, generator, Order, toolbox);
registerStudioBlocks(Blockly, generator, Order, toolbox);
instrumentBlocks(Blockly, generator);

function diagnose(workspace) { return [...diagnoseSystems(workspace), ...diagnoseStudio(workspace)]; }

export function generate(workspace, metadata) {
  const problems = diagnose(workspace);
  if (problems.length) throw new Error(problems.map(problem => problem.message).join('\n'));
  generator.addReservedWords(workspace.getBlocksByType('mjs_function', false).map(block => 'mcjs_fn_' + block.getFieldValue('NAME')).join(','));
  generator.init(workspace);
  const startup = [], shutdown = [];
  const roots = new Set(['mjs_enable', 'mjs_disable', 'mjs_event', 'mjs_command', 'mjs_timer', 'procedures_defnoreturn', 'procedures_defreturn', ...systemRoots, ...studioRoots]);
  for (const block of workspace.getTopBlocks(true).sort((a, b) => Number(b.type === 'mjs_machine') - Number(a.type === 'mjs_machine'))) {
    if (!block.isEnabled()) continue;
    if (!roots.has(block.type)) throw new Error('Connect free blocks to an event, command or startup block.');
    const code = generator.blockToCode(block);
    if (block.type === 'mjs_disable') shutdown.push(code || '');
    else startup.push(code || '');
  }
  const definitions = generator.finish('').trim();
  const indent = statements => statements.join('\n').trim().split('\n').map(line => line ? `    ${line}` : '').join('\n');
  return `var pluginInfo = ${JSON.stringify(metadata, null, 2)};\n\n${systemRuntime}\n${studioRuntime}\n${definitions ? definitions + '\n\n' : ''}function onEnable() {\n${indent(startup)}\n}\n\nfunction onDisable() {\n${indent(shutdown)}\n}\n`;
}

export function starter(kind = 'welcome') {
  const system = systemTemplate(kind);
  if (system) return system;
  const message = {type: 'mjs_message', inputs: {TEXT: stringShadow('&aWelcome to the server!')}};
  const root = kind === 'command'
    ? {type: 'mjs_command', fields: {NAME: 'hello', PERMISSION: ''}, inputs: {DO: {block: message}}}
    : kind === 'empty'
      ? {type: 'mjs_enable'}
      : {type: 'mjs_event', fields: {EVENT: 'player.PlayerJoinEvent'}, inputs: {DO: {block: message}}};
  return {blocks: {languageVersion: 0, blocks: [{...root, x: 45, y: 45}]}};
}

export {Blockly, diagnose};
