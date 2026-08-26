import { useMemo } from 'react';
import EChartBox from './EChartBox';
import { chartColors } from './chartColors';
import type { EChartsOption } from '../../lib/echarts';

/**
 * PcAppDonut — PC应用使用时长 × 环形图
 * 假数据: {app, seconds}[] 7天×6app，VS Code/Chrome 占大头，AFK 单独
 */
export interface PcAppDonutProps { data?: {app:string, seconds:number}[]; loading?:boolean; height?:number; }
const FALLBACK = [
  {app:"VS Code", seconds:19200},
  {app:"Chrome", seconds:16800},
  {app:"Word", seconds:5700},
  {app:"微信", seconds:3900},
  {app:"B站", seconds:2700},
  {app:"AFK", seconds:1800},
];
export default function PcAppDonut({ data, loading, height=200 }: PcAppDonutProps){
  const src = data ?? FALLBACK;
  const option = useMemo<EChartsOption>(()=>{
    const pie = src.map(d=>({name:d.app, value: Math.round(d.seconds/60)}));
    const total = pie.reduce((s,d)=>s+d.value,0);
    return {
      tooltip:{trigger:'item'},
      legend:{bottom:0, textStyle:{fontSize:9,color:chartColors.textMuted}, type:'scroll'},
      series:[{type:'pie', radius:['46%','68%'], center:['50%','44%'], data:pie, label:{show:false}, itemStyle:{borderRadius:4, borderColor:'#fff', borderWidth:1}}],
      graphic:[{type:'text', left:'center', top:'38%', style:{text: String(total), fontSize:14, fontWeight:800, fill:'#0f172a', textAlign:'center'}}]
    } as unknown as EChartsOption;
  },[src]);
  if(loading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载中…</div>;
  return <EChartBox option={option} height={height} ariaLabel="PC应用环形" />;
}
