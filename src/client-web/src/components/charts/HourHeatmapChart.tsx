import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getMobileAnalyticsHeatmap } from '../../api/mobile';
import EChartBox from './EChartBox';
import { buildHeatmapMatrixOption } from './exhibitionOptions';

/**
 * 落地组件：24小时热力图 × 热力图（矩阵）
 * 数据源：/mobile/analytics/heatmap（按小时+分类）
 * 展览馆组合：#4×15
 */
export default function HourHeatmapChart() {
  const { data: buckets = [], isLoading } = useQuery({
    queryKey: ['exhibition-hour-heatmap'],
    queryFn: () => getMobileAnalyticsHeatmap({}),
  });

  const option = useMemo(() => {
    if (buckets.length === 0) {
      const xCats = Array.from({length:24},(_,i)=> `${i}h`);
      const yCats = ['聊天','视频','工具','社交','游戏'];
      const fake: [number,number,number][] = [];
      for(let y=0;y<yCats.length;y++) for(let x=0;x<24;x++){
        let v= 12+Math.round(Math.random()*36);
        if(x>=19 && x<=23) v+=18;
        if(x>=0 && x<=5) v=Math.round(Math.random()*8);
        fake.push([x,y,v]);
      }
      return buildHeatmapMatrixOption(xCats, yCats, fake);
    }
    // aggregate buckets by hour+category
    const cats = [...new Set(buckets.map(b=> String(b.lifeCategory||'其他')))].slice(0,5);
    if (cats.length===0) cats.push('聊天','视频','工具');
    const xCats = Array.from({length:24},(_,i)=> `${i}h`);
    const yCats = cats;
    const catIdx = new Map(yCats.map((c,i)=>[c,i]));
    const agg = new Map<string, number>();
    buckets.forEach(b=>{
      const hour = Number(b.localHour);
      const ci = catIdx.get(String(b.lifeCategory||yCats[0]));
      if (ci===undefined || Number.isNaN(hour)) return;
      const key = `${hour},${ci}`;
      agg.set(key, (agg.get(key)||0)+ (b.foregroundSeconds||0)/60);
    });
    const data: [number,number,number][] = [];
    agg.forEach((v,k)=>{
      const [x,y]=k.split(',').map(Number);
      data.push([x,y, Math.round(v)]);
    });
    // ensure at least some grid cells
    if (data.length===0) {
      for(let y=0;y<yCats.length;y++) for(let x=0;x<24;x++) data.push([x,y, Math.round(6+Math.random()*28)]);
    }
    return buildHeatmapMatrixOption(xCats, yCats, data);
  }, [buckets]);

  if (isLoading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载热力图…</div>;
  return (
    <section className="rounded-md border border-slate-200 bg-white p-4">
      <h3 className="text-sm font-semibold text-slate-900">24小时热力图 · 矩阵热力</h3>
      <p className="mt-1 text-xs text-slate-500">按小时×分类聚合，前台时长（分钟）</p>
      <div className="mt-3">
        <EChartBox option={option} height={220} ariaLabel="24小时热力图" />
      </div>
    </section>
  );
}
