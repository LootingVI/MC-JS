import {EditorView, basicSetup} from 'codemirror';
import {EditorState, Compartment} from '@codemirror/state';
import {keymap} from '@codemirror/view';
import {indentWithTab} from '@codemirror/commands';
import {javascript} from '@codemirror/lang-javascript';
import {oneDark} from '@codemirror/theme-one-dark';
import {autocompletion} from '@codemirror/autocomplete';
import {Blockly, toolbox, generate, starter, diagnose} from './blocks.js';
import {systemTemplates} from './system-templates.js';
import {apiDocs, categories} from './docs.js';
import {initStudioTools} from './studio-tools.js';
import './style.css';

const $ = id => document.getElementById(id);
let session, workspace, editor, current, plugins = [], dirty = false, loading = false, busy = false, pollTimer;
let activeTab = 'blocks';
let imported = null;
let diagnosticsTimer, selectedTemplate = null;
const readOnly = new Compartment();
const studioTools = initStudioTools({workspace: () => workspace, project: () => current, dirty: () => dirty, request, changed, log, focusBlock, showBlocks: () => renderTab('blocks')});
const docEntries = new Map(apiDocs.map(doc => [doc.label, doc]));
const completionOptions = apiDocs.map(doc => ({label: doc.label, type: 'function',
  detail: doc.signature, info: doc.info}));

async function request(path, method = 'GET', body) {
  const response = await fetch(`/api/${path}`, {
    method, credentials: 'same-origin', cache: 'no-store',
    headers: {'Content-Type': 'application/json', ...(session?.csrf ? {'X-MCJS-CSRF': session.csrf} : {})},
    ...(body === undefined ? {} : {body: JSON.stringify(body)}),
  });
  const data = await response.json();
  if (!response.ok) {
    const error = new Error(data.error || `Request failed (${response.status}).`);
    error.status = response.status;
    throw error;
  }
  return data;
}

function log(message, error = false) {
  if (error && $('project').hidden) $('session-label').textContent = message;
  const line = document.createElement('div');
  line.className = error ? 'log-error' : 'log-entry';
  const time = document.createElement('time');
  time.textContent = new Date().toLocaleTimeString('en-GB');
  const text = document.createElement('span');
  text.textContent = message;
  line.append(time, text);
  $('activity').prepend(line);
  while ($('activity').children.length > 100) $('activity').lastChild.remove();
}

function changed() {
  if (loading || !current) return;
  dirty = true;
  $('dirty-dot').hidden = false;
}

function confirmAction(title, text, accept = 'Continue') {
  return new Promise(resolve => {
    $('confirm-title').textContent = title;
    $('confirm-text').textContent = text;
    $('confirm-accept').textContent = accept;
    $('confirm-dialog').returnValue = 'cancel';
    $('confirm-dialog').addEventListener('close', () => resolve($('confirm-dialog').returnValue === 'ok'), {once: true});
    $('confirm-dialog').showModal();
  });
}

async function mayLeave() {
  return !dirty || await confirmAction('Unsaved changes', 'Your draft is not saved yet. Discard your changes?', 'Discard');
}

function initEditors() {
  if (editor) return;
  editor = new EditorView({
    parent: $('code-editor'),
    extensions: [basicSetup, javascript(), oneDark, keymap.of([indentWithTab, {
      key: 'Mod-s', run: () => { perform(() => save(false)); return true; },
    }]), readOnly.of(EditorState.readOnly.of(false)), autocompletion({override: [context => {
      const word = context.matchBefore(/[\w.]+/);
      if (!word || (!context.explicit && word.from === word.to)) return null;
      const prefix = word.text.toLowerCase();
      const options = completionOptions.filter(option => option.label.toLowerCase().startsWith(prefix) || option.label.toLowerCase().includes(prefix)).slice(0, 60);
      return {from: word.from, options};
    }]}), EditorView.updateListener.of(update => { if (update.docChanged) changed(); })],
  });
  workspace = Blockly.inject($('block-editor'), {
    toolbox, media: '/media/', sounds: false, trashcan: true,
    grid: {spacing: 24, length: 2, colour: '#354040', snap: true},
    zoom: {controls: true, wheel: true, startScale: 0.9, maxScale: 1.5, minScale: 0.4},
    move: {scrollbars: true, drag: true, wheel: true},
    theme: Blockly.Theme.defineTheme('mcjs', {
      base: Blockly.Themes.Classic,
      componentStyles: {workspaceBackgroundColour: '#141b20', toolboxBackgroundColour: '#182128',
        toolboxForegroundColour: '#d5e2dd', flyoutBackgroundColour: '#202c32', flyoutForegroundColour: '#edf4f0',
        flyoutOpacity: 0.98, scrollbarColour: '#56646a', insertionMarkerColour: '#c2f479', insertionMarkerOpacity: 0.4},
      fontStyle: {family: 'system-ui, sans-serif', size: 11},
    }),
  });
  workspace.addChangeListener(event => {
    if (event.isUiEvent) return;
    changed();
    clearTimeout(diagnosticsTimer);
    diagnosticsTimer = setTimeout(renderDiagnostics, 200);
  });
  new ResizeObserver(() => { if (!$('block-editor').hidden) Blockly.svgResize(workspace); }).observe($('block-editor'));
}

