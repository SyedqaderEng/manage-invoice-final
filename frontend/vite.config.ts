import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// Settings are read from the project-root .env or supplied by the startup script.
// Only VITE_* variables are exposed to the browser.
const envDir = '..'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, envDir, 'VITE_')
  return {
    envDir,
    plugins: [react()],
    server: {
      host: '127.0.0.1',
      port: 5177,
      strictPort: true,
      proxy: {
        '/api': env.VITE_API_PROXY_TARGET || 'http://127.0.0.1:8094'
      }
    }
  }
})
