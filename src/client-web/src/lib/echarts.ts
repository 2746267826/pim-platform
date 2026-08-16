import * as echarts from 'echarts/core';
import { BarChart, CustomChart, GaugeChart, HeatmapChart, LineChart, PieChart } from 'echarts/charts';
import { DataZoomComponent, GraphicComponent, GridComponent, LegendComponent, MarkAreaComponent, TooltipComponent, VisualMapComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
echarts.use([BarChart, CustomChart, GaugeChart, HeatmapChart, LineChart, PieChart, DataZoomComponent, GraphicComponent, GridComponent, LegendComponent, MarkAreaComponent, TooltipComponent, VisualMapComponent, CanvasRenderer]);
export { echarts };
export type EChartsOption = echarts.EChartsCoreOption;
