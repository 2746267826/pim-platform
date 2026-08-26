import { useMemo } from 'react';
import EChartBox from './EChartBox';
import { chartColors } from './chartColors';
import type { EChartsOption } from '../../lib/echarts';

/**
 * GpsTrackMap — GPS轨迹地图 × 散点/线图（echarts 版，无需 leaflet 也可展示）
 * 假数据: {lat, lng, timestamp}[] 50点 北京 39.80-40.10/116.20-116.60 连续轨迹
 */
export interface GpsPoint { lat:number; lng:number; timestamp?: string; speed?: number; }
export interface GpsTrackMapProps { data?: GpsPoint[]; loading?:boolean; height?:number; }
const FALLBACK: GpsPoint[] = [
  {lat:39.9042,lng:116.4074,speed:12},{lat:39.907,lng:116.412,speed:18},{lat:39.91,lng:116.418,speed:22},{lat:39.913,lng:116.425,speed:19},{lat:39.916,lng:116.433,speed:16},{lat:39.919,lng:116.44,speed:14},{lat:39.921,lng:116.447,speed:9},{lat:39.92,lng:116.452,speed:5},{lat:39.918,lng:116.455,speed:4},{lat:39.914,lng:116.452,speed:8},
];
export default function GpsTrackMap({ data, loading, height=220 }: GpsTrackMapProps){
  const pts = data && data.length ? data : FALLBACK;
  const option = useMemo<EChartsOption>(()=>({
    tooltip:{trigger:'item', formatter:(p:unknown)=>{ const d=p as {value?:number[]}; return `Lng:${d.value?.[0]?.toFixed(3)} Lat:${d.value?.[1]?.toFixed(3)}`; }},
    grid:{left:30,right:10,top:10,bottom:22},
    xAxis:{type:'value', min:116.20, max:116.60, name:'Lng', splitLine:{lineStyle:{color:'#f1f5f9'}}, axisLabel:{fontSize:9,color:chartColors.textMuted}},
    yAxis:{type:'value', min:39.80, max:40.10, name:'Lat', splitLine:{lineStyle:{color:'#f1f5f9'}}, axisLabel:{fontSize:9,color:chartColors.textMuted}},
    series:[
      {type:'line', data: pts.map(p=>[p.lng, p.lat]), smooth:true, symbol:'none', lineStyle:{width:2,color:chartColors.primary}, itemStyle:{color:chartColors.primary}},
      {type:'scatter', data: pts.map(p=>[p.lng, p.lat]), symbolSize:6, itemStyle:{color:chartColors.warning, opacity:0.9}},
    ]
  } as EChartsOption),[pts]);
  if(loading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载中…</div>;
  return <EChartBox option={option} height={height} ariaLabel="GPS轨迹" />;
}
