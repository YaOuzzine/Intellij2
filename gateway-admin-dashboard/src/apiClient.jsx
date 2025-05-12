// gateway-admin-dashboard/src/apiClient.jsx
import axios from 'axios';

const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api', // Use Vite proxy
});

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

apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            // Here you could implement token refresh logic if you have a refresh token
            // For now, we'll just log out
            console.warn('Token expired or invalid, logging out.');
            localStorage.removeItem('token');
            localStorage.removeItem('userData');
            // Redirect to login page, ensuring it's done outside of React's render cycle
            // if not already on login page
            if (window.location.pathname !== '/login') {
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);

export default apiClient;