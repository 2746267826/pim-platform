import assert from 'node:assert/strict';
import { test } from 'vitest';
import type {
  ActivityClassificationPreview,
  ActivityClassificationSettings,
} from '../../src/client-web/src/types';

test('pc classification preview and settings use camelCase fields', () => {
  const preview: ActivityClassificationPreview = {
    affectedRecordCount: 2,
    affectedDurationSeconds: 300,
    currentCategoryCounts: { Uncategorized: 2 },
    newCategoryCounts: { Work: 2 },
    samples: [],
    requiresConfirmation: true,
    summary: '2 records will be updated.',
  };

  const settings: ActivityClassificationSettings = {
    recommendedMinimumClassificationDurationMinutes: 5,
    supportedRecommendedMinimumDurations: [1, 5, 10, 15],
  };

  assert.equal(preview.requiresConfirmation, true);
  assert.equal(settings.supportedRecommendedMinimumDurations.includes(5), true);
});
