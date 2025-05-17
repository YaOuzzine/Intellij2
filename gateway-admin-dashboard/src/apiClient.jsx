import axios from 'axios';

// Create a more verbose debug version of the client
const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
});

// Add request interceptor with detailed logging
apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        console.log('Request URL:', config.url);
        console.log('Request method:', config.method);

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
            console.log('Token present, first 20 chars:', token.substring(0, 20) + '...');
            console.log('Full authorization header:', config.headers.Authorization);
        } else {
            console.log('No token found in localStorage');
        }
        console.log('Request headers:', JSON.stringify(config.headers));
        return config;
    },
    (error) => {
        console.error('Request error:', error);
        return Promise.reject(error);
    }
);

apiClient.interceptors.response.use(
    (response) => {
        console.log('Response successful:', response.config.url);
        console.log('Response status:', response.status);
        console.log('Response data:', response.data);
        return response;
    },
    async (error) => {
        const originalRequest = error.config;

        console.error('Response error URL:', originalRequest?.url);
        console.error('Response error status:', error.response?.status);
        console.error('Response error data:', error.response?.data);
        console.error('Full error object:', error);

        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            console.warn('Auth error details:', {
                url: originalRequest?.url,
                method: originalRequest?.method,
                status: error.response?.status,
                statusText: error.response?.statusText,
                headers: originalRequest?.headers,
                responseHeaders: error.response?.headers,
                data: error.response?.data
            });

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