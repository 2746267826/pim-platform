import type { TodaySectionKind, TodaySectionRegistryItem } from '../types';

/**
 * 今日页区块的列分配（#285）。
 *
 * 背景：原实现把区块放进 `grid xl:grid-cols-4`，CSS Grid 默认 `align-items: stretch`
 * 会把同一行的短卡片拉伸到与该行最高卡片同高，于是「任务关注 / 分类建议」这类长板块
 * 会把同行卡片撑出成片空白，整页被拉得极长（#285，同类问题见 #192）。
 *
 * 做法：按「预估高度权重」把区块贪心分配到若干**独立列**中（每列是独立的纵向堆叠，
 * 列之间互不影响，配合容器 `items-start` 即不再有强制等高拉伸）。
 * 长板块权重高，贪心分配会让它们各自占据一列，短卡片则两两堆叠，避免大片空白。
 *
 * 同时保留 `pc.activity` 的「跨列加宽」语义：它由调用方按 `isWideTodaySection`
 * 渲染为整行（见 TodayPage），行内只有一个区块，同样不会被拉伸。
 */

/** 长板块：内容条数不可控，必须独占一列（并在组件内自带独立滚动）。 */
const LONG_SECTION_KINDS: readonly TodaySectionKind[] = [
  'calendar.tasks',
  'pc.classification_suggestions',
];

/** 跨列加宽的区块（保持原有视觉权重）。 */
const WIDE_SECTION_KINDS: readonly TodaySectionKind[] = ['pc.activity'];

function sectionWeight(kind: TodaySectionKind | string): number {
  if (LONG_SECTION_KINDS.includes(kind as TodaySectionKind)) return 3;
  if (WIDE_SECTION_KINDS.includes(kind as TodaySectionKind)) return 2;
  return 1;
}

/** 该区块是否应按「整行加宽」渲染（自身独占一行，行内无其他卡片可被拉伸）。 */
export function isWideTodaySection(kind: TodaySectionKind | string): boolean {
  return WIDE_SECTION_KINDS.includes(kind as TodaySectionKind);
}

/**
 * 把区块分配到 `columnCount` 个独立列：每次把下一个区块放进当前「预估高度最小」的列，
 * 从而得到高度均衡、且长板块各占一列的结果。输入顺序被保留（今日页区块顺序有意义）。
 */
export function distributeTodaySections(
  sections: readonly TodaySectionRegistryItem[],
  columnCount: number,
): TodaySectionRegistryItem[][] {
  const count = Math.max(1, Math.floor(columnCount));
  const columns: TodaySectionRegistryItem[][] = Array.from({ length: count }, () => []);
  const heights = new Array<number>(count).fill(0);

  for (const section of sections) {
    let target = 0;
    for (let i = 1; i < count; i++) {
      if (heights[i] < heights[target]) target = i;
    }
    columns[target].push(section);
    heights[target] += sectionWeight(section.kind);
  }

  return columns;
}

/** 各密度模式下的列数（与修复前的 xl 断点列数保持一致）。 */
export function todayColumnCount(densityMode: 'focus' | 'dense' | 'standard'): number {
  return densityMode === 'focus' ? 3 : 4;
}
