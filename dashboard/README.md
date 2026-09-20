# MC-JS Plugin Studio

## Setup

The MC-JS JAR includes the web server, CodeMirror and Blockly. Editor scripts are served locally, without a CDN. No separate Node process is required on the Minecraft server.

1. Copy the JAR into `plugins/` and start Paper using Java 25.
2. Configure the dashboard address in `plugins/MC-JS/config.yml`.
3. Grant trusted administrators `mcjs.dashboard`. Operators and `mcjs.admin` have this permission by default. It permits writing and executing server code.
4. Run `/mjs dashboard` in game and open the one-time link.
5. Run the browser's `/mjs dashboard <verify-code>` command with the same player.

The default listener is `127.0.0.1:8765`, accessible only on the server computer. For remote access, place an HTTPS reverse proxy in front of this port and set its address as `public-url`:

```yaml
dashboard:
  enabled: true
  bind: "127.0.0.1"
  port: 8765
  public-url: "https://mcjs.example.com"
  verify-timeout-seconds: 300
  session-minutes: 30
```

Example Caddy configuration on the same machine:

```caddy
mcjs.example.com {
    reverse_proxy 127.0.0.1:8765
}
```

The proxy must preserve the original `Host` header. Use a dedicated domain without a URL path prefix. HTTP is intended for local development. For a direct test on a private LAN, set `bind` to `0.0.0.0` and `public-url` to `http://SERVER-IP:8765`, with the port accessible to clients. Use HTTPS for a public installation because the dashboard manages executable server code.

`/jsconfig reload` applies configuration changes and restarts the dashboard, ending current sessions. Requesting a new `/mjs dashboard` link, signing out, restarting the server or leaving the game also revokes access. Every protected request checks online status and permissions on the server thread. Links and pending verifications expire after five minutes by default; verified sessions expire after 30 minutes. Verification timeout accepts 30–900 seconds, and session duration accepts 1–240 minutes.

## Editor

- Create, search, open, save, start, stop and delete plugins.
- JavaScript editing with syntax highlighting, line numbers, search, indentation, undo/redo, API suggestions and Ctrl+S.
- Syntax checking with the same Rhino version used by the server. Checking and saving never execute source code. Startup errors appear in the activity panel; further callback output appears in the server console.
- Blockly with lifecycle events, player events, commands and permissions, messages, titles, items, player values, sounds, teleportation, timers, conditions, loops, variables, functions, text, lists and persistent configuration values.
- Complete, editable block systems for a coins shop, daily kit, mining quest and player homes, plus welcome messages, commands and empty projects.
- Block search, a flow navigator, automatic arrangement, live connection diagnostics and a step-by-step builder guide.
- JavaScript import/export and `.mcjs.json` import/export for complete block projects.

A block project's Code tab initially displays a read-only preview. Choose "Edit code" to make the generated JavaScript editable. Arbitrary JavaScript cannot be converted back into blocks automatically; export the block project before switching if you want to keep it.

Plugins remain individual `.js` files in `plugins/MC-JS/js-plugins/`. Block data is stored in `.dashboard/<name>.json` alongside them. If the JavaScript file changes externally, the dashboard opens it as source code so stale blocks cannot overwrite the changes. Filenames support 1–64 letters, numbers, hyphens or underscores and must start with a letter or number. The editor supports up to 512 KiB of JavaScript and 1 MiB of block data. Existing plugins with other filenames must be renamed before editing them in the dashboard.

Saving writes a file without changing its running instance. "Save & start" validates and saves the draft, then reloads that plugin. If `onEnable` fails, resources registered through the MC-JS API are cleaned up and the error is shown. The previous instance is stopped during reload and is not automatically restored after a runtime error. World changes made by plugin code cannot be rolled back automatically.

Stopping lasts until the next load or server restart. To disable plugins persistently, use `plugins.disabled-plugins` or `settings.enable-example-plugin`; the dashboard respects these settings. Concurrent writes to changed source code produce a conflict error. Export your draft and reopen the current file before continuing.

