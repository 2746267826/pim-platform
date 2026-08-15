import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'node:url';

const repoRoot = fileURLToPath(new URL('../..', import.meta.url));

export default defineConfig({
  root: repoRoot,
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: false,
    include: [fileURLToPath(new URL('../../tests/client-web/labelingQueue.test.tsx', import.meta.url))],
    server: {
      fs: { allow: [repoRoot] },
    },
  },
});
