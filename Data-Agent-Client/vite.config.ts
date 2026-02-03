import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
    plugins: [react()],
    server: {
        proxy: {
            '/api': {
                target: 'http://localhost:8081',
                changeOrigin: true,
            },
        },
    },
    build: {
        rollupOptions: {
            output: {
                manualChunks(id) {
                    if (id.includes('node_modules')) {
                        // Monaco 体积最大，单独拆出
                        if (id.includes('monaco') || id.includes('monaco-editor')) return 'monaco'
                        // Radix UI 一组
                        if (id.includes('@radix-ui')) return 'radix'
                        // i18n 一组
                        if (id.includes('i18next')) return 'i18n'
                        // 其余第三方（含 React）打成一个 vendor，避免与 react-vendor 循环引用
                        return 'vendor'
                    }
                },
            },
        },
        chunkSizeWarningLimit: 600,
    },
})
