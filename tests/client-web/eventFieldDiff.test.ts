import assert from 'node:assert/strict';
import { describe, it, before } from 'node:test';
import { createRequire } from 'node:module';
import type { EventFieldDiffEntry } from '../../src/client-web/src/utils/eventFieldDiff';

const requireFromWeb = createRequire(new URL('../../src/client-web/package.json', import.meta.url));
const { JSDOM } = requireFromWeb('jsdom') as typeof import('jsdom');

let diffEventFields: (
  before: Record<string, unknown>,
  after: Record<string, unknown>,
) => EventFieldDiffEntry[];
let EVENT_FIELD_LABELS: Record<string, string>;

before(async () => {
  const dom = new JSDOM('<!DOCTYPE html><html><body></body></html>');
  globalThis.window = dom.window as unknown as Window & typeof globalThis;
  globalThis.document = dom.window.document;
  globalThis.Node = dom.window.Node;
  globalThis.DocumentFragment = dom.window.DocumentFragment;
  globalThis.Element = dom.window.Element;
  globalThis.HTMLElement = dom.window.HTMLElement;
  globalThis.HTMLDocument = dom.window.HTMLDocument;

  const mod = await import('../../src/client-web/src/utils/eventFieldDiff');
  diffEventFields = mod.diffEventFields;
  EVENT_FIELD_LABELS = mod.EVENT_FIELD_LABELS;
});