function setCode(source) {
  const before = loading;
  loading = true;
  editor.dispatch({changes: {from: 0, to: editor.state.doc.length, insert: source}});
  loading = before;
}

function metadata() {
  return {id: current.name, name: $('meta-name').value || current.name,
    version: $('meta-version').value || '1.0.0', author: $('meta-author').value};
}

function source() {
  return current.mode === 'blocks' ? generate(workspace, metadata()) : editor.state.doc.toString();
}

function blockProject() {
  return {...Blockly.serialization.workspaces.save(workspace), mcjs: metadata(), systemTemplate: selectedTemplate};
}

function renderDiagnostics() {
  if (!workspace || !current || current.mode !== 'blocks') return;
  const problems = diagnose(workspace);
  $('block-status').textContent = problems.length ? `${problems.length} block issue${problems.length === 1 ? '' : 's'}` : `${workspace.getAllBlocks(false).length} blocks · Ready to check`;
  $('block-status').classList.toggle('danger', problems.length > 0);
  $('block-problems').replaceChildren();
  for (const problem of problems) {
    const button = document.createElement('button');
    button.textContent = problem.message;
    button.onclick = () => {
      renderTab('blocks');
      focusBlock(problem.id);
    };
    $('block-problems').append(button);
  }
  $('block-problems').hidden = !problems.length;
  const selected = $('flow-select').value;
  $('flow-select').replaceChildren(new Option('Jump to a flow …', ''));
  for (const block of workspace.getTopBlocks(true)) {
    const label = block.type === 'mjs_function' ? `Function: ${block.getFieldValue('NAME')}` : block.type === 'mjs_command' ? `Command: /${block.getFieldValue('NAME')}` : block.toString(75);
    $('flow-select').append(new Option(label, block.id));
  }
  if (workspace.getBlockById(selected)) $('flow-select').value = selected;
}

function focusBlock(id) {
  const block = workspace.getBlockById(id);
  if (!block) return;
  workspace.setScale(Math.max(0.8, workspace.getScale()));
  const position = block.getRelativeToSurfaceXY();
  workspace.scroll(24 - position.x * workspace.getScale(), 24 - position.y * workspace.getScale());
  block.select();
}

function filterBlocks() {
  const query = $('block-search').value.trim().toLowerCase();
  const contents = toolbox.contents.filter(category => !query || category.contents || category.name.toLowerCase().includes(query)).map(category => ({...category, ...(category.contents ? {contents: category.contents.filter(item => {
    if (!query) return true;
    const sample = new Blockly.Workspace();
    try {
      const block = sample.newBlock(item.type);
      return `${category.name} ${item.type} ${block.toString()} ${block.getTooltip()}`.toLowerCase().includes(query);
    } finally { sample.dispose(); }
  })} : {})})).filter(category => !category.contents || category.contents.length);
  workspace.updateToolbox({...toolbox, contents: contents.length ? contents : [{kind: 'category', name: 'No matching blocks', contents: []}]});
}

function showTemplateGuide() {
  const template = systemTemplates.find(item => item.id === selectedTemplate);
  $('system-note').hidden = !template;
  $('system-note').textContent = template ? `${template.title} · ${template.commands} — ${template.description}` : '';
}

