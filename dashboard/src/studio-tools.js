import {Blockly} from './blocks.js';
import {schema, textBlock, numberBlock, append, attach, contents, replaceInput, createMenu, menuButtons, slotNumber, readItem, setMenuButton, createMachine, renameState, captureLibrary, insertLibrary} from './studio-model.js';
import './studio.css';

const markup = `<dialog id="menu-designer" class="studio-dialog"><header><div><small>VISUAL MENU DESIGNER</small><h2>Build an inventory menu</h2></div><button data-close="menu-designer" aria-label="Close menu designer">✕</button></header><div class="studio-dialog-body">
<div class="studio-row"><label>Menu<select id="designer-menu"></select></label><button id="designer-new">New menu</button><button id="designer-blocks">Edit menu blocks</button></div>
<div class="studio-row"><label>Title<input id="designer-title"></label><label>Rows<input id="designer-rows" type="number" min="1" max="6" value="3"></label><button id="designer-settings">Apply menu settings</button></div>
<p id="designer-note"></p><div class="designer-layout"><div><div id="inventory-grid" class="inventory-grid" aria-label="Inventory slots"></div><p class="studio-hint">Select a slot. Each row has nine slots, starting at zero.</p></div><form id="slot-form"><h3 id="slot-title">Slot 0</h3><label>Material<input id="slot-material" value="DIAMOND" required></label><div class="studio-row"><label>Amount<input id="slot-amount" type="number" min="1" max="64" value="1" required></label><label>Button label<input id="slot-label" placeholder="&aBuy a diamond"></label></div><label>Click action<select id="slot-action"><option value="custom">Keep existing blocks / custom action</option><option value="message">Send message</option><option value="function">Run function / open another menu</option><option value="vault">Buy this item with Vault</option><option value="close">Close menu</option></select></label><label id="slot-value-label">Message or function name<input id="slot-value" value="&aHello!"></label><label id="slot-cost-label" hidden>Vault price<input id="slot-cost" type="number" min="0" step="any" value="10"></label><p id="slot-note" class="studio-hint"></p><div class="studio-row"><button class="primary">Apply button</button><button id="slot-edit" type="button">Edit action blocks</button><button id="slot-remove" type="button" class="danger">Remove</button></div></form></div><p class="studio-error" id="menu-error" role="alert"></p></div></dialog>
<dialog id="state-editor" class="studio-dialog"><header><div><small>STATE EDITOR</small><h2>Connect the phases of your system</h2></div><button data-close="state-editor" aria-label="Close state editor">✕</button></header><div class="studio-dialog-body">
<div class="studio-row"><label>Machine<select id="machine-select"></select></label><button id="machine-new">New machine</button><button id="machine-start">Add startup flow</button><button id="machine-command">Add event command</button></div><div class="studio-row"><label>Machine name<input id="machine-name"></label><label>Initial state<select id="machine-initial"></select></label><button id="machine-save">Apply</button></div><p class="studio-hint">Create a machine, choose its starting state and connect states with named events. Start it per player or globally using blocks. Blue arrows show the allowed transitions.</p><div id="state-graph" class="state-graph"></div>
<div class="studio-row"><label>Selected state<input id="state-name"></label><button id="state-rename">Rename</button><button id="state-enter">Edit enter / exit actions</button><button id="state-add">Add state</button><button id="state-remove" class="danger">Remove state</button></div>
<div class="studio-row"><label>Timer seconds<input id="state-seconds" type="number" min="0" max="86400" value="10"></label><label>Timer event<input id="state-timer-event" value="next"></label><button id="state-timer">Add timer to this state</button></div>
<h3>Transitions</h3><div id="transition-list"></div><form id="transition-form" class="studio-row"><label>From<select id="transition-from"></select></label><label>Event<input id="transition-event" value="next" required></label><label>To<select id="transition-to"></select></label><button class="primary">Add transition</button></form><p class="studio-hint">Edit a transition's condition in Blocks. Timers are cancelled when their state ends. Enter actions may send events; exit actions and conditions must not change the machine.</p><p id="state-error" class="studio-error" role="alert"></p></div></dialog>
<dialog id="block-library" class="studio-dialog"><header><div><small>BLOCK LIBRARY</small><h2>Reuse your building blocks</h2></div><button data-close="block-library" aria-label="Close block library">✕</button></header><div class="studio-dialog-body"><p>Shared on this server with dashboard administrators. Referenced functions and state machines are included. Imported groups receive new block IDs. Duplicate commands or function names appear in block issues.</p><form id="library-save-form" class="studio-row"><label>Group name<input id="library-name" required maxlength="80" placeholder="Daily reward menu"></label><label>Include<select id="library-scope"><option value="selected">Selected block and its stack</option><option value="all">All project flows</option></select></label><button class="primary">Save group</button></form><div class="studio-row"><input id="library-search" type="search" placeholder="Search library" aria-label="Search library"><button id="library-refresh">Refresh</button><label class="file-button">Import group<input id="library-import" type="file" accept=".json"></label></div><div id="library-list"></div><p id="library-error" class="studio-error" role="alert"></p></div></dialog>
<dialog id="data-inspector" class="studio-dialog"><header><div><small>DATA INSPECTOR</small><h2>Explore saved player data</h2></div><button data-close="data-inspector" aria-label="Close data inspector">✕</button></header><div class="studio-dialog-body"><p>Read-only view of this plugin's persistent system data and cooldowns. Use a player UUID in the scope filter. Global values use “global”.</p><form id="data-form" class="studio-row"><label>Scope or player UUID<input id="data-scope" placeholder="player: or global"></label><label>Key or value<input id="data-query" type="search" placeholder="coins, quest, home …"></label><button class="primary">Search</button></form><div id="data-count" role="status"></div><div id="data-rows"></div><div class="studio-row"><button id="data-prev">Previous</button><button id="data-next">Next 100</button></div><p id="data-error" class="studio-error" role="alert"></p></div></dialog>
<section id="debug-panel" class="debug-panel" hidden><div class="studio-row"><h3>Block debugger</h3><span id="debug-status" role="status">Capture is off</span><button id="debug-start">Start capture</button><button id="debug-stop">Stop capture</button><button id="debug-clear">Clear view</button><button id="debug-hide">Hide</button></div><p class="studio-hint">Live trace, without pausing server ticks. Capture expires after five minutes. Save &amp; start to debug the current blocks. Select a recorded step to see its values and location.</p><div class="debug-layout"><div id="debug-events"></div><pre id="debug-values">No step selected.</pre></div></section>`;

