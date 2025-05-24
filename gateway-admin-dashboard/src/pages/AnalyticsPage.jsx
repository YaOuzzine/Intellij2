import React, { useState, useEffect, useCallback } from 'react';
import {
    Typography,
    Box,
    Grid,
    Paper,
    Card,
    CardContent,
    CircularProgress,
    Tabs,
    Tab,
    Alert,
    AlertTitle,
    Chip,
    LinearProgress,
    IconButton,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    useTheme,
    useMediaQuery,
    List,
    ListItem,
    ListItemText,
    ListItemIcon,
    Divider,
    Stack,
    Badge
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
    ResponsiveContainer,
    RadialBarChart,
    RadialBar
} from 'recharts';

// Icons
import RefreshIcon from '@mui/icons-material/Refresh';
import SecurityIcon from '@mui/icons-material/Security';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import WarningIcon from '@mui/icons-material/Warning';
import ShieldIcon from '@mui/icons-material/Shield';
import GavelIcon from '@mui/icons-material/Gavel';
import SpeedIcon from '@mui/icons-material/Speed';
import TimelineIcon from '@mui/icons-material/Timeline';
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';

import { analyticsApi } from '../services/analyticsApi';

// Animations
const fadeIn = keyframes`
    from { opacity: 0; transform: translateY(20px); }
    to { opacity: 1; transform: translateY(0); }
`;

const pulse = keyframes`
    0%, 100% { transform: scale(1); }
    50% { transform: scale(1.05); }
`;

const aiGlow = keyframes`
    0%, 100% { box-shadow: 0 0 20px rgba(255, 145, 77, 0.3); }
    50% { box-shadow: 0 0 30px rgba(255, 145, 77, 0.6); }
`;

// Styled Components with Orange Theme
const StyledCard = styled(Card)(({ theme }) => ({
    borderRadius: '16px',
    boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
    height: '100%',
    transition: 'all 0.3s ease',
    overflow: 'hidden',
    animation: `${fadeIn} 0.6s ease-out`,
    '&:hover': {
        transform: 'translateY(-2px)',
        boxShadow: '0 12px 40px rgba(0, 0, 0, 0.15)',
    },
}));

const AICard = styled(Card)(({ theme }) => ({
    borderRadius: '16px',
    background: 'linear-gradient(135deg, rgba(255, 145, 77, 0.1) 0%, rgba(255, 193, 7, 0.1) 100%)',
    border: '2px solid rgba(255, 145, 77, 0.2)',
    animation: `${aiGlow} 3s ease-in-out infinite`,
    '&:hover': {
        animation: `${aiGlow} 1s ease-in-out infinite`,
    },
}));

const MetricCard = styled(Paper)(({ theme, color = '#FF914D' }) => ({
    padding: theme.spacing(3),
    borderRadius: '16px',
    background: `linear-gradient(135deg, ${alpha(color, 0.1)} 0%, ${alpha(color, 0.05)} 100%)`,
    border: `1px solid ${alpha(color, 0.2)}`,
    position: 'relative',
    overflow: 'hidden',
    minHeight: '120px',
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'center',
    '&::before': {
        content: '""',
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        height: '4px',
        background: color,
    },
}));

const ThreatCard = styled(Card)(({ severity }) => {
    const colors = {
        CRITICAL: '#d32f2f',
        HIGH: '#FF914D',
        MEDIUM: '#ff9800',
        LOW: '#4caf50'
    };

    return {
        borderRadius: '12px',
        borderLeft: `4px solid ${colors[severity] || colors.LOW}`,
        background: `linear-gradient(135deg, ${alpha(colors[severity] || colors.LOW, 0.05)} 0%, transparent 100%)`,
        marginBottom: '12px',
        transition: 'all 0.3s ease',
        '&:hover': {
            transform: 'translateX(5px)',
            boxShadow: `0 4px 20px ${alpha(colors[severity] || colors.LOW, 0.3)}`,
        },
    };
});

const HealthScoreCard = styled(Paper)(({ theme, score }) => {
    const getColor = (score) => {
        if (score >= 90) return '#4caf50';
        if (score >= 70) return '#FF914D';
        if (score >= 50) return '#f44336';
        return '#9c27b0';
    };

    return {
        padding: theme.spacing(3),
        borderRadius: '20px',
        background: `linear-gradient(135deg, ${alpha(getColor(score), 0.1)} 0%, ${alpha(getColor(score), 0.05)} 100%)`,
        border: `2px solid ${alpha(getColor(score), 0.3)}`,
        textAlign: 'center',
        minHeight: '180px',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
    };
});

// Orange color palette
const ORANGE_COLORS = ['#FF914D', '#FF7043', '#FF8A65', '#FFB74D', '#FFA726', '#FF9800', '#FB8C00'];
const THREAT_COLORS = {
    'CRITICAL': '#d32f2f',
    'HIGH': '#FF914D',
    'MEDIUM': '#ff9800',
    'LOW': '#4caf50'
};

