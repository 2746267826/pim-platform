import { defineConfig } from 'vite'
import { resolve } from 'path'
import { copyFileSync, mkdirSync, existsSync } from 'fs'

export default defineConfig({
    build: {
        outDir: 'dist',
        emptyOutDir: true,
        rollupOptions: {
            input: {
                'background/main': resolve(__dirname, 'src/background/main.ts'),
            },
            output: {
                entryFileNames: '[name].js',
                chunkFileNames: '[name].js',
                assetFileNames: '[name].[ext]',
            },
        },
    },
    plugins: [
        {
            name: 'copy-manifest',
            closeBundle() {
                try {
                    if (!existsSync('dist')) mkdirSync('dist', { recursive: true })
                    copyFileSync('src/manifest.json', 'dist/manifest.json')
                } catch {}
            },
        },
    ],
})
