import { useMemo } from 'react';
import EChartBox from './EChartBox';
import type { EChartsOption } from '../../lib/echarts';

/**
 * HabitStreakRing — 习惯打卡 × 进度环
 * 假数据: {habit, streak, rate} 连续天数与打卡率
 */
export interface HabitStreakRingProps { data?: {habit:string, streak:number, rate:number}[]; loading?:boolean; height?:number; }
const FALLBACK = [
  {habit:"早起", streak:12, rate:72},
  {habit:"阅读", streak:8, rate:65},
  {habit:"运动", streak:3, rate:45},
  {habit:"冥想", streak:6, rate:58},
  {habit:"写作", streak:2, rate:40},
];
export default function HabitStreakRing({ data, loading, height=180 }: HabitStreakRingProps){
  const src = data ?? FALLBACK;
  const option = useMemo<EChartsOption>(()=>{
    return {
      tooltip:{trigger:'item'},
      series: src.map((d, idx)=>({
        type:'gauge',
        center:[`${(idx*20+10)}%`,'55%'],
        radius:'38%',
        min:0, max:100,
        progress:{show:true, width:8},
        axisLine:{lineStyle:{width:8, color:[[1,'#f1f5f9']]}},
        pointer:{show:false}, axisTick:{show:false}, splitLine:{show:false}, axisLabel:{show:false},
        detail:{valueAnimation:true, fontSize:11, fontWeight:'bold', color:'#0f172a', offsetCenter:[0,0], formatter: d.rate+'%'},
        title:{show:true, offsetCenter:[0,'72%'], fontSize:9, color:'#64748b'},
        data:[{value:d.rate, name:d.habit}]
      }))
    } as EChartsOption;
  },[src]);
  if(loading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载中…</div>;
  return <EChartBox option={option} height={height} ariaLabel="习惯进度环" />;
}
