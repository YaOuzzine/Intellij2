// gateway-admin-dashboard/vite.config.js
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { resolve } from "path";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      src: resolve(__dirname, "src"),
    },
  },
  server: {
    port: 5173, // Frontend port
    proxy: {
      // Proxy requests to /api/auth/** to the gateway (demo 2)
      '/api/auth': {
        target: 'http://localhost:9080', // Gateway (demo 2) port
        changeOrigin: true,
        // No rewrite needed if gateway endpoint is /api/auth/login
      },
      // Proxy other /api/** requests (user profile, routes, etc.) to the admin service (gateway-admin)
      '/api': {
        target: 'http://localhost:8081', // Admin service (gateway-admin) port
        changeOrigin: true,
        // No rewrite needed if admin service endpoints start with /api
      },
    },
  },
});