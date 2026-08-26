import { useEffect, useMemo, useState } from 'react';
import PageHeader from '../ui/PageHeader';
import MobileCategoryDonut from '../components/charts/MobileCategoryDonut';
import HourHeatmapChart from '../components/charts/HourHeatmapChart';
import PcAppGradientBar from '../components/charts/PcAppGradientBar';
import TaskCompletionArea from '../components/charts/TaskCompletionArea';
import HabitCalendarHeatmap from '../components/charts/HabitCalendarHeatmap';
import DeviceHealthGauge from '../components/charts/DeviceHealthGauge';
import LocationBubbleChart from '../components/charts/LocationBubbleChart';
import WeekTrendLine from '../components/charts/WeekTrendLine';
import WeekTrendArea from '../components/charts/WeekTrendArea';
import CategoryStackedBar from '../components/charts/CategoryStackedBar';
import DayHeatmapMatrix from '../components/charts/DayHeatmapMatrix';
import GpsTrackMap from '../components/charts/GpsTrackMap';
import LocationHeatmap from '../components/charts/LocationHeatmap';
import SpeedHistogram from '../components/charts/SpeedHistogram';
import PcAppDonut from '../components/charts/PcAppDonut';
import KeyboardHeatmap from '../components/charts/KeyboardHeatmap';
import HabitStreakRing from '../components/charts/HabitStreakRing';
import DeviceStatusTimeline from '../components/charts/DeviceStatusTimeline';
import TaskFunnel from '../components/charts/TaskFunnel';
import DataQualityGauge from '../components/charts/DataQualityGauge';

/**
 * 展览馆 React 画廊页：复用 20+ 落地组件做网格，支持筛选/打分/选中/导出
 * 复用静态展览馆的 localStorage key：exhibition_ratings / exhibition_selected，保证“挑”与“落”打通
 * 静态展览馆：src/client-web/src/components/dashboard-exhibition/Exhibition.html（双击可开，CDN echarts）
 */
type GalleryItem = {
  id: string;
  title: string;
  module: string;
  dataType: string;
  chartType: string;
  file: string;
  Component: React.ComponentType<Record<string,never>>;
  desc: string;
};

