import { useMemo } from 'react';
import EChartBox from './EChartBox';
import { chartColors } from './chartColors';
import type { EChartsOption } from '../../lib/echarts';

/**
 * CategoryStackedBar — App分类占比 × 堆叠柱状图
 * 假数据: {category:string, percentage:number}[] 8类和为100
 */
export interface CategoryStackedBarProps { data?: {category:string, percentage:number}[]; loading?:boolean; height?:number; }
const FALLBACK = [
  {category:"聊天", percentage:27.5},
  {category:"视频", percentage:21.3},
  {category:"社交", percentage:14.2},
  {category:"工具", percentage:12.8},
  {category:"游戏", percentage:8.5},
  {category:"学习", percentage:6.1},
  {category:"购物", percentage:5.4},
  {category:"其他", percentage:4.2},
];
export default function CategoryStackedBar({ data, loading, height=200 }: CategoryStackedBarProps){
  const items = data && data.length ? data : FALLBACK;
  // 堆叠示例：按 3 段拆分每类（示例语义：前台/后台/系统）
  const s1 = items.map(d=> Math.round(d.percentage*0.52));
  const s2 = items.map(d=> Math.round(d.percentage*0.31));
  const s3 = items.map(d=> Math.max(1, Math.round(d.percentage - s1[items.indexOf(d)] - s2[items.indexOf(d)])));
  const option = useMemo<EChartsOption>(()=>({
    tooltip:{trigger:'axis'},
    legend:{bottom:0, textStyle:{fontSize:9,color:chartColors.textMuted}},
    grid:{left:32,right:8,top:10,bottom:26},
    xAxis:{type:'category', data: items.map(d=>d.category), axisLabel:{fontSize:9,color:chartColors.textMuted, interval:0, rotate:14}, axisTick:{show:false}},
    yAxis:{type:'value', max:30, splitLine:{lineStyle:{color:'#f1f5f9'}}, axisLabel:{fontSize:9,color:'#94a3b8'}},
    series:[
      {name:'前台', type:'bar', stack:'st', data:s1, itemStyle:{color:'#2563eb'}},
      {name:'后台', type:'bar', stack:'st', data:s2, itemStyle:{color:'#14b8a6'}},
      {name:'系统', type:'bar', stack:'st', data:s3, itemStyle:{color:'#f59e0b'}},
    ]
  } as EChartsOption),[items]);
  if(loading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载中…</div>;
  return <EChartBox option={option} height={height} ariaLabel="分类堆叠柱状" />;
}
