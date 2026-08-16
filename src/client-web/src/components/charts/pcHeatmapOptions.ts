import { chartColors } from './chartColors';
import type { EChartsOption } from '../../lib/echarts';
import { PC_BUSINESS_DAY_START_HOUR, PC_BUSINESS_HOURS, pcHourLabel } from '../../utils/pcBusinessDay';
import type { HeatmapBucket, HeatmapGridResponse, PcActivityAnalysisBlock, TimelineItem } from '../../types';

/**
 * 电脑记录页热力/甘特图表 option 纯函数：输入数据、输出 EChartsOption，不依赖组件/页面。
 * 桶到单元格的映射集中在 mapActivityGrid（纯函数，可测），组件点击通过 data 项携带的
 * bucket/segment/blockIndex 反查原始数据。
 */

const MS_PER_DAY = 24 * 60 * 60 * 1000;
const MONDAY_EPOCH_MS = new Date('1970-01-05T00:00:00').getTime();

/** 周一起算的周内序号（周一=0 … 周日=6） */
function mondayWeekday(date: Date): number {
  return (date.getDay() + 6) % 7;
}

function localDateStr(ms: number): string {
  const d = new Date(ms);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** 桶 start 的本地日期部分（后端写入的日期字面量，取前 10 位即 YYYY-MM-DD） */
export function bucketDatePart(start: string): string {
  return start.slice(0, 10);
}

/** 活动热力单元格：x/y 网格坐标 + 原始桶 + 用于着色的强度值 */
export interface ActivityCell {
  x: number;
  y: number;
  bucket: HeatmapBucket;
  value: number;
}

export interface ActivityGridMap {
  cells: ActivityCell[];
  xLabels: string[];
  yLabels: string[];
}

function parseBucketDate(start: string): Date | null {
  const d = new Date(start);
  return Number.isNaN(d.getTime()) ? null : d;
}

/**
 * 桶 → 单元格映射（纯函数，四维度）：
 * - hour：x = 业务时序号（04:00 起，(hour - 4 + 24) % 24），y = 0；
 * - day：x = 周一起算的周内序号，y = 周一锚定周的行序号（跨周升序）；
 * - month：x = 当月第几天 - 1（1-31），y = 月份行序号（升序）；
 * - year：x = 年内周序号（周一锚定，0..52，GitHub calendar 形态），y = 周内序号。
 */
export function mapActivityGrid(data: HeatmapGridResponse | undefined): ActivityGridMap | null {
  if (!data) return null;
  const buckets = (data.grid ?? [])
    .flatMap(row => Array.isArray(row) ? row : [])
    .filter((c): c is HeatmapBucket => !!c && typeof c.start === 'string')
    .sort((a, b) => a.start.localeCompare(b.start));

  if (buckets.length === 0) return null;

  const dimension = data.dimension || 'day';
  const cells: ActivityCell[] = [];

  if (dimension === 'hour') {
    for (const bucket of buckets) {
      const x = ((bucket.hour - PC_BUSINESS_DAY_START_HOUR) % 24 + 24) % 24;
      cells.push({ x, y: 0, bucket, value: bucket.intensityScore ?? 0 });
    }
    return { cells, xLabels: PC_BUSINESS_HOURS.map(pcHourLabel), yLabels: ['强度'] };
  }

  if (dimension === 'day') {
    const weekKeyOf = (ms: number) => Math.floor((ms - MONDAY_EPOCH_MS) / (7 * MS_PER_DAY));
    const entries = buckets.map(bucket => {
      const d = parseBucketDate(bucket.start);
      if (!d) return null;
      const ms = d.getTime();
      return { bucket, ms, weekday: mondayWeekday(d), weekKey: weekKeyOf(ms - mondayWeekday(d) * MS_PER_DAY) };
    }).filter((e): e is NonNullable<typeof e> => e !== null);
    const minWeek = Math.min(...entries.map(e => e.weekKey));
    for (const e of entries) {
      cells.push({ x: e.weekday, y: e.weekKey - minWeek, bucket: e.bucket, value: e.bucket.intensityScore ?? 0 });
    }
    const maxWeek = Math.max(...entries.map(e => e.weekKey));
    const yLabels: string[] = [];
    for (let w = minWeek; w <= maxWeek; w++) yLabels.push(localDateStr(MONDAY_EPOCH_MS + w * 7 * MS_PER_DAY));
    return { cells, xLabels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'], yLabels };
  }

  if (dimension === 'month') {
    const entries = buckets.map(bucket => {
      const d = parseBucketDate(bucket.start);
      if (!d) return null;
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      return { bucket, key, dayIndex: d.getDate() - 1 };
    }).filter((e): e is NonNullable<typeof e> => e !== null);
    const monthKeys = [...new Set(entries.map(e => e.key))].sort();
    for (const e of entries) {
      cells.push({ x: e.dayIndex, y: monthKeys.indexOf(e.key), bucket: e.bucket, value: e.bucket.intensityScore ?? 0 });
    }
    return { cells, xLabels: Array.from({ length: 31 }, (_, i) => String(i + 1)), yLabels: monthKeys };
  }

  // year：53 周列 × 7 行
  for (const bucket of buckets) {
    const d = parseBucketDate(bucket.start);
    if (!d) continue;
    const yearStart = new Date(d.getFullYear(), 0, 1);
    const dayOfYear = Math.floor((d.getTime() - yearStart.getTime()) / MS_PER_DAY);
    const offset = mondayWeekday(yearStart);
    const weekOfYear = Math.floor((dayOfYear + offset) / 7);
    cells.push({ x: Math.min(weekOfYear, 52), y: mondayWeekday(d), bucket, value: bucket.intensityScore ?? 0 });
  }
  return {
    cells,
    xLabels: Array.from({ length: 53 }, (_, i) => String(i + 1)),
    yLabels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
  };
}

function pad(n: number) {
  return String(n).padStart(2, '0');
}

function formatClock(ms: number): string {
  const d = new Date(ms);
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** 分类时间线甘特：xAxis time、yAxis 段 start 本地小时去重升序行，custom rect（行高一半、白描边、圆角 4）。 */
export function buildCategoryGanttOption(timeline: TimelineItem[]): EChartsOption {
  const segments = timeline.filter(item => item.start && item.end);
  const rows: string[] = [];
  const data: {
    value: [number, number, number];
    itemStyle: { color: string };
    segment: TimelineItem;
  }[] = [];
  for (const item of segments) {
    const start = new Date(item.start);
    const end = new Date(item.end);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) continue;
    const hourLabel = `${pad(start.getHours())}:00`;
    let rowIdx = rows.indexOf(hourLabel);
    if (rowIdx === -1) {
      rows.push(hourLabel);
      rows.sort();
      rowIdx = rows.indexOf(hourLabel);
    }
    data.push({
      value: [rowIdx, start.getTime(), end.getTime()],
      itemStyle: { color: item.categoryColor || '#94a3b8' },
      segment: item,
    });
  }

  const option: EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: (params: unknown) => {
        const p = (Array.isArray(params) ? params[0] : params) as { data?: { segment?: TimelineItem } } | undefined;
        const seg = p?.data?.segment;
        if (!seg) return '';
        const lines = [`${seg.categoryName || '其他'} · ${seg.appName || '未知应用'}`];
        if (seg.windowTitle) lines.push(seg.windowTitle);
        const start = new Date(seg.start);
        const end = new Date(seg.end);
        lines.push(`${formatClock(start.getTime())} - ${formatClock(end.getTime())} · ${Math.round(seg.durationMinutes)} 分钟`);
        return lines.join('\n');
      },
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      textStyle: { color: '#fff', fontSize: 11 },
    },
    grid: { left: 40, right: 12, top: 8, bottom: 22 },
    xAxis: [
      {
        type: 'time',
        axisLabel: { fontSize: 10, color: chartColors.textMuted },
        axisLine: { lineStyle: { color: chartColors.borderSoft } },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: chartColors.borderSoft } },
      },
    ],
    yAxis: [
      {
        type: 'category',
        data: rows,
        axisLabel: { fontSize: 10, color: chartColors.textMuted },
        axisLine: { lineStyle: { color: chartColors.borderSoft } },
        axisTick: { show: false },
      },
    ],
    series: [
      {
        type: 'custom',
        // value = [小时行索引, startMs, endMs]：y 取维度 0，x 取维度 1-2 的时间区间，
        // 让 time 轴 min/max 与刻度按真实区间计算
        encode: { x: [1, 2], y: 0 },
        renderItem: (params: unknown, api: unknown) => {
          const p = params as { value?: number[]; data?: { itemStyle?: { color?: string } } };
          const a = api as { coord?: (v: number[]) => number[]; size?: (v: number[]) => number[] };
          const value = Array.isArray(p.value) ? p.value : [0, 0, 0];
          const yIdx = Number(value[0]) || 0;
          const startMs = Number(value[1]);
          const endMs = Number(value[2]);
          let x = 0;
          let y = 0;
          let width = 4;
          let rowH = 44;
          try {
            const start = a.coord ? a.coord([startMs, yIdx]) : [0, 0];
            const end = a.coord ? a.coord([endMs, yIdx]) : [0, 0];
            const size = a.size ? a.size([0, 1]) : [1, 44];
            rowH = Number(size[1]) || 44;
            x = start[0];
            width = Math.max(end[0] - start[0], 2);
            y = start[1];
          } catch {
            // 纯函数环境下 api 不可用时返回占位 rect，不影响 option 结构断言
          }
          const fill = p.data?.itemStyle?.color || '#94a3b8';
          return {
            type: 'rect',
            shape: { x, y: y - rowH * 0.25, width, height: rowH * 0.5, r: 4 },
            style: { fill, stroke: '#ffffff', lineWidth: 1 },
          };
        },
        data,
        emphasis: { focus: 'series' },
      },
    ],
  };
  return option as EChartsOption;
}

