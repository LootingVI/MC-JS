import {Blockly} from './blocks.js';

export const schema = (type, fields = {}, inputs = {}) => ({type, fields, inputs: Object.fromEntries(Object.entries(inputs).map(([name, block]) => [name, {block}]))});
export const textBlock = value => schema('text', {TEXT: value});
export const numberBlock = value => schema('math_number', {NUM: value});
export const itemBlock = (material, amount = 1, name = '') => name ? schema('mjs_item_name', {NAME: name}, {ITEM: itemBlock(material, amount)}) : schema('mjs_item_stack', {MATERIAL: material}, {AMOUNT: numberBlock(amount)});
export function append(workspace, state) { return Blockly.serialization.blocks.append(state, workspace, {recordUndo: true}); }
export function attach(parent, input, block) {
  let previous = parent.getInput(input).connection;
  while (previous.targetBlock()) previous = previous.targetBlock().nextConnection;
  if (!previous || !block.previousConnection) throw new Error('This block cannot be attached here.');
  previous.connect(block.previousConnection);
  return block;
}
export function replaceInput(parent, input, state) {
  parent.getInputTargetBlock(input)?.dispose(false);
  if (!state) return;
  const block = append(parent.workspace, state);
  parent.getInput(input).connection.connect(block.outputConnection || block.previousConnection);
}
export function contents(parent, input = 'DO') {
  const result = [];
  for (let block = parent.getInputTargetBlock(input); block; block = block.getNextBlock()) result.push(block);
  return result;
}
export function uniqueName(workspace, type, prefix) {
  const names = new Set(workspace.getBlocksByType(type, false).map(block => block.getFieldValue('NAME')));
  let number = 1;
  while (names.has(prefix + number)) number++;
  return prefix + number;
}
export function createMenu(workspace) {
  const root = append(workspace, schema('mjs_function', {NAME: uniqueName(workspace, 'mjs_function', 'open_menu_')}, {
    DO: schema('mjs_menu', {ROWS: 3}, {TITLE: textBlock('&6Server menu')}),
  }));
  root.moveBy(40, workspace.getTopBlocks(false).length * 100);
  return root.getInputTargetBlock('DO');
}
export function menuButtons(menu) { return contents(menu).filter(block => block.type === 'mjs_menu_button'); }
export function slotNumber(button) {
  const slot = button.getInputTargetBlock('SLOT');
  return slot?.type === 'math_number' ? Number(slot.getFieldValue('NUM')) : null;
}
export function readItem(button) {
  let item = button?.getInputTargetBlock('ITEM');
  const result = {material: 'DIAMOND', amount: 1, label: ''};
  if (item?.type === 'mjs_item_name') { result.label = item.getFieldValue('NAME'); item = item.getInputTargetBlock('ITEM'); }
  if (item?.type === 'mjs_item_stack') {
    result.material = item.getFieldValue('MATERIAL');
    const amount = item.getInputTargetBlock('AMOUNT');
    result.amount = amount?.type === 'math_number' ? Number(amount.getFieldValue('NUM')) : 1;
    if (amount && amount.type !== 'math_number') result.custom = true;
  } else if (item) result.custom = true;
  return result;
}
export function setMenuButton(menu, slot, config) {
  if (!Number.isInteger(slot) || slot < 0 || slot >= Number(menu.getFieldValue('ROWS')) * 9) throw new Error('Choose a valid slot.');
  if (!/^[A-Z][A-Z0-9_]*$/.test(config.material) || config.material === 'AIR') throw new Error('Enter a Minecraft material such as DIAMOND.');
  if (!Number.isInteger(config.amount) || config.amount < 1 || config.amount > 64) throw new Error('Item amount must be 1–64.');
  if (config.action === 'vault' && (!Number.isFinite(config.cost) || config.cost < 0)) throw new Error('Price must be a non-negative number.');
  if (config.action === 'function' && !/^[A-Za-z][A-Za-z0-9_]{0,47}$/.test(config.value)) throw new Error('Enter a valid function name.');
  let button = menuButtons(menu).find(button => slotNumber(button) === slot);
  if (!button) button = attach(menu, 'DO', append(menu.workspace, schema('mjs_menu_button', {}, {SLOT: numberBlock(slot)})));
  replaceInput(button, 'ITEM', itemBlock(config.material, config.amount, config.label));
  let action;
  if (config.action === 'message') action = schema('mjs_message', {}, {TEXT: textBlock(config.value)});
  if (config.action === 'function') action = schema('mjs_call', {NAME: config.value}, {PLAYER: schema('mjs_current_player')});
  if (config.action === 'close') action = schema('mjs_menu_close');
  if (config.action === 'vault') {
    action = schema('mjs_vault_purchase', {}, {
      PLAYER: schema('mjs_current_player'), AMOUNT: numberBlock(config.cost), ITEM: itemBlock(config.material, config.amount),
      DO: schema('mjs_message', {}, {TEXT: textBlock('&aPurchase complete.')}),
      ELSE: schema('mjs_message', {}, {TEXT: schema('mjs_vault_result', {FIELD: 'getError'})}),
    });
  }
  if (action) replaceInput(button, 'DO', action);
  return button;
}
export function createMachine(workspace) {
  const root = append(workspace, schema('mjs_machine', {NAME: uniqueName(workspace, 'mjs_machine', 'game_'), INITIAL: 'Lobby'}));
  for (const name of ['Lobby', 'Countdown', 'Running', 'Finished']) attach(root, 'DO', append(workspace, schema('mjs_state', {NAME: name})));
  for (const [from, to] of [['Lobby', 'Countdown'], ['Countdown', 'Running'], ['Running', 'Finished'], ['Finished', 'Lobby']]) attach(root, 'DO', append(workspace, schema('mjs_transition', {FROM: from, EVENT: 'next', TO: to})));
  return root;
}
export function renameState(machine, state, name) {
  if (!/^[A-Za-z][A-Za-z0-9_-]{0,47}$/.test(name)) throw new Error('State names must start with a letter and use letters, numbers, _ or -.');
  const parts = contents(machine), old = state.getFieldValue('NAME');
  if (parts.some(block => block !== state && block.type === 'mjs_state' && block.getFieldValue('NAME') === name)) throw new Error('That state already exists.');
  state.setFieldValue(name, 'NAME');
  if (machine.getFieldValue('INITIAL') === old) machine.setFieldValue(name, 'INITIAL');
  for (const block of parts.filter(block => block.type === 'mjs_transition')) for (const field of ['FROM', 'TO']) if (block.getFieldValue(field) === old) block.setFieldValue(name, field);
}

