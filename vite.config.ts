import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'
import { serviceWorker } from './pwa/service-worker-plugin.ts'

// https://vite.dev/config/
export default defineConfig({
  base: './',
  plugins: [react(), serviceWorker()],
  // e2e/ belongs to Playwright, not Vitest.
  test: { include: ['tests/**/*.test.{ts,tsx}'] },
})