/** 活动热力图：四维度统一入口，色阶 chartColors.githubGreen，visualMap max 取 maxKeyCount。 */
export function buildActivityHeatmapOption(data: HeatmapGridResponse | undefined): EChartsOption {
  if (!data) {
    return { series: [{ type: 'heatmap', data: [] }] } as EChartsOption;
  }
  const map = mapActivityGrid(data);
  if (!map) {
    return { series: [{ type: 'heatmap', data: [] }] } as EChartsOption;
  }
  const dimension = data.dimension || 'day';
  const grid =
    dimension === 'hour' ? { left: 34, right: 8, top: 8, bottom: 24 } :
    dimension === 'day' ? { left: 66, right: 8, top: 8, bottom: 16 } :
    dimension === 'month' ? { left: 58, right: 8, top: 8, bottom: 20 } :
    { left: 34, right: 8, top: 8, bottom: 10 };
  const axisLabelInterval = dimension === 'hour' ? 2 : dimension === 'month' ? 4 : undefined;

  const option: EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: (params: unknown) => {
        const p = (Array.isArray(params) ? params[0] : params) as { data?: { bucket?: HeatmapBucket } } | undefined;
        const bucket = p?.data?.bucket;
        if (!bucket) return '';
        return `${bucketDatePart(bucket.start)} · ${bucket.intensityScore ?? 0} 次输入 · ${bucket.activeMinutes ?? 0} 分钟`;
      },
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      textStyle: { color: '#fff', fontSize: 11 },
    },
    grid,
    xAxis: [
      {
        type: 'category',
        data: map.xLabels,
        show: dimension !== 'year',
        axisLabel: { fontSize: 10, color: chartColors.textMuted, interval: axisLabelInterval },
        axisLine: { lineStyle: { color: chartColors.borderSoft } },
        axisTick: { show: false },
      },
    ],
    yAxis: [
      {
        type: 'category',
        data: map.yLabels,
        axisLabel: { fontSize: 10, color: chartColors.textMuted },
        axisLine: { lineStyle: { color: chartColors.borderSoft } },
        axisTick: { show: false },
      },
    ],
    visualMap: {
      min: 0,
      max: Math.max(data.maxKeyCount || 1, 1),
      calculable: false,
      show: false,
      inRange: { color: chartColors.githubGreen },
    },
    series: [
      {
        type: 'heatmap',
        data: map.cells.map(cell => ({ value: [cell.x, cell.y, cell.value], bucket: cell.bucket })),
        itemStyle: { borderColor: '#ffffff', borderWidth: 0.5 },
        emphasis: { itemStyle: { shadowBlur: 4, shadowColor: 'rgba(0, 0, 0, 0.3)' } },
        label: { show: false },
      },
    ],
  };
  return option as EChartsOption;
}

