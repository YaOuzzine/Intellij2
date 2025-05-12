// src/services/api.js
import apiClient from '../apiClient';

export { apiClient };

// Define all API endpoints here
export const api = {
  // Auth
  login: (credentials) => apiClient.post('/auth/login', credentials),
  
  // Gateway Routes
  getRoutes: () => apiClient.get('/gateway-routes'),
  getRouteById: (id) => apiClient.get(`/gateway-routes/${id}`),
  createRoute: (route) => apiClient.post('/gateway-routes', route),
  updateRoute: (id, route) => apiClient.put(`/gateway-routes/${id}`, route),
  deleteRoute: (id) => apiClient.delete(`/gateway-routes/${id}`),
  
  // IP Management
  getIps: () => apiClient.get('/ip-addresses'),
  getIpById: (id) => apiClient.get(`/ip-addresses/${id}`),
  createIp: (ip) => apiClient.post('/ip-addresses', ip),
  updateIp: (id, ip) => apiClient.put(`/ip-addresses/${id}`, ip),
  deleteIp: (id, routeId) => apiClient.delete(`/ip-addresses/${id}/gateway/${routeId}`),
  
  // Rate Limits
  getRateLimits: () => apiClient.get('/rate-limit'),
  getRateLimitById: (id) => apiClient.get(`/rate-limit/${id}`),
  createRateLimit: (rateLimit) => apiClient.post('/rate-limit', rateLimit),
  updateRateLimit: (id, rateLimit) => apiClient.put(`/rate-limit/${id}`, rateLimit),
  deleteRateLimit: (id) => apiClient.delete(`/rate-limit/${id}`)
};