function renderTab(tab) {
  studioTools.mode();
  activeTab = tab;
  if (tab === 'code' && current.mode === 'blocks') setCode(source());
  $('block-editor').hidden = tab !== 'blocks';
  $('code-editor').hidden = tab !== 'code';
  $('block-controls').hidden = tab !== 'blocks';
  $('builder-tools').hidden = tab !== 'blocks';
  $('builder-feedback').hidden = tab !== 'blocks';
  $('mode-blocks').classList.toggle('active', tab === 'blocks');
  $('mode-code').classList.toggle('active', tab === 'code');
  $('mode-blocks').setAttribute('aria-pressed', String(tab === 'blocks'));
  $('mode-code').setAttribute('aria-pressed', String(tab === 'code'));
  $('detach-code').hidden = !(tab === 'code' && current.mode === 'blocks');
  $('export-project').hidden = current.mode !== 'blocks';
  editor.dispatch({effects: readOnly.reconfigure(EditorState.readOnly.of(current.mode === 'blocks'))});
  $('editor-hint').textContent = current.mode === 'blocks'
    ? tab === 'code' ? 'Read-only preview · Unlock editing via “Edit code”.' : 'Blocks are turned into JavaScript automatically. Right-click a block for more options.'
    : 'Ctrl+S save · Ctrl+F search · Ctrl+Space suggestions';
  if (tab === 'blocks') { renderDiagnostics(); requestAnimationFrame(() => Blockly.svgResize(workspace)); }
  else editor.requestMeasure();
}

function openProject(project) {
  initEditors();
  loading = true;
  Blockly.Events.disable();
  try {
    workspace.clear();
    if (project.mode === 'blocks' && project.workspace) Blockly.serialization.workspaces.load(project.workspace, workspace);
    current = project;
    selectedTemplate = project.workspace?.systemTemplate || null;
    $('block-search').value = '';
    workspace.updateToolbox(toolbox);
    showTemplateGuide();
    const meta = project.workspace?.mcjs || {};
    $('meta-name').value = meta.name || project.name;
    $('meta-version').value = meta.version || '1.0.0';
    $('meta-author').value = meta.author || session.player;
    setCode(project.source || '');
    $('welcome').hidden = true;
    $('project').hidden = false;
    $('project-title').textContent = project.name;
    $('breadcrumb').textContent = `${project.name}.js`;
    $('dirty-dot').hidden = true;
    dirty = false;
    renderTab(project.mode === 'blocks' ? 'blocks' : 'code');
    updateRuntime();
    renderPlugins();
    studioTools.refresh();
    if (project.mode === 'blocks') requestAnimationFrame(() => {
      if (project.arrange) workspace.cleanUp();
      workspace.setScale(0.9);
      focusBlock(workspace.getTopBlocks(true)[0]?.id);
    });
  } finally {
    Blockly.Events.enable();
    loading = false;
  }
}

function updateRuntime() {
  const info = plugins.find(item => item.name === current?.name);
  $('runtime-state').textContent = info?.running ? '● Running' : info?.disabled ? 'Disabled' : current?.revision ? 'Stopped' : 'Draft';
  $('runtime-state').classList.toggle('running', Boolean(info?.running));
}

function renderPlugins() {
  $('plugin-count').textContent = plugins.length;
  $('plugin-list').replaceChildren();
  const list = plugins.filter(plugin => plugin.name.toLowerCase().includes($('search').value.toLowerCase()));
  if (!list.length) {
    const empty = document.createElement('p');
    empty.className = 'list-empty';
    empty.textContent = plugins.length ? 'No matches.' : 'Your collection starts here.';
    $('plugin-list').append(empty);
  }
  for (const plugin of list) {
    const button = document.createElement('button');
    button.className = `plugin-item${current?.name === plugin.name ? ' selected' : ''}`;
    const icon = document.createElement('span');
    icon.className = 'file-icon';
    icon.textContent = 'JS';
    const name = document.createElement('span');
    name.textContent = plugin.name;
    const status = document.createElement('i');
    status.className = plugin.running ? 'status-dot on' : 'status-dot';
    status.title = plugin.running ? 'Running' : 'Stopped';
    button.append(icon, name, status);
    button.onclick = () => perform(async () => {
      if (!await mayLeave()) return;
      const project = await request(`plugin?name=${encodeURIComponent(plugin.name)}`);
      openProject(project);
      log(`${plugin.name}.js opened.`);
    });
    $('plugin-list').append(button);
  }
}

