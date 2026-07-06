import assert from 'node:assert/strict';
import path from 'node:path';
import { createRequire } from 'node:module';
import AppKnowledgeTabs from '../../src/client-web/src/components/app-knowledge/AppKnowledgeTabs';

const requireFromClient = createRequire(path.join(process.cwd(), 'src/client-web/package.json'));
const React = requireFromClient('react') as typeof import('react');
const { renderToStaticMarkup } = requireFromClient('react-dom/server') as typeof import('react-dom/server');
const { MemoryRouter } = requireFromClient('react-router-dom') as typeof import('react-router-dom');
const reactGlobal = globalThis as typeof globalThis & { React: typeof React };
reactGlobal.React = React;

function test(_name: string, run: () => void) {
  run();
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
