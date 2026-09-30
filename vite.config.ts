import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'
import { serviceWorker } from './pwa/service-worker-plugin.ts'

// https://vite.dev/config/
import { readFileSync } from 'node:fs'

const pkg = JSON.parse(readFileSync(new URL('./package.json', import.meta.url), 'utf8')) as { version: string }

export default defineConfig({
  define: { __APP_VERSION__: JSON.stringify(pkg.version) },
  base: './',
  plugins: [
    react(),
    serviceWorker(),
    {
      // A content security policy for the built site only (the dev server needs inline scripts for hot reload).
      name: 'csp',
      apply: 'build',
      transformIndexHtml: (html: string) =>
        html.replace(
          '<meta charset="UTF-8" />',
          `<meta charset="UTF-8" />\n    <meta http-equiv="Content-Security-Policy" content="default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; media-src 'self' blob:; font-src 'self' data:; connect-src 'self'; worker-src 'self' blob:; manifest-src 'self'; object-src 'none'; base-uri 'self'; form-action 'none'" />`
        ),
    },
  ],
  // e2e/ belongs to Playwright, not Vitest.
  test: { include: ['tests/**/*.test.{ts,tsx}'] },
})
