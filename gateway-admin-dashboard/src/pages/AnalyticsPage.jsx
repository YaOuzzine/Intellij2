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
    Button,
    Tabs,
    Tab,
    Divider,
    useTheme,
    useMediaQuery
} from '@mui/material';
import { styled, alpha, keyframes } from '@mui/material/styles';
import {
    LineChart,
    Line,
    BarChart,
    Bar,
    PieChart,
    Pie,
    Cell,
    AreaChart,
    Area,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip as RechartsTooltip,
    Legend,
    ResponsiveContainer
} from 'recharts';
import RefreshIcon from '@mui/icons-material/Refresh';
import DateRangeIcon from '@mui/icons-material/DateRange';
import NetworkCheckIcon from '@mui/icons-material/NetworkCheck';
import SecurityIcon from '@mui/icons-material/Security';
import SpeedIcon from '@mui/icons-material/Speed';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import InsightsIcon from '@mui/icons-material/Insights';
import TimelineIcon from '@mui/icons-material/Timeline';
import PieChartIcon from '@mui/icons-material/PieChart';
import BarChartIcon from '@mui/icons-material/BarChart';
import SyncButton from '../components/SyncButton';
import { apiClient } from '../services/api';

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
    borderRadius: '16px',
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
    borderRadius: '16px',
    boxShadow: '0 8px 16px rgba(0, 0, 0, 0.1)',
    animation: `${fadeIn} 0.6s ease-out`,
    minHeight: '500px',
    display: 'flex',
    flexDirection: 'column',
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

const StyledTabs = styled(Tabs)(({ theme }) => ({
    marginBottom: theme.spacing(3),
    '& .MuiTab-root': {
        minWidth: 'auto',
        padding: theme.spacing(1.5, 2),
        textTransform: 'none',
        fontWeight: 500,
        borderRadius: '8px 8px 0 0',
        transition: 'all 0.2s ease',
        '&.Mui-selected': {
            fontWeight: 700,
            color: theme.palette.primary.main,
        },
    },
    '& .MuiTabs-indicator': {
        height: 3,
        borderRadius: '3px 3px 0 0',
    },
}));

// Chart colors
const COLORS = ['#FF914D', '#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#A569BD'];
const REJECTION_COLORS = {
    'IP Filter': '#9528eb',
    'Token Validation': '#FF9800',
    'Rate Limit': '#F44336',
    'Invalid Request': '#9C27B0',
    'Other': '#795548'
};

// Tab panel component
function TabPanel(props) {
    const { children, value, index, ...other } = props;

    return (
        <div
            role="tabpanel"
            hidden={value !== index}
            id={`analytics-tabpanel-${index}`}
            aria-labelledby={`analytics-tab-${index}`}
            style={{ height: '100%', width: '100%' }}
            {...other}
        >
            {value === index && (
                <Box sx={{ height: '100%', pt: 2 }}>
                    {children}
                </Box>
            )}
        </div>
    );
}

