import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';

import fs from 'fs';
import os from 'os';

function localFileStoragePlugin() {
  return {
    name: 'edumind-local-storage-plugin',
    configureServer(server: any) {
      server.middlewares.use((req: any, res: any, next: any) => {
        if (req.url && req.url.startsWith('/api/storage/files/')) {
          const urlWithoutQuery = req.url.split('?')[0];
          const relativePath = decodeURIComponent(urlWithoutQuery.replace(/^\/api\/storage\/files\//, ''));
          const possiblePaths = [
            path.resolve(__dirname, '../backend/data', relativePath),
            path.join('C:/temp/edumind-storage', relativePath),
            path.join(os.tmpdir(), 'edumind-storage', relativePath)
          ];
          for (const targetPath of possiblePaths) {
            if (fs.existsSync(targetPath) && fs.statSync(targetPath).isFile()) {
              const ext = path.extname(targetPath).toLowerCase();
              const mimeTypes: Record<string, string> = {
                '.jpg': 'image/jpeg',
                '.jpeg': 'image/jpeg',
                '.png': 'image/png',
                '.gif': 'image/gif',
                '.webp': 'image/webp',
                '.svg': 'image/svg+xml'
              };
              res.setHeader('Content-Type', mimeTypes[ext] || 'application/octet-stream');
              res.setHeader('Cache-Control', 'public, max-age=86400');
              res.setHeader('Access-Control-Allow-Origin', '*');
              return fs.createReadStream(targetPath).pipe(res);
            }
          }
        }
        next();
      });
    }
  };
}

export default defineConfig({
  plugins: [vue(), localFileStoragePlugin()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/styles/variables.scss" as *; @use "@/styles/mixins.scss" as *;`
      }
    }
  },
  server: {
    port: 3000,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});
