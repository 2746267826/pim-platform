/**
 * #285 今日页布局回归护栏：长板块不得把同行卡片撑出成片空白。
 *
 * 根因：TodayPage 原来把全部区块放进一个 `grid ... xl:grid-cols-4`。CSS Grid 默认
 * `align-items: stretch`，同一行的卡片会被拉伸到与该行最高卡片同高；于是「任务关注」
 * （19 项）与「分类建议」（长列表）所在行的短卡片（今日安排 / PC 记录概览 / 系统健康 /
 * PC 数据质量）下方出现大片空白，整页被拉得极长。
 *
 * 同类问题 WorkbenchPage 已处理（#192：两列独立布局 + 长列表独立滚动）。
 * 工单《docs/pim-webui-redesign-20260902.zip》要求：「左右列取消 CSS Grid 强制等高拉伸」
 * 「长列表独立滚动，无论内容多少均不撑大整页视口」。
 *
 * 因此本用例的判定标准是**结构性的**，而不是「类名里不许出现 grid」：
 *   1. 容器必须显式 items-start（取消行内等高拉伸）；
 *   2. 区块必须按列独立堆叠，而非直接平铺进同一个网格；
 *   3. 长列表板块必须自带独立滚动容器。
 */
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import {
  distributeTodaySections,
  isWideTodaySection,
  todayColumnCount,
} from '../../src/client-web/src/pages/todaySectionLayout';
import type { TodaySectionRegistryItem } from '../../src/client-web/src/types';

function test(name: string, run: () => void) { run(); }

const read = (relative: string) => readFileSync(path.join(process.cwd(), relative), 'utf8');
const todayPage = read('src/client-web/src/pages/TodayPage.tsx');

function section(kind: string, id = kind): TodaySectionRegistryItem {
  return { id, kind, title: kind } as TodaySectionRegistryItem;
}

test('#285 区块容器显式 items-start（取消 CSS Grid 强制等高拉伸）', () => {
  // 关键：只要容器仍允许 stretch，同行短卡片就会被最高卡片撑高。
  assert.ok(
    /items-start/.test(todayPage),
    '今日页区块容器必须显式 items-start',
  );
});

test('#285 区块按列独立堆叠，而不是平铺进同一个网格', () => {
  // 每一列是一个独立的纵向容器（flex-col / space-y-*），列内堆叠、列间互不拉伸。
  assert.ok(
    /today-column-/.test(todayPage),
    '今日页应把区块渲染进按列划分的独立容器（today-column-*）',
  );
  assert.ok(
    /distributeTodaySections/.test(todayPage),
    '应使用 distributeTodaySections 做列分配',
  );
});

test('#285 长板块（任务关注 / 分类建议）独占一列', () => {
  const columns = distributeTodaySections(
    [
      section('calendar.schedule'),
      section('calendar.tasks'),
      section('operations.health'),
      section('pc.quality'),
      section('pc.classification_suggestions'),
    ],
    4,
  );

  const findColumn = (kind: string) => columns.findIndex(col => col.some(s => s.kind === kind));
  const taskCol = findColumn('calendar.tasks');
  const suggestionCol = findColumn('pc.classification_suggestions');

  assert.notEqual(taskCol, -1);
  assert.notEqual(suggestionCol, -1);
  assert.notEqual(taskCol, suggestionCol, '两个长板块应分处不同列（各自独占）');

  for (const col of columns) {
    const longs = col.filter(s => ['calendar.tasks', 'pc.classification_suggestions'].includes(s.kind));
    assert.ok(longs.length <= 1, `同一列不应堆叠两个长板块：${col.map(s => s.kind).join(', ')}`);
  }
});

test('#285 短卡片被均衡分配（不出现某列堆满、另一列空置）', () => {
  const sections = [
    section('calendar.schedule'),
    section('calendar.tasks'),
    section('operations.health'),
    section('pc.quality'),
    section('pc.classification_suggestions'),
  ];
  const columns = distributeTodaySections(sections, 4);

  // 所有区块都必须被分配一次（不丢区块）
  const assigned = columns.flat().map(s => s.kind).sort();
  assert.deepEqual(assigned, sections.map(s => s.kind).sort());

  const sizes = columns.map(c => c.length);
  assert.ok(Math.max(...sizes) - Math.min(...sizes) <= 1,
    `列内区块数应尽量均衡，实际 ${sizes.join(', ')}`);
});

test('#285 跨列区块仍按整行渲染（行内只有它自己，无法被拉伸）', () => {
  assert.equal(isWideTodaySection('pc.activity'), true);
  assert.equal(isWideTodaySection('calendar.tasks'), false);
  assert.ok(/wideSections/.test(todayPage), 'TodayPage 应单独渲染跨列区块');
});

test('#285 各密度模式的列数与修复前一致（视觉密度不缩水）', () => {
  assert.equal(todayColumnCount('focus'), 3);
  assert.equal(todayColumnCount('dense'), 4);
  assert.equal(todayColumnCount('standard'), 4);
});

test('#285 长列表板块拥有独立滚动容器', () => {
  const taskColumn = read('src/client-web/src/components/today/TodayTaskColumn.tsx');
  assert.ok(
    /max-h-\[/.test(taskColumn) && /overflow-y-auto/.test(taskColumn),
    '「任务关注」应有 max-h-* + overflow-y-auto 的独立滚动容器',
  );
});

test('#285 展示的区块种类未缩水', () => {
  const host = read('src/client-web/src/components/today/TodaySectionHost.tsx');
  for (const kind of [
    'calendar.schedule',
    'pc.activity',
    'calendar.tasks',
    'operations.health',
    'pc.quality',
    'pc.classification_suggestions',
  ]) {
    assert.ok(host.includes(kind), `区块种类 ${kind} 应保留`);
  }
});

test('#285 其他既有能力未被破坏', () => {
  for (const kept of ['日程任务工作台', 'densityMode', '高密度', '专注']) {
    assert.ok(todayPage.includes(kept), `今日页应保留「${kept}」`);
  }
});

console.log('todayLayout tests passed');
