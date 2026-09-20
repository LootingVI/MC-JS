import {test} from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import {Blockly, generate, starter, diagnose} from '../src/blocks.js';
import {systemTemplates} from '../src/system-templates.js';

function compile(kind, state = starter(kind)) {
  const workspace = new Blockly.Workspace();
  try {
    Blockly.serialization.workspaces.load(state, workspace);
    assert.deepEqual(diagnose(workspace), []);
    const saved = Blockly.serialization.workspaces.save(workspace);
    workspace.clear();
    Blockly.serialization.workspaces.load(saved, workspace);
    return generate(workspace, {id: kind, name: kind, version: '1.0.0', author: 'Test'});
  } finally { workspace.dispose(); }
}

function runtime(kind, storage = new Map()) {
  const commands = new Map(), events = [], messages = [], menus = [];
  let now = 100000;
  const key = (name, scope, field) => JSON.stringify([name, scope, field]);
  const read = (name, scope, field, fallback) => storage.get(key(name, scope, field)) ?? fallback;
  const write = (name, scope, field, value) => storage.set(key(name, scope, field), value);
  const systems = {
    readData: read, writeData: write,
    changeNumber: (name, scope, field, value) => write(name, scope, field, JSON.stringify(JSON.parse(read(name, scope, field, '0')) + value)),
    registerEvent: (name, observe, fn) => events.push({name, observe, fn}), eventPlayer: event => event.getPlayer(),
    cooldownRemaining: (name, scope, field) => Math.max(0, JSON.parse(read(name, 'cooldowns:' + scope, field, '0')) - now),
    claimCooldown: (name, scope, field, seconds) => {
      if (systems.cooldownRemaining(name, scope, field)) return false;
      write(name, 'cooldowns:' + scope, field, JSON.stringify(now + seconds));
      return true;
    },
    grantKit: (name, player, field, seconds, items) => {
      const scope = 'player:' + player.getUniqueId();
      if (systems.cooldownRemaining(name, scope, field)) return 'cooldown';
      if (player.full) return 'space';
      systems.claimCooldown(name, scope, field, seconds);
      player.items.push(...items);
      return 'ok';
    },
    purchase: (name, player, field, cost, item) => {
      const scope = 'player:' + player.getUniqueId();
      const balance = JSON.parse(read(name, scope, field, '0'));
      if (balance < cost) return 'funds';
      if (player.full) return 'space';
      write(name, scope, field, JSON.stringify(balance - cost));
      player.items.push(item);
      return 'ok';
    },
    createMenu: (title, rows) => {
      const buttons = new Map();
      const menu = {title, rows, buttons, button: (slot, item, fn) => buttons.set(slot, {item, fn}), fill: () => {}, open: player => {menu.player = player; menus.push(menu);}};
      return menu;
    },
    locationToJson: JSON.stringify, locationFromJson: json => JSON.parse(json),
  };
  const api = {
    systems, registerCommand: (name, fn) => commands.set(name, fn), getPlayerFromSender: sender => sender.player,
    sendMessage: (player, message) => messages.push({player, message}),
    getMaterial: value => value, createItemStack: (material, amount) => ({material, amount}),
    setItemDisplayName: (item, name) => ({...item, name}),
    scoreboard: {createScoreboard: () => ({}), createObjective: board => ({getScoreboard: () => board, getScore: label => ({setScore: score => {board[label] = score;}})})},
  };
  const context = vm.createContext({api});
  vm.runInContext(compile(kind), context);
  context.onEnable();
  return {storage, commands, events, messages, menus, context,
    value: (player, field) => JSON.parse(read(kind, 'player:' + player.getUniqueId(), field, 'null')),
    execute: (name, player, args = []) => commands.get(name)({player}, args),
    event: (name, player, extras = {}, cancelled = false) => events.filter(event => event.name === name && !(event.observe && cancelled)).forEach(event => event.fn({getPlayer: () => player, ...extras})),
    advance: seconds => {now += seconds;},
  };
}
const player = name => ({items: [], full: false, location: {world: 'world', x: 12, y: 70, z: -45, yaw: 90, pitch: 0}, getUniqueId: () => name, getName: () => name, getLocation() {return this.location;}, teleport(location) {this.location = location;}, setScoreboard(board) {this.board = board;}});

for (const template of systemTemplates) test(`${template.title} consists of editable blocks and survives a save/load round trip`, () => {
  assert.doesNotMatch(JSON.stringify(starter(template.id)), /mjs_raw/);
  assert.doesNotThrow(() => new vm.Script(compile(template.id)));
});

