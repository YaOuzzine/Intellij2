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
        secure: false,
        configure: (proxy, _options) => {
          proxy.on('error', (err, _req, _res) => {
            console.log('proxy error', err);
          });
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            console.log('Sending Request:', req.method, req.url);
          });
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            console.log('Received Response:', proxyRes.statusCode, req.url);
          });
        }
      },
      // Add this block to route metrics requests to the gateway (demo 2)
      '/api/metrics': {
        target: 'http://localhost:9080', // Gateway (demo 2) port
        changeOrigin: true,
        secure: false,
        configure: (proxy, _options) => {
          proxy.on('error', (err, _req, _res) => {
            console.log('proxy error', err);
          });
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            console.log('Sending Metrics Request:', req.method, req.url);
          });
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            console.log('Received Metrics Response:', proxyRes.statusCode, req.url);
          });
        }
      },
      // Proxy other /api/** requests (user profile, routes, etc.) to the admin service (gateway-admin)
      '/api': {
        target: 'http://localhost:8081', // Admin service (gateway-admin) port
        changeOrigin: true,
        secure: false,
        configure: (proxy, _options) => {
          proxy.on('error', (err, _req, _res) => {
            console.log('proxy error', err);
          });
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            console.log('Sending Request to admin:', req.method, req.url);
            // Log headers being sent to the backend
            console.log('Headers:', JSON.stringify(req.headers));
          });
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            console.log('Received Response from admin:', proxyRes.statusCode, req.url);
          });
        }
      },
    },
  },
});