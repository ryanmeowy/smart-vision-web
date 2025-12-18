import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173, // 前端运行端口
    proxy: {
      // 告诉前端：只要是 /api 开头的请求，都转发给 Spring Boot 后端
      '/api': {
        target: 'http://localhost:8080', // 你的后端地址
        changeOrigin: true
      }
    }
  }
})