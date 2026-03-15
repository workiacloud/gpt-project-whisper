import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    extensions: ['.js', '.jsx']
  },
  esbuild: {
    loader: 'jsx',
    include: /src\/.*\.jsx$/,
    exclude: []
  },
  server: {
    port: 3000,
    open: true,
    watch: {
      usePolling: true,
      interval: 100 // You can adjust this polling interval (in milliseconds)
    },
    hmr: {
      overlay: true
    }
  }
})