const AnalyticsPage = () => {
    const theme = useTheme();
    const isMobile = useMediaQuery(theme.breakpoints.down('md'));
    const isTablet = useMediaQuery(theme.breakpoints.down('lg'));

    // State for tab selection
    const [tabValue, setTabValue] = useState(0);

    // State for all route data - this will always contain data for all routes
    const [allRouteData, setAllRouteData] = useState({});

    // State for aggregated/filtered data
    const [displayMetrics, setDisplayMetrics] = useState({
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

    // Handle tab change
    const handleChangeTab = (event, newValue) => {
        setTabValue(newValue);
    };

    // Initial data loading
    useEffect(() => {
        fetchAllData()
            .catch(err => {
                console.error("Error fetching data:", err);
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
    }, [timeRange]); // Remove selectedRoute dependency

    // Process data when selectedRoute changes
    useEffect(() => {
        processDataForSelectedRoute();
    }, [selectedRoute, allRouteData]);

    // Fetch all data for all routes
    const fetchAllData = async (showLoading = true) => {
        if (showLoading) setLoading(true);
        setRefreshing(true);

        try {
            console.log("[Analytics] Fetching all route data...");

            // Always fetch data for all routes (no filtering on backend)
            const [routesResponse] = await Promise.all([
                apiClient.get('/gateway-routes') // Get route definitions
            ]);

            // Process routes for the dropdown
            const fetchedAdminRoutes = routesResponse.data;
            const routeDataForDropdown = fetchedAdminRoutes.map(adminRoute => {
                const analyticsServiceRouteId = adminRoute.routeId && adminRoute.routeId.trim() !== ''
                    ? adminRoute.routeId
                    : `route-${adminRoute.id}`;
                return {
                    id: adminRoute.id,
                    label: adminRoute.predicates || analyticsServiceRouteId,
                    value: analyticsServiceRouteId,
                    adminRoute: adminRoute
                };
            });
            setRoutes([{ id: 'all', label: 'All Routes', value: 'all' }, ...routeDataForDropdown]);
            console.log("[Analytics] Routes loaded:", routeDataForDropdown);

            // Fetch metrics for each individual route + global metrics
            const routeMetricsPromises = [
                // Global metrics (no routeId parameter)
                {
                    routeId: 'all',
                    requests: apiClient.get('/metrics/requests'),
                    minutely: apiClient.get('/metrics/minutely'),
                    rejections: apiClient.get('/metrics/rejections'),
                    timeseries: apiClient.get(`/metrics/timeseries?timeRange=${timeRange}`)
                },
                // Individual route metrics
                ...routeDataForDropdown.map(route => ({
                    routeId: route.value,
                    requests: apiClient.get(`/metrics/requests?routeId=${route.value}`),
                    minutely: apiClient.get(`/metrics/minutely?routeId=${route.value}`),
                    rejections: apiClient.get(`/metrics/rejections?routeId=${route.value}`),
                    timeseries: apiClient.get(`/metrics/timeseries?timeRange=${timeRange}&routeId=${route.value}`)
                }))
            ];

            console.log("[Analytics] Fetching metrics for", routeMetricsPromises.length, "routes/global");

            // Fetch all metrics in parallel
            const allMetrics = await Promise.all(
                routeMetricsPromises.map(async (routePromises) => {
                    try {
                        const [requests, minutely, rejections, timeseries] = await Promise.all([
                            routePromises.requests,
                            routePromises.minutely,
                            routePromises.rejections,
                            routePromises.timeseries
                        ]);

                        return {
                            routeId: routePromises.routeId,
                            requests: requests.data,
                            minutely: minutely.data,
                            rejections: rejections.data,
                            timeseries: timeseries.data.timeSeries
                        };
                    } catch (error) {
                        console.error(`[Analytics] Error fetching data for route ${routePromises.routeId}:`, error);
                        return {
                            routeId: routePromises.routeId,
                            requests: { requestCount: 0, rejectedCount: 0 },
                            minutely: { requestsCurrentMinute: 0, requestsPreviousMinute: 0, rejectedCurrentMinute: 0, rejectedPreviousMinute: 0 },
                            rejections: { rejectionReasons: {} },
                            timeseries: []
                        };
                    }
                })
            );

            // Convert to route data object
            const routeDataMap = {};
            allMetrics.forEach(routeData => {
                routeDataMap[routeData.routeId] = routeData;
                console.log(`[Analytics] Route ${routeData.routeId}:`, {
                    requests: routeData.requests.requestCount,
                    rejected: routeData.requests.rejectedCount,
                    timeseriesPoints: routeData.timeseries.length
                });
            });

            setAllRouteData(routeDataMap);
            console.log("[Analytics] All route data updated:", Object.keys(routeDataMap));

        } catch (error) {
            console.error('[Analytics] Error fetching metrics:', error);
            throw error;
        } finally {
            if (showLoading) setLoading(false);
            setRefreshing(false);
        }
    };

    // Process data based on selected route
    const processDataForSelectedRoute = () => {
        if (!allRouteData || Object.keys(allRouteData).length === 0) {
            console.log("[Analytics] No route data available for processing");
            return;
        }

        console.log(`[Analytics] Processing data for selected route: ${selectedRoute}`);
        console.log("[Analytics] Available route data keys:", Object.keys(allRouteData));

        if (selectedRoute === 'all') {
            // Aggregate data from all individual routes (exclude the 'all' entry to avoid double counting)
            const individualRoutes = Object.keys(allRouteData).filter(routeId => routeId !== 'all');
            console.log("[Analytics] Aggregating data from individual routes:", individualRoutes);

            if (individualRoutes.length === 0) {
                // Fallback to global data if no individual routes
                console.log("[Analytics] No individual route data, using global data");
                const globalData = allRouteData['all'];
                if (globalData) {
                    processRouteData(globalData, true);
                }
                return;
            }

            // Aggregate metrics from individual routes
            let aggregatedRequests = 0;
            let aggregatedRejected = 0;
            let aggregatedCurrentMinute = 0;
            let aggregatedPreviousMinute = 0;
            let aggregatedCurrentRejected = 0;
            let aggregatedPreviousRejected = 0;

            const allTimeSeriesData = [];
            const allRejectionReasons = {};
            const allRouteDistribution = [];

            individualRoutes.forEach(routeId => {
                const routeData = allRouteData[routeId];
                if (routeData && routeData.requests) {
                    aggregatedRequests += routeData.requests.requestCount || 0;
                    aggregatedRejected += routeData.requests.rejectedCount || 0;
                    aggregatedCurrentMinute += routeData.minutely.requestsCurrentMinute || 0;
                    aggregatedPreviousMinute += routeData.minutely.requestsPreviousMinute || 0;
                    aggregatedCurrentRejected += routeData.minutely.rejectedCurrentMinute || 0;
                    aggregatedPreviousRejected += routeData.minutely.rejectedPreviousMinute || 0;

                    // Aggregate rejection reasons
                    Object.entries(routeData.rejections.rejectionReasons || {}).forEach(([reason, count]) => {
                        allRejectionReasons[reason] = (allRejectionReasons[reason] || 0) + count;
                    });

                    // Add to route distribution
                    const routeInfo = routes.find(r => r.value === routeId);
                    if (routeInfo && (routeData.requests.requestCount > 0 || routeData.requests.rejectedCount > 0)) {
                        allRouteDistribution.push({
                            id: routeInfo.id,
                            name: routeInfo.label,
                            requests: routeData.requests.requestCount,
                            rejected: routeData.requests.rejectedCount,
                            avgResponseTime: 0, // Could aggregate this too if needed
                            color: COLORS[allRouteDistribution.length % COLORS.length]
                        });
                    }
                }
            });

            // Merge time series data (this is more complex - for now, use global timeseries if available)
            const globalTimeseriesData = allRouteData['all']?.timeseries || [];

            // Set aggregated data
            const acceptanceRate = aggregatedRequests > 0
                ? Math.round(((aggregatedRequests - aggregatedRejected) / aggregatedRequests) * 100)
                : 100;

            setDisplayMetrics({
                totalRequests: aggregatedRequests,
                totalRejected: aggregatedRejected,
                acceptanceRate,
                currentMinuteRequests: aggregatedCurrentMinute,
                previousMinuteRequests: aggregatedPreviousMinute,
                currentMinuteRejected: aggregatedCurrentRejected,
                previousMinuteRejected: aggregatedPreviousRejected,
            });

            setTimeSeriesData(globalTimeseriesData);
            setRejectionData(Object.entries(allRejectionReasons).map(([name, value]) => ({ name, value })));
            setRouteDistribution(allRouteDistribution.sort((a, b) => b.requests - a.requests));
            setResponseTimeData(globalTimeseriesData.map(point => ({
                time: point.time,
                avg: point.avgResponseTime || 0,
                p95: 0
            })));

            console.log("[Analytics] Aggregated data processed:", {
                totalRequests: aggregatedRequests,
                totalRejected: aggregatedRejected,
                routeCount: allRouteDistribution.length
            });

        } else {
            // Show data for specific route
            const routeData = allRouteData[selectedRoute];
            console.log(`[Analytics] Using data for specific route ${selectedRoute}:`, routeData);

            if (routeData) {
                processRouteData(routeData, false);
            } else {
                console.warn(`[Analytics] No data found for route: ${selectedRoute}`);
                // Set empty data
                setDisplayMetrics({
                    totalRequests: 0,
                    totalRejected: 0,
                    acceptanceRate: 100,
                    currentMinuteRequests: 0,
                    previousMinuteRequests: 0,
                    currentMinuteRejected: 0,
                    previousMinuteRejected: 0,
                });
                setTimeSeriesData([]);
                setRejectionData([]);
                setRouteDistribution([]);
                setResponseTimeData([]);
            }
        }
    };

    // Helper function to process individual route data
    const processRouteData = (routeData, isGlobal) => {
        const { requestCount, rejectedCount } = routeData.requests;
        const {
            requestsCurrentMinute,
            requestsPreviousMinute,
            rejectedCurrentMinute,
            rejectedPreviousMinute
        } = routeData.minutely;

        const acceptanceRate = requestCount > 0
            ? Math.round(((requestCount - rejectedCount) / requestCount) * 100)
            : 100;

        setDisplayMetrics({
            totalRequests: requestCount,
            totalRejected: rejectedCount,
            acceptanceRate,
            currentMinuteRequests: requestsCurrentMinute,
            previousMinuteRequests: requestsPreviousMinute,
            currentMinuteRejected: rejectedCurrentMinute,
            previousMinuteRejected: rejectedPreviousMinute,
        });

        setTimeSeriesData(routeData.timeseries);
        setRejectionData(Object.entries(routeData.rejections.rejectionReasons || {}).map(([name, value]) => ({ name, value })));

        // For single route, show just that route in distribution
        if (!isGlobal) {
            const routeInfo = routes.find(r => r.value === selectedRoute);
            if (routeInfo) {
                setRouteDistribution([{
                    id: routeInfo.id,
                    name: routeInfo.label,
                    requests: requestCount,
                    rejected: rejectedCount,
                    avgResponseTime: 0,
                    color: COLORS[0]
                }]);
            }
        }

        setResponseTimeData(routeData.timeseries.map(point => ({
            time: point.time,
            avg: point.avgResponseTime || 0,
            p95: 0
        })));

        console.log(`[Analytics] Processed ${isGlobal ? 'global' : 'route-specific'} data:`, {
            requests: requestCount,
            rejected: rejectedCount,
            timeseriesPoints: routeData.timeseries.length
        });
    };

    // Handle manual refresh
    const handleRefresh = () => {
        fetchAllData(true)
            .catch(err => {
                console.error("Error during refresh:", err);
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
        displayMetrics.currentMinuteRequests,
        displayMetrics.previousMinuteRequests
    );

    const rejectedChange = calculateChange(
        displayMetrics.currentMinuteRejected,
        displayMetrics.previousMinuteRejected
    );

    // Get display name for selected route
    const getSelectedRouteDisplayName = () => {
        if (selectedRoute === 'all') return 'All Routes';
        const route = routes.find(r => r.value === selectedRoute);
        return route ? route.label : selectedRoute;
    };

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
                                console.error("Error retry:", err);
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
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4, flexWrap: 'wrap', gap: 2 }}>
                <Box>
                    <Typography variant="h4" component="h1" sx={{ fontWeight: 700, color: 'primary.main' }}>
                        Traffic Analytics
                    </Typography>
                    {selectedRoute !== 'all' && (
                        <Typography variant="subtitle1" color="text.secondary" sx={{ mt: 1 }}>
                            Showing data for: <strong>{getSelectedRouteDisplayName()}</strong>
                        </Typography>
                    )}
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
                    {/* Time range selector */}
                    <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        <DateRangeIcon sx={{ mr: 1, color: 'text.secondary', display: { xs: 'none', sm: 'block' } }} />
                        <TimeRangeToggle
                            value={timeRange}
                            exclusive
                            onChange={handleTimeRangeChange}
                            aria-label="time range"
                            size={isMobile ? "small" : "medium"}
                        >
                            <ToggleButton value="1h">Last Hour</ToggleButton>
                            <ToggleButton value="24h">24 Hours</ToggleButton>
                            <ToggleButton value="7d">7 Days</ToggleButton>
                        </TimeRangeToggle>
                    </Box>

                    {/* Route filter */}
                    <FormControl size="small" sx={{ minWidth: { xs: '100%', sm: 200 } }}>
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
                                        {displayMetrics.totalRequests.toLocaleString()}
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
                                        {displayMetrics.totalRejected.toLocaleString()}
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
                                        {displayMetrics.currentMinuteRequests.toLocaleString()}
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
                                        {displayMetrics.acceptanceRate}%
                                    </MetricValue>
                                </Box>
                            </Box>
                            <Box sx={{ display: 'flex', alignItems: 'center' }}>
                                <Typography
                                    variant="body2"
                                    color="text.secondary"
                                >
                                    {displayMetrics.totalRequests - displayMetrics.totalRejected} accepted requests
                                </Typography>
                            </Box>
                        </CardContent>
                    </StyledCard>
                </Grid>
            </Grid>

            {/* Chart Tabs */}
            <ChartContainer>
                <StyledTabs
                    value={tabValue}
                    onChange={handleChangeTab}
                    variant={isMobile ? "scrollable" : "fullWidth"}
                    scrollButtons={isMobile ? "auto" : false}
                    aria-label="analytics charts tabs"
                >
                    <Tab
                        icon={<TimelineIcon />}
                        iconPosition="start"
                        label="Traffic Over Time"
                        id="analytics-tab-0"
                        aria-controls="analytics-tabpanel-0"
                    />
                    <Tab
                        icon={<PieChartIcon />}
                        iconPosition="start"
                        label="Rejection Reasons"
                        id="analytics-tab-1"
                        aria-controls="analytics-tabpanel-1"
                    />
                    <Tab
                        icon={<InsightsIcon />}
                        iconPosition="start"
                        label="Response Time"
                        id="analytics-tab-2"
                        aria-controls="analytics-tabpanel-2"
                    />
                    <Tab
                        icon={<BarChartIcon />}
                        iconPosition="start"
                        label="Traffic by Route"
                        id="analytics-tab-3"
                        aria-controls="analytics-tabpanel-3"
                    />
                </StyledTabs>

                <Divider sx={{ mb: 3 }} />

                {/* Traffic Over Time Tab Panel */}
                <TabPanel value={tabValue} index={0}>
                    <Box sx={{ height: 400 }}>
                        <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                            Traffic Over Time
                            {selectedRoute !== 'all' && (
                                <Typography component="span" variant="body2" color="text.secondary" sx={{ ml: 1 }}>
                                    - {getSelectedRouteDisplayName()}
                                </Typography>
                            )}
                        </Typography>
                        {timeSeriesData.length === 0 ? (
                            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80%' }}>
                                <Typography variant="body1" color="text.secondary">
                                    No traffic data available for the selected time range
                                </Typography>
                            </Box>
                        ) : (
                            <ResponsiveContainer width="100%" height="100%">
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
                        )}
                    </Box>
                </TabPanel>

                {/* Rejection Reasons Tab Panel */}
                <TabPanel value={tabValue} index={1}>
                    <Box sx={{ height: 400 }}>
                        <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                            Rejection Reasons
                            {selectedRoute !== 'all' && (
                                <Typography component="span" variant="body2" color="text.secondary" sx={{ ml: 1 }}>
                                    - {getSelectedRouteDisplayName()}
                                </Typography>
                            )}
                        </Typography>
                        {rejectionData.length === 0 ? (
                            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80%' }}>
                                <Typography variant="body1" color="text.secondary">
                                    No rejected requests to display
                                </Typography>
                            </Box>
                        ) : (
                            <ResponsiveContainer width="100%" height="100%">
                                <PieChart>
                                    <Pie
                                        data={rejectionData}
                                        cx="50%"
                                        cy="50%"
                                        labelLine={false}
                                        outerRadius={150}
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
                    </Box>
                </TabPanel>

                {/* Response Time Tab Panel */}
                <TabPanel value={tabValue} index={2}>
                    <Box sx={{ height: 400 }}>
                        <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                            Response Time
                            {selectedRoute !== 'all' && (
                                <Typography component="span" variant="body2" color="text.secondary" sx={{ ml: 1 }}>
                                    - {getSelectedRouteDisplayName()}
                                </Typography>
                            )}
                        </Typography>
                        {responseTimeData.every(point => point.avg === 0) ? (
                            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80%' }}>
                                <Typography variant="body1" color="text.secondary">
                                    No response time data available
                                </Typography>
                            </Box>
                        ) : (
                            <ResponsiveContainer width="100%" height="100%">
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
                                </LineChart>
                            </ResponsiveContainer>
                        )}
                    </Box>
                </TabPanel>

                {/* Traffic by Route Tab Panel */}
                <TabPanel value={tabValue} index={3}>
                    <Box sx={{ height: 400 }}>
                        <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                            Traffic by Route
                            {selectedRoute !== 'all' && (
                                <Typography component="span" variant="body2" color="text.secondary" sx={{ ml: 1 }}>
                                    - Filtered to {getSelectedRouteDisplayName()}
                                </Typography>
                            )}
                        </Typography>
                        {routeDistribution.length === 0 || routeDistribution.every(route => route.requests === 0) ? (
                            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80%' }}>
                                <Typography variant="body1" color="text.secondary">
                                    No traffic data available for routes
                                </Typography>
                            </Box>
                        ) : (
                            <ResponsiveContainer width="100%" height="100%">
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
                        )}
                    </Box>
                </TabPanel>
            </ChartContainer>
        </Box>
    );
};

export default AnalyticsPage;