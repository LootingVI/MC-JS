export const studioRuntime = `function mcjsStatePlayer(player, scope) {
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
`;

export const studioRoots = ['mjs_machine'];

export function registerStudioBlocks(Blockly, generator, Order, toolbox) {
  const input = (name, check) => ({type: 'input_value', name, ...(check ? {check} : {})});
  const field = (name, text) => ({type: 'field_input', name, text});
  const statement = {previousStatement: null, nextStatement: null};
  const body = (name, check) => ({type: 'input_statement', name, ...(check ? {check} : {})});
  const scope = () => ({type: 'field_dropdown', name: 'SCOPE', options: [['this player', 'player'], ['the whole plugin', 'global']]});
  const define = (type, message0, args0, options = {}) => ({type, message0, args0, colour: 265, tooltip: message0.replace(/%\d+/g, '…'), ...options});
  Blockly.defineBlocksWithJsonArray([
    define('mjs_machine', 'State machine %1 initial state %2 %3', [field('NAME', 'game'), field('INITIAL', 'Lobby'), body('DO', 'StateContent')]),
    define('mjs_state', 'State %1 on enter %2 on exit %3', [field('NAME', 'Lobby'), body('DO'), body('EXIT')], {previousStatement: 'StateContent', nextStatement: 'StateContent'}),
    define('mjs_transition', 'From %1 on event %2 go to %3 if %4', [field('FROM', 'Lobby'), field('EVENT', 'next'), field('TO', 'Countdown'), input('IF', 'Boolean')], {previousStatement: 'StateContent', nextStatement: 'StateContent'}),
    define('mjs_state_start', 'Start machine %1 for %2 with data %3', [field('NAME', 'game'), scope(), input('DATA')], statement),
    define('mjs_state_stop', 'Stop machine %1 for %2 with data %3', [field('NAME', 'game'), scope(), input('DATA')], statement),
    define('mjs_state_send', 'Send event %1 to machine %2 for %3 with data %4', [input('EVENT', 'String'), field('NAME', 'game'), scope(), input('DATA')], statement),
    define('mjs_state_current', 'current state of %1 for %2', [field('NAME', 'game'), scope()], {output: 'String'}),
    define('mjs_state_after', 'In machine %1 after %2 seconds send %3 with data %4', [field('NAME', 'game'), input('SECONDS', 'Number'), input('EVENT', 'String'), input('DATA')], statement),
    define('mjs_vault_available', 'Vault economy is available', [], {output: 'Boolean', colour: 150}),
    define('mjs_vault_provider', 'Vault economy provider', [], {output: 'String', colour: 150}),
    define('mjs_vault_balance', 'Vault balance of %1', [input('PLAYER', 'Player')], {output: 'Number', colour: 150}),
    define('mjs_vault_format', 'format Vault money %1', [input('AMOUNT', 'Number')], {output: 'String', colour: 150}),
    define('mjs_vault_has', 'player %1 has Vault money %2', [input('PLAYER', 'Player'), input('AMOUNT', 'Number')], {output: 'Boolean', colour: 150}),
    define('mjs_vault_deposit', 'Deposit Vault money %1 to %2 on success %3 otherwise %4', [input('AMOUNT', 'Number'), input('PLAYER', 'Player'), body('DO'), body('ELSE')], {...statement, colour: 150}),
    define('mjs_vault_withdraw', 'Withdraw Vault money %1 from %2 on success %3 otherwise %4', [input('AMOUNT', 'Number'), input('PLAYER', 'Player'), body('DO'), body('ELSE')], {...statement, colour: 150}),
    define('mjs_vault_transfer', 'Transfer Vault money %1 from %2 to %3 on success %4 otherwise %5', [input('AMOUNT', 'Number'), input('PLAYER', 'Player'), input('TARGET', 'Player'), body('DO'), body('ELSE')], {...statement, colour: 150}),
    define('mjs_vault_purchase', 'Buy %1 for Vault money %2 for %3 on success %4 otherwise %5', [input('ITEM', 'Item'), input('AMOUNT', 'Number'), input('PLAYER', 'Player'), body('DO'), body('ELSE')], {...statement, colour: 150}),
    define('mjs_vault_result', 'Vault transaction %1', [{type: 'field_dropdown', name: 'FIELD', options: [['status', 'getCode'], ['error', 'getError'], ['balance', 'getBalance'], ['amount', 'getAmount']]}], {output: null, colour: 150}),
    define('mjs_vault_group', 'Vault primary group of %1', [input('PLAYER', 'Player')], {output: 'String', colour: 150}),
    define('mjs_vault_prefix', 'Vault chat prefix of %1', [input('PLAYER', 'Player')], {output: 'String', colour: 150}),
    define('mjs_debug_watch', 'Debug value %1 named %2', [input('VALUE'), field('NAME', 'value')], {...statement, colour: 15}),
  ]);
  generator.addReservedWords('stateMachine,mcjsStatePlayer,mcjsDebugJson,mcjsStep,mcjsError,mcjsCaught,mcjsReportedError,mcjsHasReportedError,vaultResult');
  const g = generator.forBlock, q = JSON.stringify;
  const value = (block, gen, name, fallback = 'null') => gen.valueToCode(block, name, Order.NONE) || fallback;
  const bodyCode = (block, gen, name = 'DO') => gen.statementToCode(block, name);
  const player = '(typeof player !== "undefined" ? player : null)';
  const target = (block, gen) => value(block, gen, 'PLAYER', player);
  const owner = block => `mcjsStatePlayer(${player}, ${q(block.getFieldValue('SCOPE'))})`;
  const expr = code => [code, Order.FUNCTION_CALL];
  g.mjs_machine = (block, gen) => `(function(stateMachine) {\n${bodyCode(block, gen)}})(api.states.create(${q(block.getFieldValue('NAME'))}, ${q(block.getFieldValue('INITIAL'))}));\n`;
  g.mjs_state = (block, gen) => `stateMachine.state(${q(block.getFieldValue('NAME'))}, function(player, data) {\n${bodyCode(block, gen)}}, function(player, data) {\n${bodyCode(block, gen, 'EXIT')}});\n`;
  g.mjs_transition = (block, gen) => `stateMachine.transition(${q(block.getFieldValue('FROM'))}, ${q(block.getFieldValue('EVENT'))}, ${q(block.getFieldValue('TO'))}, function(player, data) { return ${value(block, gen, 'IF', 'true')}; });\n`;
  for (const method of ['start', 'stop']) g['mjs_state_' + method] = (block, gen) => `api.states.${method}(${q(block.getFieldValue('NAME'))}, ${owner(block)}, ${value(block, gen, 'DATA')});\n`;
  g.mjs_state_send = (block, gen) => `api.states.send(${q(block.getFieldValue('NAME'))}, ${owner(block)}, String(${value(block, gen, 'EVENT', '"next"')}), ${value(block, gen, 'DATA')});\n`;
  g.mjs_state_current = block => expr(`String(api.states.current(${q(block.getFieldValue('NAME'))}, ${owner(block)}))`);
  g.mjs_state_after = (block, gen) => `api.states.after(${q(block.getFieldValue('NAME'))}, ${player}, Number(${value(block, gen, 'SECONDS', '10')}), String(${value(block, gen, 'EVENT', '"next"')}), ${value(block, gen, 'DATA')});\n`;
  g.mjs_vault_available = () => expr('api.vault.isAvailable()');
  g.mjs_vault_provider = () => expr('String(api.vault.getProviderName())');
  g.mjs_vault_balance = (block, gen) => expr(`api.vault.balance(${target(block, gen)})`);
  g.mjs_vault_format = (block, gen) => expr(`String(api.vault.format(Number(${value(block, gen, 'AMOUNT', '0')})))`);
  g.mjs_vault_has = (block, gen) => expr(`api.vault.has(${target(block, gen)}, Number(${value(block, gen, 'AMOUNT', '1')}))`);
  for (const method of ['deposit', 'withdraw', 'transfer', 'purchase']) g['mjs_vault_' + method] = (block, gen) => {
    const amount = `Number(${value(block, gen, 'AMOUNT', '10')})`;
    const args = method === 'transfer' ? `${target(block, gen)}, ${value(block, gen, 'TARGET')}, ${amount}` : `${target(block, gen)}, ${amount}${method === 'purchase' ? ', ' + value(block, gen, 'ITEM') : ''}`;
    return `(function(vaultResult) { if (vaultResult.isSuccess()) {\n${bodyCode(block, gen)}} else {\n${bodyCode(block, gen, 'ELSE')}} })(api.vault.${method}(${args}));\n`;
  };
  g.mjs_vault_result = block => expr(`(typeof vaultResult !== "undefined" ? ${['getCode', 'getError'].includes(block.getFieldValue('FIELD')) ? 'String(vaultResult.' + block.getFieldValue('FIELD') + '())' : 'vaultResult.' + block.getFieldValue('FIELD') + '()'} : null)`);
  g.mjs_vault_group = (block, gen) => expr(`String(api.vault.getPrimaryGroup(${target(block, gen)}))`);
  g.mjs_vault_prefix = (block, gen) => expr(`String(api.vault.getPrefix(${target(block, gen)}))`);
  g.mjs_debug_watch = (block, gen) => `mcjsStep(${q(block.id)}, function() { return {${q(block.getFieldValue('NAME'))}: ${value(block, gen, 'VALUE')}}; });\n`;
  const num = number => ({shadow: {type: 'math_number', fields: {NUM: number}}});
  const text = value => ({shadow: {type: 'text', fields: {TEXT: value}}});
  const current = () => ({block: {type: 'mjs_current_player'}});
  const defaults = type => type.startsWith('mjs_vault') ? {PLAYER: current(), AMOUNT: num(10), ...(type === 'mjs_vault_purchase' ? {ITEM: {block: {type: 'mjs_item_stack', fields: {MATERIAL: 'DIAMOND'}, inputs: {AMOUNT: num(1)}}}} : {})} : {EVENT: text('next'), SECONDS: num(10)};
  for (const [name, colour, types] of [
    ['State machines', '#ad83e6', ['mjs_machine', 'mjs_state', 'mjs_transition', 'mjs_state_start', 'mjs_state_send', 'mjs_state_current', 'mjs_state_after', 'mjs_state_stop']],
    ['Vault', '#70bc86', ['mjs_vault_available', 'mjs_vault_provider', 'mjs_vault_balance', 'mjs_vault_format', 'mjs_vault_has', 'mjs_vault_deposit', 'mjs_vault_withdraw', 'mjs_vault_transfer', 'mjs_vault_purchase', 'mjs_vault_result', 'mjs_vault_group', 'mjs_vault_prefix']],
    ['Debugging', '#e5a17a', ['mjs_debug_watch']],
  ]) toolbox.contents.push({kind: 'category', name, colour, contents: types.map(type => {
    const sample = new Blockly.Workspace();
    try {
      const block = sample.newBlock(type);
      return {kind: 'block', type, inputs: Object.fromEntries(Object.entries(defaults(type)).filter(([key]) => block.getInput(key)))};
    } finally { sample.dispose(); }
  })});
}

