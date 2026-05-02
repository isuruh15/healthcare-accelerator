import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { writeFileSync } from 'fs'
import { resolve } from 'path'

function assetManifestPlugin() {
  return {
    name: 'asset-manifest',
    apply: 'build',
    closeBundle() {
      const manifest = {
        files: {},
        entrypoints: [],
      }
      const outputDir = resolve(__dirname, 'dist')
      const fs = require('fs')
      function walk(dir, base) {
        for (const entry of fs.readdirSync(dir)) {
          const full = resolve(dir, entry)
          const rel = base ? `${base}/${entry}` : entry
          if (fs.statSync(full).isDirectory()) {
            walk(full, rel)
          } else {
            manifest.files[rel] = `./${rel}`
            if (rel.endsWith('.js') && !rel.endsWith('.map')) {
              manifest.entrypoints.push(rel)
            }
          }
        }
      }
      walk(outputDir, '')
      writeFileSync(resolve(outputDir, 'asset-manifest.json'), JSON.stringify(manifest, null, 2))
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), assetManifestPlugin()],
  base: './',
  server: {
    proxy: {
      '/api': 'http://localhost:9091',
      '/store-scopes': 'http://localhost:9091',
    },
  },
})
