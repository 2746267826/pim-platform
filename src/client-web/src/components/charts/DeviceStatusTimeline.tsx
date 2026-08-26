import { useMemo } from 'react';
import EChartBox from './EChartBox';
import { chartColors } from './chartColors';
import type { EChartsOption } from '../../lib/echarts';

/**
 * DeviceStatusTimeline — 设备健康状态 × 时间线/甘特
 * 假数据: {device, status, lastSync} 4设备 在线/离线/告警
 */
export interface DeviceStatusTimelineProps { data?: {device:string, status:string, lastSync:string, health:number}[]; loading?:boolean; height?:number; }
const FALLBACK = [
  {device:"小米 13", status:"在线", lastSync:"2分钟前", health:92},
  {device:"ThinkPad X1", status:"在线", lastSync:"5分钟前", health:88},
  {device:"iPad Pro", status:"离线", lastSync:"3小时前", health:43},
  {device:"Watch S8", status:"告警", lastSync:"18分钟前", health:58},
];
export default function DeviceStatusTimeline({ data, loading, height=180 }: DeviceStatusTimelineProps){
  const src = data ?? FALLBACK;
  const option = useMemo<EChartsOption>(()=>{
    const cats = src.map(d=>d.device);
    const health = src.map(d=>d.health);
    return {
      tooltip:{trigger:'axis'},
      grid:{left:88,right:16,top:8,bottom:8},
      xAxis:{type:'value', max:100, splitLine:{lineStyle:{color:'#f1f5f9'}}, axisLabel:{fontSize:9,color:'#94a3b8'}},
      yAxis:{type:'category', data:cats, inverse:true, axisLabel:{fontSize:10,color:'#334155'}, axisTick:{show:false}},
      series:[
        {type:'bar', data: health, barWidth:12, itemStyle:{borderRadius:[0,8,8,0], color:{type:'linear',x:0,y:0,x2:1,y2:0,colorStops:[{offset:0,color:chartColors.primary},{offset:1,color:'#22d3ee'}]} as unknown as string}, label:{show:true, position:'right', fontSize:9, color:chartColors.textMuted, formatter:(p:unknown)=>{ const idx=(p as {dataIndex:number}).dataIndex; return src[idx].status; }}},
      ]
    } as EChartsOption;
  },[src]);
  if(loading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载中…</div>;
  return <EChartBox option={option} height={height} ariaLabel="设备状态时间线" />;
}
