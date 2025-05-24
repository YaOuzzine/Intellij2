// src/pages/AnalyticsPage.jsx - Enhanced AI-Powered Security Analytics Dashboard
import React, { useState, useEffect, useCallback } from 'react';
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
    useMediaQuery,
    Alert,
    AlertTitle,
    Chip,
    LinearProgress,
    IconButton,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
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
    RadialBar,
    Treemap,
    ScatterChart,
    Scatter
} from 'recharts';
import RefreshIcon from '@mui/icons-material/Refresh';
import DateRangeIcon from '@mui/icons-material/DateRange';
import SecurityIcon from '@mui/icons-material/Security';
import SpeedIcon from '@mui/icons-material/Speed';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import InsightsIcon from '@mui/icons-material/Insights';
import TimelineIcon from '@mui/icons-material/Timeline';
import PieChartIcon from '@mui/icons-material/PieChart';
import BarChartIcon from '@mui/icons-material/BarChart';
import WarningIcon from '@mui/icons-material/Warning';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import BugReportIcon from '@mui/icons-material/BugReport';
import ShieldIcon from '@mui/icons-material/Shield';
import GavelIcon from '@mui/icons-material/Gavel';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import apiClient from '../apiClient';

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

const aiGlow = keyframes`
    0%, 100% { box-shadow: 0 0 20px rgba(138, 43, 226, 0.3); }
    50% { box-shadow: 0 0 30px rgba(138, 43, 226, 0.6); }
`;

// Styled Components
const StyledCard = styled(Card)(({ theme }) => ({
    borderRadius: '16px',
    boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
    height: '100%',
    transition: 'all 0.3s ease',
    overflow: 'hidden',
    animation: `${fadeIn} 0.6s ease-out`,
    '&:hover': {
        transform: 'translateY(-5px)',
        boxShadow: '0 12px 40px rgba(0, 0, 0, 0.15)',
    },
}));

const AICard = styled(Card)(({ theme }) => ({
    borderRadius: '16px',
    background: 'linear-gradient(135deg, rgba(138, 43, 226, 0.1) 0%, rgba(75, 0, 130, 0.1) 100%)',
    border: '1px solid rgba(138, 43, 226, 0.2)',
    animation: `${aiGlow} 3s ease-in-out infinite`,
    '&:hover': {
        animation: `${aiGlow} 1s ease-in-out infinite`,
    },
}));

const ThreatCard = styled(Card)(({ severity }) => {
    const colors = {
        CRITICAL: '#d32f2f',
        HIGH: '#f57c00',
        MEDIUM: '#fbc02d',
        LOW: '#388e3c'
    };

    return {
        borderRadius: '12px',
        borderLeft: `4px solid ${colors[severity] || colors.LOW}`,
        background: `linear-gradient(135deg, ${alpha(colors[severity] || colors.LOW, 0.05)} 0%, transparent 100%)`,
        transition: 'all 0.3s ease',
        '&:hover': {
            transform: 'translateX(5px)',
            boxShadow: `0 4px 20px ${alpha(colors[severity] || colors.LOW, 0.3)}`,
        },
    };
});

