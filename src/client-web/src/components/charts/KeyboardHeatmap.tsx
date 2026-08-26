import { useMemo } from 'react';
import EChartBox from './EChartBox';
import { chartColors } from './chartColors';
import type { EChartsOption } from '../../lib/echarts';

/**
 * KeyboardHeatmap — 键盘热力图 × 热力图
 * 假数据: {key, pressCount}[] QWERTY 真实频率，Space 3420 最高，Q/Z 最低
 */
export interface KeyboardHeatmapProps { data?: {key:string, pressCount:number}[]; loading?:boolean; height?:number; }
const FALLBACK: {key:string, pressCount:number}[] = [
  {key:"Q", pressCount:85},{key:"W", pressCount:320},{key:"E", pressCount:1820},{key:"R", pressCount:750},{key:"T", pressCount:1410},{key:"Y", pressCount:420},{key:"U", pressCount:620},{key:"I", pressCount:1100},{key:"O", pressCount:1210},{key:"P", pressCount:480},
  {key:"A", pressCount:1320},{key:"S", pressCount:960},{key:"D", pressCount:680},{key:"F", pressCount:520},{key:"G", pressCount:410},{key:"H", pressCount:820},{key:"J", pressCount:85},{key:"K", pressCount:540},{key:"L", pressCount:680},
  {key:"Z", pressCount:45},{key:"X", pressCount:120},{key:"C", pressCount:340},{key:"V", pressCount:180},{key:"B", pressCount:260},{key:"N", pressCount:980},{key:"M", pressCount:540},
  {key:"Space", pressCount:3420},{key:"Enter", pressCount:920},
];
export default function KeyboardHeatmap({ data, loading, height=180 }: KeyboardHeatmapProps){
  const src = data ?? FALLBACK;
  const option = useMemo<EChartsOption>(()=>{
    const rows = ["QWERTYUIOP","ASDFGHJKL","ZXCVBNM"];
    const yCats = rows;
    const xCats = Array.from({length:10},(_,i)=> String(i+1));
    const map = new Map(src.map(d=>[d.key, d.pressCount]));
    const hm: [number,number,number][] = [];
    rows.forEach((row,ri)=>{
      for(let ci=0; ci<10; ci++){
        const k=row[ci];
        if(!k) continue;
        const v = map.get(k) ?? 120;
        hm.push([ci, ri, v]);
      }
    });
    // Space/Enter 单独不进矩阵，仅提示
    return {
      tooltip:{formatter:(p:unknown)=>{ const d=p as {value?:number[]}; return `键 ${d.value?.[0]},${d.value?.[1]} : ${d.value?.[2]}次`; }},
      grid:{left:8,right:8,top:8,bottom:8},
      xAxis:{type:'category', data:xCats, show:false},
      yAxis:{type:'category', data:yCats, show:false},
      visualMap:{show:false, min:0, max:3500, inRange:{color:chartColors.heatmapTeal}},
      series:[{type:'heatmap', data:hm, label:{show:false}, itemStyle:{borderWidth:1, borderColor:'#fff', borderRadius:3}}]
    } as EChartsOption;
  },[src]);
  if(loading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载中…</div>;
  return <div>
    <EChartBox option={option} height={height} ariaLabel="键盘热力" />
    <p className="mt-1 text-center text-[10px] text-slate-400">Space {src.find(d=>d.key==="Space")?.pressCount} · Enter {src.find(d=>d.key==="Enter")?.pressCount}</p>
  </div>;
}