export function initStudioTools(context) {
  const host = document.createElement('div'); host.innerHTML = markup; document.body.append(host);
  document.querySelector('#project').append(document.getElementById('debug-panel'));
  const $ = id => document.getElementById(id);
  const workspace = () => context.workspace();
  const project = () => context.project();
  let menuId, selectedSlot = 0, machineId, stateId, library, selectedLibraryBlock, dataOffset = 0;
  let debugTimer, debugCursor = 0, debugPlugin, debugEnabled = false, debugGeneration = 0;
  const debugSeen = new Set();
  const blockMode = () => { if (project()?.mode !== 'blocks') throw new Error('Open a block project first.'); };
  const safe = (errorId, fn) => async event => {
    event?.preventDefault(); $(errorId).textContent = '';
    try { await fn(event); } catch (error) { $(errorId).textContent = error.message; context.log(error.message, true); }
  };
  const edit = block => { if (!block) throw new Error('Select a block first.'); for (const dialog of host.querySelectorAll('dialog[open]')) dialog.close(); context.showBlocks(); context.focusBlock(block.id); };
  const option = (value, label) => new Option(label, value);
  const selectOptions = (id, values, selected) => { $(id).replaceChildren(...values.map(([value, label]) => option(value, label))); if (values.some(([value]) => value === selected)) $(id).value = selected; };
  const text = (tag, value, className) => { const element = document.createElement(tag); element.textContent = value; if (className) element.className = className; return element; };
  const button = (label, action) => { const element = text('button', label); element.onclick = action; return element; };
  for (const element of host.querySelectorAll('[data-close]')) element.onclick = () => $(element.dataset.close).close();

  const menu = () => workspace().getBlockById(menuId);
  function renderMenus() {
    const menus = workspace().getBlocksByType('mjs_menu', false);
    if (!menus.length) { menus.push(createMenu(workspace())); context.changed(); }
    if (!menus.some(block => block.id === menuId)) menuId = menus[0].id;
    selectOptions('designer-menu', menus.map(block => [block.id, block.getInputTargetBlock('TITLE')?.getFieldValue('TEXT') || 'Dynamic title']), menuId);
    const title = menu().getInputTargetBlock('TITLE');
    $('designer-title').value = title?.getFieldValue('TEXT') || '';
    $('designer-title').disabled = title && title.type !== 'text';
    $('designer-rows').value = menu().getFieldValue('ROWS');
    const dynamic = menuButtons(menu()).some(block => slotNumber(block) === null) || contents(menu()).some(block => !['mjs_menu_button', 'mjs_menu_fill', 'mjs_menu_close_event'].includes(block.type));
    $('designer-note').textContent = dynamic ? 'This menu includes dynamic slots or flows. They are preserved; edit them with Edit menu blocks.' : 'Buttons and actions are stored as ordinary editable blocks. Call the surrounding function from a command or another button to open this menu.';
    const size = Number(menu().getFieldValue('ROWS')) * 9;
    selectedSlot = Math.min(selectedSlot, size - 1);
    $('inventory-grid').replaceChildren();
    for (let slot = 0; slot < size; slot++) {
      const block = menuButtons(menu()).find(item => slotNumber(item) === slot), item = readItem(block);
      const cell = button('', () => { selectedSlot = slot; renderMenus(); });
      cell.className = `inventory-slot${slot === selectedSlot ? ' selected' : ''}${block ? ' filled' : ''}`;
      cell.setAttribute('aria-label', `Slot ${slot}${block ? ': ' + item.material : ': empty'}`);
      cell.append(text('small', String(slot)), text('strong', block ? item.material.split('_').map(word => word[0]).join('').slice(0, 3) : '+'), text('span', block ? String(item.amount) : ''));
      cell.title = block ? `${item.material} · ${item.label}` : 'Empty slot';
      $('inventory-grid').append(cell);
    }
    const current = menuButtons(menu()).find(block => slotNumber(block) === selectedSlot), item = readItem(current);
    $('slot-title').textContent = `Slot ${selectedSlot}`;
    $('slot-material').value = item.material; $('slot-amount').value = item.amount; $('slot-label').value = item.label;
    $('slot-action').value = current?.getInputTargetBlock('DO') ? 'custom' : 'message';
    $('slot-note').textContent = item.custom ? 'Applying a button replaces its custom item expression. Keep existing blocks preserves its click actions.' : 'Choose Keep existing blocks to preserve the current click action. Other actions replace that button’s click stack.';
    $('slot-edit').disabled = !current; $('slot-remove').disabled = !current;
    renderSlotAction();
  }
  function renderSlotAction() {
    $('slot-value-label').hidden = !['message', 'function'].includes($('slot-action').value);
    $('slot-cost-label').hidden = $('slot-action').value !== 'vault';
  }
  $('designer-menu').onchange = () => { menuId = $('designer-menu').value; renderMenus(); };
  $('designer-new').onclick = safe('menu-error', () => { menuId = createMenu(workspace()).id; context.changed(); renderMenus(); });
  $('designer-blocks').onclick = safe('menu-error', () => edit(menu()));
  $('slot-action').onchange = renderSlotAction;
  $('designer-settings').onclick = safe('menu-error', () => {
    const rows = Number($('designer-rows').value);
    if (!Number.isInteger(rows) || rows < 1 || rows > 6) throw new Error('Use 1–6 rows.');
    if (menuButtons(menu()).some(block => slotNumber(block) !== null && slotNumber(block) >= rows * 9)) throw new Error('Remove buttons outside the new row count first.');
    menu().setFieldValue(rows, 'ROWS');
    if (!$('designer-title').disabled) replaceInput(menu(), 'TITLE', textBlock($('designer-title').value));
    context.changed(); renderMenus();
  });
  $('slot-form').onsubmit = safe('menu-error', () => {
    Blockly.Events.setGroup(true);
    try { setMenuButton(menu(), selectedSlot, {material: $('slot-material').value.trim().toUpperCase(), amount: Number($('slot-amount').value), label: $('slot-label').value, action: $('slot-action').value, value: $('slot-value').value.trim(), cost: Number($('slot-cost').value)}); }
    finally { Blockly.Events.setGroup(false); }
    context.changed(); renderMenus();
  });
  $('slot-edit').onclick = safe('menu-error', () => edit(menuButtons(menu()).find(block => slotNumber(block) === selectedSlot)));
  $('slot-remove').onclick = safe('menu-error', () => { menuButtons(menu()).find(block => slotNumber(block) === selectedSlot)?.dispose(true); context.changed(); renderMenus(); });

  const machine = () => workspace().getBlockById(machineId);
  const states = () => contents(machine()).filter(block => block.type === 'mjs_state');
  const state = () => workspace().getBlockById(stateId);
  function renderStates() {
    const machines = workspace().getBlocksByType('mjs_machine', false);
    if (!machines.length) { machines.push(createMachine(workspace())); context.changed(); }
    if (!machines.some(block => block.id === machineId)) machineId = machines[0].id;
    selectOptions('machine-select', machines.map(block => [block.id, block.getFieldValue('NAME')]), machineId);
    $('machine-name').value = machine().getFieldValue('NAME');
    const all = states();
    if (!all.some(block => block.id === stateId)) stateId = all[0]?.id;
    const options = all.map(block => [block.getFieldValue('NAME'), block.getFieldValue('NAME')]);
    selectOptions('machine-initial', options, machine().getFieldValue('INITIAL'));
    selectOptions('transition-from', options, $('transition-from').value);
    selectOptions('transition-to', options, $('transition-to').value);
    $('state-name').value = state()?.getFieldValue('NAME') || '';
    $('state-graph').replaceChildren();
    const graph = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    graph.setAttribute('viewBox', `0 0 ${Math.max(680, all.length * 175)} 210`);
    graph.setAttribute('role', 'img'); graph.setAttribute('aria-label', 'State transition diagram');
    const positions = new Map(all.map((block, index) => [block.getFieldValue('NAME'), 15 + index * 175]));
    const transitions = contents(machine()).filter(block => block.type === 'mjs_transition');
    transitions.forEach((transition, index) => {
      const from = positions.get(transition.getFieldValue('FROM')), to = positions.get(transition.getFieldValue('TO'));
      if (from == null || to == null) return;
      const path = document.createElementNS(graph.namespaceURI, 'path');
      const y = 100 + index % 3 * 25;
      path.setAttribute('d', `M ${from + 75} 80 Q ${(from + to) / 2 + 75} ${y + 70} ${to + 75} 85 l -5 10 m 5 -10 l 5 10`);
      path.setAttribute('class', 'transition-arrow'); graph.append(path);
      const label = document.createElementNS(graph.namespaceURI, 'text');
      label.setAttribute('x', (from + to) / 2 + 75); label.setAttribute('y', y + 30); label.textContent = transition.getFieldValue('EVENT'); graph.append(label);
    });
    all.forEach(block => {
      const group = document.createElementNS(graph.namespaceURI, 'g');
      const x = positions.get(block.getFieldValue('NAME'));
      const rect = document.createElementNS(graph.namespaceURI, 'rect');
      rect.setAttribute('x', x); rect.setAttribute('y', 20); rect.setAttribute('width', 150); rect.setAttribute('height', 60); rect.setAttribute('rx', 12); rect.setAttribute('class', block.id === stateId ? 'state-node selected' : 'state-node'); group.append(rect);
      const label = document.createElementNS(graph.namespaceURI, 'text'); label.setAttribute('x', x + 75); label.setAttribute('y', 53); label.textContent = (machine().getFieldValue('INITIAL') === block.getFieldValue('NAME') ? '▶ ' : '') + block.getFieldValue('NAME'); group.append(label);
      group.setAttribute('role', 'button'); group.setAttribute('tabindex', '0'); group.setAttribute('aria-label', `Select state ${block.getFieldValue('NAME')}`);
      group.onclick = () => { stateId = block.id; renderStates(); };
      group.onkeydown = event => { if (event.key === 'Enter' || event.key === ' ') group.onclick(); };
      graph.append(group);
    });
    $('state-graph').append(graph);
    $('transition-list').replaceChildren();
    for (const transition of transitions) {
      const row = document.createElement('div'); row.className = 'transition-row';
      row.append(text('span', `${transition.getFieldValue('FROM')} → ${transition.getFieldValue('TO')} · ${transition.getFieldValue('EVENT')}${transition.getInputTargetBlock('IF') ? ' · conditional' : ''}`), button('Edit condition', () => edit(transition)), button('Remove', () => { transition.dispose(true); context.changed(); renderStates(); }));
      $('transition-list').append(row);
    }
  }
  $('machine-select').onchange = () => { machineId = $('machine-select').value; renderStates(); };
  $('machine-new').onclick = safe('state-error', () => { machineId = createMachine(workspace()).id; context.changed(); renderStates(); });
  $('machine-save').onclick = safe('state-error', () => {
    const name = $('machine-name').value.trim(), old = machine().getFieldValue('NAME');
    if (!/^[A-Za-z][A-Za-z0-9_-]{0,47}$/.test(name)) throw new Error('Use a machine name starting with a letter.');
    if (workspace().getBlocksByType('mjs_machine', false).some(block => block !== machine() && block.getFieldValue('NAME') === name)) throw new Error('Machine name already exists.');
    machine().setFieldValue(name, 'NAME'); machine().setFieldValue($('machine-initial').value, 'INITIAL');
    for (const block of workspace().getAllBlocks(false)) if (block.type.startsWith('mjs_state_') && block.getFieldValue('NAME') === old) block.setFieldValue(name, 'NAME');
    context.changed(); renderStates();
  });
  $('machine-start').onclick = safe('state-error', () => { const root = append(workspace(), schema('mjs_enable', {}, {DO: schema('mjs_state_start', {NAME: machine().getFieldValue('NAME'), SCOPE: 'global'})})); context.changed(); edit(root); });
  $('machine-command').onclick = safe('state-error', () => { const root = append(workspace(), schema('mjs_command', {NAME: machine().getFieldValue('NAME').toLowerCase().slice(0, 32), PERMISSION: 'mcjs.state.manage'}, {DO: schema('mjs_state_send', {NAME: machine().getFieldValue('NAME'), SCOPE: 'global'}, {EVENT: schema('mjs_argument', {INDEX: 1})})})); context.changed(); edit(root); });
  $('state-rename').onclick = safe('state-error', () => { if (!state()) throw new Error('Select a state.'); renameState(machine(), state(), $('state-name').value.trim()); context.changed(); renderStates(); });
  $('state-enter').onclick = safe('state-error', () => edit(state()));
  $('state-add').onclick = safe('state-error', () => { let number = 1; while (states().some(block => block.getFieldValue('NAME') === 'State' + number)) number++; stateId = attach(machine(), 'DO', append(workspace(), schema('mjs_state', {NAME: 'State' + number}))).id; context.changed(); renderStates(); });
  $('state-remove').onclick = safe('state-error', () => {
    if (!state() || states().length <= 1) throw new Error('A machine needs at least one state.');
    const name = state().getFieldValue('NAME');
    for (const transition of contents(machine()).filter(block => block.type === 'mjs_transition' && ['FROM', 'TO'].some(field => block.getFieldValue(field) === name))) transition.dispose(true);
    state().dispose(true);
    if (machine().getFieldValue('INITIAL') === name) machine().setFieldValue(states()[0].getFieldValue('NAME'), 'INITIAL');
    context.changed(); renderStates();
  });
  $('state-timer').onclick = safe('state-error', () => {
    if (!state()) throw new Error('Select a state.');
    const seconds = Number($('state-seconds').value), event = $('state-timer-event').value.trim();
    if (!Number.isFinite(seconds) || seconds < 0 || seconds > 86400 || !/^[A-Za-z][A-Za-z0-9_-]{0,47}$/.test(event)) throw new Error('Use a valid event and 0–86400 seconds.');
    attach(state(), 'DO', append(workspace(), schema('mjs_state_after', {NAME: machine().getFieldValue('NAME')}, {SECONDS: numberBlock(seconds), EVENT: textBlock(event), DATA: schema('mjs_function_data')})));
    context.changed(); context.log('State timer added. It is cancelled when the state changes.');
  });
  $('transition-form').onsubmit = safe('state-error', () => {
    const event = $('transition-event').value.trim();
    if (!/^[A-Za-z][A-Za-z0-9_-]{0,47}$/.test(event)) throw new Error('Use an event name starting with a letter.');
    attach(machine(), 'DO', append(workspace(), schema('mjs_transition', {FROM: $('transition-from').value, TO: $('transition-to').value, EVENT: event})));
    context.changed(); renderStates();
  });

  function download(name, value) {
    const url = URL.createObjectURL(new Blob([JSON.stringify(value, null, 2)], {type: 'application/json'}));
    const link = document.createElement('a'); link.href = url; link.download = name; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  async function loadLibrary() { library = await context.request('library'); renderLibrary(); }
  async function saveLibrary(entries) { library = await context.request('library', 'PUT', {entries, revision: library.revision}); renderLibrary(); }
  function renderLibrary() {
    $('library-list').replaceChildren();
    for (const entry of library.entries.filter(entry => entry.name.toLowerCase().includes($('library-search').value.toLowerCase()))) {
      const row = document.createElement('article'); row.className = 'library-entry';
      row.append(text('h3', entry.name), text('p', `${entry.blocks.length} flows · ${entry.source || 'Imported group'}`));
      const actions = document.createElement('div'); actions.className = 'studio-row';
      actions.append(button('Insert into project', safe('library-error', () => { const blocks = insertLibrary(workspace(), entry.blocks); context.changed(); edit(blocks[0]); context.log(`Inserted ${entry.name}. Check duplicate names and connect any free actions.`); })), button('Export', () => download(entry.name.replace(/[^\w-]/g, '-') + '.mcjs-library.json', {format: 'mcjs-library-v1', entry})), button('Remove', safe('library-error', () => saveLibrary(library.entries.filter(item => item.id !== entry.id)))));
      row.append(actions); $('library-list').append(row);
    }
    if (!$('library-list').children.length) $('library-list').append(text('p', 'No saved groups yet. Select blocks or save all project flows.'));
  }
  $('library-save-form').onsubmit = safe('library-error', async () => {
    const selected = $('library-scope').value === 'selected' ? workspace().getBlockById(selectedLibraryBlock) : null;
    if ($('library-scope').value === 'selected' && !selected) throw new Error('Select a block on the canvas before opening the library.');
    const entry = {id: 'group-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 8), name: $('library-name').value.trim(), source: project().name, blocks: captureLibrary(workspace(), selected)};
    if (!entry.name) throw new Error('Enter a group name.');
    await saveLibrary([...library.entries, entry]);
    $('library-name').value = ''; context.log('Block group saved to the server library.');
  });
  $('library-search').oninput = () => renderLibrary();
  $('library-refresh').onclick = safe('library-error', loadLibrary);
  $('library-import').onchange = safe('library-error', async event => {
    const file = event.target.files[0];
    if (!file) return;
    if (file.size > 1024 * 1024) throw new Error('Group file exceeds 1 MiB.');
    const data = JSON.parse(await file.text());
    if (data.format !== 'mcjs-library-v1' || !data.entry?.name || !Array.isArray(data.entry.blocks)) throw new Error('Invalid block library file.');
    const check = new Blockly.Workspace();
    try { insertLibrary(check, data.entry.blocks); } finally { check.dispose(); }
    await saveLibrary([...library.entries, {...data.entry, id: 'import-' + Date.now().toString(36)}]);
    event.target.value = '';
  });

  async function inspectData() {
    if (!project()?.revision) throw new Error('Save this plugin first.');
    const response = await context.request('data?' + new URLSearchParams({name: project().name, scope: $('data-scope').value.trim(), q: $('data-query').value.trim(), offset: dataOffset}));
    $('data-count').textContent = `${response.total} matching values · showing ${response.rows.length ? dataOffset + 1 : 0}–${dataOffset + response.rows.length}`;
    $('data-rows').replaceChildren();
    for (const row of response.rows) {
      const entry = document.createElement('article'); entry.className = 'data-entry';
      entry.append(text('h3', row.key), text('code', row.scope), text('pre', JSON.stringify(row.value, null, 2)));
      if (row.remainingSeconds !== undefined) entry.append(text('p', `Cooldown remaining: ${row.remainingSeconds} seconds`));
      if (row.truncated) entry.append(text('p', 'Value preview truncated to 8 KiB.'));
      $('data-rows').append(entry);
    }
    if (!response.rows.length) $('data-rows').append(text('p', 'No matching saved data. Values appear after your plugin writes them in game.'));
    $('data-prev').disabled = dataOffset === 0; $('data-next').disabled = dataOffset + response.rows.length >= response.total;
  }
  $('data-form').onsubmit = safe('data-error', () => { dataOffset = 0; return inspectData(); });
  $('data-prev').onclick = safe('data-error', () => { dataOffset = Math.max(0, dataOffset - 100); return inspectData(); });
  $('data-next').onclick = safe('data-error', () => { dataOffset += 100; return inspectData(); });

  function displayDebug(response) {
    debugEnabled = response.enabled; debugCursor = response.cursor;
    const tracePlugin = debugPlugin, traceRevision = response.revision;
    const matching = project()?.revision === response.revision && !context.dirty();
    $('debug-status').textContent = response.enabled ? matching ? 'Capturing live · expires in five minutes' : 'Capturing a different revision · save & start your current blocks' : 'Capture is off';
    if (response.dropped) context.log('Some debug events were dropped because the trace buffer filled.');
    for (const event of response.events) {
      if (debugSeen.has(event.sequence)) continue;
      debugSeen.add(event.sequence);
      while (debugSeen.size > 600) debugSeen.delete(debugSeen.values().next().value);
      const row = button(`${new Date(event.time).toLocaleTimeString('en-GB')} · ${event.type === 'error' ? event.message : workspace()?.getBlockById(event.blockId)?.toString(65) || event.blockId}`, () => {
        $('debug-values').textContent = JSON.stringify({type: event.type, message: event.message, values: event.values}, null, 2);
        if (project()?.name === tracePlugin && project()?.revision === traceRevision && !context.dirty() && workspace()?.getBlockById(event.blockId)) { context.showBlocks(); context.focusBlock(event.blockId); workspace().highlightBlock(event.blockId); }
      });
      row.className = event.type === 'error' ? 'debug-error' : 'debug-step';
      $('debug-events').prepend(row);
      while ($('debug-events').children.length > 300) $('debug-events').lastChild.remove();
      if (matching && project()?.mode === 'blocks' && event.blockId) workspace().highlightBlock(event.blockId);
      if (event.type === 'error' && matching) workspace()?.getBlockById(event.blockId)?.setWarningText(event.message, 'runtime');
    }
  }
  async function pollDebug() {
    if (!debugPlugin || $('debug-panel').hidden) return;
    clearTimeout(debugTimer);
    const name = debugPlugin, generation = debugGeneration;
    try {
      const response = await context.request('debug?' + new URLSearchParams({name, after: debugCursor}));
      if (project()?.name !== name || generation !== debugGeneration) return;
      displayDebug(response);
      if (response.enabled && !$('debug-panel').hidden) debugTimer = setTimeout(pollDebug, 750);
    } catch (error) { $('debug-status').textContent = error.message; }
  }
  $('debug-start').onclick = async () => {
    try {
      if (!project()?.revision) throw new Error('Save & start your plugin before capturing.');
      clearTimeout(debugTimer); debugPlugin = project().name;
      const generation = ++debugGeneration, name = debugPlugin;
      const response = await context.request('debug', 'POST', {name, enabled: true});
      if (generation !== debugGeneration || name !== project()?.name) return;
      displayDebug(response);
      await pollDebug();
    } catch (error) { $('debug-status').textContent = error.message; }
  };
  $('debug-stop').onclick = async () => {
    clearTimeout(debugTimer);
    const generation = ++debugGeneration, name = debugPlugin;
    try { if (name) { const response = await context.request('debug', 'POST', {name, enabled: false}); if (generation === debugGeneration && name === project()?.name) displayDebug(response); } }
    catch (error) { $('debug-status').textContent = error.message; }
    workspace()?.highlightBlock(null);
  };
  $('debug-clear').onclick = () => { $('debug-events').replaceChildren(); $('debug-values').textContent = 'No step selected.'; workspace()?.getAllBlocks(false).forEach(block => block.setWarningText(null, 'runtime')); workspace()?.highlightBlock(null); };
  $('debug-hide').onclick = () => { $('debug-panel').hidden = true; clearTimeout(debugTimer); };

  $('open-menu-designer').onclick = safe('menu-error', () => { blockMode(); renderMenus(); $('menu-designer').showModal(); });
  $('open-state-editor').onclick = safe('state-error', () => { blockMode(); renderStates(); $('state-editor').showModal(); });
  $('open-block-library').onclick = safe('library-error', async () => { blockMode(); selectedLibraryBlock = Blockly.getSelected()?.id; $('block-library').showModal(); await loadLibrary(); });
  $('open-data-inspector').onclick = safe('data-error', async () => { $('data-inspector').showModal(); dataOffset = 0; await inspectData(); });
  $('open-debugger').onclick = () => { ++debugGeneration; $('debug-panel').hidden = false; if (debugPlugin !== project()?.name) { debugPlugin = project()?.name; debugCursor = 0; debugSeen.clear(); $('debug-events').replaceChildren(); } pollDebug(); };
  return {
    mode() { for (const id of ['open-menu-designer', 'open-state-editor', 'open-block-library']) $(id).disabled = project()?.mode !== 'blocks'; },
    refresh() {
      clearTimeout(debugTimer); ++debugGeneration; debugSeen.clear();
      if (debugPlugin && debugPlugin !== project()?.name && debugEnabled) context.request('debug', 'POST', {name: debugPlugin, enabled: false}).catch(() => {});
      debugPlugin = project()?.name; debugCursor = 0; debugEnabled = false; $('debug-events').replaceChildren(); $('debug-panel').hidden = true;
      this.mode();
      context.request('integrations').then(status => { $('vault-status').textContent = status.vault ? `Vault · ${status.provider}` : 'Vault · economy unavailable'; }).catch(() => { $('vault-status').textContent = 'Vault · status unavailable'; });
    },
  };
}
