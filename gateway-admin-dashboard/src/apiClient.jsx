// src/apiClient.js
import axios from 'axios';

const apiClient = axios.create({
  baseURL: '/api', // Will use Vite's proxy configuration
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add request interceptor to include token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Add response interceptor for token expiration
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      // Handle token expiration
      if (error.response.status === 401) {
        // Token expired, log out user
        localStorage.removeItem('token');
        localStorage.removeItem('userData');
        window.location.href = '/login';
      }
      
      // Handle server validation errors
      if (error.response.status === 422) {
        return Promise.reject({
          ...error,
          isValidationError: true,
          validationErrors: error.response.data.errors || {}
        });
      }
    }
    return Promise.reject(error);
  }
);

// Add user-related API endpoints
apiClient.user = {
  getProfile: () => apiClient.get('/user/profile'),
  updateProfile: (data) => apiClient.put('/user/profile', data),
  updatePassword: (data) => apiClient.put('/user/password', data),
  updateSecurity: (data) => apiClient.put('/user/security', data),
  uploadAvatar: (formData) => apiClient.post('/user/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
};

export default apiClient;