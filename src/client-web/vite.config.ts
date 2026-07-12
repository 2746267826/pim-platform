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
      '/api': { target: 'http://localhost:5858', changeOrigin: true }
    }
  },
  build: {
    outDir: '../Pim.Api/wwwroot',
    emptyOutDir: true
  },
  define: {
    __APP_VERSION__: JSON.stringify(process.env.VITE_APP_VERSION || '0.0.0-local')
  }
})