const GALLERY: GalleryItem[] = [
  { id:'1-10', title:'日使用时长 × 环形图', module:'手机使用', dataType:'日使用时长分布', chartType:'环形图', file:'MobileCategoryDonut.tsx', Component: MobileCategoryDonut, desc:' TopApp 长尾：微信>VS Code>抖音' },
  { id:'2-5', title:'周趋势 × 折线图', module:'手机使用', dataType:'周使用趋势', chartType:'折线图', file:'WeekTrendLine.tsx', Component: WeekTrendLine, desc:' 周一/周五高峰、周末低谷' },
  { id:'2-7', title:'周趋势 × 面积图', module:'手机使用', dataType:'周使用趋势', chartType:'面积图', file:'WeekTrendArea.tsx', Component: WeekTrendArea, desc:' 堆叠面积展示周周期' },
  { id:'3-3', title:'分类占比 × 堆叠柱状', module:'手机使用', dataType:'App分类占比', chartType:'堆叠柱状图', file:'CategoryStackedBar.tsx', Component: CategoryStackedBar, desc:' 8类和100%，聊天+视频占大头' },
  { id:'3-10', title:'分类占比 × 环形图', module:'手机使用', dataType:'App分类占比', chartType:'环形图', file:'MobileCategoryDonut.tsx', Component: MobileCategoryDonut, desc:' 8类占比，聊天27.5%+视频21.3%' },
  { id:'4-15', title:'24h热力 × 矩阵热力', module:'手机使用', dataType:'24小时热力图', chartType:'热力图（矩阵）', file:'DayHeatmapMatrix.tsx', Component: DayHeatmapMatrix, desc:' 8-12/19-23双峰' },
  { id:'4-15b', title:'24h热力 × 时段热力', module:'手机使用', dataType:'24小时热力图', chartType:'热力图（矩阵）', file:'HourHeatmapChart.tsx', Component: HourHeatmapChart, desc:' 按小时×分类' },
  { id:'5-12', title:'GPS轨迹 × 散点/线图', module:'位置轨迹', dataType:'GPS轨迹地图', chartType:'散点图', file:'GpsTrackMap.tsx', Component: GpsTrackMap, desc:' 北京39.8-40.1/116.2-116.6连续轨迹' },
  { id:'5-40', title:'GPS轨迹 × 六边形分箱', module:'位置轨迹', dataType:'GPS轨迹地图', chartType:'六边形分箱', file:'LocationHeatmap.tsx', Component: LocationHeatmap, desc:' 轨迹密度分箱' },
  { id:'6-13', title:'常去地点 × 气泡图', module:'位置轨迹', dataType:'常去地点气泡图', chartType:'气泡图', file:'LocationBubbleChart.tsx', Component: LocationBubbleChart, desc:' 家128 公司96 学校42' },
  { id:'7-24', title:'速度分布 × 直方图', module:'位置轨迹', dataType:'速度分布', chartType:'柱状图', file:'SpeedHistogram.tsx', Component: SpeedHistogram, desc:' 步行<5 骑行15-25 高铁250-350' },
  { id:'8-32', title:'PC应用时长 × 渐变条', module:'PC活动', dataType:'PC应用使用时长', chartType:'渐变进度条', file:'PcAppGradientBar.tsx', Component: PcAppGradientBar, desc:' VS Code/Chrome占大头，AFK单独' },
  { id:'8-10', title:'PC应用时长 × 环形图', module:'PC活动', dataType:'PC应用使用时长', chartType:'环形图', file:'PcAppDonut.tsx', Component: PcAppDonut, desc:' PC 6类占比' },
  { id:'9-15', title:'键盘热力 × 矩阵热力', module:'PC活动', dataType:'键盘热力图', chartType:'热力图（矩阵）', file:'KeyboardHeatmap.tsx', Component: KeyboardHeatmap, desc:' QWERTY真实频率 Space最高' },
  { id:'10-7', title:'任务完成率 × 面积图', module:'日程习惯', dataType:'任务完成率', chartType:'面积图', file:'TaskCompletionArea.tsx', Component: TaskCompletionArea, desc:' 60-90%波动' },
  { id:'10-20', title:'任务完成率 × 漏斗图', module:'日程习惯', dataType:'任务完成率', chartType:'漏斗图', file:'TaskFunnel.tsx', Component: TaskFunnel, desc:' 创建→进行→完成' },
  { id:'11-16', title:'习惯打卡 × 日历热力', module:'日程习惯', dataType:'习惯打卡热力', chartType:'日历热力图', file:'HabitCalendarHeatmap.tsx', Component: HabitCalendarHeatmap, desc:' 5习惯×30天 40-80%' },
  { id:'11-31', title:'习惯打卡 × 进度环', module:'日程习惯', dataType:'习惯打卡热力', chartType:'进度环', file:'HabitStreakRing.tsx', Component: HabitStreakRing, desc:' 连续天数+打卡率' },
  { id:'12-21', title:'设备健康 × 仪表盘', module:'设备健康', dataType:'设备健康状态', chartType:'仪表盘', file:'DeviceHealthGauge.tsx', Component: DeviceHealthGauge, desc:' 2在线1离线1告警' },
  { id:'12-26', title:'设备健康 × 状态条', module:'设备健康', dataType:'设备健康状态', chartType:'平行坐标/条形', file:'DeviceStatusTimeline.tsx', Component: DeviceStatusTimeline, desc:' 在线/离线/告警' },
  { id:'12-21b', title:'数据质量 × 仪表盘', module:'设备健康', dataType:'设备健康状态', chartType:'仪表盘', file:'DataQualityGauge.tsx', Component: DataQualityGauge, desc:' 数据质量评分' },
];

function loadRatings(): Map<string, number> {
  try {
    const raw = localStorage.getItem('exhibition_ratings');
    if (raw) return new Map(JSON.parse(raw));
  } catch {}
  return new Map();
}
function loadSelected(): Set<string> {
  try {
    const raw = localStorage.getItem('exhibition_selected');
    if (raw) return new Set(JSON.parse(raw));
  } catch {}
  return new Set();
}

