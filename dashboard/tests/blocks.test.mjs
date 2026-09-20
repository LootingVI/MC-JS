import {test} from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import {Blockly, generate, starter, toolbox} from '../src/blocks.js';
import {append, schema, createMachine, contents} from '../src/studio-model.js';

const metadata = {id: 'test', name: 'Test', version: '1.0.0', author: 'Builder'};
function compile(state) {
  const workspace = new Blockly.Workspace();
  Blockly.serialization.workspaces.load(state, workspace);
  const source = generate(workspace, metadata);
  workspace.dispose();
  return source;
}

test('welcome template registers a real event and sends to the joining player', () => {
  const source = compile(starter('welcome'));
  let registered, sent;
  const player = {getName: () => 'Alex'};
  const context = {api: {
    systems: {registerEvent: (name, observe, callback) => { registered = {name, callback}; }, eventPlayer: event => event.getPlayer()},
    sendMessage: (target, message) => { sent = {target, message}; },
  }};
  vm.createContext(context);
  vm.runInContext(source, context);
  assert.equal(registered, undefined);
  context.onEnable();
  assert.equal(registered.name, 'player.PlayerJoinEvent');
  registered.callback({getPlayer: () => player});
  assert.equal(sent.target, player);
  assert.match(sent.message, /Welcome/);
});

test('command permission prevents actions and keeps console execution safe', () => {
  const state = starter('command');
  state.blocks.blocks[0].fields.PERMISSION = 'test.use';
  const source = compile(state);
  const messages = [];
  let command;
  const context = {api: {registerCommand: (name, callback) => {command = callback;},
    getPlayerFromSender: sender => sender.player, sendMessage: (target, text) => messages.push(text)}};
  vm.createContext(context);
  vm.runInContext(source, context);
  context.onEnable();
  assert.equal(command({hasPermission: () => false}, []), true);
  assert.match(messages.pop(), /permission/i);
  assert.equal(command({hasPermission: () => true}, []), true);
  assert.match(messages.pop(), /in-game/);
  assert.equal(command({hasPermission: () => true, player: {}}, []), true);
  assert.match(messages.pop(), /Welcome/);
});

test('lifecycle code, timers and config values survive serialization', () => {
  const state = {blocks: {languageVersion: 0, blocks: [
    {type: 'mjs_enable', inputs: {DO: {block: {type: 'mjs_config_set', fields: {KEY: 'ready'}, inputs: {VALUE: {block: {type: 'math_number', fields: {NUM: 42}}}}}}}},
    {type: 'mjs_disable', inputs: {DO: {block: {type: 'mjs_log', inputs: {TEXT: {block: {type: 'text', fields: {TEXT: 'stop'}}}}}}}},
    {type: 'mjs_timer', fields: {SECONDS: 2}, inputs: {DO: {block: {type: 'mjs_log', inputs: {TEXT: {block: {type: 'text', fields: {TEXT: 'tick'}}}}}}}},
  ]}};
  const source = compile(state);
  const messages = [], values = [];
  let timer;
  const context = {logger: {info: text => messages.push(text)}, api: {
    setPluginConfigValue: (...args) => values.push(args), runTaskTimer: (delay, ticks, fn) => {timer = {ticks, fn};},
  }};
  vm.createContext(context);
  vm.runInContext(source, context);
  context.onEnable();
  assert.deepEqual(values[0], ['test', 'ready', 42]);
  assert.equal(timer.ticks, 40);
  timer.fn();
  context.onDisable();
  assert.deepEqual(messages, ['tick', 'stop']);
});

test('every custom toolbox block generates parseable JavaScript with quotes escaped', () => {
  const workspace = new Blockly.Workspace();
  for (const category of toolbox.contents) {
    for (const item of category.contents || []) {
      if (!item.type.startsWith('mjs_')) continue;
      workspace.clear();
      const block = workspace.newBlock(item.type);
      if (block.getField('TEXT')) block.setFieldValue('"; throw Error("injected")', 'TEXT');
      const rootTypes = ['mjs_enable', 'mjs_disable', 'mjs_event', 'mjs_command', 'mjs_timer', 'mjs_function', 'mjs_observe_event', 'mjs_machine', 'mjs_state', 'mjs_transition'];
      if (item.type === 'mjs_machine') block.getInput('DO').connection.connect(workspace.newBlock('mjs_state').previousConnection);
      if (item.type === 'mjs_state' || item.type === 'mjs_transition') {
        const machine = createMachine(workspace);
        if (item.type === 'mjs_state') block.setFieldValue('Extra', 'NAME');
        contents(machine).at(-1).nextConnection.connect(block.previousConnection);
      }
      if (item.type.startsWith('mjs_state_')) createMachine(workspace).setFieldValue('game', 'NAME');
      if (!rootTypes.includes(item.type)) {
        const root = workspace.newBlock('mjs_function');
        let parent = root;
        if (['mjs_menu_button', 'mjs_menu_fill', 'mjs_menu_close_event', 'mjs_sidebar_line', 'mjs_purchase_result', 'mjs_kit_result', 'mjs_event_set', 'mjs_cancel', 'mjs_vault_result'].includes(item.type)) {
          const type = item.type.startsWith('mjs_menu') ? 'mjs_menu' : item.type === 'mjs_sidebar_line' ? 'mjs_sidebar_board' : item.type === 'mjs_purchase_result' ? 'mjs_purchase' : item.type === 'mjs_kit_result' ? 'mjs_kit' : item.type === 'mjs_vault_result' ? 'mjs_vault_deposit' : 'mjs_event';
          parent = workspace.newBlock(type);
          if (parent.previousConnection) root.getInput('DO').connection.connect(parent.previousConnection);
        }
        if (block.previousConnection) parent.getInput('DO').connection.connect(block.previousConnection);
        else {
          const log = workspace.newBlock('mjs_log');
          parent.getInput('DO').connection.connect(log.previousConnection);
          log.getInput('TEXT').connection.connect(block.outputConnection);
        }
      }
      assert.doesNotThrow(() => new vm.Script(generate(workspace, metadata)), item.type);
    }
  }
  workspace.dispose();
});

test('floating actions and reserved commands cannot silently create broken plugins', () => {
  const workspace = new Blockly.Workspace();
  workspace.newBlock('mjs_message');
  assert.throws(() => generate(workspace, metadata), /Connect/);
  workspace.clear();
  workspace.newBlock('mjs_command').setFieldValue('mjs', 'NAME');
  assert.throws(() => generate(workspace, metadata), /reserved/);
  workspace.dispose();
});

test('world, item and GUI blocks generate executable code', () => {
  const state = {blocks: {languageVersion: 0, blocks: [
    {type: 'mjs_enable', inputs: {DO: {block: {type: 'mjs_world_time', fields: {WORLD: 'world', TIME: '6000'}}}}},
    {type: 'mjs_enable', inputs: {DO: {block: {type: 'mjs_gui_open', fields: {ROWS: 3}, inputs: {DO: {block: {type: 'mjs_log', inputs: {TEXT: {block: {type: 'text', fields: {TEXT: 'clicked'}}}}}}}, x: 0, y: 0}}}},
  ]}};
  const source = compile(state);
  const calls = [];
  const context = {
    server: {getWorld: () => ({})},
    api: {
      world: {setWorldTime: (w, t) => calls.push(['time', t])},
      createGUI: (title, rows) => ({onClick: fn => { calls.push(['handler', rows]); }, open: () => {}}),
    },
  };
  vm.createContext(context);
  vm.runInContext(source, context);
  context.onEnable();
  assert.deepEqual(calls, [['time', 6000], ['handler', 3]]);
});