async function refresh() {
  plugins = (await request('plugins')).plugins;
  renderPlugins();
  if (current) updateRuntime();
}

async function perform(action) {
  if (busy) return;
  busy = true;
  $('studio').classList.add('busy');
  const controls = document.querySelectorAll('.actions button, .more-menu button, .studio-tools button, #new-plugin, #refresh, #logout');
  controls.forEach(button => { button.disabled = true; });
  $('project').setAttribute('aria-busy', 'true');
  document.querySelector('.editor-shell').inert = true;
  $('block-controls').inert = true;
  try { await action(); }
  catch (error) { log(error.message, true); }
  finally {
    busy = false;
    $('studio').classList.remove('busy');
    controls.forEach(button => { button.disabled = false; });
    studioTools.mode();
    $('project').setAttribute('aria-busy', 'false');
    document.querySelector('.editor-shell').inert = false;
    $('block-controls').inert = false;
  }
}

async function save(run) {
  if (!current) return;
  const payload = {name: current.name, source: source(), revision: current.revision,
    workspace: current.mode === 'blocks' ? blockProject() : null};
  const saved = await request('plugin', 'PUT', payload);
  current.revision = saved.revision;
  current.source = saved.source;
  dirty = false;
  $('dirty-dot').hidden = true;
  log(`${current.name}.js saved. Syntax check passed.`);
  try {
    if (run) {
      await request('run', 'POST', {name: current.name, revision: current.revision});
      log(`${current.name} was started. Changes are live in game.`);
    }
  } finally { await refresh(); }
}

function download(name, content, type) {
  const url = URL.createObjectURL(new Blob([content], {type}));
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

async function createDialog(template = 'welcome') {
  if (busy || !await mayLeave()) return;
  imported = null;
  $('create-form').reset();
  $('new-template').value = template;
  updateTemplateDescription();
  $('create-error').textContent = '';
  $('create-dialog').showModal();
  $('new-name').focus();
}

$('create-form').addEventListener('submit', event => {
  event.preventDefault();
  const name = $('new-name').value.trim();
  if (plugins.some(plugin => plugin.name.toLowerCase() === name.toLowerCase()) || /^(CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])$/i.test(name)) {
    $('create-error').textContent = 'This file name already exists or is reserved.';
    return;
  }
  try {
    const template = $('new-template').value;
    const mode = imported ? imported.mode : template === 'code' ? 'code' : 'blocks';
    const code = imported?.source || `var pluginInfo = ${JSON.stringify({name, version: '1.0.0', author: session.player}, null, 2)};\n\nfunction onEnable() {\n    logger.info(pluginInfo.name + " started");\n}\n\nfunction onDisable() {\n}\n`;
    openProject({name, source: code, revision: '', mode, workspace: imported?.workspace || starter(template), arrange: !imported});
    changed();
    $('create-dialog').close();
    log(`Draft ${name}.js created. Save it to deploy it to the server.`);
  } catch (error) { $('create-error').textContent = error.message; }
});

$('import-file').addEventListener('change', async event => {
  const file = event.target.files[0];
  if (!file) return;
  try {
    if (file.size > 1024 * 1024) throw new Error('Import file too large (maximum 1 MiB).');
    const data = await file.text();
    if (file.name.endsWith('.js')) imported = {mode: 'code', source: data};
    else {
      const parsed = JSON.parse(data);
      if (parsed.format !== 'mcjs-blocks-v1' || !parsed.workspace?.blocks) throw new Error('Not a valid MC-JS block project.');
      imported = {mode: 'blocks', workspace: parsed.workspace};
    }
    $('new-name').value = file.name.replace(/\.mcjs\.json$|\.(js|json)$/i, '').replace(/[^A-Za-z0-9_-]/g, '-').slice(0, 64);
    $('create-error').textContent = `Import ready for ${file.name}.`;
  } catch (error) { imported = null; $('create-error').textContent = error.message; }
});