/** 时间块热力：x = 块序号 1..n，y 单行，value = intensityScore，色阶沿用现有 0-4 五档青绿。 */
export function buildAnalysisBlocksOption(
  blocks: PcActivityAnalysisBlock[],
  selectedStart?: string | null,
): EChartsOption {
  const option: EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: (params: unknown) => {
        const p = (Array.isArray(params) ? params[0] : params) as { data?: { blockIndex?: number } } | undefined;
        const idx = p?.data?.blockIndex;
        const block = idx !== undefined ? blocks[idx] : undefined;
        if (!block) return '';
        const start = new Date(block.start);
        const end = new Date(block.end);
        const activeMinutes = Math.round(block.activeDurationSeconds / 60);
        return `${formatClock(start.getTime())} - ${formatClock(end.getTime())} · ${activeMinutes} 活跃分钟\n${block.pendingClassificationCount} 条待分类 · ${block.contextSwitchCount} 次上下文切换`;
      },
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      textStyle: { color: '#fff', fontSize: 11 },
    },
    grid: { left: 24, right: 8, top: 8, bottom: 18 },
    xAxis: [
      {
        type: 'category',
        data: blocks.map((_, i) => i + 1),
        axisLabel: { fontSize: 10, color: chartColors.textMuted },
        axisLine: { lineStyle: { color: chartColors.borderSoft } },
        axisTick: { show: false },
      },
    ],
    yAxis: [
      {
        type: 'category',
        data: ['强度'],
        show: false,
      },
    ],
    visualMap: {
      min: 0,
      max: 4,
      calculable: false,
      show: false,
      inRange: { color: ['#f8fafc', '#d9f2ec', '#9fdacf', '#43afa3', '#0f8f88'] },
    },
    series: [
      {
        type: 'heatmap',
        data: blocks.map((block, i) => ({
          value: [i, 0, block.intensityScore ?? 0],
          blockIndex: i,
          itemStyle:
            selectedStart && block.start === selectedStart
              ? { borderColor: chartColors.primary, borderWidth: 2 }
              : block.pendingClassificationCount > 0
                ? { borderColor: chartColors.warning, borderWidth: 1 }
                : { borderColor: '#ffffff', borderWidth: 0.5 },
        })),
        itemStyle: { borderColor: '#ffffff', borderWidth: 0.5 },
        label: { show: false },
      },
    ],
  };
  return option as EChartsOption;
}
