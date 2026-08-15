import { apiGet, apiPost } from './client';
import type { ApiResponse } from '../types';

export interface LabelingQueueItem {
  targetType: 'app' | 'domain' | 'mobile_app';
  target: string;
  displayName: string;
  minutes: number;
  sampleTitles: string[];
}

export interface CategoryDictionaryItem {
  id: string;
  name: string;
  color: string;
  icon: string | null;
}

export interface LabelingQueueResponse {
  items: LabelingQueueItem[];
}

export interface SubmitLabelResponse {
  ok: boolean;
  categoryId?: string | null;
  categoryName?: string | null;
  created: string;
}

export interface SubmitLabelRequest {
  targetType: 'app' | 'domain' | 'mobile_app';
  target: string;
  categoryId?: string;
  categoryName?: string;
  scope: 'all' | 'keyword';
  keyword?: string;
}

export function fetchLabelingQueue(limit = 20) {
  return apiGet<ApiResponse<LabelingQueueResponse>>(`/pc/classification/queue?limit=${limit}`)
    .then(r => r.data);
}

export function fetchCategoryDictionary() {
  return apiGet<ApiResponse<CategoryDictionaryItem[]>>('/pc/categories/dictionary')
    .then(r => r.data);
}

export function submitLabel(body: SubmitLabelRequest) {
  return apiPost<ApiResponse<SubmitLabelResponse>>('/pc/classification/label', body)
    .then(r => r.data);
}
