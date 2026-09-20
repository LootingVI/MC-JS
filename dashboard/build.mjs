import {build} from 'esbuild';
import {cp, mkdir, readFile, readdir, writeFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import {Blockly, generate, starter} from './src/blocks.js';
import {systemTemplates} from './src/system-templates.js';

const root = path.dirname(fileURLToPath(import.meta.url));
const output = path.resolve(root, '../src/main/resources/dashboard');
await mkdir(output, {recursive: true});
await build({
  entryPoints: [path.join(root, 'src/app.js')],
  outfile: path.join(output, 'app.js'),
  bundle: true,
  minify: true,
  legalComments: 'none',
  format: 'iife',
  target: ['es2022'],
  define: {'process.env.NODE_ENV': '"production"'},
});
await cp(path.join(root, 'src/index.html'), path.join(output, 'index.html'));
await cp(path.join(root, 'node_modules/blockly/media'), path.join(output, 'media'), {recursive: true});
for (const name of await readdir(path.join(output, 'media'))) {
  if (name.endsWith('.svg')) {
    const file = path.join(output, 'media', name);
    await writeFile(file, (await readFile(file, 'utf8')).replace(/<!--[\s\S]*?-->/g, ''));
  }
}
const licenses = [];
async function collect(directory) {
  for (const item of await readdir(directory, {withFileTypes: true})) {
    if (!item.isDirectory() || item.name.startsWith('.')) continue;
    const folder = path.join(directory, item.name);
    if (item.name.startsWith('@')) { await collect(folder); continue; }
    const licenseFiles = (await readdir(folder)).filter(name => /^(license|copying|notice)(\.|$)/i.test(name));
    for (const name of licenseFiles) licenses.push(`${item.name}\n${await readFile(path.join(folder, name), 'utf8')}`);
  }
}
await collect(path.join(root, 'node_modules'));
await writeFile(path.join(output, 'THIRD-PARTY-LICENSES.txt'), licenses.join('\n\n────────────────────────────────\n\n'));
const fixtures = path.resolve(root, '../src/test/resources/dashboard-systems');
await mkdir(fixtures, {recursive: true});
for (const template of systemTemplates) {
  const workspace = new Blockly.Workspace();
  try {
    Blockly.serialization.workspaces.load(starter(template.id), workspace);
    await writeFile(path.join(fixtures, template.id + '.js'), generate(workspace, {id: template.id, name: template.title, version: '1.0.0', author: 'Test'}));
  } finally { workspace.dispose(); }
}