export function instrumentBlocks(Blockly, generator) {
  for (const [type, handler] of Object.entries(generator.forBlock)) {
    generator.forBlock[type] = function(block, gen) {
      const code = handler.call(this, block, gen);
      if (Array.isArray(code) || !code || ['mjs_machine', 'mjs_state', 'mjs_transition', 'mjs_function'].includes(type) || type.startsWith('procedures_def')) return code;
      const vars = block.workspace.getVariableMap().getAllVariables().slice(0, 20).map(variable => {
        const name = gen.nameDB_.getName(variable.getId(), Blockly.Names.NameType.VARIABLE);
        return JSON.stringify(variable.name) + ': (typeof ' + name + ' === "undefined" ? null : ' + name + ')';
      });
      const snapshot = `function() { return {player: typeof player !== "undefined" && player ? String(player.getName()) : null, data: typeof data !== "undefined" ? data : null, variables: {${vars.join(',')}}}; }`;
      return `mcjsStep(${JSON.stringify(block.id)}, ${snapshot});\ntry {\n${code}} catch (mcjsCaught) { mcjsError(${JSON.stringify(block.id)}, mcjsCaught, ${snapshot}); throw mcjsCaught; }\n`;
    };
  }
}

export function diagnoseStudio(workspace) {
  const problems = [], machines = new Map();
  const active = block => block.isEnabled() && !block.getInheritedDisabled();
  const parent = (block, types) => { for (let p = block.getSurroundParent(); p; p = p.getSurroundParent()) if (types.includes(p.type)) return p; return null; };
  const error = (block, message) => problems.push({id: block.id, message});
  const valid = name => /^[A-Za-z][A-Za-z0-9_-]{0,47}$/.test(name);
  for (const machine of workspace.getBlocksByType('mjs_machine', false).filter(active)) {
    const name = machine.getFieldValue('NAME');
    if (!valid(name) || machines.has(name)) error(machine, 'State machines need unique names starting with a letter.');
    machines.set(name, machine);
    const contents = machine.getDescendants(false).filter(block => active(block) && parent(block, ['mjs_machine']) === machine);
    const states = new Set();
    for (const state of contents.filter(block => block.type === 'mjs_state')) {
      const name = state.getFieldValue('NAME');
      if (!valid(name) || states.has(name)) error(state, 'States need unique names starting with a letter.');
      states.add(name);
    }
    if (!states.has(machine.getFieldValue('INITIAL'))) error(machine, 'Choose an initial state that exists in this machine.');
    for (const transition of contents.filter(block => block.type === 'mjs_transition')) {
      if (!states.has(transition.getFieldValue('FROM')) || !states.has(transition.getFieldValue('TO'))) error(transition, 'Both transition states must exist.');
      if (!valid(transition.getFieldValue('EVENT'))) error(transition, 'Transition event names must start with a letter.');
    }
  }
  for (const block of workspace.getAllBlocks(false).filter(active)) {
    if (['mjs_state', 'mjs_transition'].includes(block.type) && !parent(block, ['mjs_machine'])) error(block, 'Put states and transitions inside a State machine block.');
    if (['mjs_state_start', 'mjs_state_send', 'mjs_state_stop', 'mjs_state_current', 'mjs_state_after'].includes(block.type) && !machines.has(block.getFieldValue('NAME'))) error(block, 'Create the referenced state machine first.');
    if (block.type === 'mjs_vault_result' && !parent(block, ['mjs_vault_purchase', 'mjs_vault_deposit', 'mjs_vault_withdraw', 'mjs_vault_transfer'])) error(block, 'Vault transaction values belong inside a Vault transaction block.');
  }
  return problems;
}
