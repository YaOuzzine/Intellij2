import axios from 'axios';

// Create a more verbose debug version of the client
const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
});

// Add request interceptor with detailed logging
apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            // Make sure the Bearer prefix is correctly formatted with a space
            config.headers.Authorization = `Bearer ${token}`;
            console.log('Request with token:', config.url, config.headers.Authorization.substring(0, 15) + '...');
        } else {
            console.log('Request without token:', config.url);
        }
        return config;
    },
    (error) => {
        console.error('Request error:', error);
        return Promise.reject(error);
    }
);

// Modified response interceptor with better error handling
apiClient.interceptors.response.use(
    (response) => {
        console.log('Response successful:', response.config.url);
        return response;
    },
    async (error) => {
        const originalRequest = error.config;

        // Log detailed error information
        console.error('Response error:', originalRequest?.url, error.response?.status, error.response?.data);

        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            console.warn('Authentication error. Status:', error.response?.status);
            if (error.response?.data) {
                console.warn('Error response data:', error.response.data);
            }

            // Only clear token/redirect for certain 401 errors (not login attempts)
            if (originalRequest.url !== '/auth/login') {
                console.warn('Token expired or invalid, logging out.');
                localStorage.removeItem('token');
                localStorage.removeItem('userData');

                // Only redirect if not already on login page
                if (window.location.pathname !== '/login') {
                    window.location.href = '/login';
                }
            }
        }
        return Promise.reject(error);
    }
);

export default apiClient;