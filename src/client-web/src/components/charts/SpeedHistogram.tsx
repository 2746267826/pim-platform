import { useMemo } from 'react';
import EChartBox from './EChartBox';
import { chartColors } from './chartColors';
import type { EChartsOption } from '../../lib/echarts';

/**
 * SpeedHistogram — 速度分布 × 直方图/柱状
 * 假数据: {speed, count}[] 步行<5、骑行15-25、高铁250-350 分桶
 */
export interface SpeedHistogramProps { data?: {speed:number, count:number}[]; loading?:boolean; height?:number; }
const FALLBACK = [
  {speed:2.8, count:86},{speed:19, count:42},{speed:48, count:68},{speed:310, count:11},
];
export default function SpeedHistogram({ data, loading, height=180 }: SpeedHistogramProps){
  const src = data ?? FALLBACK;
  // 展开为直方图：按 0-360 分箱
  const option = useMemo<EChartsOption>(()=>{
    const isBinned = src.length<=4;
    if(isBinned){
      return {
        tooltip:{trigger:'axis'},
        grid:{left:36,right:10,top:10,bottom:22},
        xAxis:{type:'category', data: src.map(d=> d.speed<10? d.speed+" km/h" : d.speed+" km/h"), axisLabel:{fontSize:9,color:chartColors.textMuted}, axisTick:{show:false}, axisLine:{lineStyle:{color:chartColors.borderSoft}}},
        yAxis:{type:'value', splitLine:{lineStyle:{color:'#f1f5f9'}}, axisLabel:{fontSize:9,color:'#94a3b8'}},
        series:[{type:'bar', data: src.map(d=>d.count), barMaxWidth:32, itemStyle:{color:chartColors.primary, borderRadius:[4,4,0,0]}}]
      } as EChartsOption;
    }
    return {
      tooltip:{trigger:'axis'},
      grid:{left:36,right:10,top:10,bottom:22},
      xAxis:{type:'value', name:'km/h', splitLine:{lineStyle:{color:'#f1f5f9'}}, axisLabel:{fontSize:9,color:chartColors.textMuted}},
      yAxis:{type:'value', splitLine:{lineStyle:{color:'#f1f5f9'}}, axisLabel:{fontSize:9,color:'#94a3b8'}},
      series:[{type:'bar', data: src.map(d=>[d.speed, d.count]), barWidth:10, itemStyle:{color:chartColors.primary, borderRadius:[4,4,0,0]}}]
    } as EChartsOption;
  },[src]);
  if(loading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载中…</div>;
  return <EChartBox option={option} height={height} ariaLabel="速度直方图" />;
}
