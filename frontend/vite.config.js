import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  define: { global: {} },
  server: {
    port: 5175,
    proxy: {
      '/api': 'http://localhost:8090',
      '/ws': { target: 'http://localhost:8090', ws: true }
    }
  }
})
