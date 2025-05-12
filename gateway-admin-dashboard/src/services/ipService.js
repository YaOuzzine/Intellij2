// src/services/ipService.js
import apiClient from '../apiClient';

export const fetchIpAddresses = async () => {
  try {
    const response = await apiClient.get('/ip-addresses');
    return response.data;
  } catch (error) {
    console.error('Error fetching IP addresses:', error);
    throw error;
  }
};

export const fetchRoutesForIpManagement = async () => {
  try {
    const response = await apiClient.get('/ip-addresses/routes');
    return response.data;
  } catch (error) {
    console.error('Error fetching routes for IP management:', error);
    throw error;
  }
};

export const addIpAddress = async (ipData) => {
  try {
    const response = await apiClient.post('/ip-addresses', ipData);
    return response.data;
  } catch (error) {
    console.error('Error adding IP address:', error);
    throw error;
  }
};

export const updateIpAddress = async (id, updatedData) => {
  try {
    const response = await apiClient.put(`/ip-addresses/${id}`, updatedData);
    return response.data;
  } catch (error) {
    console.error('Error updating IP address:', error);
    throw error;
  }
};

export const deleteIpAddress = async (id, routeId) => {
  try {
    const response = await apiClient.delete(`/ip-addresses/${id}/gateway/${routeId}`);
    return response.data;
  } catch (error) {
    console.error('Error deleting IP address:', error);
    throw error;
  }
};

export const deleteAllIpsForRoute = async (routeId) => {
  try {
    const response = await apiClient.delete(`/ip-addresses/route/${routeId}`);
    return response.data;
  } catch (error) {
    console.error('Error deleting all IPs for route:', error);
    throw error;
  }
};