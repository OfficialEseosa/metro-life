import react from '@vitejs/plugin-react'
import { copyFileSync, existsSync, mkdirSync } from 'node:fs'
import { basename, dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig, loadEnv, type Plugin } from 'vite'

const root = dirname(fileURLToPath(import.meta.url))
const sharedSource = resolve(root, '../shared')
const sharedCopy = resolve(root, 'src/shared-data')

/** Keeps src/shared-data in step with ../shared while the dev server runs (npm scripts do the initial copy). */
function sharedData(): Plugin {
  return {
    name: 'metro-shared-data',
    apply: 'serve',
    configureServer(server) {
      if (!existsSync(sharedSource)) return
      server.watcher.add(sharedSource)
      const copy = (file: string) => {
        if (dirname(file) !== sharedSource || !file.endsWith('.json')) return
        mkdirSync(sharedCopy, { recursive: true })
        copyFileSync(file, join(sharedCopy, basename(file)))
      }
      server.watcher.on('change', copy)
      server.watcher.on('add', copy)
    },
  }
}

/**
 * Phosphor draws every icon in six weights and the bundle carries all of them. The game uses bold,
 * duotone and fill, so the thin, light and regular drawings are dropped from the production build;
 * a stray "regular" icon falls back to the duotone drawing instead of disappearing.
 */
function slimIcons(): Plugin {
  return {
    name: 'metro-slim-icons',
    apply: 'build',
    transform(code, id) {
      if (!/@phosphor-icons[\\/]react[\\/]dist[\\/]defs[\\/]/.test(id)) return
      const map = /const (\w+) = [^\n]*new Map\(\[/.exec(code)?.[1]
      if (!map) return
      const slim = code
        .replace(/\n {2}\[\n {4}"(?:thin|light|regular)",[\s\S]*?\n {2}\],?(?=\n)/g, '')
        .replace(/\n\]\);/, `\n]);\n${map}.set('regular', ${map}.get('duotone'));`)
      return slim === code ? undefined : { code: slim, map: null }
    },
  }
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, root, '')
  const target = (env.METRO_API_PROXY || 'http://localhost:8080').replace(/\/+$/, '')
  return {
    plugins: [react(), sharedData(), slimIcons()],
    build: {
      rolldownOptions: {
        output: {
          // Vendor code in its own long-lived chunks, fetched and compiled in parallel with the app.
          codeSplitting: {
            groups: [
              { name: 'react', test: /[\\/]node_modules[\\/](react|react-dom|scheduler)[\\/]/, priority: 30 },
              { name: 'three', test: /[\\/]node_modules[\\/]three[\\/]build[\\/]/, priority: 30 },
              { name: 'icons', test: /[\\/]node_modules[\\/]@phosphor-icons[\\/]/, priority: 30 },
            ],
          },
        },
      },
    },
    server: {
      port: 5173,
      proxy: {
        '/api': target,
        '/ws': { target: target.replace(/^http/, 'ws'), ws: true },
      },
    },
  }
})
