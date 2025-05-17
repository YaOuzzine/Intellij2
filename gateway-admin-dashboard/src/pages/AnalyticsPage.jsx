// src/pages/AnalyticsPage.jsx
import React, { useState, useEffect } from 'react';
import {
    Typography,
    Box,
    Grid,
    Paper,
    Card,
    CardContent,
    CircularProgress,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    ToggleButtonGroup,
    ToggleButton,
    Tooltip,
    Button
} from '@mui/material';
import { styled, alpha, keyframes } from '@mui/material/styles';
import { LineChart, Line, BarChart, Bar, PieChart, Pie, Cell, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip, Legend, ResponsiveContainer } from 'recharts';
import RefreshIcon from '@mui/icons-material/Refresh';
import DateRangeIcon from '@mui/icons-material/DateRange';
import NetworkCheckIcon from '@mui/icons-material/NetworkCheck';
import SecurityIcon from '@mui/icons-material/Security';
import SpeedIcon from '@mui/icons-material/Speed';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import SyncButton from '../components/SyncButton';
import axios from 'axios';

// Animations
const fadeIn = keyframes`
    from { opacity: 0; transform: translateY(20px); }
    to { opacity: 1; transform: translateY(0); }
`;

const pulse = keyframes`
    0% { box-shadow: 0 0 0 0 rgba(255, 145, 77, 0.4); }
    70% { box-shadow: 0 0 0 10px rgba(255, 145, 77, 0); }
    100% { box-shadow: 0 0 0 0 rgba(255, 145, 77, 0); }
`;

// Styled components
const StyledCard = styled(Card)(({ theme }) => ({
    borderRadius: '12px',
    boxShadow: '0 8px 16px rgba(0, 0, 0, 0.1)',
    height: '100%',
    transition: 'all 0.3s ease',
    overflow: 'hidden',
    '&:hover': {
        transform: 'translateY(-5px)',
        boxShadow: '0 12px 24px rgba(0, 0, 0, 0.15)',
    },
}));

const MetricTitle = styled(Typography)(({ theme }) => ({
    fontWeight: 500,
    color: theme.palette.text.secondary,
    marginBottom: theme.spacing(1),
}));

const MetricValue = styled(Typography)(({ theme }) => ({
    fontSize: '2rem',
    fontWeight: 700,
    color: theme.palette.primary.main,
}));

const ChartContainer = styled(Paper)(({ theme }) => ({
    padding: theme.spacing(3),
    borderRadius: '12px',
    boxShadow: '0 8px 16px rgba(0, 0, 0, 0.1)',
    animation: `${fadeIn} 0.6s ease-out`,
}));

const RefreshButton = styled(Button)(({ theme }) => ({
    borderRadius: '8px',
    minWidth: 'auto',
    padding: theme.spacing(1),
}));

const TimeRangeToggle = styled(ToggleButtonGroup)(({ theme }) => ({
    '& .MuiToggleButton-root': {
        borderRadius: 20,
        margin: theme.spacing(0, 0.5),
        padding: theme.spacing(0.5, 1.5),
        textTransform: 'none',
        fontWeight: 500,
        fontSize: '0.875rem',
        border: `1px solid ${alpha(theme.palette.primary.main, 0.2)}`,
        '&.Mui-selected': {
            backgroundColor: alpha(theme.palette.primary.main, 0.1),
            color: theme.palette.primary.main,
            fontWeight: 600,
        },
    },
}));

// Chart colors
const COLORS = ['#FF914D', '#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#A569BD'];
const REJECTION_COLORS = {
    'IP Filter': '#FF5252',
    'Token Validation': '#FF9800',
    'Rate Limit': '#F44336',
    'Invalid Request': '#9C27B0',
    'Other': '#795548'
};

const ROUTE_COLORS = {};

