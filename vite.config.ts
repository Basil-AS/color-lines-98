import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'
import { serviceWorker } from './pwa/service-worker-plugin.ts'

// https://vite.dev/config/
import { readFileSync } from 'node:fs'

const pkg = JSON.parse(readFileSync(new URL('./package.json', import.meta.url), 'utf8')) as { version: string }

export default defineConfig({
  define: { __APP_VERSION__: JSON.stringify(pkg.version) },
  base: './',
  plugins: [react(), serviceWorker()],
  // e2e/ belongs to Playwright, not Vitest.
  test: { include: ['tests/**/*.test.{ts,tsx}'] },
})