$('new-plugin').onclick = () => createDialog();
for (const template of systemTemplates) {
  const button = document.createElement('button');
  button.className = 'template system-template';
  button.dataset.template = template.id;
  for (const [tag, className, value] of [['span', 'template-icon', template.icon], ['small', '', 'COMPLETE BLOCK SYSTEM'], ['h2', '', template.title], ['p', '', template.description], ['code', 'template-commands', template.commands], ['span', 'feature-tags', template.features.join(' · ')], ['b', '', 'Build this system ↗']]) {
    const element = document.createElement(tag);
    element.className = className;
    element.textContent = value;
    button.append(element);
  }
  $('system-gallery').append(button);
  const option = document.createElement('option');
  option.value = template.id;
  option.textContent = `System · ${template.title}`;
  $('system-options').append(option);
}
document.querySelectorAll('[data-template]').forEach(button => { button.onclick = () => createDialog(button.dataset.template); });
function updateTemplateDescription() {
  const template = systemTemplates.find(item => item.id === $('new-template').value);
  $('template-description').textContent = template ? `${template.description} Commands: ${template.commands}. Every part is editable with blocks.` : 'Start small and combine blocks into your own system.';
}
$('new-template').onchange = updateTemplateDescription;
$('block-search').oninput = filterBlocks;
$('arrange-blocks').onclick = () => { workspace.cleanUp(); workspace.zoomToFit(); };
$('flow-select').onchange = event => focusBlock(event.target.value);
$('fit-blocks').onclick = () => workspace.zoomToFit();
$('builder-help').onclick = () => $('builder-guide').showModal();
$('builder-close').onclick = () => $('builder-guide').close();
$('cancel-create').onclick = () => $('create-dialog').close();
$('search').oninput = renderPlugins;
$('refresh').onclick = () => perform(refresh);
$('save').onclick = () => perform(() => save(false));
$('run').onclick = () => perform(() => save(true));
$('validate').onclick = () => perform(async () => { await request('validate', 'POST', {source: source()}); log('Syntax is valid. The code was not executed.'); });
$('clear-log').onclick = () => $('activity').replaceChildren();
$('more').onclick = () => { $('more-menu').hidden = !$('more-menu').hidden; };
document.addEventListener('click', event => { if (!event.target.closest('#more, #more-menu')) $('more-menu').hidden = true; });
$('stop').onclick = () => perform(async () => {
  await request('stop', 'POST', {name: current.name, revision: current.revision});
  log(`${current.name} stopped. It will be loaded again on the next server start if enabled in config.yml.`);
  await refresh();
});
$('export').onclick = () => { try { download(`${current.name}.js`, source(), 'text/javascript'); } catch (error) { log(error.message, true); } };
$('export-project').onclick = () => download(`${current.name}.mcjs.json`, JSON.stringify({format: 'mcjs-blocks-v1', workspace: blockProject()}, null, 2), 'application/json');
$('delete').onclick = () => perform(async () => {
  if (!await confirmAction('Delete plugin?', `${current.name}.js will be stopped and removed from the server. Export it first if you want to keep it.`, 'Delete')) return;
  if (current.revision) await request('plugin', 'DELETE', {name: current.name, revision: current.revision});
  current = null;
  dirty = false;
  $('project').hidden = true;
  $('welcome').hidden = false;
  $('breadcrumb').textContent = 'Overview';
  await refresh();
});
$('mode-code').onclick = () => { try { renderTab('code'); } catch (error) { log(error.message, true); } };
$('mode-blocks').onclick = async () => {
  if (busy) return;
  if (current.mode !== 'blocks') {
    if (!await confirmAction('Start a new block project?', 'Free JavaScript code cannot be converted to blocks automatically. Your code will be replaced by an empty block project. Export it first if needed.', 'Start block project')) return;
    current.mode = 'blocks';
    Blockly.serialization.workspaces.load(starter('empty'), workspace);
    changed();
  }
  renderTab('blocks');
};
$('detach-code').onclick = async () => {
  if (!await confirmAction('Edit JavaScript freely?', 'The generated code becomes editable. Further code changes cannot be converted back into blocks. Export your block project first to keep it.', 'Unlock code')) return;
  setCode(source());
  current.mode = 'code';
  changed();
  renderTab('code');
};
for (const id of ['meta-name', 'meta-version', 'meta-author']) $(id).oninput = changed;
$('logout').onclick = () => perform(async () => {
  if (!await mayLeave()) return;
  await request('logout', 'POST', {});
  dirty = false;
  location.reload();
});
$('copy-command').onclick = async () => {
  $('verify-command').select();
  try {
    if (navigator.clipboard && window.isSecureContext) await navigator.clipboard.writeText($('verify-command').value);
    else if (!document.execCommand('copy')) throw new Error();
    $('copy-command').textContent = 'Copied ✓';
  } catch { $('gate-error').textContent = 'Command selected. Copy it with Ctrl+C.'; }
};
window.addEventListener('beforeunload', event => { if (dirty) { event.preventDefault(); event.returnValue = ''; } });
document.addEventListener('keydown', event => {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's' && current) { event.preventDefault(); perform(() => save(false)); }
});