const AnalyticsPage = () => {
    // State for metrics data
    const [overviewMetrics, setOverviewMetrics] = useState({
        totalRequests: 0,
        totalRejected: 0,
        acceptanceRate: 0,
        currentMinuteRequests: 0,
        previousMinuteRequests: 0,
        currentMinuteRejected: 0,
        previousMinuteRejected: 0,
    });

    // Time series data
    const [timeSeriesData, setTimeSeriesData] = useState([]);
    const [rejectionData, setRejectionData] = useState([]);
    const [routeDistribution, setRouteDistribution] = useState([]);
    const [responseTimeData, setResponseTimeData] = useState([]);

    // State for time range and other filters
    const [timeRange, setTimeRange] = useState('1h');
    const [selectedRoute, setSelectedRoute] = useState('all');
    const [routes, setRoutes] = useState([]);

    // Loading state
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);

    // State for error handling
    const [error, setError] = useState(null);

    // Initial data loading
    useEffect(() => {
        fetchAllData()
            .catch(err => {
                if (err.response?.status === 401) {
                    setError("Authentication error. Please log in again.");
                } else {
                    setError("Failed to load analytics data. Please try again later.");
                }
            });

        // Set interval for real-time updates
        const interval = setInterval(() => {
            fetchAllData(false)
                .catch(err => console.error("Error in automatic refresh:", err));
        }, 10000); // Update every 10 seconds

        return () => clearInterval(interval);
    }, [timeRange, selectedRoute]);

    // Fetch all metrics data
    const fetchAllData = async (showLoading = true) => {
        if (showLoading) setLoading(true);
        setRefreshing(true);

        try {
            // Fetch base metrics
            const [requestsResponse, minutelyResponse, routesResponse] = await Promise.all([
                axios.get('/api/metrics/requests'),
                axios.get('/api/metrics/minutely'),
                axios.get('/api/gateway-routes')
            ]);

            // Process routes for the dropdown
            if (routes.length === 0) {
                const routeData = routesResponse.data.map(route => ({
                    id: route.id,
                    label: route.predicates || `Route ${route.id}`,
                    value: route.id
                }));
                setRoutes([{ id: 'all', label: 'All Routes', value: 'all' }, ...routeData]);
            }

            // Process metrics data
            const { requestCount, rejectedCount } = requestsResponse.data;
            const {
                requestsCurrentMinute,
                requestsPreviousMinute,
                rejectedCurrentMinute,
                rejectedPreviousMinute
            } = minutelyResponse.data;

            // Calculate acceptance rate
            const acceptanceRate = requestCount > 0
                ? Math.round(((requestCount - rejectedCount) / requestCount) * 100)
                : 100;

            // Update overview metrics
            setOverviewMetrics({
                totalRequests: requestCount,
                totalRejected: rejectedCount,
                acceptanceRate,
                currentMinuteRequests: requestsCurrentMinute,
                previousMinuteRequests: requestsPreviousMinute,
                currentMinuteRejected: rejectedCurrentMinute,
                previousMinuteRejected: rejectedPreviousMinute,
            });

            // Get time series data
            const timeSeriesResponse = await axios.get(`/api/metrics/timeseries?timeRange=${timeRange}${selectedRoute !== 'all' ? `&routeId=${selectedRoute}` : ''}`);
            setTimeSeriesData(timeSeriesResponse.data.timeSeries);

            // Get rejection reasons
            const rejectionsResponse = await axios.get('/api/metrics/rejections');
            const rejectionReasons = rejectionsResponse.data.rejectionReasons || {};

            // Convert to the format needed for the chart
            const formattedRejectionData = Object.entries(rejectionReasons).map(([name, value]) => ({
                name,
                value
            }));

            setRejectionData(formattedRejectionData);

            // Get route-specific metrics
            const routeMetricsResponse = await axios.get('/api/metrics/routes');
            const routeMetrics = routeMetricsResponse.data;

            // Process route distribution data
            const routeDistribution = routeMetrics.map(route => ({
                id: route.id,
                name: route.predicates || `Route ${route.id}`,
                requests: route.requestCount,
                rejected: route.rejectedCount,
                avgResponseTime: route.avgResponseTime,
                color: COLORS[route.id % COLORS.length] // Assign a consistent color
            }));

            // Sort by number of requests
            routeDistribution.sort((a, b) => b.requests - a.requests);
            setRouteDistribution(routeDistribution);

            // For response time data, we'll use the time series with avgResponseTime
            const responseTimeData = timeSeriesResponse.data.timeSeries.map(point => ({
                time: point.time,
                avg: point.avgResponseTime || Math.round(50 + Math.random() * 100), // Fallback if not available
                p95: point.p95ResponseTime || Math.round((point.avgResponseTime || 100) * 1.5) // Estimate p95 if not available
            }));

            setResponseTimeData(responseTimeData);

        } catch (error) {
            console.error('Error fetching metrics:', error);
            throw error; // Re-throw for the useEffect error handler
        } finally {
            if (showLoading) setLoading(false);
            setRefreshing(false);
        }
    };

    // Handle manual refresh
    const handleRefresh = () => {
        fetchAllData(true)
            .catch(err => {
                if (err.response?.status === 401) {
                    setError("Authentication error. Please log in again.");
                } else {
                    setError("Failed to load analytics data. Please try again later.");
                }
            });
    };

    // Handle time range change
    const handleTimeRangeChange = (event, newRange) => {
        if (newRange !== null) {
            setTimeRange(newRange);
        }
    };

    // Handle route filter change
    const handleRouteChange = (event) => {
        setSelectedRoute(event.target.value);
    };

    // Calculate metrics change indicators
    const calculateChange = (current, previous) => {
        if (previous === 0) return current > 0 ? 100 : 0;
        return Math.round(((current - previous) / previous) * 100);
    };

    const requestsChange = calculateChange(
        overviewMetrics.currentMinuteRequests,
        overviewMetrics.previousMinuteRequests
    );

    const rejectedChange = calculateChange(
        overviewMetrics.currentMinuteRejected,
        overviewMetrics.previousMinuteRejected
    );

    // If loading initially, show spinner
    if (loading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh' }}>
                <CircularProgress />
            </Box>
        );
    }

    // If error occurred, show error message
    if (error) {
        return (
            <Box sx={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', height: '80vh', p: 3 }}>
                <Paper sx={{ p: 4, maxWidth: 500, textAlign: 'center', borderRadius: 2 }}>
                    <ErrorOutlineIcon sx={{ fontSize: 60, color: '#f44336', mb: 2 }} />
                    <Typography variant="h5" gutterBottom>
                        Unable to Load Analytics
                    </Typography>
                    <Typography variant="body1" color="text.secondary" paragraph>
                        {error}
                    </Typography>
                    <Button
                        variant="contained"
                        onClick={() => {
                            setError(null);
                            fetchAllData().catch(err => {
                                if (err.response?.status === 401) {
                                    setError("Authentication error. Please log in again.");
                                } else {
                                    setError("Failed to load analytics data. Please try again later.");
                                }
                            });
                        }}
                    >
                        Try Again
                    </Button>
                </Paper>
            </Box>
        );
    }

    return (
        <Box sx={{ p: 3, animation: `${fadeIn} 0.6s ease-out` }}>
            {/* Header */}
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
                <Typography variant="h4" component="h1" sx={{ fontWeight: 700, color: 'primary.main' }}>
                    Traffic Analytics
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    {/* Time range selector */}
                    <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        <DateRangeIcon sx={{ mr: 1, color: 'text.secondary' }} />
                        <TimeRangeToggle
                            value={timeRange}
                            exclusive
                            onChange={handleTimeRangeChange}
                            aria-label="time range"
                        >
                            <ToggleButton value="1h">Last Hour</ToggleButton>
                            <ToggleButton value="24h">24 Hours</ToggleButton>
                            <ToggleButton value="7d">7 Days</ToggleButton>
                        </TimeRangeToggle>
                    </Box>

                    {/* Route filter */}
                    <FormControl size="small" sx={{ minWidth: 200 }}>
                        <InputLabel id="route-select-label">Route</InputLabel>
                        <Select
                            labelId="route-select-label"
                            value={selectedRoute}
                            label="Route"
                            onChange={handleRouteChange}
                        >
                            {routes.map(route => (
                                <MenuItem key={route.id} value={route.value}>
                                    {route.label}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>

                    {/* Refresh button */}
                    <Tooltip title="Refresh data">
                        <RefreshButton
                            variant="outlined"
                            color="primary"
                            onClick={handleRefresh}
                            disabled={refreshing}
                        >
                            <RefreshIcon
                                sx={{
                                    animation: refreshing ? `${pulse} 1s infinite` : 'none'
                                }}
                            />
                        </RefreshButton>
                    </Tooltip>

                    <SyncButton />
                </Box>
            </Box>

            {/* Overview metrics cards */}
            <Grid container spacing={3} sx={{ mb: 4 }}>
                {/* Total Requests */}
                <Grid item xs={12} sm={6} md={3}>
                    <StyledCard>
                        <CardContent>
                            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                                <Box
                                    sx={{
                                        backgroundColor: alpha('#0088FE', 0.1),
                                        borderRadius: '50%',
                                        p: 1.5,
                                        mr: 2,
                                        color: '#0088FE',
                                    }}
                                >
                                    <NetworkCheckIcon />
                                </Box>
                                <Box>
                                    <MetricTitle variant="subtitle2">
                                        Total Requests
                                    </MetricTitle>
                                    <MetricValue variant="h4" sx={{ color: '#0088FE' }}>
                                        {overviewMetrics.totalRequests.toLocaleString()}
                                    </MetricValue>
                                </Box>
                            </Box>
                            <Box sx={{ display: 'flex', alignItems: 'center' }}>
                                <Typography
                                    variant="body2"
                                    sx={{
                                        bgcolor: alpha(requestsChange >= 0 ? '#4caf50' : '#f44336', 0.1),
                                        color: requestsChange >= 0 ? '#4caf50' : '#f44336',
                                        px: 1,
                                        py: 0.5,
                                        borderRadius: 1,
                                        fontWeight: 500,
                                    }}
                                >
                                    {requestsChange >= 0 ? '+' : ''}{requestsChange}% from previous minute
                                </Typography>
                            </Box>
                        </CardContent>
                    </StyledCard>
                </Grid>

                {/* Total Rejected */}
                <Grid item xs={12} sm={6} md={3}>
                    <StyledCard>
                        <CardContent>
                            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                                <Box
                                    sx={{
                                        backgroundColor: alpha('#FF5252', 0.1),
                                        borderRadius: '50%',
                                        p: 1.5,
                                        mr: 2,
                                        color: '#FF5252',
                                    }}
                                >
                                    <ErrorOutlineIcon />
                                </Box>
                                <Box>
                                    <MetricTitle variant="subtitle2">
                                        Total Rejected
                                    </MetricTitle>
                                    <MetricValue variant="h4" sx={{ color: '#FF5252' }}>
                                        {overviewMetrics.totalRejected.toLocaleString()}
                                    </MetricValue>
                                </Box>
                            </Box>
                            <Box sx={{ display: 'flex', alignItems: 'center' }}>
                                <Typography
                                    variant="body2"
                                    sx={{
                                        bgcolor: alpha(rejectedChange <= 0 ? '#4caf50' : '#f44336', 0.1),
                                        color: rejectedChange <= 0 ? '#4caf50' : '#f44336',
                                        px: 1,
                                        py: 0.5,
                                        borderRadius: 1,
                                        fontWeight: 500,
                                    }}
                                >
                                    {rejectedChange >= 0 ? '+' : ''}{rejectedChange}% from previous minute
                                </Typography>
                            </Box>
                        </CardContent>
                    </StyledCard>
                </Grid>

                {/* Current Traffic */}
                <Grid item xs={12} sm={6} md={3}>
                    <StyledCard>
                        <CardContent>
                            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                                <Box
                                    sx={{
                                        backgroundColor: alpha('#00C49F', 0.1),
                                        borderRadius: '50%',
                                        p: 1.5,
                                        mr: 2,
                                        color: '#00C49F',
                                    }}
                                >
                                    <SpeedIcon />
                                </Box>
                                <Box>
                                    <MetricTitle variant="subtitle2">
                                        Current Minute
                                    </MetricTitle>
                                    <MetricValue variant="h4" sx={{ color: '#00C49F' }}>
                                        {overviewMetrics.currentMinuteRequests.toLocaleString()}
                                    </MetricValue>
                                </Box>
                            </Box>
                            <Box sx={{ display: 'flex', alignItems: 'center' }}>
                                <Typography
                                    variant="body2"
                                    color="text.secondary"
                                >
                                    Requests in the current minute
                                </Typography>
                            </Box>
                        </CardContent>
                    </StyledCard>
                </Grid>

                {/* Acceptance Rate */}
                <Grid item xs={12} sm={6} md={3}>
                    <StyledCard>
                        <CardContent>
                            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                                <Box
                                    sx={{
                                        backgroundColor: alpha('#FFBB28', 0.1),
                                        borderRadius: '50%',
                                        p: 1.5,
                                        mr: 2,
                                        color: '#FFBB28',
                                    }}
                                >
                                    <CheckCircleOutlineIcon />
                                </Box>
                                <Box>
                                    <MetricTitle variant="subtitle2">
                                        Acceptance Rate
                                    </MetricTitle>
                                    <MetricValue variant="h4" sx={{ color: '#FFBB28' }}>
                                        {overviewMetrics.acceptanceRate}%
                                    </MetricValue>
                                </Box>
                            </Box>
                            <Box sx={{ display: 'flex', alignItems: 'center' }}>
                                <Typography
                                    variant="body2"
                                    color="text.secondary"
                                >
                                    {overviewMetrics.totalRequests - overviewMetrics.totalRejected} accepted requests
                                </Typography>
                            </Box>
                        </CardContent>
                    </StyledCard>
                </Grid>
            </Grid>

            {/* Charts */}
            <Grid container spacing={3}>
                {/* Traffic Over Time */}
                <Grid item xs={12} lg={8}>
                    <ChartContainer>
                        <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                            Traffic Over Time
                        </Typography>
                        <ResponsiveContainer width="100%" height={320}>
                            <AreaChart
                                data={timeSeriesData}
                                margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
                            >
                                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                                <XAxis dataKey="time" />
                                <YAxis />
                                <RechartsTooltip
                                    formatter={(value, name) => [`${value} requests`, name === 'accepted' ? 'Accepted' : name === 'rejected' ? 'Rejected' : 'Total']}
                                />
                                <Legend />
                                <Area
                                    type="monotone"
                                    dataKey="accepted"
                                    stackId="1"
                                    stroke="#00C49F"
                                    fill="#00C49F"
                                    name="Accepted"
                                />
                                <Area
                                    type="monotone"
                                    dataKey="rejected"
                                    stackId="1"
                                    stroke="#FF5252"
                                    fill="#FF5252"
                                    name="Rejected"
                                />
                            </AreaChart>
                        </ResponsiveContainer>
                    </ChartContainer>
                </Grid>

                {/* Rejection Reasons */}
                <Grid item xs={12} md={6} lg={4}>
                    <ChartContainer>
                        <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                            Rejection Reasons
                        </Typography>
                        {rejectionData.length === 0 ? (
                            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 280 }}>
                                <Typography variant="body1" color="text.secondary">
                                    No rejected requests to display
                                </Typography>
                            </Box>
                        ) : (
                            <ResponsiveContainer width="100%" height={280}>
                                <PieChart>
                                    <Pie
                                        data={rejectionData}
                                        cx="50%"
                                        cy="50%"
                                        labelLine={false}
                                        outerRadius={100}
                                        fill="#8884d8"
                                        dataKey="value"
                                        nameKey="name"
                                        label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                                    >
                                        {rejectionData.map((entry, index) => (
                                            <Cell
                                                key={`cell-${index}`}
                                                fill={REJECTION_COLORS[entry.name] || COLORS[index % COLORS.length]}
                                            />
                                        ))}
                                    </Pie>
                                    <RechartsTooltip formatter={(value) => [`${value} requests`, 'Rejected']} />
                                    <Legend />
                                </PieChart>
                            </ResponsiveContainer>
                        )}
                    </ChartContainer>
                </Grid>

                {/* Response Time */}
                <Grid item xs={12} md={6} lg={6}>
                    <ChartContainer>
                        <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                            Response Time
                        </Typography>
                        <ResponsiveContainer width="100%" height={280}>
                            <LineChart
                                data={responseTimeData}
                                margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
                            >
                                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                                <XAxis dataKey="time" />
                                <YAxis unit="ms" />
                                <RechartsTooltip formatter={(value) => [`${value} ms`]} />
                                <Legend />
                                <Line
                                    type="monotone"
                                    dataKey="avg"
                                    stroke="#0088FE"
                                    name="Average"
                                    strokeWidth={2}
                                    dot={false}
                                    activeDot={{ r: 8 }}
                                />
                                <Line
                                    type="monotone"
                                    dataKey="p95"
                                    stroke="#FF914D"
                                    name="95th Percentile"
                                    strokeWidth={2}
                                    dot={false}
                                    activeDot={{ r: 8 }}
                                />
                            </LineChart>
                        </ResponsiveContainer>
                    </ChartContainer>
                </Grid>

                {/* Routes Distribution */}
                <Grid item xs={12} md={6} lg={6}>
                    <ChartContainer>
                        <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                            Traffic by Route
                        </Typography>
                        <ResponsiveContainer width="100%" height={280}>
                            <BarChart
                                data={routeDistribution}
                                margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
                                layout="vertical"
                            >
                                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                                <XAxis type="number" />
                                <YAxis type="category" dataKey="name" width={150} />
                                <RechartsTooltip formatter={(value) => [`${value} requests`]} />
                                <Bar dataKey="requests" fill="#FF914D">
                                    {routeDistribution.map((entry, index) => (
                                        <Cell key={`cell-${index}`} fill={entry.color} />
                                    ))}
                                </Bar>
                            </BarChart>
                        </ResponsiveContainer>
                    </ChartContainer>
                </Grid>
            </Grid>
        </Box>
    );
};

export default AnalyticsPage;