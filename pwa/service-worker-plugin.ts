import { createHash } from 'node:crypto';
import { readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs';
import { join, relative } from 'node:path';
import type { Plugin } from 'vite';
import { buildServiceWorker, precacheList } from './precache.ts';

function walk(dir: string): string[] {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name);
    return statSync(path).isDirectory() ? walk(path) : [path];
  });
}

/**
 * Writes sw.js listing every file of the finished build (hashed assets, public files, the HTML),
 * so the game works offline. It runs after the files are on disk because only then are the final
 * file names known.
 */
export function serviceWorker(): Plugin {
  return {
    name: 'color-lines-service-worker',
    apply: 'build',
    writeBundle(options) {
      const outDir = options.dir;
      if (!outDir) return;
      const files = walk(outDir).map((p) => relative(outDir, p).split('\\').join('/'));
      const seed = createHash('sha1');
      for (const f of files.filter((f) => f !== 'sw.js').sort()) seed.update(readFileSync(join(outDir, f)));
      writeFileSync(join(outDir, 'sw.js'), buildServiceWorker(precacheList(files), seed.digest('hex')));
    },
  };
}