const MetricCard = styled(Paper)(({ theme, color = '#1976d2' }) => ({
    padding: theme.spacing(3),
    borderRadius: '16px',
    background: `linear-gradient(135deg, ${alpha(color, 0.1)} 0%, ${alpha(color, 0.05)} 100%)`,
    border: `1px solid ${alpha(color, 0.2)}`,
    position: 'relative',
    overflow: 'hidden',
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

const HealthScoreCard = styled(Paper)(({ theme, score }) => {
    const getColor = (score) => {
        if (score >= 90) return '#4caf50';
        if (score >= 70) return '#ff9800';
        if (score >= 50) return '#f44336';
        return '#9c27b0';
    };

    return {
        padding: theme.spacing(3),
        borderRadius: '20px',
        background: `linear-gradient(135deg, ${alpha(getColor(score), 0.1)} 0%, ${alpha(getColor(score), 0.05)} 100%)`,
        border: `2px solid ${alpha(getColor(score), 0.3)}`,
        textAlign: 'center',
        position: 'relative',
        overflow: 'hidden',
    };
});

const TimelineItem = styled(Box)(({ theme, severity }) => {
    const colors = {
        CRITICAL: '#d32f2f',
        HIGH: '#f57c00',
        MEDIUM: '#fbc02d',
        LOW: '#388e3c'
    };

    return {
        display: 'flex',
        alignItems: 'center',
        padding: theme.spacing(1.5),
        marginBottom: theme.spacing(1),
        borderRadius: '8px',
        background: alpha(colors[severity] || colors.LOW, 0.05),
        border: `1px solid ${alpha(colors[severity] || colors.LOW, 0.1)}`,
        transition: 'all 0.2s ease',
        '&:hover': {
            background: alpha(colors[severity] || colors.LOW, 0.1),
            transform: 'translateX(5px)',
        },
    };
});

// Chart Colors
const COLORS = ['#FF914D', '#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#A569BD', '#8884d8'];
const THREAT_COLORS = {
    'CRITICAL': '#d32f2f',
    'HIGH': '#f57c00',
    'MEDIUM': '#fbc02d',
    'LOW': '#388e3c'
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

    // State management
    const [tabValue, setTabValue] = useState(0);
    const [timeRange, setTimeRange] = useState('24h');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [refreshing, setRefreshing] = useState(false);

    // Data state
    const [dashboardData, setDashboardData] = useState({});
    const [aiInsights, setAiInsights] = useState({});
    const [threatIntelligence, setThreatIntelligence] = useState({});
    const [activeAlerts, setActiveAlerts] = useState([]);
    const [complianceData, setComplianceData] = useState({});
    const [threatLandscape, setThreatLandscape] = useState({});

    // Dialog state
    const [alertDetailDialog, setAlertDetailDialog] = useState({ open: false, alert: null });
    const [insightDetailDialog, setInsightDetailDialog] = useState({ open: false, insight: null });

    // Fetch all analytics data
    const fetchAnalyticsData = useCallback(async (showLoading = true) => {
        if (showLoading) setLoading(true);
        setRefreshing(true);
        setError(null);

        try {
            console.log('[Enhanced Analytics] Fetching comprehensive analytics data...');

            // Fetch all data in parallel for better performance
            const [
                dashboardResponse,
                aiInsightsResponse,
                threatIntelResponse,
                alertsResponse,
                complianceResponse,
                threatLandscapeResponse
            ] = await Promise.all([
                apiClient.get('/analytics/dashboard'),
                apiClient.get('/analytics/ai/security-insights'),
                apiClient.get('/analytics/threat-intelligence/realtime'),
                apiClient.get('/analytics/alerts/active'),
                apiClient.get('/analytics/compliance/dashboard'),
                apiClient.get('/analytics/threat-analysis/landscape')
            ]);

            // Update state with fetched data
            setDashboardData(dashboardResponse.data);
            setAiInsights(aiInsightsResponse.data);
            setThreatIntelligence(threatIntelResponse.data);
            setActiveAlerts(alertsResponse.data);
            setComplianceData(complianceResponse.data);
            setThreatLandscape(threatLandscapeResponse.data);

            console.log('[Enhanced Analytics] All data loaded successfully');

        } catch (error) {
            console.error('[Enhanced Analytics] Error fetching data:', error);
            if (error.response?.status === 401) {
                setError("Authentication error. Please log in again.");
            } else {
                setError("Failed to load analytics data. Please try again later.");
            }
        } finally {
            if (showLoading) setLoading(false);
            setRefreshing(false);
        }
    }, []);

    // Initial data load and auto-refresh
    useEffect(() => {
        fetchAnalyticsData();

        // Set up real-time updates every 30 seconds
        const interval = setInterval(() => {
            fetchAnalyticsData(false);
        }, 30000);

        return () => clearInterval(interval);
    }, [fetchAnalyticsData]);

    // Handle tab change
    const handleChangeTab = (event, newValue) => {
        setTabValue(newValue);
    };

    // Handle time range change
    const handleTimeRangeChange = (event, newRange) => {
        if (newRange !== null) {
            setTimeRange(newRange);
            // Trigger data refresh with new time range
            fetchAnalyticsData();
        }
    };

    // Handle manual refresh
    const handleRefresh = () => {
        fetchAnalyticsData(true);
    };

    // Get security health score
    const getSecurityHealthScore = () => {
        return dashboardData?.securityHealthScore || 0;
    };

    // Get threat level color
    const getThreatLevelColor = (level) => {
        const colors = {
            'CRITICAL': '#d32f2f',
            'HIGH': '#f57c00',
            'MEDIUM': '#fbc02d',
            'LOW': '#388e3c',
            'NORMAL': '#2196f3'
        };
        return colors[level] || colors.NORMAL;
    };

    // Format timestamp
    const formatTimestamp = (timestamp) => {
        return new Date(timestamp).toLocaleString();
    };

    // Handle alert detail view
    const handleAlertClick = (alert) => {
        setAlertDetailDialog({ open: true, alert });
    };

    // Handle AI insight detail view
    const handleInsightClick = (insight) => {
        setInsightDetailDialog({ open: true, insight });
    };

    // Loading state
    if (loading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh' }}>
                <Box sx={{ textAlign: 'center' }}>
                    <CircularProgress size={60} sx={{ color: 'primary.main', mb: 2 }} />
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
                    <Button variant="contained" onClick={() => { setError(null); fetchAnalyticsData(); }}>
                        Retry
                    </Button>
                </Paper>
            </Box>
        );
    }

    return (
        <Box sx={{ p: 3, animation: `${fadeIn} 0.6s ease-out` }}>
            {/* Header with AI Badge */}
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4, flexWrap: 'wrap', gap: 2 }}>
                <Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
                        <Typography variant="h4" component="h1" sx={{ fontWeight: 700, color: 'primary.main' }}>
                            Security Analytics
                        </Typography>
                        <Chip
                            icon={<AutoAwesomeIcon />}
                            label="AI-Enhanced"
                            color="secondary"
                            variant="outlined"
                            sx={{
                                background: 'linear-gradient(45deg, rgba(138, 43, 226, 0.1), rgba(75, 0, 130, 0.1))',
                                border: '1px solid rgba(138, 43, 226, 0.3)'
                            }}
                        />
                    </Box>
                    <Typography variant="subtitle1" color="text.secondary">
                        Real-time threat detection with AI-powered insights
                    </Typography>
                </Box>

                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
                    {/* Time Range Selector */}
                    <ToggleButtonGroup
                        value={timeRange}
                        exclusive
                        onChange={handleTimeRangeChange}
                        aria-label="time range"
                        size={isMobile ? "small" : "medium"}
                    >
                        <ToggleButton value="1h">1H</ToggleButton>
                        <ToggleButton value="24h">24H</ToggleButton>
                        <ToggleButton value="7d">7D</ToggleButton>
                        <ToggleButton value="30d">30D</ToggleButton>
                    </ToggleButtonGroup>

                    {/* Refresh Button */}
                    <Tooltip title="Refresh data">
                        <IconButton
                            onClick={handleRefresh}
                            disabled={refreshing}
                            sx={{
                                backgroundColor: 'background.paper',
                                border: '1px solid',
                                borderColor: 'divider'
                            }}
                        >
                            <RefreshIcon sx={{ animation: refreshing ? `${pulse} 1s infinite` : 'none' }} />
                        </IconButton>
                    </Tooltip>
                </Box>
            </Box>

            {/* Security Health Score & Key Metrics */}
            <Grid container spacing={3} sx={{ mb: 4 }}>
                {/* Security Health Score */}
                <Grid item xs={12} md={4}>
                    <HealthScoreCard score={getSecurityHealthScore()}>
                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', mb: 2 }}>
                            <ShieldIcon sx={{ fontSize: 40, color: getThreatLevelColor(getSecurityHealthScore() >= 70 ? 'LOW' : 'HIGH'), mr: 1 }} />
                            <Typography variant="h6" fontWeight={600}>
                                Security Health
                            </Typography>
                        </Box>
                        <Typography variant="h2" fontWeight={700} sx={{ mb: 1 }}>
                            {getSecurityHealthScore().toFixed(0)}%
                        </Typography>
                        <LinearProgress
                            variant="determinate"
                            value={getSecurityHealthScore()}
                            sx={{
                                height: 8,
                                borderRadius: 4,
                                mb: 1,
                                backgroundColor: alpha(getThreatLevelColor(getSecurityHealthScore() >= 70 ? 'LOW' : 'HIGH'), 0.2),
                                '& .MuiLinearProgress-bar': {
                                    backgroundColor: getThreatLevelColor(getSecurityHealthScore() >= 70 ? 'LOW' : 'HIGH')
                                }
                            }}
                        />
                        <Typography variant="body2" color="text.secondary">
                            {getSecurityHealthScore() >= 90 ? 'Excellent' :
                                getSecurityHealthScore() >= 70 ? 'Good' :
                                    getSecurityHealthScore() >= 50 ? 'Fair' : 'Critical'}
                        </Typography>
                    </HealthScoreCard>
                </Grid>

                {/* Active Threats */}
                <Grid item xs={12} md={4}>
                    <MetricCard color="#f44336">
                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                            <Typography variant="h6" fontWeight={600}>
                                Active Threats
                            </Typography>
                            <BugReportIcon sx={{ color: '#f44336' }} />
                        </Box>
                        <Typography variant="h3" fontWeight={700} sx={{ mb: 1, color: '#f44336' }}>
                            {activeAlerts?.length || 0}
                        </Typography>
                        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                            {Object.entries(activeAlerts?.reduce((acc, alert) => {
                                acc[alert.severity] = (acc[alert.severity] || 0) + 1;
                                return acc;
                            }, {}) || {}).map(([severity, count]) => (
                                <Chip
                                    key={severity}
                                    label={`${severity}: ${count}`}
                                    size="small"
                                    sx={{
                                        backgroundColor: alpha(getThreatLevelColor(severity), 0.1),
                                        color: getThreatLevelColor(severity),
                                        fontWeight: 600
                                    }}
                                />
                            ))}
                        </Box>
                    </MetricCard>
                </Grid>

                {/* AI Confidence */}
                <Grid item xs={12} md={4}>
                    <MetricCard color="#9c27b0">
                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                            <Typography variant="h6" fontWeight={600}>
                                AI Confidence
                            </Typography>
                            <SmartToyIcon sx={{ color: '#9c27b0' }} />
                        </Box>
                        <Typography variant="h3" fontWeight={700} sx={{ mb: 1, color: '#9c27b0' }}>
                            {((aiInsights?.confidenceLevel || 0.75) * 100).toFixed(0)}%
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            Machine learning accuracy in threat detection
                        </Typography>
                    </MetricCard>
                </Grid>
            </Grid>

            {/* Main Dashboard Tabs */}
            <StyledCard sx={{ minHeight: '600px' }}>
                <Box sx={{ p: 3 }}>
                    <Tabs
                        value={tabValue}
                        onChange={handleChangeTab}
                        variant={isMobile ? "scrollable" : "fullWidth"}
                        scrollButtons={isMobile ? "auto" : false}
                        sx={{
                            mb: 3,
                            '& .MuiTab-root': {
                                minWidth: 'auto',
                                padding: theme.spacing(1.5, 2),
                                textTransform: 'none',
                                fontWeight: 500,
                                borderRadius: '8px 8px 0 0',
                                '&.Mui-selected': {
                                    fontWeight: 700,
                                    color: 'primary.main',
                                },
                            },
                        }}
                    >
                        <Tab icon={<TimelineIcon />} iconPosition="start" label="Threat Timeline" />
                        <Tab icon={<SmartToyIcon />} iconPosition="start" label="AI Insights" />
                        <Tab icon={<SecurityIcon />} iconPosition="start" label="Attack Patterns" />
                        <Tab icon={<GavelIcon />} iconPosition="start" label="Compliance" />
                        <Tab icon={<BarChartIcon />} iconPosition="start" label="Performance" />
                    </Tabs>

                    <Divider sx={{ mb: 3 }} />

                    {/* Real-time Threat Timeline */}
                    <TabPanel value={tabValue} index={0}>
                        <Box sx={{ height: 500 }}>
                            <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>
                                Real-time Threat Timeline
                            </Typography>

                            <Grid container spacing={3}>
                                {/* Recent Alerts Timeline */}
                                <Grid item xs={12} md={8}>
                                    <Paper sx={{ p: 2, height: 400, overflow: 'auto' }}>
                                        {activeAlerts?.slice(0, 10).map((alert, index) => (
                                            <TimelineItem
                                                key={alert.id || index}
                                                severity={alert.severity}
                                                onClick={() => handleAlertClick(alert)}
                                                sx={{ cursor: 'pointer' }}
                                            >
                                                <FiberManualRecordIcon
                                                    sx={{
                                                        color: getThreatLevelColor(alert.severity),
                                                        mr: 2,
                                                        fontSize: 12
                                                    }}
                                                />
                                                <Box sx={{ flexGrow: 1 }}>
                                                    <Typography variant="subtitle2" fontWeight={600}>
                                                        {alert.title}
                                                    </Typography>
                                                    <Typography variant="body2" color="text.secondary">
                                                        {alert.sourceIp} • {formatTimestamp(alert.timestamp)}
                                                    </Typography>
                                                </Box>
                                                <Chip
                                                    label={alert.severity}
                                                    size="small"
                                                    sx={{
                                                        backgroundColor: alpha(getThreatLevelColor(alert.severity), 0.1),
                                                        color: getThreatLevelColor(alert.severity)
                                                    }}
                                                />
                                            </TimelineItem>
                                        ))}

                                        {(!activeAlerts || activeAlerts.length === 0) && (
                                            <Box sx={{ textAlign: 'center', py: 4 }}>
                                                <CheckCircleOutlineIcon sx={{ fontSize: 60, color: 'success.main', mb: 2 }} />
                                                <Typography variant="h6" color="success.main">
                                                    No Active Threats
                                                </Typography>
                                                <Typography variant="body2" color="text.secondary">
                                                    Your security posture is currently stable
                                                </Typography>
                                            </Box>
                                        )}
                                    </Paper>
                                </Grid>

                                {/* Threat Statistics */}
                                <Grid item xs={12} md={4}>
                                    <Paper sx={{ p: 2, height: 400 }}>
                                        <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
                                            Threat Distribution
                                        </Typography>

                                        {Object.entries(activeAlerts?.reduce((acc, alert) => {
                                            acc[alert.severity] = (acc[alert.severity] || 0) + 1;
                                            return acc;
                                        }, {}) || {}).length > 0 ? (
                                            <ResponsiveContainer width="100%" height={300}>
                                                <PieChart>
                                                    <Pie
                                                        data={Object.entries(activeAlerts?.reduce((acc, alert) => {
                                                            acc[alert.severity] = (acc[alert.severity] || 0) + 1;
                                                            return acc;
                                                        }, {}) || {}).map(([name, value]) => ({ name, value }))}
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
                                            <Box sx={{ textAlign: 'center', py: 4 }}>
                                                <Typography variant="body2" color="text.secondary">
                                                    No threat data to display
                                                </Typography>
                                            </Box>
                                        )}
                                    </Paper>
                                </Grid>
                            </Grid>
                        </Box>
                    </TabPanel>

                    {/* AI Insights Panel */}
                    <TabPanel value={tabValue} index={1}>
                        <Box sx={{ height: 500 }}>
                            <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, display: 'flex', alignItems: 'center' }}>
                                <SmartToyIcon sx={{ mr: 1 }} />
                                AI Security Insights
                            </Typography>

                            <Grid container spacing={3}>
                                {/* AI Insights Cards */}
                                <Grid item xs={12} md={6}>
                                    <AICard sx={{ p: 2, height: 400, overflow: 'auto' }}>
                                        <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
                                            Behavioral Anomalies
                                        </Typography>

                                        {aiInsights?.behavioralAnomalies?.map((anomaly, index) => (
                                            <Box
                                                key={index}
                                                sx={{
                                                    p: 2,
                                                    mb: 2,
                                                    borderRadius: 2,
                                                    backgroundColor: alpha('#9c27b0', 0.05),
                                                    border: '1px solid',
                                                    borderColor: alpha('#9c27b0', 0.1),
                                                    cursor: 'pointer',
                                                    '&:hover': {
                                                        backgroundColor: alpha('#9c27b0', 0.1)
                                                    }
                                                }}
                                                onClick={() => handleInsightClick(anomaly)}
                                            >
                                                <Typography variant="subtitle2" fontWeight={600}>
                                                    {anomaly.clientIp}
                                                </Typography>
                                                <Typography variant="body2" color="text.secondary">
                                                    Anomaly Score: {(anomaly.anomalyScore * 100).toFixed(1)}%
                                                </Typography>
                                                <Typography variant="caption" color="text.secondary">
                                                    {anomaly.eventCount} events analyzed
                                                </Typography>
                                            </Box>
                                        )) || (
                                            <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
                                                No behavioral anomalies detected
                                            </Typography>
                                        )}
                                    </AICard>
                                </Grid>

                                {/* AI Recommendations */}
                                <Grid item xs={12} md={6}>
                                    <AICard sx={{ p: 2, height: 400, overflow: 'auto' }}>
                                        <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
                                            AI Recommendations
                                        </Typography>

                                        {aiInsights?.aiRecommendations?.map((rec, index) => (
                                            <Alert
                                                key={index}
                                                severity={rec.priority === 'CRITICAL' ? 'error' :
                                                    rec.priority === 'HIGH' ? 'warning' : 'info'}
                                                sx={{ mb: 2 }}
                                            >
                                                <AlertTitle>{rec.title}</AlertTitle>
                                                {rec.description}
                                            </Alert>
                                        )) || (
                                            <Alert severity="success">
                                                <AlertTitle>All Systems Optimal</AlertTitle>
                                                No immediate recommendations from AI analysis
                                            </Alert>
                                        )}
                                    </AICard>
                                </Grid>

                                {/* Threat Predictions */}
                                <Grid item xs={12}>
                                    <Paper sx={{ p: 2 }}>
                                        <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
                                            AI Threat Predictions (Next 6 Hours)
                                        </Typography>

                                        {aiInsights?.threatPredictions?.predictions ? (
                                            <ResponsiveContainer width="100%" height={200}>
                                                <LineChart data={aiInsights.threatPredictions.predictions}>
                                                    <CartesianGrid strokeDasharray="3 3" />
                                                    <XAxis dataKey="hour" />
                                                    <YAxis />
                                                    <RechartsTooltip />
                                                    <Legend />
                                                    <Line
                                                        type="monotone"
                                                        dataKey="attackLikelihood"
                                                        stroke="#9c27b0"
                                                        strokeWidth={3}
                                                        dot={{ fill: '#9c27b0', strokeWidth: 2, r: 4 }}
                                                        name="Threat Likelihood"
                                                    />
                                                </LineChart>
                                            </ResponsiveContainer>
                                        ) : (
                                            <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
                                                Threat prediction data unavailable
                                            </Typography>
                                        )}
                                    </Paper>
                                </Grid>
                            </Grid>
                        </Box>
                    </TabPanel>

                    {/* Attack Patterns Visualization */}
                    <TabPanel value={tabValue} index={2}>
                        <Box sx={{ height: 500 }}>
                            <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>
                                Interactive Attack Pattern Analysis
                            </Typography>

                            <Grid container spacing={3}>
                                {/* Attack Pattern Analysis */}
                                <Grid item xs={12} md={8}>
                                    <Paper sx={{ p: 2, height: 400 }}>
                                        <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
                                            Attack Pattern Distribution
                                        </Typography>

                                        {threatLandscape?.topThreatPatterns ? (
                                            <ResponsiveContainer width="100%" height={350}>
                                                <BarChart data={threatLandscape.topThreatPatterns.slice(0, 10)}>
                                                    <CartesianGrid strokeDasharray="3 3" />
                                                    <XAxis dataKey="patternName" angle={-45} textAnchor="end" height={100} />
                                                    <YAxis />
                                                    <RechartsTooltip />
                                                    <Bar dataKey="triggerCount" fill="#FF914D" />
                                                </BarChart>
                                            </ResponsiveContainer>
                                        ) : (
                                            <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
                                                No attack pattern data available
                                            </Typography>
                                        )}
                                    </Paper>
                                </Grid>

                                {/* Geographic Threats */}
                                <Grid item xs={12} md={4}>
                                    <Paper sx={{ p: 2, height: 400, overflow: 'auto' }}>
                                        <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
                                            Geographic Threats
                                        </Typography>

                                        {threatIntelligence?.realTimeMetrics?.geographicThreats?.map((threat, index) => (
                                            <Box key={index} sx={{ p: 1.5, mb: 1, borderRadius: 1, backgroundColor: 'background.default' }}>
                                                <Typography variant="body2" fontWeight={600}>
                                                    {threat}
                                                </Typography>
                                            </Box>
                                        )) || (
                                            <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
                                                No geographic threat data
                                            </Typography>
                                        )}
                                    </Paper>
                                </Grid>
                            </Grid>
                        </Box>
                    </TabPanel>

                    {/* Compliance Dashboard */}
                    <TabPanel value={tabValue} index={3}>
                        <Box sx={{ height: 500 }}>
                            <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>
                                Security Compliance Dashboard
                            </Typography>

                            <Grid container spacing={3}>
                                {/* Compliance Overview */}
                                <Grid item xs={12} md={8}>
                                    <Paper sx={{ p: 2, height: 400 }}>
                                        <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
                                            Compliance Framework Status
                                        </Typography>

                                        {complianceData?.frameworkStatuses ? (
                                            <ResponsiveContainer width="100%" height={350}>
                                                <BarChart data={complianceData.frameworkStatuses}>
                                                    <CartesianGrid strokeDasharray="3 3" />
                                                    <XAxis dataKey="framework" />
                                                    <YAxis />
                                                    <RechartsTooltip />
                                                    <Bar dataKey="score" fill="#00C49F" />
                                                </BarChart>
                                            </ResponsiveContainer>
                                        ) : (
                                            <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
                                                Compliance data loading...
                                            </Typography>
                                        )}
                                    </Paper>
                                </Grid>

                                {/* Compliance Metrics */}
                                <Grid item xs={12} md={4}>
                                    <Paper sx={{ p: 2, height: 400, overflow: 'auto' }}>
                                        <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
                                            Compliance Metrics
                                        </Typography>

                                        <Box sx={{ mb: 2 }}>
                                            <Typography variant="body2" color="text.secondary">
                                                Average Compliance Score
                                            </Typography>
                                            <Typography variant="h5" fontWeight={700} color="primary">
                                                {(complianceData?.overallStatus?.averageComplianceScore || 0).toFixed(1)}%
                                            </Typography>
                                        </Box>

                                        <Box sx={{ mb: 2 }}>
                                            <Typography variant="body2" color="text.secondary">
                                                Critical Issues
                                            </Typography>
                                            <Typography variant="h5" fontWeight={700} color="error">
                                                {complianceData?.overallStatus?.totalCriticalIssues || 0}
                                            </Typography>
                                        </Box>

                                        <Box sx={{ mb: 2 }}>
                                            <Typography variant="body2" color="text.secondary">
                                                Frameworks Monitored
                                            </Typography>
                                            <Typography variant="h5" fontWeight={700}>
                                                {complianceData?.overallStatus?.frameworkCount || 0}
                                            </Typography>
                                        </Box>
                                    </Paper>
                                </Grid>
                            </Grid>
                        </Box>
                    </TabPanel>

                    {/* Performance Analytics */}
                    <TabPanel value={tabValue} index={4}>
                        <Box sx={{ height: 500 }}>
                            <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>
                                Performance & System Analytics
                            </Typography>

                            <Grid container spacing={3}>
                                {/* Performance Metrics */}
                                <Grid item xs={12} md={6}>
                                    <Paper sx={{ p: 2, height: 400 }}>
                                        <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
                                            Request Performance
                                        </Typography>

                                        {dashboardData?.routeAnalytics ? (
                                            <ResponsiveContainer width="100%" height={350}>
                                                <ScatterChart data={dashboardData.routeAnalytics}>
                                                    <CartesianGrid strokeDasharray="3 3" />
                                                    <XAxis dataKey="totalRequests" name="Total Requests" />
                                                    <YAxis dataKey="averageResponseTime" name="Avg Response Time (ms)" />
                                                    <RechartsTooltip cursor={{ strokeDasharray: '3 3' }} />
                                                    <Scatter name="Route Performance" dataKey="averageResponseTime" fill="#8884d8" />
                                                </ScatterChart>
                                            </ResponsiveContainer>
                                        ) : (
                                            <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
                                                Performance data loading...
                                            </Typography>
                                        )}
                                    </Paper>
                                </Grid>

                                {/* Route Health */}
                                <Grid item xs={12} md={6}>
                                    <Paper sx={{ p: 2, height: 400, overflow: 'auto' }}>
                                        <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
                                            Route Health Status
                                        </Typography>

                                        {dashboardData?.routeAnalytics?.map((route, index) => (
                                            <Box
                                                key={route.routeId || index}
                                                sx={{
                                                    p: 2,
                                                    mb: 2,
                                                    borderRadius: 2,
                                                    backgroundColor: 'background.default',
                                                    display: 'flex',
                                                    justifyContent: 'space-between',
                                                    alignItems: 'center'
                                                }}
                                            >
                                                <Box>
                                                    <Typography variant="subtitle2" fontWeight={600}>
                                                        {route.routeId}
                                                    </Typography>
                                                    <Typography variant="body2" color="text.secondary">
                                                        {route.totalRequests} requests • {(route.rejectionRate * 100).toFixed(1)}% rejected
                                                    </Typography>
                                                </Box>
                                                <Box sx={{ textAlign: 'right' }}>
                                                    <Chip
                                                        label={route.securityStatus || 'NORMAL'}
                                                        size="small"
                                                        color={route.securityStatus === 'CRITICAL' ? 'error' :
                                                            route.securityStatus === 'HIGH_RISK' ? 'warning' : 'success'}
                                                    />
                                                    <Typography variant="caption" display="block" color="text.secondary">
                                                        Threat: {((route.threatScore || 0) * 100).toFixed(0)}%
                                                    </Typography>
                                                </Box>
                                            </Box>
                                        )) || (
                                            <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
                                                No route data available
                                            </Typography>
                                        )}
                                    </Paper>
                                </Grid>
                            </Grid>
                        </Box>
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
                        <Grid container spacing={2}>
                            <Grid item xs={12}>
                                <Typography variant="h6">{alertDetailDialog.alert.title}</Typography>
                                <Typography variant="body2" color="text.secondary" paragraph>
                                    {alertDetailDialog.alert.description}
                                </Typography>
                            </Grid>
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
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setAlertDetailDialog({ open: false, alert: null })}>
                        Close
                    </Button>
                </DialogActions>
            </Dialog>

            {/* AI Insight Detail Dialog */}
            <Dialog
                open={insightDetailDialog.open}
                onClose={() => setInsightDetailDialog({ open: false, insight: null })}
                maxWidth="md"
                fullWidth
            >
                <DialogTitle>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <SmartToyIcon sx={{ color: '#9c27b0' }} />
                        AI Behavioral Analysis
                    </Box>
                </DialogTitle>
                <DialogContent>
                    {insightDetailDialog.insight && (
                        <Grid container spacing={2}>
                            <Grid item xs={12}>
                                <Typography variant="h6">IP Address: {insightDetailDialog.insight.clientIp}</Typography>
                            </Grid>
                            <Grid item xs={6}>
                                <Typography variant="subtitle2">Anomaly Score</Typography>
                                <LinearProgress
                                    variant="determinate"
                                    value={insightDetailDialog.insight.anomalyScore * 100}
                                    sx={{ mt: 1, height: 8, borderRadius: 4 }}
                                />
                                <Typography variant="body2">{(insightDetailDialog.insight.anomalyScore * 100).toFixed(1)}%</Typography>
                            </Grid>
                            <Grid item xs={6}>
                                <Typography variant="subtitle2">Events Analyzed</Typography>
                                <Typography variant="body1">{insightDetailDialog.insight.eventCount}</Typography>
                            </Grid>
                            <Grid item xs={12}>
                                <Typography variant="subtitle2">Suspicious Activities</Typography>
                                {insightDetailDialog.insight.suspiciousActivities?.map((activity, index) => (
                                    <Typography key={index} variant="body2" sx={{ ml: 2 }}>
                                        • {activity}
                                    </Typography>
                                )) || <Typography variant="body2">None detected</Typography>}
                            </Grid>
                        </Grid>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setInsightDetailDialog({ open: false, insight: null })}>
                        Close
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default AnalyticsPage;