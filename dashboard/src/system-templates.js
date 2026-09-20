const block = (type, fields = {}, inputs = {}) => ({type, fields, inputs: Object.fromEntries(Object.entries(inputs).map(([key, value]) => [key, {block: value}]))});
const text = value => block('text', {TEXT: value});
const number = value => block('math_number', {NUM: value});
const current = () => block('mjs_current_player');
const sequence = (...items) => {
  for (let index = 0; index < items.length - 1; index++) items[index].next = {block: items[index + 1]};
  return items[0];
};
const join = (...items) => ({...block('text_join', {}, Object.fromEntries(items.map((item, index) => ['ADD' + index, item]))), extraState: {itemCount: items.length}});
const message = value => block('mjs_message', {}, {TEXT: typeof value === 'string' ? text(value) : value});
const item = (material, amount = 1, name) => {
  const stack = block('mjs_item_stack', {MATERIAL: material}, {AMOUNT: number(amount)});
  return name ? block('mjs_item_name', {NAME: name}, {ITEM: stack}) : stack;
};
const read = (key, fallback = 0) => block('mjs_data_get', {SCOPE: 'player'}, {KEY: text(key), DEFAULT: typeof fallback === 'number' ? number(fallback) : fallback});
const write = (key, value) => block('mjs_data_set', {SCOPE: 'player'}, {KEY: text(key), VALUE: value});
const change = (key, amount) => block('mjs_data_change', {SCOPE: 'player'}, {KEY: text(key), AMOUNT: number(amount)});
const compare = (left, op, right) => block('logic_compare', {OP: op}, {A: left, B: right});
const iff = (condition, yes, no) => ({...block('controls_if', {}, {IF0: condition, DO0: yes, ...(no ? {ELSE: no} : {})}), ...(no ? {extraState: {hasElse: true}} : {})});
const call = name => block('mjs_call', {NAME: name}, {PLAYER: current()});
const fn = (name, body) => block('mjs_function', {NAME: name}, {DO: body});
const command = (name, body) => block('mjs_command', {NAME: name, PERMISSION: ''}, {DO: body});
const button = (slot, icon, body) => block('mjs_menu_button', {}, {SLOT: number(slot), ITEM: icon, DO: body});
const menu = (title, contents) => block('mjs_menu', {ROWS: 3}, {TITLE: text(title), DO: contents});
const list = (...items) => ({...block('lists_create_with', {}, Object.fromEntries(items.map((item, index) => ['ADD' + index, item]))), extraState: {itemCount: items.length}});

export const systemTemplates = [
  {id: 'vaultshop', title: 'Vault economy shop', description: 'A real-money shop connected to your server economy, with balance display, provider checks and purchase failure actions.', commands: '/vshop, /money', icon: '$', features: ['Vault economy', 'Visual menu', 'Purchase recovery']},
  {id: 'shop', title: 'Coins shop', description: 'A clickable shop, persistent player balances, a first-join bonus and purchases that check funds and inventory space.', commands: '/shop, /coins', icon: '◇', features: ['Menu buttons', 'Per-player data', 'Safe purchases']},
  {id: 'kit', title: 'Daily kit', description: 'A reusable kit action, a claim menu and a 24-hour cooldown that survives restarts. Full inventories keep the reward available.', commands: '/kit, /kits', icon: '▣', features: ['Reusable actions', 'Persistent cooldowns', 'Item lists']},
  {id: 'quest', title: 'Mining quest', description: 'Mine 10 stone blocks, track progress per player, receive a one-time reward and view progress on a sidebar.', commands: '/quest', icon: '⚒', features: ['Event data', 'Conditions', 'One-time rewards']},
  {id: 'homes', title: 'Player homes', description: 'Save a personal home with world, coordinates and facing direction, then teleport back with a cooldown.', commands: '/sethome, /home', icon: '⌂', features: ['Location records', 'Saved data', 'Cooldowns']},
];

