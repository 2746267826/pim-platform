import assert from 'node:assert/strict';
import path from 'node:path';
import { createRequire } from 'node:module';
import { primaryNavItems } from '../../src/client-web/src/layout/Sidebar';
import AppKnowledgeTabs from '../../src/client-web/src/components/app-knowledge/AppKnowledgeTabs';

const requireFromClient = createRequire(path.join(process.cwd(), 'src/client-web/package.json'));
const React = requireFromClient('react') as typeof import('react');
const { renderToStaticMarkup } = requireFromClient('react-dom/server') as typeof import('react-dom/server');
const { MemoryRouter, Navigate } = requireFromClient('react-router-dom') as typeof import('react-router-dom');
const reactGlobal = globalThis as typeof globalThis & { React: typeof React };
reactGlobal.React = React;

function test(_name: string, run: () => void) {
  run();
}

test('sidebar exposes app knowledge but not standalone classification pages', () => {
  const labels = primaryNavItems.map(item => item.label);

  assert.equal(labels.includes('App知识库'), true);
  assert.equal(labels.includes('分类管理'), false);
  assert.equal(labels.includes('分类树'), false);
});

test('app knowledge tabs include category tree as a secondary page', () => {
  const html = renderToStaticMarkup(
    React.createElement(
      MemoryRouter,
      null,
      React.createElement(AppKnowledgeTabs, { active: 'categories' })
    )
  );

  assert.equal(html.includes('App 列表'), true);
  assert.equal(html.includes('分类树'), true);
  assert.equal(html.includes('/app-knowledge-base/categories'), true);
});

test('legacy category route can redirect to nested app knowledge category route', () => {
  const html = renderToStaticMarkup(
    React.createElement(
      MemoryRouter,
      null,
      React.createElement(Navigate, { to: '/app-knowledge-base/categories', replace: true })
    )
  );

  assert.equal(typeof html, 'string');
});