// Tab Panel Component
function TabPanel(props) {
    const { children, value, index, ...other } = props;
    return (
        <div
            role="tabpanel"
            hidden={value !== index}
            id={`analytics-tabpanel-${index}`}
            aria-labelledby={`analytics-tab-${index}`}
            {...other}
        >
            {value === index && (
                <Box sx={{ py: 2 }}>
                    {children}
                </Box>
            )}
        </div>
    );
}

const AnalyticsPage = () => {
    const theme = useTheme();
    const isMobile = useMediaQuery(theme.breakpoints.down('md'));

    // State management
    const [tabValue, setTabValue] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [refreshing, setRefreshing] = useState(false);

    // Data state
    const [dashboardData, setDashboardData] = useState(null);
    const [securityInsights, setSecurityInsights] = useState(null);
    const [activeAlerts, setActiveAlerts] = useState([]);
    const [threatLandscape, setThreatLandscape] = useState(null);
    const [complianceData, setComplianceData] = useState(null);
    const [timeSeriesData, setTimeSeriesData] = useState([]);
    const [alertStatistics, setAlertStatistics] = useState(null);

    // Dialog state
    const [alertDetailDialog, setAlertDetailDialog] = useState({ open: false, alert: null });

    // Fetch all analytics data
    const fetchAnalyticsData = useCallback(async (showLoading = true) => {
        if (showLoading) setLoading(true);
        setRefreshing(true);
        setError(null);

        try {
            console.log('[AnalyticsPage] Fetching data from backend...');

            // Fetch core data in parallel
            const [
                dashboardResponse,
                securityResponse,
                alertsResponse,
                threatResponse,
                complianceResponse,
                timeseriesResponse,
                alertStatsResponse
            ] = await Promise.allSettled([
                analyticsApi.getDashboard(),
                analyticsApi.getSecurityInsights(),
                analyticsApi.getActiveAlerts(),
                analyticsApi.getThreatLandscape(),
                analyticsApi.getComplianceDashboard(),
                analyticsApi.getTimeSeries('24h'),
                analyticsApi.getAlertStatistics()
            ]);

            // Handle responses
            if (dashboardResponse.status === 'fulfilled') {
                setDashboardData(dashboardResponse.value.data);
                console.log('[AnalyticsPage] Dashboard data loaded:', dashboardResponse.value.data);
            } else {
                console.error('[AnalyticsPage] Dashboard error:', dashboardResponse.reason);
            }

            if (securityResponse.status === 'fulfilled') {
                setSecurityInsights(securityResponse.value.data);
                console.log('[AnalyticsPage] Security insights loaded');
            } else {
                console.error('[AnalyticsPage] Security insights error:', securityResponse.reason);
            }

            if (alertsResponse.status === 'fulfilled') {
                setActiveAlerts(alertsResponse.value.data || []);
                console.log('[AnalyticsPage] Alerts loaded:', alertsResponse.value.data?.length || 0);
            } else {
                console.error('[AnalyticsPage] Alerts error:', alertsResponse.reason);
            }

            if (threatResponse.status === 'fulfilled') {
                setThreatLandscape(threatResponse.value.data);
                console.log('[AnalyticsPage] Threat landscape loaded');
            } else {
                console.error('[AnalyticsPage] Threat landscape error:', threatResponse.reason);
            }

            if (complianceResponse.status === 'fulfilled') {
                setComplianceData(complianceResponse.value.data);
                console.log('[AnalyticsPage] Compliance data loaded');
            } else {
                console.error('[AnalyticsPage] Compliance error:', complianceResponse.reason);
            }

            if (timeseriesResponse.status === 'fulfilled') {
                setTimeSeriesData(timeseriesResponse.value.data?.timeSeries || []);
                console.log('[AnalyticsPage] Time series loaded');
            } else {
                console.error('[AnalyticsPage] Time series error:', timeseriesResponse.reason);
            }

            if (alertStatsResponse.status === 'fulfilled') {
                setAlertStatistics(alertStatsResponse.value.data);
                console.log('[AnalyticsPage] Alert statistics loaded');
            } else {
                console.error('[AnalyticsPage] Alert stats error:', alertStatsResponse.reason);
            }

        } catch (error) {
            console.error('[AnalyticsPage] Fetch error:', error);
            setError("Failed to load analytics data. Please check service connectivity.");
        } finally {
            if (showLoading) setLoading(false);
            setRefreshing(false);
        }
    }, []);

    // Initial load and refresh interval
    useEffect(() => {
        fetchAnalyticsData();

        const interval = setInterval(() => {
            fetchAnalyticsData(false);
        }, 30000); // Refresh every 30 seconds

        return () => clearInterval(interval);
    }, [fetchAnalyticsData]);

    // Event handlers
    const handleTabChange = (event, newValue) => {
        setTabValue(newValue);
    };

    const handleRefresh = () => {
        fetchAnalyticsData(true);
    };

    const handleAlertClick = (alert) => {
        setAlertDetailDialog({ open: true, alert });
    };

    // Helper functions
    const getSecurityHealthScore = () => {
        return dashboardData?.securityHealthScore || securityInsights?.securityHealthScore || 0;
    };

    const getThreatLevelColor = (level) => {
        return THREAT_COLORS[level] || THREAT_COLORS.LOW;
    };

    const formatTimestamp = (timestamp) => {
        if (!timestamp) return 'Unknown';
        return new Date(timestamp).toLocaleString();
    };

    // Loading state
    if (loading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh' }}>
                <Box sx={{ textAlign: 'center' }}>
                    <CircularProgress size={60} sx={{ color: '#FF914D', mb: 2 }} />
                    <Typography variant="h6" color="text.secondary">
                        Loading AI-Enhanced Analytics...
                    </Typography>
                </Box>
            </Box>
        );
    }

    // Error state
    if (error) {
        return (
            <Box sx={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', height: '80vh', p: 3 }}>
                <Paper sx={{ p: 4, maxWidth: 500, textAlign: 'center', borderRadius: 2 }}>
                    <ErrorOutlineIcon sx={{ fontSize: 60, color: '#f44336', mb: 2 }} />
                    <Typography variant="h5" gutterBottom>
                        Analytics Unavailable
                    </Typography>
                    <Typography variant="body1" color="text.secondary" paragraph>
                        {error}
                    </Typography>
                    <Button
                        variant="contained"
                        onClick={() => { setError(null); fetchAnalyticsData(); }}
                        sx={{ backgroundColor: '#FF914D' }}
                    >
                        Retry
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
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
                        <Typography variant="h4" component="h1" sx={{ fontWeight: 700, color: '#FF914D' }}>
                            Security Analytics
                        </Typography>
                        <Chip
                            icon={<AutoAwesomeIcon />}
                            label="AI-Enhanced"
                            sx={{
                                background: 'linear-gradient(45deg, rgba(255, 145, 77, 0.1), rgba(255, 193, 7, 0.1))',
                                border: '1px solid rgba(255, 145, 77, 0.3)',
                                color: '#FF914D'
                            }}
                        />
                    </Box>
                    <Typography variant="subtitle1" color="text.secondary">
                        Real-time threat detection with AI-powered insights
                    </Typography>
                </Box>

                <IconButton
                    onClick={handleRefresh}
                    disabled={refreshing}
                    sx={{
                        backgroundColor: '#FF914D',
                        color: 'white',
                        '&:hover': { backgroundColor: '#FF7043' },
                        '&:disabled': { backgroundColor: '#FFB74D' }
                    }}
                >
                    <RefreshIcon sx={{ animation: refreshing ? `${pulse} 1s infinite` : 'none' }} />
                </IconButton>
            </Box>

            {/* Key Metrics Cards */}
            <Grid container spacing={3} sx={{ mb: 4 }}>
                <Grid item xs={12} md={3}>
                    <HealthScoreCard score={getSecurityHealthScore()}>
                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', mb: 2 }}>
                            <ShieldIcon sx={{ fontSize: 40, color: '#FF914D', mr: 1 }} />
                            <Typography variant="h6" fontWeight={600}>
                                Security Health
                            </Typography>
                        </Box>
                        <Typography variant="h2" fontWeight={700} sx={{ mb: 1, color: '#FF914D' }}>
                            {getSecurityHealthScore().toFixed(0)}%
                        </Typography>
                        <LinearProgress
                            variant="determinate"
                            value={getSecurityHealthScore()}
                            sx={{
                                height: 8,
                                borderRadius: 4,
                                backgroundColor: alpha('#FF914D', 0.2),
                                '& .MuiLinearProgress-bar': { backgroundColor: '#FF914D' }
                            }}
                        />
                    </HealthScoreCard>
                </Grid>

                <Grid item xs={12} md={3}>
                    <MetricCard color="#f44336">
                        <Box sx={{ textAlign: 'center' }}>
                            <WarningIcon sx={{ fontSize: 40, color: '#f44336', mb: 1 }} />
                            <Typography variant="h6" fontWeight={600} gutterBottom>
                                Active Threats
                            </Typography>
                            <Typography variant="h3" fontWeight={700} sx={{ color: '#f44336' }}>
                                {activeAlerts?.length || 0}
                            </Typography>
                        </Box>
                    </MetricCard>
                </Grid>

                <Grid item xs={12} md={3}>
                    <MetricCard color="#FF914D">
                        <Box sx={{ textAlign: 'center' }}>
                            <TimelineIcon sx={{ fontSize: 40, color: '#FF914D', mb: 1 }} />
                            <Typography variant="h6" fontWeight={600} gutterBottom>
                                Total Requests
                            </Typography>
                            <Typography variant="h3" fontWeight={700} sx={{ color: '#FF914D' }}>
                                {dashboardData?.globalMetrics?.totalRequests || 0}
                            </Typography>
                        </Box>
                    </MetricCard>
                </Grid>

                <Grid item xs={12} md={3}>
                    <MetricCard color="#9c27b0">
                        <Box sx={{ textAlign: 'center' }}>
                            <SmartToyIcon sx={{ fontSize: 40, color: '#9c27b0', mb: 1 }} />
                            <Typography variant="h6" fontWeight={600} gutterBottom>
                                AI Confidence
                            </Typography>
                            <Typography variant="h3" fontWeight={700} sx={{ color: '#9c27b0' }}>
                                {((securityInsights?.confidenceLevel || 0.75) * 100).toFixed(0)}%
                            </Typography>
                        </Box>
                    </MetricCard>
                </Grid>
            </Grid>

            {/* Main Dashboard Tabs */}
            <StyledCard>
                <Box sx={{ p: 3 }}>
                    <Tabs
                        value={tabValue}
                        onChange={handleTabChange}
                        variant={isMobile ? "scrollable" : "fullWidth"}
                        scrollButtons={isMobile ? "auto" : false}
                        sx={{
                            mb: 3,
                            '& .MuiTab-root': {
                                minWidth: 'auto',
                                padding: theme.spacing(1.5, 2),
                                textTransform: 'none',
                                fontWeight: 500,
                                '&.Mui-selected': {
                                    fontWeight: 700,
                                    color: '#FF914D',
                                },
                            },
                            '& .MuiTabs-indicator': {
                                backgroundColor: '#FF914D',
                                height: 3,
                            }
                        }}
                    >
                        <Tab icon={<TimelineIcon />} iconPosition="start" label="Threat Overview" />
                        <Tab icon={<SmartToyIcon />} iconPosition="start" label="AI Insights" />
                        <Tab icon={<SecurityIcon />} iconPosition="start" label="Attack Analysis" />
                        <Tab icon={<GavelIcon />} iconPosition="start" label="Compliance" />
                        <Tab icon={<SpeedIcon />} iconPosition="start" label="Performance" />
                    </Tabs>

                    {/* Threat Overview Tab */}
                    <TabPanel value={tabValue} index={0}>
                        <Stack spacing={3}>
                            {/* Active Alerts Section */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 2, fontWeight: 600, color: '#FF914D' }}>
                                    Active Security Alerts
                                </Typography>
                                <Grid container spacing={2}>
                                    {activeAlerts?.slice(0, 6).map((alert, index) => (
                                        <Grid item xs={12} md={6} key={alert.id || index}>
                                            <ThreatCard
                                                severity={alert.severity}
                                                onClick={() => handleAlertClick(alert)}
                                                sx={{ cursor: 'pointer' }}
                                            >
                                                <CardContent>
                                                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                                                        <Typography variant="subtitle1" fontWeight={600}>
                                                            {alert.title}
                                                        </Typography>
                                                        <Chip
                                                            label={alert.severity}
                                                            size="small"
                                                            sx={{
                                                                backgroundColor: alpha(getThreatLevelColor(alert.severity), 0.1),
                                                                color: getThreatLevelColor(alert.severity),
                                                                fontWeight: 600
                                                            }}
                                                        />
                                                    </Box>
                                                    <Typography variant="body2" color="text.secondary" gutterBottom>
                                                        Source: {alert.sourceIp || 'Unknown'}
                                                    </Typography>
                                                    <Typography variant="body2" color="text.secondary">
                                                        {formatTimestamp(alert.firstSeen)}
                                                    </Typography>
                                                </CardContent>
                                            </ThreatCard>
                                        </Grid>
                                    ))}
                                </Grid>

                                {(!activeAlerts || activeAlerts.length === 0) && (
                                    <Paper sx={{ p: 4, textAlign: 'center', backgroundColor: alpha('#4caf50', 0.05) }}>
                                        <CheckCircleOutlineIcon sx={{ fontSize: 60, color: '#4caf50', mb: 2 }} />
                                        <Typography variant="h6" color="#4caf50" gutterBottom>
                                            No Active Threats
                                        </Typography>
                                        <Typography variant="body2" color="text.secondary">
                                            Your security posture is currently stable
                                        </Typography>
                                    </Paper>
                                )}
                            </Box>

                            {/* Global Metrics Section */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 2, fontWeight: 600, color: '#FF914D' }}>
                                    Traffic Overview
                                </Typography>
                                <Grid container spacing={2}>
                                    <Grid item xs={12} md={4}>
                                        <Paper sx={{ p: 3, textAlign: 'center' }}>
                                            <Typography variant="h4" fontWeight={700} color="#FF914D">
                                                {dashboardData?.globalMetrics?.totalRequests || 0}
                                            </Typography>
                                            <Typography variant="body1" color="text.secondary">
                                                Total Requests
                                            </Typography>
                                        </Paper>
                                    </Grid>
                                    <Grid item xs={12} md={4}>
                                        <Paper sx={{ p: 3, textAlign: 'center' }}>
                                            <Typography variant="h4" fontWeight={700} color="#f44336">
                                                {dashboardData?.globalMetrics?.totalRejections || 0}
                                            </Typography>
                                            <Typography variant="body1" color="text.secondary">
                                                Total Rejections
                                            </Typography>
                                        </Paper>
                                    </Grid>
                                    <Grid item xs={12} md={4}>
                                        <Paper sx={{ p: 3, textAlign: 'center' }}>
                                            <Typography variant="h4" fontWeight={700} color="#4caf50">
                                                {dashboardData?.globalMetrics?.currentMinuteRequests || 0}
                                            </Typography>
                                            <Typography variant="body1" color="text.secondary">
                                                Current Minute
                                            </Typography>
                                        </Paper>
                                    </Grid>
                                </Grid>
                            </Box>
                        </Stack>
                    </TabPanel>

                    {/* AI Insights Tab */}
                    <TabPanel value={tabValue} index={1}>
                        <Stack spacing={3}>
                            {/* AI Recommendations */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 2, fontWeight: 600, color: '#FF914D' }}>
                                    AI Security Recommendations
                                </Typography>
                                <Stack spacing={2}>
                                    {securityInsights?.aiRecommendations?.map((rec, index) => (
                                        <Alert
                                            key={index}
                                            severity={rec.priority === 'CRITICAL' ? 'error' :
                                                rec.priority === 'HIGH' ? 'warning' : 'info'}
                                            sx={{ '& .MuiAlert-icon': { color: '#FF914D' } }}
                                        >
                                            <AlertTitle>{rec.title}</AlertTitle>
                                            {rec.description}
                                            {rec.recommendedActions && rec.recommendedActions.length > 0 && (
                                                <Box sx={{ mt: 1 }}>
                                                    <Typography variant="body2" fontWeight={600}>
                                                        Recommended Actions:
                                                    </Typography>
                                                    {rec.recommendedActions.map((action, actionIndex) => (
                                                        <Typography key={actionIndex} variant="body2" sx={{ ml: 2 }}>
                                                            • {action}
                                                        </Typography>
                                                    ))}
                                                </Box>
                                            )}
                                        </Alert>
                                    )) || (
                                        <Alert severity="success">
                                            <AlertTitle>All Systems Optimal</AlertTitle>
                                            No immediate recommendations from AI analysis
                                        </Alert>
                                    )}
                                </Stack>
                            </Box>

                            {/* Behavioral Anomalies */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 2, fontWeight: 600, color: '#FF914D' }}>
                                    Behavioral Anomalies
                                </Typography>
                                <Grid container spacing={2}>
                                    {securityInsights?.behavioralAnomalies?.map((anomaly, index) => (
                                        <Grid item xs={12} md={6} key={index}>
                                            <AICard>
                                                <CardContent>
                                                    <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                                                        {anomaly.clientIp}
                                                    </Typography>
                                                    <LinearProgress
                                                        variant="determinate"
                                                        value={anomaly.anomalyScore * 100}
                                                        sx={{
                                                            mb: 1,
                                                            height: 8,
                                                            borderRadius: 4,
                                                            backgroundColor: alpha('#FF914D', 0.2),
                                                            '& .MuiLinearProgress-bar': { backgroundColor: '#FF914D' }
                                                        }}
                                                    />
                                                    <Typography variant="body2" color="text.secondary">
                                                        Anomaly Score: {(anomaly.anomalyScore * 100).toFixed(1)}%
                                                    </Typography>
                                                    <Typography variant="body2" color="text.secondary">
                                                        Events: {anomaly.eventCount}
                                                    </Typography>
                                                </CardContent>
                                            </AICard>
                                        </Grid>
                                    )) || (
                                        <Grid item xs={12}>
                                            <Paper sx={{ p: 3, textAlign: 'center' }}>
                                                <Typography variant="body1" color="text.secondary">
                                                    No behavioral anomalies detected
                                                </Typography>
                                            </Paper>
                                        </Grid>
                                    )}
                                </Grid>
                            </Box>

                            {/* Threat Predictions */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 2, fontWeight: 600, color: '#FF914D' }}>
                                    AI Threat Predictions
                                </Typography>
                                <Paper sx={{ p: 3 }}>
                                    {securityInsights?.threatPredictions?.predictions ? (
                                        <ResponsiveContainer width="100%" height={300}>
                                            <LineChart data={securityInsights.threatPredictions.predictions}>
                                                <CartesianGrid strokeDasharray="3 3" />
                                                <XAxis dataKey="hour" />
                                                <YAxis />
                                                <RechartsTooltip />
                                                <Legend />
                                                <Line
                                                    type="monotone"
                                                    dataKey="attackLikelihood"
                                                    stroke="#FF914D"
                                                    strokeWidth={3}
                                                    dot={{ fill: '#FF914D', strokeWidth: 2, r: 6 }}
                                                    name="Threat Likelihood"
                                                />
                                            </LineChart>
                                        </ResponsiveContainer>
                                    ) : (
                                        <Typography variant="body1" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
                                            Threat prediction data unavailable
                                        </Typography>
                                    )}
                                </Paper>
                            </Box>
                        </Stack>
                    </TabPanel>

                    {/* Attack Analysis Tab */}
                    <TabPanel value={tabValue} index={2}>
                        <Stack spacing={3}>
                            {/* Attack Pattern Distribution */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 2, fontWeight: 600, color: '#FF914D' }}>
                                    Attack Pattern Analysis
                                </Typography>
                                <Paper sx={{ p: 3 }}>
                                    {threatLandscape?.topThreatPatterns?.length > 0 ? (
                                        <ResponsiveContainer width="100%" height={350}>
                                            <BarChart data={threatLandscape.topThreatPatterns.slice(0, 10)}>
                                                <CartesianGrid strokeDasharray="3 3" />
                                                <XAxis
                                                    dataKey="patternName"
                                                    angle={-45}
                                                    textAnchor="end"
                                                    height={100}
                                                    fontSize={12}
                                                />
                                                <YAxis />
                                                <RechartsTooltip />
                                                <Bar dataKey="triggerCount" fill="#FF914D" />
                                            </BarChart>
                                        </ResponsiveContainer>
                                    ) : (
                                        <Typography variant="body1" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
                                            No attack pattern data available
                                        </Typography>
                                    )}
                                </Paper>
                            </Box>

                            {/* Threat Severity Distribution */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 2, fontWeight: 600, color: '#FF914D' }}>
                                    Threat Severity Distribution
                                </Typography>
                                <Grid container spacing={2}>
                                    <Grid item xs={12} md={6}>
                                        <Paper sx={{ p: 3 }}>
                                            {alertStatistics?.severityDistribution &&
                                            Object.keys(alertStatistics.severityDistribution).length > 0 ? (
                                                <ResponsiveContainer width="100%" height={300}>
                                                    <PieChart>
                                                        <Pie
                                                            data={Object.entries(alertStatistics.severityDistribution).map(([name, value]) => ({ name, value }))}
                                                            cx="50%"
                                                            cy="50%"
                                                            outerRadius={80}
                                                            fill="#8884d8"
                                                            dataKey="value"
                                                            label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                                                        >
                                                            {Object.keys(THREAT_COLORS).map((entry, index) => (
                                                                <Cell key={`cell-${index}`} fill={THREAT_COLORS[entry]} />
                                                            ))}
                                                        </Pie>
                                                        <RechartsTooltip />
                                                    </PieChart>
                                                </ResponsiveContainer>
                                            ) : (
                                                <Typography variant="body1" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
                                                    No threat distribution data available
                                                </Typography>
                                            )}
                                        </Paper>
                                    </Grid>

                                    <Grid item xs={12} md={6}>
                                        <Paper sx={{ p: 3 }}>
                                            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                                                Attack Statistics
                                            </Typography>
                                            <Stack spacing={2}>
                                                <Box>
                                                    <Typography variant="body2" color="text.secondary">
                                                        Total Open Alerts
                                                    </Typography>
                                                    <Typography variant="h5" fontWeight={700} color="#FF914D">
                                                        {alertStatistics?.totalOpenAlerts || 0}
                                                    </Typography>
                                                </Box>
                                                <Box>
                                                    <Typography variant="body2" color="text.secondary">
                                                        Resolution Rate
                                                    </Typography>
                                                    <Typography variant="h5" fontWeight={700} color="#4caf50">
                                                        {(alertStatistics?.resolutionRate || 0).toFixed(1)}%
                                                    </Typography>
                                                </Box>
                                                <Box>
                                                    <Typography variant="body2" color="text.secondary">
                                                        Avg Response Time
                                                    </Typography>
                                                    <Typography variant="h5" fontWeight={700} color="#ff9800">
                                                        {(alertStatistics?.avgResponseTimeMinutes || 0).toFixed(0)}m
                                                    </Typography>
                                                </Box>
                                            </Stack>
                                        </Paper>
                                    </Grid>
                                </Grid>
                            </Box>
                        </Stack>
                    </TabPanel>

                    {/* Compliance Tab */}
                    <TabPanel value={tabValue} index={3}>
                        <Stack spacing={3}>
                            {/* Compliance Overview */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 2, fontWeight: 600, color: '#FF914D' }}>
                                    Compliance Framework Status
                                </Typography>
                                <Grid container spacing={2}>
                                    {complianceData?.frameworkStatuses?.map((framework, index) => (
                                        <Grid item xs={12} md={4} key={index}>
                                            <Paper sx={{ p: 3, textAlign: 'center' }}>
                                                <Typography variant="h6" fontWeight={600} gutterBottom>
                                                    {framework.framework}
                                                </Typography>
                                                <Typography variant="h4" fontWeight={700} sx={{ color: '#FF914D', mb: 1 }}>
                                                    {framework.score?.toFixed(0) || 0}%
                                                </Typography>
                                                <Chip
                                                    label={framework.status}
                                                    color={framework.score >= 90 ? 'success' : framework.score >= 70 ? 'warning' : 'error'}
                                                    sx={{ mb: 1 }}
                                                />
                                                <Typography variant="body2" color="text.secondary">
                                                    {framework.criticalIssues || 0} critical issues
                                                </Typography>
                                            </Paper>
                                        </Grid>
                                    )) || (
                                        <Grid item xs={12}>
                                            <Paper sx={{ p: 3, textAlign: 'center' }}>
                                                <Typography variant="body1" color="text.secondary">
                                                    No compliance data available
                                                </Typography>
                                            </Paper>
                                        </Grid>
                                    )}
                                </Grid>
                            </Box>

                            {/* Overall Compliance Metrics */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 2, fontWeight: 600, color: '#FF914D' }}>
                                    Overall Compliance Metrics
                                </Typography>
                                <Grid container spacing={2}>
                                    <Grid item xs={12} md={3}>
                                        <Paper sx={{ p: 3, textAlign: 'center' }}>
                                            <Typography variant="h4" fontWeight={700} color="#FF914D">
                                                {(complianceData?.overallStatus?.averageComplianceScore || 0).toFixed(1)}%
                                            </Typography>
                                            <Typography variant="body1" color="text.secondary">
                                                Average Score
                                            </Typography>
                                        </Paper>
                                    </Grid>
                                    <Grid item xs={12} md={3}>
                                        <Paper sx={{ p: 3, textAlign: 'center' }}>
                                            <Typography variant="h4" fontWeight={700} color="#f44336">
                                                {complianceData?.overallStatus?.totalCriticalIssues || 0}
                                            </Typography>
                                            <Typography variant="body1" color="text.secondary">
                                                Critical Issues
                                            </Typography>
                                        </Paper>
                                    </Grid>
                                    <Grid item xs={12} md={3}>
                                        <Paper sx={{ p: 3, textAlign: 'center' }}>
                                            <Typography variant="h4" fontWeight={700} color="#4caf50">
                                                {complianceData?.overallStatus?.frameworkCount || 0}
                                            </Typography>
                                            <Typography variant="body1" color="text.secondary">
                                                Frameworks
                                            </Typography>
                                        </Paper>
                                    </Grid>
                                    <Grid item xs={12} md={3}>
                                        <Paper sx={{ p: 3, textAlign: 'center' }}>
                                            <Chip
                                                label={complianceData?.overallStatus?.overallStatus || 'UNKNOWN'}
                                                color={
                                                    complianceData?.overallStatus?.overallStatus === 'EXCELLENT' ? 'success' :
                                                        complianceData?.overallStatus?.overallStatus === 'GOOD' ? 'warning' : 'error'
                                                }
                                                sx={{ fontSize: '1.2rem', p: 2 }}
                                            />
                                            <Typography variant="body1" color="text.secondary" sx={{ mt: 1 }}>
                                                Overall Status
                                            </Typography>
                                        </Paper>
                                    </Grid>
                                </Grid>
                            </Box>
                        </Stack>
                    </TabPanel>

                    {/* Performance Tab */}
                    <TabPanel value={tabValue} index={4}>
                        <Stack spacing={3}>
                            {/* Route Performance Overview */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 2, fontWeight: 600, color: '#FF914D' }}>
                                    Route Performance Overview
                                </Typography>
                                <Grid container spacing={2}>
                                    {dashboardData?.routeAnalytics?.map((route, index) => (
                                        <Grid item xs={12} md={6} key={route.routeId || index}>
                                            <Paper sx={{ p: 3 }}>
                                                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                                                    <Box>
                                                        <Typography variant="subtitle1" fontWeight={600}>
                                                            {route.routeId}
                                                        </Typography>
                                                        <Typography variant="body2" color="text.secondary">
                                                            {route.totalRequests} requests • {(route.rejectionRate * 100).toFixed(1)}% rejected
                                                        </Typography>
                                                    </Box>
                                                    <Chip
                                                        label={route.securityStatus || 'NORMAL'}
                                                        size="small"
                                                        color={route.securityStatus === 'CRITICAL' ? 'error' :
                                                            route.securityStatus === 'HIGH_RISK' ? 'warning' : 'success'}
                                                    />
                                                </Box>
                                                <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 2 }}>
                                                    <Box>
                                                        <Typography variant="body2" color="text.secondary">
                                                            Avg Response Time
                                                        </Typography>
                                                        <Typography variant="h6" color="#FF914D">
                                                            {route.averageResponseTime?.toFixed(0) || 0}ms
                                                        </Typography>
                                                    </Box>
                                                    <Box>
                                                        <Typography variant="body2" color="text.secondary">
                                                            Threat Score
                                                        </Typography>
                                                        <Typography variant="h6" color="#f44336">
                                                            {((route.threatScore || 0) * 100).toFixed(0)}%
                                                        </Typography>
                                                    </Box>
                                                </Box>
                                            </Paper>
                                        </Grid>
                                    )) || (
                                        <Grid item xs={12}>
                                            <Paper sx={{ p: 3, textAlign: 'center' }}>
                                                <Typography variant="body1" color="text.secondary">
                                                    No route performance data available
                                                </Typography>
                                            </Paper>
                                        </Grid>
                                    )}
                                </Grid>
                            </Box>

                            {/* Time Series Chart */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 2, fontWeight: 600, color: '#FF914D' }}>
                                    Request Timeline (24h)
                                </Typography>
                                <Paper sx={{ p: 3 }}>
                                    {timeSeriesData?.length > 0 ? (
                                        <ResponsiveContainer width="100%" height={350}>
                                            <AreaChart data={timeSeriesData}>
                                                <CartesianGrid strokeDasharray="3 3" />
                                                <XAxis dataKey="time" />
                                                <YAxis />
                                                <RechartsTooltip />
                                                <Legend />
                                                <Area
                                                    type="monotone"
                                                    dataKey="total"
                                                    stackId="1"
                                                    stroke="#FF914D"
                                                    fill="#FF914D"
                                                    fillOpacity={0.6}
                                                    name="Total Requests"
                                                />
                                                <Area
                                                    type="monotone"
                                                    dataKey="rejected"
                                                    stackId="2"
                                                    stroke="#f44336"
                                                    fill="#f44336"
                                                    fillOpacity={0.6}
                                                    name="Rejected Requests"
                                                />
                                            </AreaChart>
                                        </ResponsiveContainer>
                                    ) : (
                                        <Typography variant="body1" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
                                            No time series data available
                                        </Typography>
                                    )}
                                </Paper>
                            </Box>
                        </Stack>
                    </TabPanel>
                </Box>
            </StyledCard>

            {/* Alert Detail Dialog */}
            <Dialog
                open={alertDetailDialog.open}
                onClose={() => setAlertDetailDialog({ open: false, alert: null })}
                maxWidth="md"
                fullWidth
            >
                <DialogTitle>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <WarningIcon sx={{ color: getThreatLevelColor(alertDetailDialog.alert?.severity) }} />
                        Threat Alert Details
                    </Box>
                </DialogTitle>
                <DialogContent>
                    {alertDetailDialog.alert && (
                        <Stack spacing={2}>
                            <Box>
                                <Typography variant="h6">{alertDetailDialog.alert.title}</Typography>
                                <Typography variant="body2" color="text.secondary">
                                    {alertDetailDialog.alert.description}
                                </Typography>
                            </Box>
                            <Grid container spacing={2}>
                                <Grid item xs={6}>
                                    <Typography variant="subtitle2">Severity</Typography>
                                    <Chip
                                        label={alertDetailDialog.alert.severity}
                                        sx={{
                                            backgroundColor: alpha(getThreatLevelColor(alertDetailDialog.alert.severity), 0.1),
                                            color: getThreatLevelColor(alertDetailDialog.alert.severity)
                                        }}
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <Typography variant="subtitle2">Source IP</Typography>
                                    <Typography variant="body1">{alertDetailDialog.alert.sourceIp}</Typography>
                                </Grid>
                                <Grid item xs={6}>
                                    <Typography variant="subtitle2">Target Route</Typography>
                                    <Typography variant="body1">{alertDetailDialog.alert.targetRoute || 'N/A'}</Typography>
                                </Grid>
                                <Grid item xs={6}>
                                    <Typography variant="subtitle2">Threat Score</Typography>
                                    <Typography variant="body1">{((alertDetailDialog.alert.threatScore || 0) * 100).toFixed(1)}%</Typography>
                                </Grid>
                            </Grid>
                        </Stack>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setAlertDetailDialog({ open: false, alert: null })}>
                        Close
                    </Button>
                    <Button
                        variant="contained"
                        sx={{ backgroundColor: '#FF914D' }}
                        onClick={() => {
                            // Handle alert action
                            setAlertDetailDialog({ open: false, alert: null });
                        }}
                    >
                        Mark as Resolved
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default AnalyticsPage;