export default function ExhibitionPage() {
  const [moduleFilter, setModuleFilter] = useState('');
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState<'default'|'ratingDesc'|'ratingAsc'>('default');
  const [ratings, setRatings] = useState<Map<string,number>>(()=> loadRatings());
  const [selected, setSelected] = useState<Set<string>>(()=> loadSelected());

  useEffect(()=>{ try{ localStorage.setItem('exhibition_ratings', JSON.stringify([...ratings.entries()])); }catch{} },[ratings]);
  useEffect(()=>{ try{ localStorage.setItem('exhibition_selected', JSON.stringify([...selected])); }catch{} },[selected]);

  const filtered = useMemo(()=>{
    let list = GALLERY.filter(g=>{
      if(moduleFilter && g.module!==moduleFilter) return false;
      if(search){
        const q=search.toLowerCase();
        const hay=(g.title+" "+g.dataType+" "+g.chartType+" "+g.module+" "+g.desc).toLowerCase();
        if(!hay.includes(q)) return false;
      }
      return true;
    });
    if(sortBy==='ratingDesc') list = [...list].sort((a,b)=> (ratings.get(b.id)||0)-(ratings.get(a.id)||0));
    if(sortBy==='ratingAsc') list = [...list].sort((a,b)=> (ratings.get(a.id)||0)-(ratings.get(b.id)||0));
    return list;
  },[moduleFilter, search, sortBy, ratings]);

  const toggleSelect = (id:string)=> setSelected(prev=>{
    const n=new Set(prev);
    if(n.has(id)) n.delete(id); else n.add(id);
    return n;
  });
  const setRate = (id:string, v:number)=> setRatings(prev=>{ const n=new Map(prev); n.set(id, v); return n; });

  const exportSelected = ()=>{
    const items = [...selected].map(id=>{
      const g=GALLERY.find(x=>x.id===id);
      return g ? {id:g.id, title:g.title, module:g.module, dataType:g.dataType, chartType:g.chartType, file:`src/client-web/src/components/charts/${g.file}`, rating: ratings.get(id)||0} : {id, rating: ratings.get(id)||0};
    });
    // 若未选中，导出当前筛选结果提示
    const payload = {
      exportedAt: new Date().toISOString(),
      totalSelected: items.length,
      note: 'PIM 展览馆选中清单（React 画廊，复用 exhibition_selected）',
      items: items.length? items : filtered.slice(0,3).map(g=>({id:g.id, title:g.title, module:g.module, file:`src/client-web/src/components/charts/${g.file}`, rating: ratings.get(g.id)||0, hint:'未选中，已导出筛选前3作为示例'})),
    };
    const blob=new Blob([JSON.stringify(payload,null,2)],{type:'application/json'});
    const url=URL.createObjectURL(blob);
    const a=document.createElement('a');
    a.href=url;
    a.download=`pim-exhibition-selected-${new Date().toISOString().slice(0,10)}.json`;
    a.id='btnExport';
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(()=> URL.revokeObjectURL(url),800);
  };

  return (
    <div className="mx-auto w-full max-w-[1400px] space-y-4 pb-24 md:pb-4">
      <PageHeader
        title="展览馆 · React 画廊（20+ 落地）"
        subtitle="复用 20+ 生产组件 · 筛选/打分/选中/导出与静态展览馆打通（localStorage 同步）"
        actions={
          <div className="flex flex-wrap gap-2">
            <a href="/dashboard-exhibition/Exhibition.html" target="_blank" rel="noreferrer" className="pim-button-secondary px-3 py-2 text-sm">打开静态展览馆</a>
            <button type="button" id="btnExport" onClick={exportSelected} className="pim-button-primary px-3 py-2 text-sm">导出选中 JSON</button>
          </div>
        }
      />

      <section className="pim-panel p-4">
        <div className="grid grid-cols-1 gap-3 md:grid-cols-4">
          <label className="text-xs">
            <span className="font-semibold text-slate-500">模块</span>
            <select value={moduleFilter} onChange={e=> setModuleFilter(e.target.value)} className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
              <option value="">全部模块</option>
              <option value="手机使用">手机使用</option>
              <option value="位置轨迹">位置轨迹</option>
              <option value="PC活动">PC活动</option>
              <option value="日程习惯">日程习惯</option>
              <option value="设备健康">设备健康</option>
            </select>
          </label>
          <label className="text-xs">
            <span className="font-semibold text-slate-500">排序</span>
            <select value={sortBy} onChange={e=> setSortBy(e.target.value as never)} className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
              <option value="default">默认</option>
              <option value="ratingDesc">评分从高到低</option>
              <option value="ratingAsc">评分从低到高</option>
            </select>
          </label>
          <label className="text-xs md:col-span-2">
            <span className="font-semibold text-slate-500">搜索</span>
            <input value={search} onChange={e=> setSearch(e.target.value)} placeholder="搜 任务/热力/玫瑰/GPS…"
              className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm" />
          </label>
        </div>
        <div className="mt-3 flex flex-wrap gap-2 text-xs">
          <span className="rounded-full bg-blue-50 px-2.5 py-1 font-semibold text-blue-700">{GALLERY.length} 组件已落地</span>
          <span className="rounded-full bg-emerald-50 px-2.5 py-1 font-semibold text-emerald-700">已选 {selected.size}</span>
          <span className="rounded-full bg-amber-50 px-2.5 py-1 font-semibold text-amber-700">筛选 {filtered.length}</span>
          <span className="rounded-full bg-slate-100 px-2.5 py-1 font-semibold text-slate-600">localStorage: exhibition_ratings / exhibition_selected</span>
        </div>
      </section>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
        {filtered.map(item=>{
          const rating = ratings.get(item.id)||0;
          const isSel = selected.has(item.id);
          const Comp = item.Component;
          return (
            <section key={item.id} className={`pim-card flex flex-col overflow-hidden ${isSel?'ring-2 ring-blue-200 border-blue-300':''}`}>
              <div className="flex items-start justify-between gap-2 border-b border-slate-100 px-3 py-2">
                <div className="min-w-0">
                  <h3 className="truncate text-sm font-semibold text-slate-900">{item.title}</h3>
                  <p className="truncate text-xs text-slate-500">{item.dataType} · {item.chartType} · #{item.id}</p>
                  <p className="mt-1 line-clamp-2 text-xs text-slate-500">{item.desc}</p>
                </div>
                <input type="checkbox" checked={isSel} onChange={()=> toggleSelect(item.id)} className="h-4 w-4 accent-blue-600" aria-label="选中" />
              </div>
              <div className="flex flex-wrap gap-1 px-3 py-2">
                <span className="rounded-full bg-blue-50 px-2 py-0.5 text-xs text-blue-700">{item.module}</span>
                <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600">{item.chartType}</span>
                <span className="rounded-full bg-slate-50 px-2 py-0.5 text-xs text-slate-500">{item.file}</span>
              </div>
              <div className="border-y border-slate-100 bg-slate-50/50 p-2">
                <Comp />
              </div>
              <div className="flex items-center justify-between gap-2 px-3 py-2">
                <div className="flex gap-1">
                  {[1,2,3,4,5].map(s=>(
                    <button key={s} onClick={()=> setRate(item.id, s)}
                      className={`h-7 w-7 rounded-md border text-sm ${s<=rating?'bg-amber-50 border-amber-200 text-amber-600':'bg-white border-slate-200 text-slate-400'}`}>
                      {s<=rating?'★':'☆'}
                    </button>
                  ))}
                </div>
                <span className="text-xs text-slate-400">{rating? `${rating}★`:'未评分'}</span>
                <button onClick={()=> toggleSelect(item.id)} className={`rounded-lg px-2.5 py-1 text-xs font-semibold ${isSel?'bg-slate-900 text-white':'bg-white border border-slate-200 text-slate-600'}`}>
                  {isSel?'已选中':'选中'}
                </button>
              </div>
            </section>
          );
        })}
      </div>

      {filtered.length===0 && (
        <div className="rounded-lg border border-dashed border-slate-200 bg-white p-10 text-center text-sm text-slate-500">无匹配组件，试试清空筛选</div>
      )}

      <section className="pim-panel p-4">
        <h3 className="text-sm font-semibold text-slate-900">如何体验</h3>
        <ol className="mt-2 list-decimal space-y-1 pl-5 text-sm text-slate-600">
          <li>静态展览馆（挑）：访问 <code>/dashboard-exhibition/Exhibition.html</code> 或本地打开 <code>src/client-web/src/components/dashboard-exhibition/Exhibition.html</code>，480 卡片可筛选/搜索/排序/打分/选中/对比/导出。</li>
          <li>React 画廊（落）：本页 <code>/exhibition</code> 即 20+ 生产组件的 React 网格，筛选/打分/选中/导出与静态页共享 <code>exhibition_ratings</code> / <code>exhibition_selected</code>，刷新互通。</li>
          <li>导出：点击“导出选中 JSON”下载 <code>pim-exhibition-selected-*.json</code>，未选中时自动导出当前筛选前 3 作为示例。</li>
        </ol>
      </section>
    </div>
  );
}
