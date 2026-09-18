import assert from 'node:assert/strict';
import path from 'node:path';
import { createRequire } from 'node:module';
import { buildCategoryGanttOption } from '../../src/client-web/src/components/charts/pcHeatmapOptions';
import type { TimelineItem } from '../../src/client-web/src/types';

const requireFromClient = createRequire(path.join(process.cwd(), 'src/client-web/package.json'));
const echarts = requireFromClient('echarts') as typeof import('echarts');

function test(name: string, run: () => void) { run(); }

function timelineItem(start: string, end: string, categoryColor = '#6B5EE4'): TimelineItem {
  return {
    start,
    end,
    durationMinutes: Math.round((new Date(end).getTime() - new Date(start).getTime()) / 60000),
    appName: 'Code.exe',
    windowTitle: '编辑器',
    categoryName: '编程',
    categoryColor,
    projectTag: null,
    classificationConfidence: 1,
    classificationSource: 'builtin',
    classificationExplanation: '',
  };
}

function renderAndCapture(option: unknown, width = 900, height = 240) {
  const captured: Array<{ x: number; width: number; fill: string | undefined; y: number }> = [];
  const series = (option as { series: Array<Record<string, unknown>> }).series;
  const original = series[0].renderItem as (params: unknown, api: unknown) => unknown;
  series[0].renderItem = (params: unknown, api: unknown) => {
    const result = original(params, api) as { shape?: { x: number; y: number; width: number }; style?: { fill?: string } };
    captured.push({
      x: Number(result.shape?.x),
      y: Number(result.shape?.y),
      width: Number(result.shape?.width),
      fill: result.style?.fill,
    });
    return result;
  };
  const chart = echarts.init(null, null, { renderer: 'svg', ssr: true, width, height });
  let svg = '';
  try {
    chart.setOption(option as never);
    svg = chart.renderToSVGString();
  } finally {
    chart.dispose();
  }
  return { captured, svg };
}

const crossing = timelineItem('2026-08-15T13:36:00', '2026-08-15T14:02:00');

test('跨小时段拆成 13:36→60 与 14:00→2，轴为分钟且行升序', () => {
  const option = buildCategoryGanttOption([crossing]) as any;
  assert.equal(option.xAxis[0].type, 'value');
  assert.equal(option.xAxis[0].min, 0);
  assert.equal(option.xAxis[0].max, 60);
  assert.deepEqual(option.yAxis[0].data, ['13:00', '14:00']);
  assert.deepEqual(option.series[0].data.map((d: any) => d.value), [[36, 60, 0], [0, 2, 1]]);
  assert.equal(option.series[0].data[0].itemStyle.color, '#6B5EE4');
});

test('默认只显示有数据的小时，全时段切换显示 0–23 行', () => {
  const timeline = [crossing, timelineItem('2026-08-15T09:05:00', '2026-08-15T09:10:00', '#F59E0B')];
  const onlyData = buildCategoryGanttOption(timeline) as any;
  assert.deepEqual(onlyData.yAxis[0].data, ['09:00', '13:00', '14:00']);

  const allHours = buildCategoryGanttOption(timeline, true) as any;
  assert.equal(allHours.yAxis[0].data.length, 24);
  assert.equal(allHours.yAxis[0].data[0], '00:00');
  assert.equal(allHours.yAxis[0].data[23], '23:00');
  assert.deepEqual(allHours.series[0].data.map((d: any) => d.value), [[36, 60, 13], [0, 2, 14], [5, 10, 9]]);
});

test('跨小时的两条自定义图形在 SSR 画布内并使用分类色', () => {
  const { captured, svg } = renderAndCapture(buildCategoryGanttOption([crossing]));
  assert.equal(captured.length, 2);
  for (const rect of captured) {
    assert.ok(Number.isFinite(rect.x) && rect.x >= 0 && rect.x <= 900, `x 应在画布内，实际 ${rect.x}`);
    assert.ok(rect.width > 2, `条宽应大于 2px，实际 ${rect.width}`);
    assert.equal(rect.fill, '#6B5EE4');
  }
  assert.ok(svg.includes('#6B5EE4'));
});

console.log('categoryTimelineRedesign tests passed');
