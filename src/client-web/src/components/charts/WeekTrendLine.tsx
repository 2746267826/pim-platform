import { useMemo } from 'react';
import EChartBox from './EChartBox';
import { chartColors } from './chartColors';
import type { EChartsOption } from '../../lib/echarts';

/**
 * WeekTrendLine — 周使用趋势 × 折线图
 * 假数据形状: {date: string, total: number, byCategory: Record<string,number>}[] 4周
 * 特点: 周一/周五高峰、周末低谷
 * @example fake: [{date:"W1", total:1380}, {date:"W2", total:1520}, ...]
 */
export interface WeekTrendPoint { date: string; total: number; }
export interface WeekTrendLineProps { data?: WeekTrendPoint[]; loading?: boolean; height?: number; }

const FALLBACK: WeekTrendPoint[] = [
  {date:"W1", total:1380},
  {date:"W2", total:1520},
  {date:"W3", total:1410},
  {date:"W4", total:1680},
];

export default function WeekTrendLine({ data, loading, height=180 }: WeekTrendLineProps){
  const pts = data && data.length ? data : FALLBACK;
  const option = useMemo<EChartsOption>(()=>({
    tooltip:{trigger:'axis'},
    grid:{left:32,right:10,top:10,bottom:22},
    xAxis:{type:'category', data: pts.map(p=>p.date), boundaryGap:false, axisLabel:{fontSize:9,color:chartColors.textMuted}, axisTick:{show:false}, axisLine:{lineStyle:{color:chartColors.borderSoft}}},
    yAxis:{type:'value', splitLine:{lineStyle:{color:'#f1f5f9'}}, axisLabel:{fontSize:9,color:'#94a3b8'}},
    series:[{type:'line', data: pts.map(p=>p.total), smooth:true, symbol:'circle', symbolSize:5, lineStyle:{width:2,color:chartColors.primary}, itemStyle:{color:chartColors.primary}, areaStyle:undefined}],
  } as EChartsOption),[pts]);
  if(loading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载中…</div>;
  return <EChartBox option={option} height={height} ariaLabel="周趋势折线" />;
}