describe('diffEventFields', () => {
  it('reports a modified title with its Chinese label', () => {
    const entries = diffEventFields({ title: '旧' }, { title: '新' });
    assert.equal(entries.length, 1);
    assert.deepEqual(entries[0], { key: 'title', label: '标题', kind: 'modified', before: '旧', after: '新' });
  });

  it('marks fields present only after as added', () => {
    const entries = diffEventFields({}, { title: '新' });
    assert.equal(entries.length, 1);
    assert.equal(entries[0].kind, 'added');
    assert.equal(entries[0].before, undefined);
    assert.equal(entries[0].after, '新');
  });

  it('marks fields present only before as removed', () => {
    const entries = diffEventFields({ title: '旧' }, {});
    assert.equal(entries.length, 1);
    assert.equal(entries[0].kind, 'removed');
    assert.equal(entries[0].before, '旧');
    assert.equal(entries[0].after, undefined);
  });

  it('omits unchanged fields', () => {
    assert.deepEqual(diffEventFields({ title: '同' }, { title: '同' }), []);
    assert.deepEqual(diffEventFields({ title: '同', location: 'A' }, { title: '同', location: 'A' }), []);
  });

  it('treats null, undefined and empty strings equivalently', () => {
    assert.deepEqual(diffEventFields({ description: null }, { description: undefined }), []);
    assert.deepEqual(diffEventFields({ description: '' }, { description: null }), []);
    assert.deepEqual(diffEventFields({ location: '' }, { location: '' }), []);
    const entries = diffEventFields({ location: '' }, { location: '某地' });
    assert.equal(entries.length, 1);
    assert.equal(entries[0].kind, 'added');
  });

  it('diffs arrays with element ordering', () => {
    const entries = diffEventFields({ categories: ['a', 'b'] }, { categories: ['a', 'c'] });
    assert.equal(entries.length, 1);
    assert.deepEqual(entries[0], {
      key: 'categories',
      label: '分类',
      kind: 'modified',
      before: ['a', 'b'],
      after: ['a', 'c'],
    });
    assert.deepEqual(diffEventFields({ categories: ['a', 'b'] }, { categories: ['a', 'b'] }), []);
    assert.deepEqual(diffEventFields({ categories: ['a', 'b'] }, { categories: [] })[0].kind, 'removed');
    assert.deepEqual(diffEventFields({ categories: [] }, { categories: ['a'] })[0].kind, 'added');
  });

  it('diffs nested organizer objects', () => {
    const entries = diffEventFields(
      { organizer: { name: 'A', email: 'a@example.com' } },
      { organizer: { name: 'B', email: 'a@example.com' } },
    );
    assert.equal(entries.length, 1);
    assert.equal(entries[0].key, 'organizer');
    assert.equal(entries[0].label, '组织者');
    assert.deepEqual(entries[0].before, { name: 'A', email: 'a@example.com' });
    assert.deepEqual(entries[0].after, { name: 'B', email: 'a@example.com' });
    assert.deepEqual(
      diffEventFields({ organizer: null }, { organizer: { name: 'A', email: 'a@example.com' } })[0].kind,
      'added',
    );
  });

  it('diffs nested attendee arrays', () => {
    const before = { attendees: [{ name: 'A', email: 'a@example.com', type: 'required' }] };
    const after = {
      attendees: [
        { name: 'A', email: 'a@example.com', type: 'required' },
        { name: 'B', email: 'b@example.com', type: 'optional' },
      ],
    };
    const entries = diffEventFields(before, after);
    assert.equal(entries.length, 1);
    assert.equal(entries[0].key, 'attendees');
    assert.deepEqual(entries[0].before, before.attendees);
    assert.deepEqual(entries[0].after, after.attendees);
  });

  it('turns HTML descriptions into text-only safe summaries', () => {
    const entries = diffEventFields(
      { description: '<p>旧<strong>内容</strong></p>' },
      { description: '<p>新内容</p>' },
    );
    assert.equal(entries.length, 1);
    assert.equal(entries[0].key, 'description');
    assert.equal(entries[0].before, '旧内容');
    assert.equal(entries[0].after, '新内容');
    assert.deepEqual(diffEventFields({ description: '<p>一样</p>' }, { description: '一样' }), []);
  });

  it('ignores HTML descriptions that normalize to empty', () => {
    assert.deepEqual(diffEventFields({ description: '<p> </p>' }, { description: undefined }), []);
    assert.deepEqual(diffEventFields({ description: '<p> </p>' }, { description: '' }), []);
    assert.deepEqual(diffEventFields({ description: '<p></p>' }, { description: '<p> </p>' }), []);
  });

  it('ignores whitespace-entity HTML descriptions and normalizes their text', () => {
    assert.deepEqual(diffEventFields({ description: '<p>&nbsp;</p>' }, { description: undefined }), []);
    assert.deepEqual(diffEventFields({ description: '<p>&nbsp;</p>' }, { description: '' }), []);
    assert.deepEqual(diffEventFields({ description: '<p>A&nbsp;B</p>' }, { description: 'A B' }), []);
  });

  it('ignores unwrapped whitespace-entity descriptions when the format is explicitly html', () => {
    assert.deepEqual(
      diffEventFields(
        { descriptionFormat: 'html', description: '&nbsp;' },
        { descriptionFormat: 'html', description: '' },
      ),
      [],
    );
    assert.deepEqual(
      diffEventFields(
        { descriptionFormat: 'html', description: '&Tab;' },
        { descriptionFormat: 'html', description: '' },
      ),
      [],
    );
    assert.deepEqual(
      diffEventFields(
        { descriptionFormat: 'html', description: '&#9;' },
        { descriptionFormat: 'html', description: '' },
      ),
      [],
    );
  });

  it('treats whitespace-only plain descriptions as absent', () => {
    assert.deepEqual(diffEventFields({ description: '   ' }, { description: undefined }), []);
    assert.deepEqual(diffEventFields({ description: '\t\n  ' }, { description: null }), []);
    assert.deepEqual(diffEventFields({}, { description: ' ' }), []);
    assert.deepEqual(diffEventFields({ description: ' ' }, { description: '' }), []);
  });

  it('still reports meaningful plain-text description changes', () => {
    const entries = diffEventFields({ description: '旧内容' }, { description: '新内容' });
    assert.equal(entries.length, 1);
    assert.equal(entries[0].key, 'description');
    assert.equal(entries[0].kind, 'modified');
    assert.deepEqual(diffEventFields({ description: ' ' }, { description: '新内容' })[0].kind, 'added');
  });

  it('treats explicit plain descriptions as literal text even when they look like HTML', () => {
    const entries = diffEventFields(
      { descriptionFormat: 'plain', description: 'Use <b> for bold' },
      { descriptionFormat: 'plain', description: 'Use for bold' },
    );
    assert.equal(entries.length, 1);
    assert.deepEqual(entries[0], {
      key: 'description',
      label: '描述',
      kind: 'modified',
      before: 'Use <b> for bold',
      after: 'Use for bold',
    });
    assert.deepEqual(
      diffEventFields(
        { descriptionFormat: 'plain', description: 'Use <b> for bold' },
        { descriptionFormat: 'plain', description: 'Use <b> for bold' },
      ),
      [],
    );
  });

  it('exposes stable Simplified Chinese labels for every shared field', () => {
    const before: Record<string, unknown> = {};
    const after: Record<string, unknown> = {};
    for (const key of Object.keys(EVENT_FIELD_LABELS)) {
      before[key] = '旧值';
      after[key] = '新值';
    }
    const entries = diffEventFields(before, after);
    assert.equal(entries.length, Object.keys(EVENT_FIELD_LABELS).length);
    for (const entry of entries) {
      assert.equal(entry.label, EVENT_FIELD_LABELS[entry.key]);
      assert.equal(entry.kind, 'modified');
    }
    assert.deepEqual(EVENT_FIELD_LABELS, {
      title: '标题',
      description: '描述',
      descriptionFormat: '描述格式',
      location: '地点',
      dtStart: '开始时间',
      dtEnd: '结束时间',
      isAllDay: '全天事件',
      timeZoneId: '时区',
      showAs: '显示状态',
      importance: '重要性',
      sensitivity: '敏感度',
      categories: '分类',
      isReminderOn: '提醒',
      reminderMinutesBeforeStart: '提醒提前量',
      organizer: '组织者',
      attendees: '参会者',
      isOnlineMeeting: '在线会议',
      onlineMeetingProvider: '会议提供方',
      onlineMeetingUrl: '会议链接',
      externalLink: '外部链接',
      attachmentReferences: '附件',
    });
  });

  it('never inspects or emits raw metadata, Outlook identifiers or unknown keys', () => {
    const before: Record<string, unknown> = {
      externalMetadataJson: '{"raw":1}',
      outlookEventId: 'graph-1',
      outlookEtag: 'etag-a',
      outlookCalendarBindingId: 'binding-1',
      source: 'outlook',
      uid: 'uid-1',
      rRule: 'FREQ=DAILY',
      mysteryKey: 1,
      title: '旧',
    };
    const after: Record<string, unknown> = {
      externalMetadataJson: '{"raw":2}',
      outlookEventId: 'graph-2',
      outlookEtag: 'etag-b',
      outlookCalendarBindingId: 'binding-2',
      source: 'ics',
      uid: 'uid-2',
      rRule: 'FREQ=WEEKLY',
      mysteryKey: 2,
      title: '新',
    };
    const entries = diffEventFields(before, after);
    assert.equal(entries.length, 1);
    assert.deepEqual(entries[0], { key: 'title', label: '标题', kind: 'modified', before: '旧', after: '新' });
  });

  it('does not mutate its inputs', () => {
    const before: Record<string, unknown> = { description: '<p>旧内容</p>' };
    const after: Record<string, unknown> = { description: '<p>新内容</p>' };
    diffEventFields(before, after);
    assert.equal(before.description, '<p>旧内容</p>');
    assert.equal(after.description, '<p>新内容</p>');
  });
});
