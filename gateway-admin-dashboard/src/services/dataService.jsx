// src/services/dataService.js
import apiClient from '../apiClient';

// Get all gateway routes
export const fetchGatewayRoutes = async () => {
  try {
    const response = await apiClient.get('/gateway-routes');
    return response.data;
  } catch (error) {
    console.error('Error fetching gateway routes:', error);
    return [];
  }
};

// Add a new gateway route
export const addGatewayRoute = async (routeData) => {
  const response = await apiClient.post('/gateway-routes', routeData);
  return response.data;
};

// Update a gateway route by id
export const updateGatewayRoute = async (id, updatedData) => {
  const response = await apiClient.put(`/gateway-routes/${id}`, updatedData);
  return response.data;
};

// Delete a gateway route by id
export const deleteGatewayRoute = async (id) => {
  const response = await apiClient.delete(`/gateway-routes/${id}`);
  return response.data;
};