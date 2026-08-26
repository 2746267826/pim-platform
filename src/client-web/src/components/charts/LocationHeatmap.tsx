import { useMemo } from 'react';
import EChartBox from './EChartBox';
import { chartColors } from './chartColors';
import type { EChartsOption } from '../../lib/echarts';

/**
 * LocationHeatmap — 常去地点/轨迹热力 × 六边形分箱/热力
 * 假数据: {lat,lng, count} 或 frequent places
 */
export interface LocationHeatmapProps { data?: {lat:number,lng:number, count:number}[]; loading?:boolean; height?:number; }
const FALLBACK = [
  {lat:39.9042,lng:116.4074,count:18},{lat:39.905,lng:116.41,count:22},{lat:39.907,lng:116.415,count:14},{lat:39.918,lng:116.44,count:26},{lat:39.92,lng:116.445,count:19},{lat:39.914,lng:116.442,count:12},
];
export default function LocationHeatmap({ data, loading, height=200 }: LocationHeatmapProps){
  const src = data ?? FALLBACK;
  const option = useMemo<EChartsOption>(()=>{
    const xCats = Array.from({length:12},(_,i)=> (116.20+i*0.033).toFixed(2));
    const yCats = Array.from({length:8},(_,i)=> (39.80+i*0.037).toFixed(2));
    const hm: [number,number,number][] = src.map(p=>{
      const xi=Math.floor((p.lng-116.20)/0.033), yi=Math.floor((p.lat-39.80)/0.037);
      return [Math.max(0,Math.min(11,xi)), Math.max(0,Math.min(7,yi)), p.count];
    });
    return {
      grid:{left:38,right:8,top:8,bottom:22},
      xAxis:{type:'category', data:xCats, axisLabel:{fontSize:7,color:chartColors.textMuted, interval:2}, axisTick:{show:false}},
      yAxis:{type:'category', data:yCats, axisLabel:{fontSize:7,color:chartColors.textMuted}, axisTick:{show:false}},
      visualMap:{show:false, min:0, max:30, inRange:{color:chartColors.heatmapTeal}},
      series:[{type:'heatmap', data:hm, itemStyle:{borderWidth:.5, borderColor:'#fff', borderRadius:2}}]
    } as EChartsOption;
  },[src]);
  if(loading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载中…</div>;
  return <EChartBox option={option} height={height} ariaLabel="位置热力" />;
}
