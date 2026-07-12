import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

export default defineConfig({
  plugins: [react()],
  base: './',
  publicDir: 'public',
  server: {
    fs: {
      allow: [path.resolve(__dirname, '..'), path.resolve(__dirname)],
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  test: {
    environment: 'node',
  },
});
