import { useMemo } from 'react';
import EChartBox from './EChartBox';
import type { EChartsOption } from '../../lib/echarts';

/**
 * TaskFunnel — 任务完成率 × 漏斗图
 * 假数据: {stage, count} 创建->进行->完成 递减
 */
export interface TaskFunnelProps { data?: {stage:string, count:number}[]; loading?:boolean; height?:number; }
const FALLBACK = [
  {stage:"创建", count:42},
  {stage:"进行", count:33},
  {stage:"完成", count:28},
];
export default function TaskFunnel({ data, loading, height=180 }: TaskFunnelProps){
  const src = data ?? FALLBACK;
  const option = useMemo<EChartsOption>(()=>({
    tooltip:{trigger:'item'},
    series:[{type:'funnel', left:'10%', top:10, bottom:28, width:'80%', sort:'descending', gap:2, label:{fontSize:10, color:'#334155', position:'inside', formatter:'{b}\n{c}'}, itemStyle:{borderColor:'#fff', borderWidth:1}, data: src.map(d=>({name:d.stage, value:d.count}))}]
  } as EChartsOption),[src]);
  if(loading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载中…</div>;
  return <EChartBox option={option} height={height} ariaLabel="任务漏斗" />;
}