export function systemTemplate(kind) {
  let roots;
  if (kind === 'shop') {
    roots = [
      fn('open_shop', menu('&6Coin Shop', sequence(
        button(11, item('DIAMOND', 1, '&bDiamond &7— 10 coins'), block('mjs_purchase', {}, {
          ITEM: item('DIAMOND'), COST: number(10), KEY: text('coins'), DO: message('&aPurchased one diamond!'),
          ELSE: message(join(text('&cPurchase failed: '), block('mjs_purchase_result'))),
        })),
        button(15, item('GOLD_INGOT', 1, '&eYour balance'), message(join(text('&eCoins: '), read('coins')))),
        block('mjs_menu_fill', {}, {ITEM: item('GRAY_STAINED_GLASS_PANE', 1, ' ')}),
      ))),
      command('shop', call('open_shop')),
      command('coins', message(join(text('&eCoins: '), read('coins')))),
      block('mjs_event', {EVENT: 'player.PlayerJoinEvent'}, {DO: iff(compare(read('coins', -1), 'EQ', number(-1)), sequence(write('coins', number(100)), message('&6Welcome! You received 100 coins. Use /shop.')))}),
    ];
  } else if (kind === 'vaultshop') {
    const balance = () => block('mjs_vault_format', {}, {AMOUNT: block('mjs_vault_balance', {}, {PLAYER: current()})});
    const available = action => iff(block('mjs_vault_available'), action, message('&cVault economy is unavailable. Ask an administrator to install Vault and an economy provider.'));
    roots = [
      fn('open_vault_shop', available(menu('&6Economy Shop', sequence(
        button(11, item('DIAMOND', 1, '&bBuy a diamond'), block('mjs_vault_purchase', {}, {
          PLAYER: current(), ITEM: item('DIAMOND'), AMOUNT: number(10), DO: message('&aPurchased one diamond!'),
          ELSE: message(join(text('&cPurchase failed: '), block('mjs_vault_result', {FIELD: 'getError'}))),
        })),
        button(15, item('GOLD_INGOT', 1, '&eYour balance'), message(join(text('&eBalance: '), balance()))),
        block('mjs_menu_fill', {}, {ITEM: item('GRAY_STAINED_GLASS_PANE', 1, ' ')}),
      )))),
      command('vshop', call('open_vault_shop')),
      command('money', available(message(join(text('&eBalance: '), balance())))),
    ];
  } else if (kind === 'kit') {
    roots = [
      fn('claim_daily_kit', block('mjs_kit', {}, {
        KEY: text('daily-kit'), SECONDS: number(86400), ITEMS: list(item('BREAD', 16), item('IRON_SWORD'), item('TORCH', 32)),
        DO: message('&aYour daily kit is ready!'), ELSE: iff(compare(block('mjs_kit_result'), 'EQ', text('space')),
          message('&cMake room in your inventory and try again.'),
          message(join(text('&eTry again in '), block('mjs_cooldown_remaining', {}, {KEY: text('daily-kit')}), text(' seconds.')))),
      })),
      command('kit', call('claim_daily_kit')),
      command('kits', menu('&6Daily rewards', button(13, item('CHEST', 1, '&aClaim daily kit'), call('claim_daily_kit')))),
    ];
  } else if (kind === 'quest') {
    const complete = () => read('quest-complete', block('logic_boolean', {BOOL: 'FALSE'}));
    roots = [
      block('mjs_observe_event', {EVENT: 'block.BlockBreakEvent'}, {DO: iff(
        block('logic_operation', {OP: 'AND'}, {
          A: compare(block('mjs_object_property', {GETTER: 'getType'}, {OBJECT: block('mjs_event_value', {GETTER: 'getBlock'})}), 'EQ', text('STONE')),
          B: block('logic_negate', {}, {BOOL: complete()}),
        }), sequence(change('stone-mined', 1), iff(compare(read('stone-mined'), 'GTE', number(10)), block('mjs_kit', {}, {
          KEY: text('quest-reward'), SECONDS: number(0), ITEMS: list(item('DIAMOND')),
          DO: sequence(write('quest-complete', block('logic_boolean', {BOOL: 'TRUE'})), change('coins', 50), message('&aQuest complete! You earned a diamond and 50 quest coins.')),
          ELSE: message('&eMake inventory space, then mine one more stone to claim your reward.'),
        }))))}),
      fn('show_quest', sequence(message(join(text('&eStone mined: '), read('stone-mined'), text('/10'))), block('mjs_sidebar_board', {}, {
        TITLE: text('&6Stone Miner'), DO: sequence(
          block('mjs_sidebar_line', {}, {TEXT: join(text('Stone: '), read('stone-mined'), text('/10')), SCORE: number(3)}),
          block('mjs_sidebar_line', {}, {TEXT: join(text('Completed: '), complete()), SCORE: number(2)}),
          block('mjs_sidebar_line', {}, {TEXT: join(text('Quest coins: '), read('coins')), SCORE: number(1)}),
        ),
      }))),
      command('quest', call('show_quest')),
    ];
  } else if (kind === 'homes') {
    const destination = () => block('mjs_record_location', {}, {RECORD: read('home', block('logic_null'))});
    roots = [
      command('sethome', sequence(write('home', block('mjs_location_record', {}, {LOCATION: block('mjs_player_location')})), message('&aHome saved. Use /home to return.'))),
      fn('go_home', iff(compare(destination(), 'NEQ', block('logic_null')), block('mjs_cooldown', {}, {
        KEY: text('home'), SECONDS: number(10), DO: sequence(block('mjs_teleport_location', {}, {LOCATION: destination()}), message('&aWelcome home.')),
        ELSE: message('&ePlease wait before teleporting again.'),
      }), message('&cNo available home. Use /sethome first.'))),
      command('home', call('go_home')),
    ];
  } else return null;
  return {blocks: {languageVersion: 0, blocks: roots.map((root, index) => ({...root, x: 40 + index % 2 * 700, y: 40 + Math.floor(index / 2) * 700}))}, systemTemplate: kind};
}