function renderDocs(filter = '') {
  const query = filter.trim().toLowerCase();
  const content = $('docs-content');
  content.replaceChildren();
  if (!query) {
    for (const group of categories()) {
      const section = document.createElement('section');
      section.className = 'docs-group';
      const heading = document.createElement('h3');
      heading.textContent = group.name;
      section.append(heading);
      for (const entry of group.entries) section.append(docItem(entry));
      content.append(section);
    }
    return;
  }
  const matches = apiDocs.filter(entry =>
    entry.label.toLowerCase().includes(query)
    || (entry.info || '').toLowerCase().includes(query)
    || entry.category.toLowerCase().includes(query));
  if (!matches.length) {
    const empty = document.createElement('p');
    empty.className = 'docs-empty';
    empty.textContent = 'No matches.';
    content.append(empty);
    return;
  }
  const section = document.createElement('section');
  section.className = 'docs-group';
  const heading = document.createElement('h3');
  heading.textContent = `Results (${matches.length})`;
  section.append(heading);
  for (const entry of matches) section.append(docItem(entry));
  content.append(section);
}

function docItem(entry) {
  const item = document.createElement('article');
  item.className = 'docs-item';
  const title = document.createElement('h4');
  title.textContent = entry.label;
  item.append(title);
  const sig = document.createElement('code');
  sig.className = 'sig';
  sig.textContent = entry.signature;
  item.append(sig);
  const desc = document.createElement('p');
  desc.className = 'desc';
  desc.textContent = entry.info || '';
  item.append(desc);
  return item;
}

$('docs-button').onclick = () => { renderDocs(''); $('docs-dialog').showModal(); };
$('docs-close').onclick = () => $('docs-dialog').close();
$('docs-search').oninput = event => renderDocs(event.target.value);

function schedulePoll(delay) {
  clearTimeout(pollTimer);
  pollTimer = setTimeout(async () => {
    try { await applySession(await request('session')); }
    catch (error) {
      if (error.status === 401 || error.status === 403) {
        if ($('studio').hidden) {
          $('verification').hidden = true;
          $('gate-error').textContent = error.message;
        } else {
          $('account').textContent = 'Session ended · draft can still be exported';
          $('session-label').textContent = 'Sign in again in game';
          log(error.message + ' Export unsaved work before opening a new link.', true);
        }
      } else {
        if ($('studio').hidden) $('gate-error').textContent = 'Connection lost. Retrying …';
        else $('session-label').textContent = 'Reconnecting …';
        schedulePoll(5000);
      }
    }
  }, delay);
}

async function applySession(data) {
  session = data;
  if (data.verified) {
    const first = $('studio').hidden;
    $('gate').hidden = true;
    $('studio').hidden = false;
    $('account').textContent = data.player;
    $('session-label').textContent = `${data.player} · until ${new Date(data.expiresAt).toLocaleTimeString('en-GB', {hour: '2-digit', minute: '2-digit'})}`;
    schedulePoll(20000);
    if (first) { initEditors(); await refresh(); }
  } else {
    $('verification').hidden = false;
    $('verify-command').value = data.command;
    $('verify-player').textContent = data.player;
    $('gate-text').textContent = 'One last step: confirm this browser with your Minecraft account.';
    $('gate-error').textContent = '';
    schedulePoll(1500);
  }
}

async function boot() {
  const token = new URLSearchParams(location.hash.slice(1)).get('token');
  if (location.hash) history.replaceState(null, '', location.pathname);
  try {
    await applySession(token ? await request('session', 'POST', {token}) : await request('session'));
  } catch (error) { $('gate-error').textContent = error.message; }
}
boot();
