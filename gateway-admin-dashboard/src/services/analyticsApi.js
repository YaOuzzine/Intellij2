import axios from 'axios';

// Create dedicated analytics API client
const analyticsClient = axios.create({
    baseURL: '/api',  // Will be routed correctly by proxy
    timeout: 30000,   // 30 second timeout for analytics
});

// Add request interceptor
analyticsClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        console.log('[Analytics API] Request:', config.method?.toUpperCase(), config.url);
        return config;
    },
    (error) => {
        console.error('[Analytics API] Request error:', error);
        return Promise.reject(error);
    }
);

// Add response interceptor
analyticsClient.interceptors.response.use(
    (response) => {
        console.log('[Analytics API] Response:', response.status, response.config.url);
        return response;
    },
    (error) => {
        console.error('[Analytics API] Response error:', error.response?.status, error.config?.url, error.message);

        if (error.response?.status === 401) {
            console.warn('[Analytics API] Authentication error');
            // Handle auth error
        }

        return Promise.reject(error);
    }
);

// Analytics API methods
export const analyticsApi = {
    // Core dashboard data
    getDashboard: () => analyticsClient.get('/analytics/dashboard'),

    // AI insights
    getSecurityInsights: () => analyticsClient.get('/analytics/ai/security-insights'),
    getThreatPredictions: (hoursAhead = 6) => analyticsClient.get('/analytics/ai/threat-predictions', {
        params: { hoursAhead }
    }),
    getBehavioralAnalysis: (identifier, type = 'ip') => analyticsClient.get('/analytics/ai/behavioral-analysis', {
        params: { identifier, type }
    }),

    // Alerts
    getActiveAlerts: () => analyticsClient.get('/analytics/alerts/active'),
    getAlertStatistics: () => analyticsClient.get('/analytics/alerts/statistics'),
    updateAlertStatus: (alertId, status, notes) => analyticsClient.put(`/analytics/alerts/${alertId}/status`, null, {
        params: { status, notes }
    }),

    // Threat intelligence
    getRealTimeThreatIntel: () => analyticsClient.get('/analytics/threat-intelligence/realtime'),
    getThreatLandscape: () => analyticsClient.get('/analytics/threat-analysis/landscape'),
    getSecurityRecommendations: () => analyticsClient.get('/analytics/recommendations/security'),

    // Compliance
    getComplianceDashboard: () => analyticsClient.get('/analytics/compliance/dashboard'),
    getComplianceReport: (framework, startDate, endDate) => analyticsClient.get(`/analytics/compliance/report/${framework}`, {
        params: { startDate: startDate.toISOString(), endDate: endDate.toISOString() }
    }),

    // Metrics and time series
    getTimeSeries: (timeRange = '24h', routeId = null) => analyticsClient.get('/metrics/timeseries', {
        params: { timeRange, routeId }
    }),
    getMetricsDashboard: () => analyticsClient.get('/metrics/dashboard'),

    // Route specific
    getRouteAnalytics: (routeId) => analyticsClient.get(`/analytics/routes/${routeId}`),

    // Health and config
    getAnalyticsHealth: () => analyticsClient.get('/analytics/health'),
    getAnalyticsConfig: () => analyticsClient.get('/analytics/config'),
};

export default analyticsApi;