## Building complete systems

Choose a system card on the start screen or a System starting point in New plugin. Every template uses normal editable blocks, including its commands and reusable functions. Use Jump to a flow to navigate larger projects at a readable zoom. Fit all shows the full system, and Arrange separates the flows.

- **Coins shop:** `/shop` opens a 3-row menu and `/coins` shows the balance. The first join handled by this plugin grants 100 coins. A diamond costs 10. Purchases check funds and inventory capacity; failed grants restore the balance and inventory. Rejoin after initially starting the template to receive the welcome bonus.
- **Daily kit:** `/kit` claims the kit directly; `/kits` opens its menu. Both call the same reusable function. It contains bread, an iron sword and torches. The 24-hour cooldown survives restarts. Full inventories keep the kit available.
- **Mining quest:** `/quest` displays progress and a sidebar. Ten successful stone breaks unlock a diamond and 50 quest coins. Cancelled breaks are ignored and completion is saved per player. If the inventory is full, make space and mine another stone to claim the reward.
- **Player homes:** `/sethome` saves the current world, coordinates and facing direction; `/home` returns there with a 10-second cooldown. Missing worlds are handled as unavailable homes.

The building blocks are composable: commands and subcommands, conditions, loops, event properties, player selection, reusable functions with input data and return values, lists, records, persistent per-player/global values, purchases, kit lists, cooldowns, menu buttons, multi-line sidebars and locations. A menu button can open another menu or call any reusable function.

Player data uses UUIDs and survives restarts in `configs/<plugin-id>.yml`. Global data is shared inside one plugin. **Each plugin has its own data and currency.** The quest coins and shop coins are separate when created as separate plugins; to share them, build both flows in the same block project with the same `coins` key. This storage is not a Vault economy. Ordinary Blockly variables are temporary and shared; use saved player data for player-specific progress and Function input data for call-specific values.

Commands have a permission field. Timers do not supply a player automatically: use For each online player or As player. For mob events, extract the entity's killer and run actions as that player. Observation events ignore cancelled actions and must not modify the event. Async chat actions are scheduled on the next server tick; they cannot cancel or modify the original chat event. Missing event properties return null. Callbacks and owned menus are removed when the plugin unloads.

Menu slots start at 0, with 9 slots per row. Menu button actions receive the clicking player and run on the next server tick. Inventory movement is blocked while an owned menu is open. Kit and purchase operations restore items on ordinary operation failures; filesystem data and Minecraft player saves are not one crash-atomic transaction. Use conditions and saved flags for progression; combine Buy item or Claim kit with the success branch for item rewards.

The Check action validates block structure and JavaScript syntax. It cannot prove a custom system's gameplay behavior. Test new systems on a development server before using them in your main world. Callback runtime errors appear in the Minecraft console.

## Development and tests

```sh
cd dashboard
npm ci
npm test
npm run build
cd ..
mvn clean package
```

Bundled assets are included in `src/main/resources/dashboard/`, so Maven can build the plugin without Node. Run `npm run build` after frontend changes. CI rebuilds both parts and runs their tests.

`node dashboard/remove-comments.mjs` removes comments from project Java, JavaScript, CSS, HTML, YAML and XML source and from JavaScript examples in Markdown. Dependencies and license files are preserved. Third-party licenses are included in `src/main/resources/dashboard/THIRD-PARTY-LICENSES.txt`.

Java tests cover sessions, HTTP access control, permission revocation, file paths, size limits, syntax errors, write conflicts and plugin resource cleanup. Frontend tests generate executable code from blocks and check events, command permissions, timers and lifecycle functions. The test-only `DashboardPreview` class exposes the same HTTP layer for local UI verification; Minecraft actions are simulated there. It is not included in the plugin JAR.

The editor integration uses [CodeMirror](https://codemirror.net/examples/basic/) and [Blockly JSON serialization](https://developers.google.com/blockly/guides/configure/web/serialization).