test('shop grants one welcome balance, targets clicking players, checks purchases and preserves balances on reload', () => {
  let rt = runtime('shop');
  const alex = player('Alex'), steve = player('Steve');
  rt.event('player.PlayerJoinEvent', alex);
  rt.event('player.PlayerJoinEvent', alex);
  rt.event('player.PlayerJoinEvent', steve);
  assert.equal(rt.value(alex, 'coins'), 100);
  rt.execute('shop', alex);
  assert.equal(rt.menus[0].rows, 3);
  const buy = rt.menus[0].buttons.get(11).fn;
  buy(alex);
  assert.equal(rt.value(alex, 'coins'), 90);
  assert.equal(rt.value(steve, 'coins'), 100);
  assert.equal(alex.items[0].material, 'DIAMOND');
  alex.full = true;
  buy(alex);
  assert.equal(rt.value(alex, 'coins'), 90);
  assert.match(rt.messages.at(-1).message, /space/);
  alex.full = false;
  for (let index = 0; index < 10; index++) buy(alex);
  assert.equal(rt.value(alex, 'coins'), 0);
  assert.equal(alex.items.length, 10);
  assert.match(rt.messages.at(-1).message, /funds/);
  rt = runtime('shop', rt.storage);
  rt.event('player.PlayerJoinEvent', alex);
  assert.equal(rt.value(alex, 'coins'), 0);
});

test('daily kit uses one action for commands and menus and handles cooldown and inventory branches', () => {
  let rt = runtime('kit');
  const alex = player('Alex');
  alex.full = true;
  rt.execute('kit', alex);
  assert.equal(alex.items.length, 0);
  assert.match(rt.messages.at(-1).message, /room/);
  alex.full = false;
  rt.execute('kits', alex);
  rt.menus[0].buttons.get(13).fn(alex);
  assert.equal(alex.items.length, 3);
  assert.equal(alex.items[0].amount, 16);
  rt = runtime('kit', rt.storage);
  rt.execute('kit', alex);
  assert.equal(alex.items.length, 3);
  assert.match(rt.messages.at(-1).message, /86400 seconds/);
  rt.advance(86401);
  rt.execute('kit', alex);
  assert.equal(alex.items.length, 6);
});

test('quest ignores cancelled breaks and wrong materials, separates progress and pays once', () => {
  const rt = runtime('quest'), alex = player('Alex'), steve = player('Steve');
  const mine = (target, material = 'STONE', cancelled = false) => rt.event('block.BlockBreakEvent', target, {getBlock: () => ({getType: () => material})}, cancelled);
  mine(alex, 'STONE', true);
  mine(alex, 'DIRT');
  assert.equal(rt.value(alex, 'stone-mined'), null);
  for (let index = 0; index < 9; index++) mine(alex);
  mine(steve);
  alex.full = true;
  mine(alex);
  assert.equal(rt.value(alex, 'quest-complete'), null);
  alex.full = false;
  mine(alex);
  for (let index = 0; index < 12; index++) mine(alex);
  assert.equal(rt.value(alex, 'quest-complete'), true);
  assert.equal(rt.value(alex, 'coins'), 50);
  assert.equal(alex.items.length, 1);
  assert.equal(rt.value(steve, 'stone-mined'), 1);
  rt.execute('quest', alex);
  assert.equal(alex.board['Quest coins: 50'], 1);
});

test('homes save world and facing per player and enforce a teleport cooldown', () => {
  const rt = runtime('homes'), alex = player('Alex'), steve = player('Steve');
  rt.execute('home', alex);
  assert.match(rt.messages.at(-1).message, /sethome/);
  rt.execute('sethome', alex);
  alex.location = {world: 'nether', x: 0, y: 30, z: 0};
  rt.execute('home', alex);
  assert.equal(alex.location.world, 'world');
  assert.equal(alex.location.yaw, 90);
  alex.location = {world: 'world', x: 99};
  rt.execute('home', alex);
  assert.equal(alex.location.x, 99);
  rt.advance(11);
  rt.execute('home', alex);
  assert.equal(alex.location.x, 12);
  assert.equal(rt.value(steve, 'home'), null);
});

test('diagnostics identify unknown functions, duplicate commands and missing player contexts', () => {
  const workspace = new Blockly.Workspace();
  try {
    const start = workspace.newBlock('mjs_enable');
    const data = workspace.newBlock('mjs_data_set');
    start.getInput('DO').connection.connect(data.previousConnection);
    const call = workspace.newBlock('mjs_call');
    data.nextConnection.connect(call.previousConnection);
    workspace.newBlock('mjs_command');
    workspace.newBlock('mjs_command');
    const errors = diagnose(workspace);
    assert.ok(errors.some(error => /player context/.test(error.message)));
    assert.ok(errors.some(error => /Unknown reusable/.test(error.message)));
    assert.ok(errors.some(error => /Duplicate command/.test(error.message)));
    assert.throws(() => generate(workspace, {id: 'bad'}), /player context/);
  } finally { workspace.dispose(); }
});
