import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';
export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        }
    },
    server: {
        port: 5173,
        proxy: {
            // 前端所有 /api、/files 请求都转发到后端，彻底绕开跨域
            '/api': { target: 'http://localhost:8080', changeOrigin: true },
            '/files': { target: 'http://localhost:8080', changeOrigin: true }
        }
    }
});
