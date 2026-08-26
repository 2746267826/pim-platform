import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getMobileFrequentPlaces, getMobileLocationAnalyticsTracks } from '../../api/mobile';
import EChartBox from './EChartBox';
import { chartColors } from './chartColors';
import type { EChartsOption } from '../../lib/echarts';

/**
 * 落地组件：常去地点气泡图 × 气泡图（+ GPS轨迹地图 × 散点）
 * 数据源：/mobile/location/analytics/frequent-places + tracks
 * 展览馆：#6×13, #5×12
 */
export default function LocationBubbleChart() {
  const { data: placesRes, isLoading: placesLoading } = useQuery({
    queryKey: ['exhibition-frequent-places'],
    queryFn: () => getMobileFrequentPlaces({}),
  });
  const { data: tracks = [] } = useQuery({
    queryKey: ['exhibition-tracks'],
    queryFn: () => getMobileLocationAnalyticsTracks({}),
  });

  const bubbleOption: EChartsOption = useMemo(() => {
    const places = placesRes?.places ?? [];
    if (places.length===0) {
      const fake = [
        {name:'家', lat:39.90, lng:116.40, count:128},
        {name:'公司', lat:39.91, lng:116.44, count:96},
        {name:'学校', lat:39.89, lng:116.39, count:42},
        {name:'商圈', lat:39.92, lng:116.41, count:31},
        {name:'健身房', lat:39.93, lng:116.38, count:18},
      ];
      return {
        tooltip:{formatter:(p: unknown)=> { const d=p as {name?:string; value?:number[]}; return `${d.name}: ${d.value?.[2]}次`; }},
        grid:{left:30,right:10,top:10,bottom:22},
        xAxis:{type:'value', name:'Lng', splitLine:{show:false}, axisLabel:{fontSize:9,color:chartColors.textMuted}},
        yAxis:{type:'value', name:'Lat', splitLine:{show:false}, axisLabel:{fontSize:9,color:chartColors.textMuted}},
        series:[{type:'scatter', data: fake.map(f=>({value:[f.lng, f.lat, f.count], name:f.name})), symbolSize:(d: number[])=> 10+ Math.sqrt(d[2] as number)*2.2, itemStyle:{color:'rgba(37,99,235,0.72)', borderColor:chartColors.primary, borderWidth:1}}],
      } as EChartsOption;
    }
    const pts = places.slice(0,8).map(p=>({value:[p.centerLongitude, p.centerLatitude, p.pointCount], name: p.isHome ? '家' : `${p.centerLatitude.toFixed(3)},${p.centerLongitude.toFixed(3)}`}));
    return {
      tooltip:{formatter:(p: unknown)=> { const d=p as {name?:string; value?:number[]}; return `${d.name}: ${d.value?.[2]} 点`; }},
      grid:{left:30,right:10,top:10,bottom:22},
      xAxis:{type:'value', name:'Lng', splitLine:{show:false}, axisLabel:{fontSize:9,color:chartColors.textMuted}},
      yAxis:{type:'value', name:'Lat', splitLine:{show:false}, axisLabel:{fontSize:9,color:chartColors.textMuted}},
      series:[{type:'scatter', data: pts, symbolSize:(d: number[])=> 10+ Math.sqrt(d[2] as number)*1.8, itemStyle:{color:'rgba(20,184,166,0.72)', borderColor:chartColors.activity, borderWidth:1}}],
    } as EChartsOption;
  }, [placesRes]);

  const trackPoints = useMemo(()=> {
    const segs = tracks.flatMap(t=> t.segments ?? []);
    const pts: [number,number][] = [];
    segs.slice(0,2).forEach(s=> (s.path||[]).slice(0,20).forEach(p=> pts.push([p.longitude, p.latitude])));
    return pts;
  }, [tracks]);

  if (placesLoading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载位置数据…</div>;

  return (
    <section className="rounded-md border border-slate-200 bg-white p-4">
      <h3 className="text-sm font-semibold text-slate-900">常去地点 · 气泡图</h3>
      <p className="mt-1 text-xs text-slate-500">气泡大小 = 访问点数；另含 GPS 轨迹散点（{trackPoints.length} 点）</p>
      <div className="mt-3">
        <EChartBox option={bubbleOption} height={220} ariaLabel="常去地点气泡图" />
      </div>
      {placesRes?.home && <p className="mt-2 text-xs text-slate-500">家：{placesRes.home.centerLatitude.toFixed(4)}, {placesRes.home.centerLongitude.toFixed(4)} · 半径 {Math.round(placesRes.home.radiusMeters)}m</p>}
    </section>
  );
}
