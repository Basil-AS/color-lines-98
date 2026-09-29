import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { serviceWorker } from './pwa/service-worker-plugin.ts'

// https://vite.dev/config/
export default defineConfig({
  base: './',
  plugins: [react(), serviceWorker()],
})
