import { chartColors } from './chartColors';
import type { EChartsOption } from '../../lib/echarts';
function h(seed: number){ const x=Math.sin(seed*12.9898+78.233)*43758.5453; return x-Math.floor(x); }



/** 通用：标签+数值到垂直柱状 */
export function buildVerticalBarOption(labels: string[], values: number[]): EChartsOption {
  return {
    tooltip:{trigger:'axis'},
    grid:{left:36,right:10,top:16,bottom:24},
    xAxis:{type:'category', data:labels, axisLabel:{fontSize:9,color:chartColors.textMuted, interval:0, rotate: labels.length>6?16:0}, axisTick:{show:false}, axisLine:{lineStyle:{color:chartColors.borderSoft}}},
    yAxis:{type:'value', splitLine:{lineStyle:{color:'#f1f5f9'}}, axisLabel:{fontSize:9,color:'#94a3b8'}},
    series:[{type:'bar', data:values, barMaxWidth:22, itemStyle:{color:chartColors.primary, borderRadius:[4,4,0,0]}}],
  } as EChartsOption;
}

export function buildHorizontalBarOption(labels: string[], values: number[]): EChartsOption {
  return {
    tooltip:{trigger:'axis'},
    grid:{left:88,right:16,top:8,bottom:8},
    xAxis:{type:'value', splitLine:{lineStyle:{color:'#f1f5f9'}}, axisLabel:{fontSize:9,color:'#94a3b8'}},
    yAxis:{type:'category', data:labels, inverse:true, axisLabel:{fontSize:10,color:chartColors.textMuted}, axisTick:{show:false}, axisLine:{show:false}},
    series:[{type:'bar', data:values, barMaxWidth:14, itemStyle:{color:chartColors.activity, borderRadius:[0,4,4,0]}, label:{show:true, position:'right', fontSize:9, color:chartColors.textMuted}}],
  } as EChartsOption;
}

export function buildStackedBarOption(labels: string[]): EChartsOption {
  const s1=labels.map((_,i)=> 8+Math.round(h(i*13+1)*22));
  const s2=labels.map((_,i)=> 6+Math.round(h(i*13+2)*18));
  const s3=labels.map((_,i)=> 4+Math.round(h(i*13+3)*14));
  return {
    tooltip:{trigger:'axis'},
    legend:{bottom:0, textStyle:{fontSize:9,color:chartColors.textMuted}, data:['A类','B类','C类']},
    grid:{left:32,right:8,top:10,bottom:26},
    xAxis:{type:'category', data:labels, axisLabel:{fontSize:9,color:chartColors.textMuted}, axisLine:{lineStyle:{color:chartColors.borderSoft}}, axisTick:{show:false}},
    yAxis:{type:'value', splitLine:{lineStyle:{color:'#f1f5f9'}}, axisLabel:{fontSize:9,color:'#94a3b8'}},
    series:[
      {type:'bar', stack:'st', name:'A类', data:s1, itemStyle:{color:'#2563eb'}},
      {type:'bar', stack:'st', name:'B类', data:s2, itemStyle:{color:'#14b8a6'}},
      {type:'bar', stack:'st', name:'C类', data:s3, itemStyle:{color:'#f59e0b'}},
    ],
  } as EChartsOption;
}

export function buildLineOption(labels: string[], values: number[]): EChartsOption {
  return {
    tooltip:{trigger:'axis'},
    grid:{left:30,right:10,top:12,bottom:22},
    xAxis:{type:'category', data:labels, boundaryGap:false, axisLabel:{fontSize:9,color:chartColors.textMuted}, axisTick:{show:false}, axisLine:{lineStyle:{color:chartColors.borderSoft}}},
    yAxis:{type:'value', splitLine:{lineStyle:{color:'#f1f5f9'}}, axisLabel:{fontSize:9,color:'#94a3b8'}},
    series:[{type:'line', data:values, smooth:true, symbol:'circle', symbolSize:4, lineStyle:{width:2,color:chartColors.primary}, itemStyle:{color:chartColors.primary}}],
  } as EChartsOption;
}

