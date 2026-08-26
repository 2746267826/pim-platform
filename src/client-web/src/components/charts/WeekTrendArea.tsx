import { useMemo } from 'react';
import EChartBox from './EChartBox';
import { chartColors } from './chartColors';
import type { EChartsOption } from '../../lib/echarts';

/**
 * WeekTrendArea — 周使用趋势 × 堆叠面积图
 * 假数据: 4周按分类拆分，展示周周期
 */
export interface WeekTrendAreaProps { data?: {date:string, total:number, byCategory:Record<string,number>}[]; loading?:boolean; height?:number; }
const FALLBACK = [
  {date:"W1", byCategory:{聊天:442, 视频:359, 工具:304, 社交:275}},
  {date:"W2", byCategory:{聊天:486, 视频:395, 工具:334, 社交:305}},
  {date:"W3", byCategory:{聊天:451, 视频:367, 工具:310, 社交:282}},
  {date:"W4", byCategory:{聊天:538, 视频:437, 工具:370, 社交:335}},
];
export default function WeekTrendArea({ data, loading, height=180 }: WeekTrendAreaProps){
  const src = data && data.length ? data : FALLBACK;
  const cats = ["聊天","视频","工具","社交"];
  const option = useMemo<EChartsOption>(()=>({
    tooltip:{trigger:'axis'},
    legend:{bottom:0, textStyle:{fontSize:9,color:chartColors.textMuted}, data:cats},
    grid:{left:30,right:10,top:8,bottom:26},
    xAxis:{type:'category', data: src.map(d=>d.date), boundaryGap:false, axisLabel:{fontSize:9,color:chartColors.textMuted}, axisTick:{show:false}},
    yAxis:{type:'value', splitLine:{lineStyle:{color:'#f1f5f9'}}, axisLabel:{fontSize:9,color:'#94a3b8'}},
    series: cats.map((c,i)=>({
      name:c, type:'line', stack:'st', data: src.map(d=> (d.byCategory as Record<string,number>)[c] ?? 0),
      smooth:true, symbol:'none', lineStyle:{width:1.5}, itemStyle:{color: ['#2563eb','#f59e0b','#14b8a6','#8b5cf6'][i]}, areaStyle:{color: ['rgba(37,99,235,0.18)','rgba(245,158,11,0.18)','rgba(20,184,166,0.18)','rgba(139,92,246,0.18)'][i]}
    }))
  } as EChartsOption),[src]);
  if(loading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载中…</div>;
  return <EChartBox option={option} height={height} ariaLabel="周趋势面积" />;
}
