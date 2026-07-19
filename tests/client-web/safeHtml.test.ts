import assert from 'node:assert/strict';
import { describe, it, before } from 'node:test';
import { createRequire } from 'node:module';

const requireFromWeb = createRequire(new URL('../../src/client-web/package.json', import.meta.url));
const { JSDOM } = requireFromWeb('jsdom') as typeof import('jsdom');

let looksLikeHtml: (value: string) => boolean;
let sanitizeDescriptionHtml: (value: string) => string;

before(async () => {
  const dom = new JSDOM('<!DOCTYPE html><html><body></body></html>');
  globalThis.window = dom.window as unknown as Window & typeof globalThis;
  globalThis.document = dom.window.document;
  globalThis.Node = dom.window.Node;
  globalThis.DocumentFragment = dom.window.DocumentFragment;
  globalThis.Element = dom.window.Element;
  globalThis.HTMLElement = dom.window.HTMLElement;
  globalThis.HTMLDocument = dom.window.HTMLDocument;

  const mod = await import('../../src/client-web/src/utils/safeHtml');
  looksLikeHtml = mod.looksLikeHtml;
  sanitizeDescriptionHtml = mod.sanitizeDescriptionHtml;
});

describe('looksLikeHtml', () => {
  it('detects <div> tags', () => {
    assert.equal(looksLikeHtml('<div>hello</div>'), true);
  });

  it('detects <b> tags', () => {
    assert.equal(looksLikeHtml('<b>bold</b>'), true);
  });

  it('detects <a href=...> tags', () => {
    assert.equal(looksLikeHtml('<a href="https://example.com">link</a>'), true);
  });

  it('returns false for plain text', () => {
    assert.equal(looksLikeHtml('just some plain text'), false);
  });

  it('returns false for empty string', () => {
    assert.equal(looksLikeHtml(''), false);
  });

  it('returns false for "a < b and c > d"', () => {
    assert.equal(looksLikeHtml('a < b and c > d'), false);
  });
});

describe('sanitizeDescriptionHtml', () => {
  it('allows <b> and <i>', () => {
    const result = sanitizeDescriptionHtml('<b>bold</b> and <i>italic</i>');
    assert.equal(result, '<b>bold</b> and <i>italic</i>');
  });

  it('allows <em> and <strong>', () => {
    const result = sanitizeDescriptionHtml('<em>emphasized</em> and <strong>strong</strong>');
    assert.equal(result, '<em>emphasized</em> and <strong>strong</strong>');
  });

  it('allows <a> with href', () => {
    const result = sanitizeDescriptionHtml('<a href="https://example.com">link</a>');
    assert.equal(result, '<a href="https://example.com">link</a>');
  });

  it('allows <p>, <br>, <ul>, <ol>, <li>', () => {
    const input = '<p>para</p><br><ul><li>item</li></ul><ol><li>ordered</li></ol>';
    const result = sanitizeDescriptionHtml(input);
    assert.ok(result.includes('<p>para</p>'));
    assert.ok(result.includes('<br>') || result.includes('<br/>'));
    assert.ok(result.includes('<ul>'));
    assert.ok(result.includes('<li>item</li>'));
    assert.ok(result.includes('<ol>'));
  });

  it('removes <script> tags', () => {
    const result = sanitizeDescriptionHtml('<script>alert("xss")</script>hello');
    assert.equal(result, 'hello');
  });

  it('removes <iframe> tags', () => {
    const result = sanitizeDescriptionHtml('<iframe src="https://evil.com"></iframe>safe');
    assert.equal(result, 'safe');
  });

  it('removes <img> tags', () => {
    const result = sanitizeDescriptionHtml('<img src="x" onerror="alert(1)">text');
    assert.equal(result, 'text');
  });

  it('removes on* event handlers', () => {
    const result = sanitizeDescriptionHtml('<p onclick="alert(1)">click</p>');
    assert.equal(result, '<p>click</p>');
  });

  it('removes onerror handlers', () => {
    const result = sanitizeDescriptionHtml('<b onerror="alert(1)">safe</b>');
    assert.equal(result, '<b>safe</b>');
  });

  it('removes style attributes', () => {
    const result = sanitizeDescriptionHtml('<p style="color:red">text</p>');
    assert.equal(result, '<p>text</p>');
  });

  it('removes javascript: href', () => {
    const result = sanitizeDescriptionHtml('<a href="javascript:alert(1)">click</a>');
    assert.equal(result, '<a>click</a>');
  });

  it('returns empty string for empty input', () => {
    assert.equal(sanitizeDescriptionHtml(''), '');
  });

  it('preserves safe text content', () => {
    const result = sanitizeDescriptionHtml('just plain text');
    assert.equal(result, 'just plain text');
  });

  it('removes disallowed tags but keeps their text', () => {
    const result = sanitizeDescriptionHtml('<div>text</div><span>more</span>');
    assert.equal(result, 'textmore');
  });
});
