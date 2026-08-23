import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:5858', changeOrigin: true },
      // 本地开发没有 nginx，把 /tiles 直接转发到 OSM，与生产 nginx 中转行为一致
      '/tiles': {
        target: 'https://tile.openstreetmap.org',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/tiles/, '')
      }
    }
  },
  build: {
    outDir: '../Pim.Api/wwwroot',
    emptyOutDir: true
  },
  define: {
    __APP_VERSION__: JSON.stringify(process.env.VITE_APP_VERSION || '0.0.0-local'),
    __GIT_SHA__: JSON.stringify(process.env.VITE_GIT_SHA || process.env.GITHUB_SHA?.slice(0,7) || 'local')
  }
})
