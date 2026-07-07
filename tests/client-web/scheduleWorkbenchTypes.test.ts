import assert from 'node:assert/strict';
import type {
  CalendarLayerId,
  CalendarLayerResponse,
  DataCenterQueryResponse,
  OperationConfirmation,
  OperationRiskLevel,
  OutlookDeviceCodeRequestResponse,
  OutlookSettingsResponse,
  OutlookSyncBatchResponse,
  TaskExecutionSegmentResponse,
  WorkbenchDensityMode,
} from '../../src/client-web/src/types';

const riskLevel: OperationRiskLevel = 'L3ExternalSourceOrWriteback';
const layerId: CalendarLayerId = 'task-segments';
const densityMode: WorkbenchDensityMode = 'focus';

const taskSegment: TaskExecutionSegmentResponse = {
  id: 'segment-1',
  taskId: 'task-1',
  taskTitle: 'Draft launch plan',
  startsAt: '2026-07-08T09:00:00Z',
  endsAt: '2026-07-08T10:00:00Z',
  status: 'Planned',
  source: 'manual',
  planningReason: 'Priority block',
  confirmationId: 'confirmation-1',
};

const calendarLayers: CalendarLayerResponse = {
  start: '2026-07-08T00:00:00Z',
  end: '2026-07-09T00:00:00Z',
  items: [
    {
      id: 'layer-item-1',
      layer: layerId,
      objectType: 'task-segment',
      objectId: 'segment-1',
      title: 'Draft launch plan',
      startsAt: taskSegment.startsAt,
      endsAt: taskSegment.endsAt,
      source: 'manual',
      status: 'Planned',
      color: '#2563eb',
      requiresConfirmation: true,
    },
  ],
};

const dataCenterQuery: DataCenterQueryResponse = {
  items: [
    {
      objectType: 'event',
      objectId: 'event-1',
      title: 'Outlook planning',
      source: 'outlook',
      status: 'Pending',
      startsAt: '2026-07-08T11:00:00Z',
      endsAt: '2026-07-08T12:00:00Z',
      summary: 'Awaiting confirmation',
    },
  ],
  page: 1,
  pageSize: 50,
  totalCount: 1,
};

const outlookSettings: OutlookSettingsResponse = {
  provider: 'outlook',
  tenantId: 'common',
  clientId: 'client-1',
  scopes: 'Calendars.ReadWrite offline_access',
  status: 'Connected',
  tokenHealth: 'Healthy',
  lastSyncedAt: '2026-07-08T08:00:00Z',
  lastError: null,
};

const deviceCode: OutlookDeviceCodeRequestResponse = {
  endpoint: 'https://login.microsoftonline.com/common/oauth2/v2.0/devicecode',
  verificationUri: 'https://microsoft.com/devicelogin',
  userCode: 'ABCD-EFGH',
  expiresAt: '2026-07-08T08:15:00Z',
  message: 'Use this code to sign in.',
};

const syncBatch: OutlookSyncBatchResponse = {
  id: 'batch-1',
  provider: 'outlook',
  status: 'Succeeded',
  readCount: 10,
  createdCount: 2,
  updatedCount: 3,
  conflictCount: 1,
  confirmationCount: 1,
  failureCount: 0,
  steps: [
    {
      name: 'Read calendar',
      status: 'Succeeded',
      detail: 'Imported 10 items',
      at: '2026-07-08T08:00:00Z',
    },
  ],
  errorSummary: null,
  startedAt: '2026-07-08T08:00:00Z',
  finishedAt: '2026-07-08T08:01:00Z',
};

const confirmation: OperationConfirmation = {
  id: 'confirmation-1',
  requestedByUserId: 'user-1',
  operationType: 'outlook.writeback',
  summary: 'Write back Outlook task block',
  riskLevel,
  source: 'outlook',
  payloadJson: '{}',
  previewJson: '{}',
  status: 'Pending',
  expiresAt: '2026-07-08T09:00:00Z',
  createdAt: '2026-07-08T08:00:00Z',
  confirmedAt: null,
  executedAt: null,
  resultJson: null,
  correlationId: 'correlation-1',
  changedFields: ['startsAt', 'endsAt'],
  allowedActions: ['confirm', 'reject'],
  objectType: 'task-segment',
  objectId: 'segment-1',
  requiresSecondLevelConfirmation: false,
};

assert.equal(riskLevel, 'L3ExternalSourceOrWriteback');
assert.equal(layerId, 'task-segments');
assert.equal(densityMode, 'focus');

void calendarLayers;
void dataCenterQuery;
void outlookSettings;
void deviceCode;
void syncBatch;
void confirmation;
