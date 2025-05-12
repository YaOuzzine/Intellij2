// src/services/rateLimitService.js
import apiClient from '../apiClient';

export const fetchRateLimits = async () => {
  try {
    const response = await apiClient.get('/rate-limit');
    return response.data;
  } catch (error) {
    console.error('Error fetching rate limits:', error);
    return [];
  }
};

export const addRateLimit = async (rateLimitData) => {
  const response = await apiClient.post('/rate-limit', rateLimitData);
  return response.data;
};

export const updateRateLimit = async (id, updatedData) => {
  const response = await apiClient.put(`/rate-limit/${id}`, updatedData);
  return response.data;
};

export const deleteRateLimit = async (id) => {
  const response = await apiClient.delete(`/rate-limit/${id}`);
  return response.data;
};