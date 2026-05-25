import assert from 'node:assert/strict';
import { test } from 'vitest';
import { pcClassificationApiPaths } from '../../src/client-web/src/api/pcTracker';

test('pc classification API paths stay under the classification route', () => {
  const paths = [
    pcClassificationApiPaths.rules,
    pcClassificationApiPaths.preview,
    pcClassificationApiPaths.apply,
    pcClassificationApiPaths.suggestions('2026-05-25'),
    pcClassificationApiPaths.settings,
    pcClassificationApiPaths.recentProjectTags,
  ];

  assert.deepEqual(paths, [
    '/pc/classification/rules',
    '/pc/classification/rules/preview',
    '/pc/classification/rules/apply',
    '/pc/classification/suggestions?date=2026-05-25',
    '/pc/classification/settings',
    '/pc/classification/project-tags/recent',
  ]);

  for (const path of paths) {
    assert.equal(path.startsWith('/pc/classification'), true);
  }
});
