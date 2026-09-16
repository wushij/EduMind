#!/usr/bin/env node
/**
 * Frontend layering compliance audit.
 * Scans views/, components/, and api/ for layering violations.
 */
import { readdir, readFile } from 'node:fs/promises';
import { join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const ROOT = join(__dirname, '..');
const SRC = join(ROOT, 'src');
const VIEWS_DIR = join(SRC, 'views');
const COMPONENTS_DIR = join(SRC, 'components');
const API_DIR = join(SRC, 'api');

async function collectFiles(dir, ext) {
  const entries = await readdir(dir, { withFileTypes: true }).catch(() => []);
  const files = [];
  for (const entry of entries) {
    const full = join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await collectFiles(full, ext)));
    } else if (entry.name.endsWith(ext)) {
      files.push(full);
    }
  }
  return files;
}

function analyzeView(content, relPath) {
  const lines = content.split('\n').length;
  const directApi = /from\s+['"]@\/api\//.test(content);
  const usesComposable = /from\s+['"]@\/composables\//.test(content);
  const viewsImport = /from\s+['"]@\/views\//.test(content);
  const mockImport = /from\s+['"]@\/mock\//.test(content);
  return { relPath, lines, directApi, usesComposable, viewsImport, mockImport };
}

function analyzeComponent(content, relPath) {
  const directApi = /from\s+['"]@\/api\//.test(content);
  return { relPath, directApi };
}

function analyzeApiFile(content, relPath) {
  const lines = content.split('\n').length;
  const hasMock = /\bUSE_MOCK\b/.test(content);
  const hasMapFn = /export\s+function\s+map\w+/i.test(content) || /function\s+map\w+\s*\(/.test(content);
  const exportsInterface = /export\s+interface\s+/.test(content);
  const fat = lines > 120 || hasMock || hasMapFn;
  return { relPath, lines, hasMock, hasMapFn, exportsInterface, fat };
}

async function main() {
  const viewFiles = await collectFiles(VIEWS_DIR, '.vue');
  const componentFiles = await collectFiles(COMPONENTS_DIR, '.vue');
  const apiFiles = await collectFiles(API_DIR, '.ts');

  const viewResults = [];
  for (const file of viewFiles) {
    const content = await readFile(file, 'utf8');
    viewResults.push(analyzeView(content, relative(SRC, file).replace(/\\/g, '/')));
  }

  const componentResults = [];
  for (const file of componentFiles) {
    const content = await readFile(file, 'utf8');
    componentResults.push(analyzeComponent(content, relative(SRC, file).replace(/\\/g, '/')));
  }

  const apiResults = [];
  for (const file of apiFiles) {
    const content = await readFile(file, 'utf8');
    apiResults.push(analyzeApiFile(content, relative(SRC, file).replace(/\\/g, '/')));
  }

  const viewsDirectApi = viewResults.filter((r) => r.directApi);
  const viewsInterImport = viewResults.filter((r) => r.viewsImport);
  const viewsMockImport = viewResults.filter((r) => r.mockImport);
  const over800 = viewResults.filter((r) => r.lines > 800).sort((a, b) => b.lines - a.lines);
  const over600 = viewResults.filter((r) => r.lines > 600 && r.lines <= 800).sort((a, b) => b.lines - a.lines);
  const noComposable = viewResults.filter((r) => !r.usesComposable);

  const componentsDirectApi = componentResults.filter((r) => r.directApi);
  const fatApi = apiResults.filter((r) => r.fat).sort((a, b) => b.lines - a.lines);
  const apiExportsTypes = apiResults.filter((r) => r.exportsInterface);

  console.log('=== EduMind Frontend Layering Audit ===\n');
  console.log(`Total views: ${viewResults.length}`);
  console.log(`Views direct @/api: ${viewsDirectApi.length}`);
  console.log(`Views >800 lines (warning): ${over800.length}`);
  console.log(`Views 600-800 lines: ${over600.length}`);
  console.log(`Views without composable: ${noComposable.length}`);
  console.log(`Views inter-import: ${viewsInterImport.length}`);
  console.log(`Views direct @/mock: ${viewsMockImport.length}`);
  console.log(`Components direct @/api: ${componentsDirectApi.length}`);
  console.log(`API files flagged (fat/mock/map): ${fatApi.length}`);
  console.log(`API files exporting interface: ${apiExportsTypes.length}\n`);

  if (viewsDirectApi.length) {
    console.log('--- Direct API in views (FAIL) ---');
    viewsDirectApi.forEach((r) => console.log(`  ${r.relPath}`));
    console.log('');
  }

  if (componentsDirectApi.length) {
    console.log('--- Direct API in components (FAIL) ---');
    componentsDirectApi.forEach((r) => console.log(`  ${r.relPath}`));
    console.log('');
  }

  if (viewsInterImport.length) {
    console.log('--- Views inter-import (FAIL) ---');
    viewsInterImport.forEach((r) => console.log(`  ${r.relPath}`));
    console.log('');
  }

  if (viewsMockImport.length) {
    console.log('--- Direct mock in views (FAIL) ---');
    viewsMockImport.forEach((r) => console.log(`  ${r.relPath}`));
    console.log('');
  }

  if (over800.length) {
    console.log('--- Views >800 lines (warning) ---');
    over800.forEach((r) => console.log(`  ${r.lines}\t${r.relPath}`));
    console.log('');
  }

  if (fatApi.length) {
    console.log('--- API layer warnings ---');
    fatApi.forEach((r) => {
      const flags = [
        r.lines > 120 ? `lines=${r.lines}` : null,
        r.hasMock ? 'USE_MOCK' : null,
        r.hasMapFn ? 'map*' : null
      ]
        .filter(Boolean)
        .join(', ');
      console.log(`  ${r.relPath} (${flags})`);
    });
    console.log('');
  }

  if (apiExportsTypes.length) {
    console.log('--- API exporting types (warning) ---');
    apiExportsTypes.forEach((r) => console.log(`  ${r.relPath}`));
    console.log('');
  }

  const failed =
    viewsDirectApi.length > 0 ||
    componentsDirectApi.length > 0 ||
    viewsInterImport.length > 0 ||
    viewsMockImport.length > 0;

  if (failed) {
    console.log('AUDIT: FAILED (fix direct API/mock in views, components API, or views inter-import)');
    process.exit(1);
  }

  console.log('AUDIT: PASSED (views >800 and fat api are warnings only)');
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
