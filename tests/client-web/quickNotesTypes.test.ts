import assert from 'node:assert/strict';
import type {
  QuickNoteAttachment,
  QuickNoteAttachmentUpload,
  QuickNoteDetail,
  QuickNoteListItem,
  QuickNoteStatus,
} from '../../src/client-web/src/types';
import { apiUpload } from '../../src/client-web/src/api/client';
import { restoreQuickNote, uploadQuickNoteAttachment } from '../../src/client-web/src/api/quickNotes';

const status: QuickNoteStatus = 'inbox';

function acceptsRestoreQuickNoteSignature(fn: typeof restoreQuickNote) {
  return fn('11111111-1111-1111-1111-111111111111', 'processed');
}

function acceptsApiUploadSignature(fn: typeof apiUpload) {
  return fn('/quick-notes/attachments', new FormData());
}

function acceptsUploadQuickNoteAttachmentReturn(
  result: ReturnType<typeof uploadQuickNoteAttachment>,
): Promise<QuickNoteAttachmentUpload> {
  return result;
}

void acceptsRestoreQuickNoteSignature;
void acceptsApiUploadSignature;
void acceptsUploadQuickNoteAttachmentReturn;

const attachment: QuickNoteAttachment = {
  id: '22222222-2222-2222-2222-222222222222',
  fileName: 'alpha.png',
  contentType: 'image/png',
  sizeBytes: 1024,
  downloadUrl: '/api/v1/quick-notes/attachments/22222222-2222-2222-2222-222222222222/download',
  previewUrl: null,
  createdAt: '2026-05-26T00:00:00Z',
};

const listItem: QuickNoteListItem = {
  id: '11111111-1111-1111-1111-111111111111',
  contentPreview: 'alpha',
  status,
  source: 'web-floating',
  attachmentCount: 1,
  createdAt: '2026-05-26T00:00:00Z',
  updatedAt: '2026-05-26T00:00:00Z',
  archivedAt: null,
};

const detail: QuickNoteDetail = {
  ...listItem,
  contentMarkdown: '# alpha',
  attachments: [attachment],
  metadataJson: '{}',
};

assert.equal(detail.status, 'inbox');
assert.equal(detail.attachments[0].fileName, 'alpha.png');
