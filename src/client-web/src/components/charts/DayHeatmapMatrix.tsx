import { useMemo } from 'react';
import EChartBox from './EChartBox';
import { chartColors } from './chartColors';
import type { EChartsOption } from '../../lib/echarts';

/**
 * DayHeatmapMatrix — 24小时热力 × 热力图矩阵
 * 假数据: {hour:0-23, category:string, value:number}[] 双峰 8-12/19-23
 */
export interface DayHeatmapMatrixProps { data?: {hour:number, category:string, value:number}[]; categories?: string[]; loading?:boolean; height?:number; }
const CATS = ["聊天","视频","工具","社交","游戏"];
const FALLBACK = (()=>{ const a: {hour:number, category:string, value:number}[]=[]; for(let h=0;h<24;h++){ let base=h>=8&&h<=12?68 : h>=19&&h<=23?76 : h>=0&&h<=5?7 : 32; for(const c of CATS){ let v=base; if(c==="视频"&&h>=19) v*=1.28; if(c==="工具"&&h>=9&&h<=18) v*=1.32; a.push({hour:h, category:c, value: Math.round(v)});}} return a;})();
export default function DayHeatmapMatrix({ data, categories, loading, height=220 }: DayHeatmapMatrixProps){
  const cats = categories ?? CATS;
  const src = data ?? FALLBACK;
  const option = useMemo<EChartsOption>(()=>{
    const xCats = Array.from({length:24},(_,i)=> i+":00");
    const yCats = cats;
    const hm = src.map(d=> [d.hour, yCats.indexOf(d.category), d.value] as [number,number,number]);
    return {
      tooltip:{position:'top'},
      grid:{left:46,right:8,top:8,bottom:28},
      xAxis:{type:'category', data:xCats, axisLabel:{fontSize:7,color:chartColors.textMuted, interval:1}, axisTick:{show:false}, axisLine:{show:false}},
      yAxis:{type:'category', data:yCats, axisLabel:{fontSize:9,color:chartColors.textMuted}, axisTick:{show:false}, axisLine:{show:false}},
      visualMap:{show:false, min:0, max:90, inRange:{color:chartColors.heatmapTeal}},
      series:[{type:'heatmap', data:hm, label:{show:false}, itemStyle:{borderWidth:.5, borderColor:'#fff'}}]
    } as EChartsOption;
  },[src,cats]);
  if(loading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载中…</div>;
  return <EChartBox option={option} height={height} ariaLabel="日热力矩阵" />;
}