export function captureLibrary(workspace, selected) {
  const roots = new Set(selected ? [selected] : workspace.getTopBlocks(true));
  for (const root of roots) for (const block of root.getDescendants(false)) {
    let dependency;
    if (['mjs_call', 'mjs_call_result'].includes(block.type)) dependency = workspace.getBlocksByType('mjs_function', false).find(fn => fn.getFieldValue('NAME') === block.getFieldValue('NAME'));
    if (block.type.startsWith('mjs_state_')) dependency = workspace.getBlocksByType('mjs_machine', false).find(machine => machine.getFieldValue('NAME') === block.getFieldValue('NAME'));
    if (typeof block.getProcedureCall === 'function') dependency = workspace.getTopBlocks(false).find(fn => fn.getFieldValue('NAME') === block.getProcedureCall());
    if (dependency) roots.add(dependency);
  }
  if (!roots.size) throw new Error('Create or select some blocks first.');
  return [...roots].map(root => Blockly.serialization.blocks.save(root, {doFullSerialization: true, saveIds: false}));
}
export function insertLibrary(workspace, states) {
  if (!Array.isArray(states) || !states.length || states.length > 200 || JSON.stringify(states).length > 1024 * 1024) throw new Error('Invalid or oversized block group.');
  const clone = JSON.parse(JSON.stringify(states));
  const clearIds = value => {
    if (!value || typeof value !== 'object') return;
    if (value.type || value.name) delete value.id;
    Object.values(value).forEach(clearIds);
  };
  clone.forEach(clearIds);
  const check = new Blockly.Workspace();
  try { clone.forEach(state => append(check, state)); } finally { check.dispose(); }
  const imported = [];
  const top = workspace.getTopBlocks(false);
  let y = Math.max(0, ...top.map(block => block.getRelativeToSurfaceXY().y + (block.getHeightWidth?.().height || 200))) + 70;
  Blockly.Events.setGroup(true);
  try {
    for (const state of clone) {
      const block = append(workspace, state);
      imported.push(block);
      const position = block.getRelativeToSurfaceXY();
      block.moveBy(40 - position.x, y - position.y);
      y += (block.getHeightWidth?.().height || 200) + 60;
    }
  } catch (error) { imported.forEach(block => block.dispose(false)); throw error; }
  finally { Blockly.Events.setGroup(false); }
  return imported;
}
