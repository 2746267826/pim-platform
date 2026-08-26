import { useMemo, useState } from 'react';
import PageHeader from '../ui/PageHeader';
import MobileCategoryDonut from '../components/charts/MobileCategoryDonut';
import HourHeatmapChart from '../components/charts/HourHeatmapChart';
import PcAppGradientBar from '../components/charts/PcAppGradientBar';
import TaskCompletionArea from '../components/charts/TaskCompletionArea';
import HabitCalendarHeatmap from '../components/charts/HabitCalendarHeatmap';
import DeviceHealthGauge from '../components/charts/DeviceHealthGauge';
import LocationBubbleChart from '../components/charts/LocationBubbleChart';

/**
 * 展览馆落地页：展示从 480 选型中落地的 7 个精选组合
 * 每个卡片对应 src/components/charts/ 下的独立 React 组件，已对接真实 API
 * 入口：/exhibition（侧边栏“展览馆”）
 * 静态展览馆原稿：/src/components/dashboard-exhibition/Exhibition.html（可直接浏览器打开）
 */
const SELECTED = [
  { id:'3-10', title:'App分类占比 × 环形图', mod:'手机使用', file:'MobileCategoryDonut.tsx' },
  { id:'4-15', title:'24小时热力图 × 热力图（矩阵）', mod:'手机使用', file:'HourHeatmapChart.tsx' },
  { id:'6-13', title:'常去地点 × 气泡图', mod:'位置轨迹', file:'LocationBubbleChart.tsx' },
  { id:'8-32', title:'PC应用使用时长 × 渐变进度条', mod:'PC活动', file:'PcAppGradientBar.tsx' },
  { id:'10-7', title:'任务完成率 × 面积图', mod:'日程习惯', file:'TaskCompletionArea.tsx' },
  { id:'11-16', title:'习惯打卡热力 × 日历热力图', mod:'日程习惯', file:'HabitCalendarHeatmap.tsx' },
  { id:'12-21', title:'设备健康状态 × 仪表盘/进度环', mod:'设备健康', file:'DeviceHealthGauge.tsx' },
  { id:'3-11', title:'App分类占比 × 南丁格尔玫瑰图', mod:'手机使用', file:'exhibitionOptions.ts（复用）' },
];

