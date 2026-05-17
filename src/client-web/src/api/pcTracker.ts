import { apiGet, apiPost, apiDelete } from './client';
import type { ApiResponse } from '../types';
import type {
  PcSummaryResponse, TimelineItem, HeatmapBucket,
  DetailQueryParams, DetailQueryResponse,
  AppCategoryRule, HeatmapGridResponse
} from '../types';

export function getPcSummary(date: string) {
  return apiGet<ApiResponse<PcSummaryResponse>>(`/pc/summary?date=${date}`).then(r => r.data);
}

export function getPcTimeline(date: string) {
  return apiGet<ApiResponse<TimelineItem[]>>(`/pc/aw/timeline?date=${date}`).then(r => r.data);
}

export function getPcHeatmap(start: string, end: string) {
  return apiGet<ApiResponse<HeatmapBucket[]>>(`/pc/aw/heatmap?start=${start}&end=${end}`).then(r => r.data);
}

export function getPcHeatmapGrid(start: string, end: string, dimension: string) {
  return apiGet<ApiResponse<HeatmapGridResponse>>(
    `/pc/heatmap/grid?start=${start}&end=${end}&dimension=${dimension}`
  ).then(r => r.data);
}

export function queryPcDetail(params: DetailQueryParams) {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') searchParams.set(k, String(v));
  });
  return apiGet<ApiResponse<DetailQueryResponse>>(`/pc/detail?${searchParams.toString()}`).then(r => r.data);
}

export function getPcCategories() {
  return apiGet<ApiResponse<AppCategoryRule[]>>('/pc/categories').then(r => r.data);
}

export function savePcCategory(rule: { appPattern: string; categoryName: string; color: string; priority: number }) {
  return apiPost<ApiResponse<AppCategoryRule>>('/pc/categories', rule).then(r => r.data);
}

export function deletePcCategory(id: string) {
  return apiDelete<ApiResponse<string>>(`/pc/categories/${id}`).then(r => r.data);
}