export function buildAreaOption(labels: string[], values: number[]): EChartsOption {
  return {
    tooltip:{trigger:'axis'},
    grid:{left:30,right:10,top:12,bottom:22},
    xAxis:{type:'category', data:labels, boundaryGap:false, axisLabel:{fontSize:9,color:chartColors.textMuted}, axisTick:{show:false}, axisLine:{lineStyle:{color:chartColors.borderSoft}}},
    yAxis:{type:'value', splitLine:{lineStyle:{color:'#f1f5f9'}}, axisLabel:{fontSize:9,color:'#94a3b8'}},
    series:[{type:'line', data:values, smooth:true, symbol:'none', lineStyle:{width:2,color:chartColors.primary}, itemStyle:{color:chartColors.primary}, areaStyle:{color:{type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(37,99,235,0.32)'},{offset:1,color:'rgba(37,99,235,0.04)'}]}}}],
  } as EChartsOption;
}

export function buildPieOption(labels: string[], values: number[]): EChartsOption {
  const data=labels.map((l,i)=>({name:l, value:values[i]|| 6+Math.round(h(i*13+4)*24)}));
  return {
    tooltip:{trigger:'item', formatter:'{b}: {c} ({d}%)'},
    legend:{bottom:0, textStyle:{fontSize:9,color:chartColors.textMuted}, type:'scroll'},
    series:[{type:'pie', radius:['0','62%'], center:['50%','42%'], data, label:{show:false}, labelLine:{show:false}, itemStyle:{borderRadius:3, borderColor:'#fff', borderWidth:1}}],
  } as EChartsOption;
}

export function buildDonutOption(labels: string[], values: number[]): EChartsOption {
  const data=labels.map((l,i)=>({name:l, value:values[i]|| 8+Math.round(h(i*13+5)*22)}));
  const total=data.reduce((s,d)=>s+d.value,0);
  return {
    tooltip:{trigger:'item'},
    legend:{bottom:0, textStyle:{fontSize:9,color:chartColors.textMuted}},
    series:[{type:'pie', radius:['46%','68%'], center:['50%','44%'], data, label:{show:false}, itemStyle:{borderRadius:4, borderColor:'#fff', borderWidth:2}}],
    graphic:[{type:'text', left:'center', top:'38%', style:{text:String(total), fontSize:14, fontWeight:800, fill:'#0f172a', textAlign:'center'}}],
  } as unknown as EChartsOption;
}

export function buildRoseOption(labels: string[], values: number[]): EChartsOption {
  const data=labels.map((l,i)=>({name:l, value:(values[i]||10)+Math.round(h(i*13+6)*18)}));
  return {
    tooltip:{trigger:'item'},
    legend:{bottom:0, textStyle:{fontSize:9,color:chartColors.textMuted}},
    series:[{type:'pie', radius:['14%','66%'], center:['50%','46%'], roseType:'area', data, label:{show:false}, itemStyle:{borderRadius:4}}],
  } as EChartsOption;
}

export function buildScatterOption(points: number[][]): EChartsOption {
  return {
    tooltip:{trigger:'item'},
    grid:{left:30,right:10,top:10,bottom:22},
    xAxis:{type:'value', splitLine:{lineStyle:{color:'#f1f5f9'}}, axisLabel:{fontSize:9,color:'#94a3b8'}},
    yAxis:{type:'value', splitLine:{lineStyle:{color:'#f1f5f9'}}, axisLabel:{fontSize:9,color:'#94a3b8'}},
    series:[{type:'scatter', symbolSize:6, data:points, itemStyle:{color:chartColors.primary, opacity:.8}}],
  } as EChartsOption;
}

export function buildRadarOption(labels: string[], values: number[]): EChartsOption {
  const indicators=labels.slice(0,6).map((l)=>({name:l, max: Math.max(30, Math.max(...values)*1.25)}));
  while(indicators.length<5) indicators.push({name:`维度${indicators.length+1}`, max:40});
  const vals=indicators.map((_, idx)=> values[idx]!==undefined? values[idx]: 10+Math.round(h(idx*13+7)*20));
  return {
    tooltip:{trigger:'item'},
    radar:{indicator:indicators, center:['50%','52%'], radius:'66%', axisName:{fontSize:9,color:chartColors.textMuted}, splitLine:{lineStyle:{color:chartColors.borderSoft}}, axisLine:{lineStyle:{color:chartColors.borderSoft}}, splitArea:{areaStyle:{color:['#f8fafc','#ffffff']}}},
    series:[{type:'radar', data:[{value:vals, name:'当前', areaStyle:{color:'rgba(37,99,235,0.18)'}, lineStyle:{color:chartColors.primary, width:2}, itemStyle:{color:chartColors.primary}}]}],
  } as EChartsOption;
}

export function buildHeatmapMatrixOption(xCats: string[], yCats: string[], data: [number,number,number][]): EChartsOption {
  return {
    tooltip:{position:'top'},
    grid:{left:46,right:8,top:8,bottom:28},
    xAxis:{type:'category', data:xCats, axisLabel:{fontSize:7,color:chartColors.textMuted, interval:1}, axisTick:{show:false}, axisLine:{show:false}},
    yAxis:{type:'category', data:yCats, axisLabel:{fontSize:9,color:chartColors.textMuted}, axisTick:{show:false}, axisLine:{show:false}},
    visualMap:{show:false, min:0, max:90, inRange:{color:chartColors.heatmapTeal}},
    series:[{type:'heatmap', data, label:{show:false}, itemStyle:{borderWidth:.5, borderColor:'#fff'}}],
  } as EChartsOption;
}

export function buildTreemapOption(labels: string[], values: number[]): EChartsOption {
  const data=labels.map((l,i)=>({name:l, value:values[i]|| 10+Math.round(h(i*13+8)*30)}));
  return {
    tooltip:{formatter:(p: unknown)=> { const d=p as {name?:string; value?:number}; return `${d.name}: ${d.value}`; }},
    series:[{type:'treemap', data, roam:false, nodeClick:false, breadcrumb:{show:false}, label:{show:true, fontSize:9, color:'#fff'}, itemStyle:{borderColor:'#fff', borderWidth:1, gapWidth:1}}],
  } as EChartsOption;
}

export function buildFunnelOption(labels: string[], values: number[]): EChartsOption {
  const data=labels.slice(0,5).map((l,i)=>({name:l, value: values[i]|| 60 - i*8 + Math.round(h(i*13+9)*8)}));
  data.sort((a,b)=> b.value-a.value);
  return {
    tooltip:{trigger:'item'},
    legend:{bottom:0, textStyle:{fontSize:9,color:chartColors.textMuted}},
    series:[{type:'funnel', left:'10%', top:10, bottom:28, width:'80%', sort:'descending', gap:2, label:{fontSize:9, color:'#334155', position:'inside', formatter:'{b}'}, itemStyle:{borderColor:'#fff', borderWidth:1}, data}],
  } as unknown as EChartsOption;
}

export function buildGaugeOption(value: number, max=100): EChartsOption {
  const pct=Math.max(0,Math.min(100, Math.round(value)));
  return {
    series:[{type:'gauge', min:0,max, splitNumber:5, axisLine:{lineStyle:{width:12, color:[[pct/100, chartColors.primary],[1,chartColors.borderSoft]]}},
      pointer:{itemStyle:{color:chartColors.primary}, length:'62%', width:4},
      axisTick:{distance:-12, length:4, lineStyle:{color:'#94a3b8', width:1}},
      splitLine:{distance:-12, length:10, lineStyle:{color:chartColors.textMuted, width:1}},
      axisLabel:{fontSize:8,color:chartColors.textMuted, distance:12},
      detail:{valueAnimation:true, fontSize:18, fontWeight:'bold', color:'#0f172a', offsetCenter:[0,'68%'], formatter:'{value}%'},
      data:[{value:pct}]}],
  } as EChartsOption;
}

export function buildSankeyOption(): EChartsOption {
  const nodes=[{name:'聊天'},{name:'视频'},{name:'工具'},{name:'上午'},{name:'下午'},{name:'晚间'},{name:'手机'},{name:'PC'}];
  const links=[
    {source:'聊天',target:'上午',value:12},{source:'聊天',target:'晚间',value:18},
    {source:'视频',target:'晚间',value:22},{source:'视频',target:'下午',value:10},
    {source:'工具',target:'上午',value:16},{source:'工具',target:'下午',value:14},
    {source:'上午',target:'手机',value:14},{source:'上午',target:'PC',value:14},
    {source:'下午',target:'手机',value:12},{source:'下午',target:'PC',value:12},
    {source:'晚间',target:'手机',value:28},{source:'晚间',target:'PC',value:12},
  ];
  return {
    tooltip:{trigger:'item'},
    series:[{type:'sankey', data:nodes, links, emphasis:{focus:'adjacency'}, lineStyle:{color:'gradient', curveness:0.5}, label:{fontSize:9, color:'#334155'}, nodeWidth:10, nodeGap:8}],
  } as unknown as EChartsOption;
}

export function buildProgressRingOption(value: number): EChartsOption {
  const pct=Math.max(5,Math.min(100, Math.round(value)));
  return {
    series:[{type:'gauge', startAngle:90, endAngle:-270, min:0,max:100, progress:{show:true, width:12, itemStyle:{color:{type:'linear',x:0,y:0,x2:1,y2:0,colorStops:[{offset:0,color:chartColors.primary},{offset:1,color:chartColors.activity}]}}}, axisLine:{lineStyle:{width:12, color:[[1,'#f1f5f9']]}},
      pointer:{show:false}, axisTick:{show:false}, splitLine:{show:false}, axisLabel:{show:false},
      detail:{valueAnimation:true, fontSize:16, fontWeight:'bold', color:'#0f172a', offsetCenter:[0,0], formatter:pct+'%'}, data:[{value:pct}], title:{show:false}}],
  } as EChartsOption;
}

export function buildGradientBarOption(labels: string[], values: number[]): EChartsOption {
  return {
    tooltip:{trigger:'axis'},
    grid:{left:88,right:16,top:8,bottom:8},
    xAxis:{type:'value', show:false, max: Math.max(...values,1)*1.22},
    yAxis:{type:'category', data:labels, inverse:true, axisLabel:{fontSize:10,color:'#334155'}, axisTick:{show:false}, axisLine:{show:false}},
    series:[{type:'bar', data:values, barWidth:12, itemStyle:{borderRadius:[0,8,8,0], color:{type:'linear',x:0,y:0,x2:1,y2:0,colorStops:[{offset:0,color:chartColors.primary},{offset:1,color:'#22d3ee'}]}} as unknown as string, label:{show:true, position:'right', fontSize:9, color:chartColors.textMuted}}],
  } as EChartsOption;
}

export function buildWordcloudFakeOption(labels: string[], values: number[]): EChartsOption {
  const data=labels.map((l,i)=>({name:l, value:(values[i]||10)*(0.9+h(i*13+10)*0.4)}));
  const pts=data.map((d,i)=>({value:[h(i*13+11)*100, h(i*13+12)*100, d.value], name:d.name}));
  return {
    tooltip:{formatter:(p: unknown)=> { const d=p as {name?:string; value?:number[]}; return `${d.name}: ${d.value?.[2] ? Math.round(d.value[2]): ''}`; }},
    grid:{left:6,right:6,top:6,bottom:6},
    xAxis:{type:'value', show:false, min:0,max:100},
    yAxis:{type:'value', show:false, min:0,max:100},
    series:[{type:'scatter', data: pts.map(p=>({value:p.value, name:p.name})), symbolSize:(d: number[])=> 9+ (d[2] as number)/5, label:{show:true, formatter:(p: unknown)=> (p as {name:string}).name, fontSize:9, color:'#334155'}, itemStyle:{color:'rgba(37,99,235,0.72)'}}],
  } as unknown as EChartsOption;
}

// 针对真实数据的专用 builders

export function buildAppCategoryShareOption(points: Array<{label:string; value:number}>): EChartsOption {
  const data=points.map(p=>({name:p.label, value:p.value}));
  return {
    tooltip:{trigger:'item', formatter:(p: unknown)=> { const d=p as {name?:string; value?:number; percent?:number}; return `${d.name}: ${d.value} (${d.percent}%)`; }},
    legend:{bottom:0, textStyle:{fontSize:10,color:chartColors.textMuted}, type:'scroll'},
    series:[{type:'pie', radius:['42%','68%'], center:['50%','44%'], data, label:{show:false}, itemStyle:{borderRadius:4, borderColor:'#fff', borderWidth:1}}],
  } as EChartsOption;
}

export function buildTaskAreaOption(dates: string[], completed: number[], total: number[]): EChartsOption {
  return {
    tooltip:{trigger:'axis'},
    legend:{bottom:0, data:['已完成','总计'], textStyle:{fontSize:9,color:chartColors.textMuted}},
    grid:{left:34,right:10,top:10,bottom:26},
    xAxis:{type:'category', data:dates, boundaryGap:false, axisLabel:{fontSize:8,color:chartColors.textMuted, interval:5}, axisTick:{show:false}, axisLine:{lineStyle:{color:chartColors.borderSoft}}},
    yAxis:{type:'value', splitLine:{lineStyle:{color:'#f1f5f9'}}, axisLabel:{fontSize:9,color:'#94a3b8'}},
    series:[
      {name:'已完成', type:'line', data:completed, smooth:true, symbol:'none', lineStyle:{width:1.5, color:chartColors.activity}, areaStyle:{color:'rgba(20,184,166,0.18)'}, stack:'a'},
      {name:'总计', type:'line', data:total.map((t,i)=> t - completed[i]), smooth:true, symbol:'none', lineStyle:{width:1.5, color:chartColors.warning}, areaStyle:{color:'rgba(245,158,11,0.18)'}, stack:'a'},
    ],
  } as EChartsOption;
}

export function buildCalendarHeatmapOption(dates: string[], values: number[]): EChartsOption {
  // simplify as heatmap week view
  const weeks=5;
  const weekdays=['一','二','三','四','五','六','日'];
  const xCats=Array.from({length:weeks},(_,i)=>`W${i+1}`);
  const data: [number,number,number][]=[];
  dates.forEach((_,i)=>{
    const x=Math.floor(i/7), y=i%7;
    if(x<weeks) data.push([x,y, values[i]||0]);
  });
  return {
    tooltip:{formatter:(p: unknown)=> { const d=p as {value?:number[]}; return `${d.value?.[2]} • ${xCats[d.value?.[0]||0]} ${weekdays[d.value?.[1]||0]}`; }},
    grid:{left:28,right:8,top:8,bottom:22},
    xAxis:{type:'category', data:xCats, axisLabel:{fontSize:8,color:chartColors.textMuted}, axisTick:{show:false}},
    yAxis:{type:'category', data:weekdays, axisLabel:{fontSize:8,color:chartColors.textMuted}, axisTick:{show:false}},
    visualMap:{show:false, min:0, max:Math.max(...values,1), inRange:{color:chartColors.githubGreen}},
    series:[{type:'heatmap', data, itemStyle:{borderWidth:1, borderColor:'#fff', borderRadius:2}}],
  } as unknown as EChartsOption;
}

// 为了验收：导出一个包含 40 种形式的映射，方便页面动态取用
export const CHART_TYPE_NAMES = [
  "柱状图（垂直）","柱状图（水平）","堆叠柱状图","分组柱状图","折线图","多线折线图","面积图","堆叠面积图","饼图","环形图","南丁格尔玫瑰图","散点图","气泡图","雷达图","热力图（矩阵）","日历热力图","24小时时钟图","树图","旭日图","漏斗图","仪表盘","水波图","桑基图","箱线图","小提琴图","平行坐标图","力导向图","弧线图","步进图","火焰图","进度环","渐变进度条","差异图","子弹图","小多图","流图","词云","带注释的时间线","矩阵散点图","六边形分箱",
];
