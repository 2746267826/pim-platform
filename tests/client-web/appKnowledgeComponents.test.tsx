import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
import AppKnowledgeTabs from '../../src/client-web/src/components/app-knowledge/AppKnowledgeTabs';

const requireFromClient = createRequire(path.join(process.cwd(), 'src/client-web/package.json'));
const clientSourceRoot = path.join(process.cwd(), 'src/client-web/src');
const React = requireFromClient('react') as typeof import('react');
const { renderToStaticMarkup } = requireFromClient('react-dom/server') as typeof import('react-dom/server');
const { MemoryRouter } = requireFromClient('react-router-dom') as typeof import('react-router-dom');
const reactGlobal = globalThis as typeof globalThis & { React: typeof React };
reactGlobal.React = React;

function test(_name: string, run: () => void) {
  run();
}

function readClientSource(relativePath: string) {
  return readFileSync(path.join(clientSourceRoot, relativePath), 'utf8');
}

test('category tree secondary navigation uses app knowledge language', () => {
  const html = renderToStaticMarkup(
    React.createElement(
      MemoryRouter,
      null,
      React.createElement(AppKnowledgeTabs, { active: 'categories' })
    )
  );

  assert.equal(html.includes('App 列表'), true);
  assert.equal(html.includes('分类树'), true);
  assert.equal(html.includes('分类管理'), false);
});

test('category tree page uses app knowledge category tree language and tabs', () => {
  const source = readClientSource('pages/CategoryTreePage.tsx');

  assert.equal(source.includes('PageHeader title="分类树"'), true);
  assert.equal(source.includes('subtitle="作为 App 知识库的目标分类结构"'), true);
  assert.equal(source.includes('<AppKnowledgeTabs active="categories" />'), true);
  assert.equal(source.includes('分类管理'), false);
  assert.equal(source.includes('Classification Management'), false);
});

test('app knowledge base page keeps app tab active with updated subtitle', () => {
  const source = readClientSource('pages/AppKnowledgeBasePage.tsx');

  assert.equal(source.includes('title="App 知识库"'), true);
  assert.equal(source.includes('subtitle="管理应用、域名、标题模式和分类归属知识"'), true);
  assert.equal(source.includes('<AppKnowledgeTabs active="apps" />'), true);
});
