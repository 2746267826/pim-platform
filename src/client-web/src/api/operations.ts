import { apiGet, apiPost } from './client';
import type { ApiResponse, OperationConfirmation } from '../types';

export const operationsApiPaths = {
  pendingConfirmations() {
    return '/operations/confirmations/pending';
  },
  detail(id: string) {
    return `/operations/confirmations/${encodeURIComponent(id)}`;
  },
  confirm(id: string) {
    return `/operations/confirmations/${encodeURIComponent(id)}/confirm`;
  },
  reject(id: string) {
    return `/operations/confirmations/${encodeURIComponent(id)}/reject`;
  },
} as const;

export async function getPendingConfirmations() {
  const r = await apiGet<ApiResponse<OperationConfirmation[]>>(
    operationsApiPaths.pendingConfirmations()
  );
  return r.data;
}

export async function getConfirmationDetail(id: string) {
  const r = await apiGet<ApiResponse<OperationConfirmation>>(
    operationsApiPaths.detail(id)
  );
  return r.data;
}

export async function confirmOperation(id: string) {
  const r = await apiPost<ApiResponse<OperationConfirmation>>(
    operationsApiPaths.confirm(id),
    {}
  );
  return r.data;
}

export async function rejectOperation(id: string) {
  const r = await apiPost<ApiResponse<OperationConfirmation>>(
    operationsApiPaths.reject(id),
    {}
  );
  return r.data;
}
