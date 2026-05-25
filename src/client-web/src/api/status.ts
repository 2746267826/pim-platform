import { apiGet } from './client';
import type { ApiResponse, SystemStatusDetail, SystemStatusSummary } from '../types';

export const statusApiPaths = {
  summary: '/status/summary',
  detail: '/status/',
} as const;

export async function getStatusSummary() {
  const response = await apiGet<ApiResponse<SystemStatusSummary>>(statusApiPaths.summary);
  return response.data;
}

export async function getStatusDetail() {
  const response = await apiGet<ApiResponse<SystemStatusDetail>>(statusApiPaths.detail);
  return response.data;
}
