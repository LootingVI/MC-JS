import {parser} from '@lezer/javascript';
import {readFile, writeFile, readdir} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
function removeRanges(source, ranges) {
  for (const [start, end] of ranges.reverse()) source = source.slice(0, start) + source.slice(start, end).replace(/[^\r\n]/g, ' ') + source.slice(end);
  return source;
}
function javascript(source) {
  const ranges = [];
  parser.parse(source).iterate({enter(node) { if (node.name === 'LineComment' || node.name === 'BlockComment') ranges.push([node.from, node.to]); }});
  return removeRanges(source, ranges);
}
function java(source, lineComments = true) {
  const ranges = [];
  for (let i = 0; i < source.length; i++) {
    if (source[i] === '"' || source[i] === "'") {
      const quote = source[i];
      if (source.slice(i, i + 3) === '"""') { const end = source.indexOf('"""', i + 3); i = end < 0 ? source.length : end + 2; continue; }
      while (++i < source.length) { if (source[i] === '\\') i++; else if (source[i] === quote) break; }
    } else if (source.slice(i, i + 2) === '/*') {
      const start = i, end = source.indexOf('*/', i + 2);
      i = end < 0 ? source.length : end + 1;
      ranges.push([start, i + 1]);
    } else if (lineComments && source.slice(i, i + 2) === '//') {
      const start = i;
      while (i < source.length && source[i] !== '\n' && source[i] !== '\r') i++;
      ranges.push([start, i]);
    }
  }
  return removeRanges(source, ranges);
}
function yaml(source) {
  return source.split('\n').map(line => {
    let quote = '';
    for (let i = 0; i < line.length; i++) {
      if (quote) { if (line[i] === '\\' && quote === '"') i++; else if (line[i] === quote) quote = ''; }
      else if (line[i] === '"' || line[i] === "'") quote = line[i];
      else if (line[i] === '#' && (i === 0 || /\s/.test(line[i - 1]))) return line.slice(0, i);
    }
    return line;
  }).join('\n');
}
function html(source) {
  return source.replace(/<!--[\s\S]*?-->/g, '').replace(/(<style\b[^>]*>)([\s\S]*?)(<\/style>)/gi, (_, a, b, c) => a + java(b, false) + c)
    .replace(/(<script\b[^>]*>)([\s\S]*?)(<\/script>)/gi, (_, a, b, c) => a + javascript(b) + c)
    .replace(/(<code\b[^>]*class="language-javascript"[^>]*>)([\s\S]*?)(<\/code>)/gi, (_, a, b, c) => {
      const decoded = b.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&amp;/g, '&');
      return a + javascript(decoded).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;') + c;
    });
}
let changed = 0;
async function visit(directory) {
  for (const entry of await readdir(directory, {withFileTypes: true})) {
    if (['node_modules', '.tools', '.git', '.idea', 'target'].includes(entry.name)) continue;
    const file = path.join(directory, entry.name);
    if (entry.isDirectory()) { await visit(file); continue; }
    const ext = path.extname(file);
    if (!['.java', '.js', '.mjs', '.yml', '.yaml', '.html', '.xml', '.css', '.svg', '.md'].includes(ext)) continue;
    const source = await readFile(file, 'utf8');
    let cleaned = source;
    if (ext === '.java') cleaned = java(source);
    else if (ext === '.js' || ext === '.mjs') cleaned = javascript(source);
    else if (ext === '.yml' || ext === '.yaml') cleaned = yaml(source);
    else if (ext === '.css') cleaned = java(source, false);
    else if (ext === '.html') cleaned = html(source);
    else if (ext === '.xml' || ext === '.svg') cleaned = source.replace(/<!--[\s\S]*?-->/g, '');
    else if (ext === '.md') cleaned = source.replace(/<!--[\s\S]*?-->/g, '').replace(/(```(?:javascript|js)\s*\n)([\s\S]*?)(```)/g, (_, a, b, c) => a + javascript(b) + c);
    cleaned = cleaned.replace(/[ \t]+$/gm, '').replace(/\n(?:\s*\n){2,}/g, '\n\n');
    if (source !== cleaned) { await writeFile(file, cleaned); changed++; }
  }
}
await visit(root);
process.stdout.write(`Cleaned ${changed} project files.\n`);