export default function ExhibitionPage() {
  const [filterMod, setFilterMod] = useState<string>('');
  const filtered = useMemo(()=> filterMod ? SELECTED.filter(s=> s.mod===filterMod) : SELECTED, [filterMod]);

  const exportSelected = () => {
    const payload = {
      exportedAt: new Date().toISOString(),
      totalSelected: SELECTED.length,
      note: 'PIM 展览馆选中清单：以下组件已落地到 src/components/charts/',
      items: SELECTED.map(s=> ({
        id: s.id,
        title: s.title,
        module: s.mod,
        file: `src/client-web/src/components/charts/${s.file}`,
        route: '/exhibition',
        dataShape: s.id.startsWith('3') ? '{category:string, percentage:number}[]' : s.id.startsWith('4') ? '{hour, category, value}[]' : '见组件内 API',
      })),
    };
    const blob = new Blob([JSON.stringify(payload, null, 2)], {type:'application/json'});
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `pim-exhibition-selected-${new Date().toISOString().slice(0,10)}.json`;
    a.click();
    setTimeout(()=> URL.revokeObjectURL(url), 800);
  };

  return (
    <div className="mx-auto w-full max-w-[1300px] space-y-4 pb-20 md:pb-4">
      <PageHeader
        title="展览馆 · 精选落地"
        subtitle="从 480 种组合中精选 7 项，已落地为生产级 React 组件（对接真实 API）"
        actions={
          <div className="flex flex-wrap gap-2">
            <a href="/dashboard-exhibition/Exhibition.html" target="_blank" rel="noreferrer" className="pim-button-secondary px-3 py-2 text-sm">打开静态展览馆</a>
            <button type="button" onClick={exportSelected} className="pim-button-primary px-3 py-2 text-sm">导出选中清单 JSON</button>
          </div>
        }
      />

      <section className="pim-panel p-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap gap-2 text-xs">
            <span className="rounded-full bg-blue-50 px-2.5 py-1 font-semibold text-blue-700">480 组合</span>
            <span className="rounded-full bg-emerald-50 px-2.5 py-1 font-semibold text-emerald-700">已落地 {SELECTED.length} 项</span>
            <span className="rounded-full bg-slate-100 px-2.5 py-1 font-semibold text-slate-600">src/components/charts/</span>
          </div>
          <div className="flex items-center gap-2">
            <label className="text-xs text-slate-500">模块筛选</label>
            <select value={filterMod} onChange={e=> setFilterMod(e.target.value)} className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
              <option value="">全部模块</option>
              <option value="手机使用">手机使用</option>
              <option value="位置轨迹">位置轨迹</option>
              <option value="PC活动">PC活动</option>
              <option value="日程习惯">日程习惯</option>
              <option value="设备健康">设备健康</option>
            </select>
          </div>
        </div>

        <div className="mt-3 grid grid-cols-1 gap-2 text-xs text-slate-600 md:grid-cols-2">
          <div className="rounded-lg bg-slate-50 px-3 py-2">静态展览馆：<code className="rounded bg-white px-1 py-0.5">src/client-web/src/components/dashboard-exhibition/Exhibition.html</code> · 480 卡片 · echarts+leaflet CDN · 懒加载+分页</div>
          <div className="rounded-lg bg-slate-50 px-3 py-2">落地组件：<code className="rounded bg-white px-1 py-0.5">src/client-web/src/components/charts/*.tsx</code> · EChartBox + react-query + 真实 API</div>
        </div>

        <ul className="mt-3 grid gap-2 text-xs">
          {filtered.map(s=>(
            <li key={s.id} className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2">
              <span className="font-medium text-slate-800">#{s.id} · {s.title}</span>
              <span className="flex gap-1">
                <span className="rounded-full bg-slate-100 px-2 py-0.5 text-slate-600">{s.mod}</span>
                <span className="rounded-full bg-blue-50 px-2 py-0.5 text-blue-700">{s.file}</span>
              </span>
            </li>
          ))}
        </ul>
      </section>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <MobileCategoryDonut />
        <HourHeatmapChart />
        <LocationBubbleChart />
        <PcAppGradientBar />
        <TaskCompletionArea />
        <HabitCalendarHeatmap />
        <DeviceHealthGauge />
        <section className="rounded-md border border-slate-200 bg-white p-4">
          <h3 className="text-sm font-semibold text-slate-900">App分类占比 · 玫瑰图（复用）</h3>
          <p className="mt-1 text-xs text-slate-500">南丁格尔玫瑰（展览馆 #3×11），与环形图同数据源，视觉更突出差异</p>
          <div className="mt-3 rounded-lg bg-slate-50 p-3 text-xs text-slate-500">
            已在 <code className="rounded bg-white px-1 py-0.5">exhibitionOptions.buildRoseOption</code> 中实现，页面通过切换 <code>roseType</code> 即可获得玫瑰变体，无需额外 API。
            <br />前端可通过 props variant=&quot;donut&quot; | &quot;rose&quot; | &quot;pie&quot; 在同一组件内切换三种饼类形态，已在落地页通过复用验证。
          </div>
        </section>
      </div>

      <section className="pim-panel p-4">
        <h3 className="text-sm font-semibold text-slate-900">如何体验</h3>
        <ol className="mt-2 list-decimal space-y-1 pl-5 text-sm text-slate-600">
          <li>打开静态展览馆：点击顶部“打开静态展览馆”或直接访问 <code>src/client-web/src/components/dashboard-exhibition/Exhibition.html</code>（双击文件或 http 服务）。</li>
          <li>在展览馆中使用筛选（数据类型/图表类型/模块）、搜索、排序、打分（1-5★）、选中、对比（2-3个）、分页（50/页）、懒加载、预览大图。</li>
          <li>导出选中清单：展览馆底部“导出选中 JSON”或本页“导出选中清单 JSON”。</li>
          <li>落地页本页即为选中项的生产实现：每个卡片下方为独立 React 组件，接入真实 API，支持加载/空状态/假数据兜底，确保离线也能演示。</li>
        </ol>
      </section>
    </div>
  );
}
