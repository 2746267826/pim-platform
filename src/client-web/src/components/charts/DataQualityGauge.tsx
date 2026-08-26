import { useMemo } from 'react';
import EChartBox from './EChartBox';
import { chartColors } from './chartColors';
import type { EChartsOption } from '../../lib/echarts';

/**
 * DataQualityGauge — 设备健康/数据质量 × 仪表盘
 * 假数据: {score} 0-100，综合同步状态与健康
 */
export interface DataQualityGaugeProps { data?: {score:number, label:string}; loading?:boolean; height?:number; }
const FALLBACK = {score:76, label:"数据质量"};
export default function DataQualityGauge({ data, loading, height=180 }: DataQualityGaugeProps){
  const score = data?.score ?? FALLBACK.score;
  const label = data?.label ?? FALLBACK.label;
  const pct = Math.max(0, Math.min(100, Math.round(score)));
  const option = useMemo<EChartsOption>(()=>({
    series:[{type:'gauge', min:0, max:100, splitNumber:5, axisLine:{lineStyle:{width:12, color:[[pct/100, chartColors.primary],[1,'#e2e8f0']]}},
      pointer:{itemStyle:{color:chartColors.primary}, length:'62%', width:4},
      axisTick:{distance:-12, length:4, lineStyle:{color:'#94a3b8', width:1}},
      splitLine:{distance:-12, length:10, lineStyle:{color:chartColors.textMuted, width:1}},
      axisLabel:{fontSize:8,color:chartColors.textMuted, distance:12},
      detail:{valueAnimation:true, fontSize:18, fontWeight:'bold', color:'#0f172a', offsetCenter:[0,'68%'], formatter:'{value}%'},
      title:{show:true, offsetCenter:[0,'92%'], fontSize:10, color:chartColors.textMuted},
      data:[{value:pct, name:label}]}]
  } as EChartsOption),[pct,label]);
  if(loading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载中…</div>;
  return <EChartBox option={option} height={height} ariaLabel="数据质量仪表" />;
}
