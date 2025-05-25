import React, { useState, useEffect, useCallback, useRef } from 'react';
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
    Badge,
    Tooltip,
    Collapse,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Skeleton,
    Snackbar,
    Fab,
    Switch,
    FormControlLabel,
    Select,
    MenuItem,
    FormControl,
    InputLabel,
    TextField
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
    ScatterChart,
    Scatter,
    ComposedChart
} from 'recharts';

// Enhanced Icons
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
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import PublicIcon from '@mui/icons-material/Public';
import DescriptionIcon from '@mui/icons-material/Description';
import DownloadIcon from '@mui/icons-material/Download';
import FilterListIcon from '@mui/icons-material/FilterList';
import FullscreenIcon from '@mui/icons-material/Fullscreen';
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';
import DarkModeIcon from '@mui/icons-material/DarkMode';
import LightModeIcon from '@mui/icons-material/LightMode';
import MapIcon from '@mui/icons-material/Map';
import AssessmentIcon from '@mui/icons-material/Assessment';

import { analyticsApi } from '../services/analyticsApi';

// Enhanced Animations
const fadeInUp = keyframes`
    from { 
        opacity: 0; 
        transform: translateY(30px);
    }
    to { 
        opacity: 1; 
        transform: translateY(0);
    }
`;

const slideInRight = keyframes`
    from { 
        opacity: 0; 
        transform: translateX(30px);
    }
    to { 
        opacity: 1; 
        transform: translateX(0);
    }
`;

const pulse = keyframes`
    0%, 100% { transform: scale(1); }
    50% { transform: scale(1.02); }
`;

const aiGlow = keyframes`
    0%, 100% { 
        box-shadow: 0 0 20px rgba(255, 145, 77, 0.3);
        border-color: rgba(255, 145, 77, 0.3);
    }
    50% { 
        box-shadow: 0 0 30px rgba(255, 145, 77, 0.6);
        border-color: rgba(255, 145, 77, 0.6);
    }
`;

const threatPulse = keyframes`
    0%, 100% { 
        box-shadow: 0 0 15px rgba(244, 67, 54, 0.3);
    }
    50% { 
        box-shadow: 0 0 25px rgba(244, 67, 54, 0.6);
    }
`;

// Enhanced Styled Components
const StyledCard = styled(Card)(({ theme, variant = 'default' }) => ({
    borderRadius: '16px',
    boxShadow: variant === 'elevated' ? '0 12px 40px rgba(0, 0, 0, 0.15)' : '0 8px 32px rgba(0, 0, 0, 0.1)',
    height: '100%',
    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
    overflow: 'hidden',
    animation: `${fadeInUp} 0.6s ease-out`,
    position: 'relative',
    '&:hover': {
        transform: 'translateY(-4px)',
        boxShadow: '0 16px 48px rgba(0, 0, 0, 0.2)',
    },
    '&::before': variant === 'gradient' ? {
        content: '""',
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        height: '4px',
        background: 'linear-gradient(90deg, #FF914D, #FFB74D, #FF7043)',
    } : {},
}));

const AICard = styled(Card)(({ theme, severity = 'info' }) => {
    const severityColors = {
        critical: '#d32f2f',
        high: '#FF914D',
        medium: '#ff9800',
        low: '#4caf50',
        info: '#2196f3'
    };

    const color = severityColors[severity];

    return {
        borderRadius: '16px',
        background: `linear-gradient(135deg, ${alpha(color, 0.08)} 0%, ${alpha(color, 0.03)} 100%)`,
        border: `2px solid ${alpha(color, 0.2)}`,
        animation: severity === 'critical' ? `${threatPulse} 2s infinite` :
            severity === 'info' ? `${aiGlow} 3s ease-in-out infinite` : 'none',
        transition: 'all 0.3s ease',
        '&:hover': {
            transform: 'translateY(-2px)',
            boxShadow: `0 8px 32px ${alpha(color, 0.25)}`,
        },
    };
});

const MetricCard = styled(Paper)(({ theme, color = '#FF914D', trend = 'stable' }) => {
    const trendColors = {
        up: '#4caf50',
        down: '#f44336',
        stable: color
    };

    return {
        padding: theme.spacing(3),
        borderRadius: '20px',
        background: `linear-gradient(135deg, ${alpha(color, 0.1)} 0%, ${alpha(color, 0.05)} 100%)`,
        border: `2px solid ${alpha(color, 0.2)}`,
        position: 'relative',
        overflow: 'hidden',
        minHeight: '140px',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        transition: 'all 0.3s ease',
        animation: `${slideInRight} 0.6s ease-out`,
        '&::before': {
            content: '""',
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            height: '4px',
            background: `linear-gradient(90deg, ${trendColors[trend]}, ${color})`,
        },
        '&:hover': {
            transform: 'scale(1.02)',
            boxShadow: `0 12px 40px ${alpha(color, 0.2)}`,
        },
    };
});

const InteractiveTab = styled(Tab)(({ theme, alertcount = 0 }) => ({
    minWidth: 'auto',
    padding: theme.spacing(1.5, 3),
    textTransform: 'none',
    fontWeight: 500,
    borderRadius: '12px 12px 0 0',
    margin: theme.spacing(0, 0.5),
    transition: 'all 0.3s ease',
    position: 'relative',
    '&.Mui-selected': {
        fontWeight: 700,
        color: '#FF914D',
        background: alpha('#FF914D', 0.1),
    },
    '&:hover': {
        background: alpha('#FF914D', 0.05),
    },
    ...(alertcount > 0 && {
        '&::after': {
            content: `"${alertcount}"`,
            position: 'absolute',
            top: 4,
            right: 4,
            background: '#f44336',
            color: 'white',
            borderRadius: '50%',
            width: '20px',
            height: '20px',
            fontSize: '11px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 'bold',
        }
    }),
}));

const GlassCard = styled(Paper)(({ theme }) => ({
    background: 'rgba(255, 255, 255, 0.85)',
    backdropFilter: 'blur(10px)',
    borderRadius: '20px',
    border: '1px solid rgba(255, 255, 255, 0.2)',
    padding: theme.spacing(3),
    transition: 'all 0.3s ease',
    '&:hover': {
        background: 'rgba(255, 255, 255, 0.95)',
        transform: 'translateY(-2px)',
    },
}));

// Enhanced Color Palettes
const THREAT_COLORS = {
    'CRITICAL': '#d32f2f',
    'HIGH': '#FF914D',
    'MEDIUM': '#ff9800',
    'LOW': '#4caf50',
    'UNKNOWN': '#9e9e9e'
};

const CHART_COLORS = [
    '#FF914D', '#4caf50', '#2196f3', '#ff9800',
    '#9c27b0', '#f44336', '#00bcd4', '#8bc34a'
];

// Custom Tab Panel Component
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
                <Box sx={{ py: 3, animation: `${fadeInUp} 0.4s ease-out` }}>
                    {children}
                </Box>
            )}
        </div>
    );
}

// Enhanced Loading Component
const EnhancedLoading = ({ message = "Loading..." }) => (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 8 }}>
        <Box sx={{ position: 'relative', mb: 4 }}>
            <CircularProgress
                size={60}
                thickness={4}
                sx={{
                    color: '#FF914D',
                    animation: `${pulse} 2s infinite`
                }}
            />
            <AutoAwesomeIcon
                sx={{
                    position: 'absolute',
                    top: '50%',
                    left: '50%',
                    transform: 'translate(-50%, -50%)',
                    color: '#FF914D',
                    fontSize: 24
                }}
            />
        </Box>
        <Typography variant="h6" color="text.secondary" sx={{ mb: 1 }}>
            {message}
        </Typography>
        <Typography variant="body2" color="text.secondary">
            Analyzing security data with AI insights...
        </Typography>
    </Box>
);

// Enhanced Error Component
const EnhancedError = ({ error, onRetry }) => (
    <GlassCard sx={{ textAlign: 'center', maxWidth: 600, mx: 'auto', mt: 4 }}>
        <ErrorOutlineIcon sx={{ fontSize: 64, color: '#f44336', mb: 2 }} />
        <Typography variant="h5" gutterBottom color="error">
            Analytics Service Unavailable
        </Typography>
        <Typography variant="body1" color="text.secondary" paragraph>
            {error || "Unable to load security analytics data"}
        </Typography>
        <Button
            variant="contained"
            onClick={onRetry}
            startIcon={<RefreshIcon />}
            sx={{
                backgroundColor: '#FF914D',
                '&:hover': { backgroundColor: '#FF7043' }
            }}
        >
            Retry Analysis
        </Button>
    </GlassCard>
);

// Empty State Component
const EmptyState = ({ title, description, icon: Icon = AssessmentIcon }) => (
    <Box sx={{ textAlign: 'center', py: 8 }}>
        <Icon sx={{ fontSize: 64, color: 'text.secondary', mb: 2, opacity: 0.5 }} />
        <Typography variant="h6" color="text.secondary" gutterBottom>
            {title}
        </Typography>
        <Typography variant="body2" color="text.secondary">
            {description}
        </Typography>
    </Box>
);

const AnalyticsPage = () => {
    const theme = useTheme();
    const isMobile = useMediaQuery(theme.breakpoints.down('md'));
    const isTablet = useMediaQuery(theme.breakpoints.down('lg'));

    // Enhanced State Management
    const [tabValue, setTabValue] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [refreshing, setRefreshing] = useState(false);
    const [darkMode, setDarkMode] = useState(false);
    const [autoRefresh, setAutoRefresh] = useState(true);
    const [timeRange, setTimeRange] = useState('24h');
    const [selectedRoute, setSelectedRoute] = useState('all');

    // Data State
    const [dashboardData, setDashboardData] = useState(null);
    const [securityInsights, setSecurityInsights] = useState(null);
    const [activeAlerts, setActiveAlerts] = useState([]);
    const [threatLandscape, setThreatLandscape] = useState(null);
    const [complianceData, setComplianceData] = useState(null);
    const [timeSeriesData, setTimeSeriesData] = useState([]);
    const [alertStatistics, setAlertStatistics] = useState(null);
    const [realTimeThreat, setRealTimeThreat] = useState(null);
    const [aiSecurityReport, setAiSecurityReport] = useState(null);
    const [geographicThreats, setGeographicThreats] = useState([]);

    // Dialog States
    const [reportDialog, setReportDialog] = useState({ open: false });
    const [alertDetailDialog, setAlertDetailDialog] = useState({ open: false, alert: null });
    const [fullscreenChart, setFullscreenChart] = useState({ open: false, chart: null });

    // Notification State
    const [notification, setNotification] = useState({ open: false, message: '', severity: 'info' });

    // Auto-refresh interval ref
    const refreshInterval = useRef(null);

    // Helper Functions
    const showNotification = (message, severity = 'info') => {
        setNotification({ open: true, message, severity });
    };

    const formatTimestamp = (timestamp) => {
        if (!timestamp) return 'Unknown';
        return new Date(timestamp).toLocaleString();
    };

    const getThreatLevelColor = (level) => {
        return THREAT_COLORS[level] || THREAT_COLORS.UNKNOWN;
    };

    const getSecurityHealthScore = () => {
        return dashboardData?.securityHealthScore || securityInsights?.securityHealthScore || 0;
    };

    const calculateTrend = (current, previous) => {
        if (!previous || previous === 0) return 'stable';
        const change = ((current - previous) / previous) * 100;
        if (change > 5) return 'up';
        if (change < -5) return 'down';
        return 'stable';
    };

    // Enhanced Data Fetching
    const fetchAnalyticsData = useCallback(async (showLoading = true) => {
        if (showLoading) setLoading(true);
        setRefreshing(true);
        setError(null);

        try {
            console.log('[AnalyticsPage] Fetching comprehensive analytics data...');

            // Fetch all data in parallel with error handling
            const [
                dashboardResponse,
                securityResponse,
                alertsResponse,
                threatResponse,
                complianceResponse,
                timeseriesResponse,
                alertStatsResponse,
                realTimeThreatResponse
            ] = await Promise.allSettled([
                analyticsApi.getDashboard(),
                analyticsApi.getSecurityInsights(),
                analyticsApi.getActiveAlerts(),
                analyticsApi.getThreatLandscape(),
                analyticsApi.getComplianceDashboard(),
                analyticsApi.getTimeSeries(timeRange, selectedRoute),
                analyticsApi.getAlertStatistics(),
                analyticsApi.getRealTimeThreatIntel()
            ]);

            // Process responses with proper error handling
            if (dashboardResponse.status === 'fulfilled') {
                setDashboardData(dashboardResponse.value.data);
                console.log('[AnalyticsPage] Dashboard data loaded successfully');
            } else {
                console.error('[AnalyticsPage] Dashboard error:', dashboardResponse.reason?.message);
            }

            if (securityResponse.status === 'fulfilled') {
                setSecurityInsights(securityResponse.value.data);
                console.log('[AnalyticsPage] Security insights loaded successfully');
            } else {
                console.error('[AnalyticsPage] Security insights error:', securityResponse.reason?.message);
            }

            if (alertsResponse.status === 'fulfilled') {
                const alerts = Array.isArray(alertsResponse.value.data) ? alertsResponse.value.data : [];
                setActiveAlerts(alerts);
                console.log('[AnalyticsPage] Active alerts loaded:', alerts.length);
            } else {
                console.error('[AnalyticsPage] Alerts error:', alertsResponse.reason?.message);
                setActiveAlerts([]);
            }

            if (threatResponse.status === 'fulfilled') {
                setThreatLandscape(threatResponse.value.data);
                console.log('[AnalyticsPage] Threat landscape loaded successfully');
            } else {
                console.error('[AnalyticsPage] Threat landscape error:', threatResponse.reason?.message);
            }

            if (complianceResponse.status === 'fulfilled') {
                setComplianceData(complianceResponse.value.data);
                console.log('[AnalyticsPage] Compliance data loaded successfully');
            } else {
                console.error('[AnalyticsPage] Compliance error:', complianceResponse.reason?.message);
            }

            if (timeseriesResponse.status === 'fulfilled') {
                const timeSeries = timeseriesResponse.value.data?.timeSeries || [];
                setTimeSeriesData(Array.isArray(timeSeries) ? timeSeries : []);
                console.log('[AnalyticsPage] Time series loaded:', timeSeries.length, 'data points');
            } else {
                console.error('[AnalyticsPage] Time series error:', timeseriesResponse.reason?.message);
                setTimeSeriesData([]);
            }

            if (alertStatsResponse.status === 'fulfilled') {
                setAlertStatistics(alertStatsResponse.value.data);
                console.log('[AnalyticsPage] Alert statistics loaded successfully');
            } else {
                console.error('[AnalyticsPage] Alert stats error:', alertStatsResponse.reason?.message);
            }

            if (realTimeThreatResponse.status === 'fulfilled') {
                const threatData = realTimeThreatResponse.value.data;
                setRealTimeThreat(threatData);

                // Extract geographic threats from real-time threat data
                if (threatData?.realTimeMetrics?.geographicThreats) {
                    const geoThreats = Array.isArray(threatData.realTimeMetrics.geographicThreats)
                        ? threatData.realTimeMetrics.geographicThreats
                        : [];
                    setGeographicThreats(geoThreats);
                    console.log('[AnalyticsPage] Geographic threats loaded:', geoThreats.length);
                } else {
                    setGeographicThreats([]);
                }
            } else {
                console.error('[AnalyticsPage] Real-time threat error:', realTimeThreatResponse.reason?.message);
                setGeographicThreats([]);
            }

            showNotification('Analytics data refreshed successfully', 'success');

        } catch (error) {
            console.error('[AnalyticsPage] Fatal error:', error);
            setError("Failed to load analytics data. Please check service connectivity.");
            showNotification('Failed to load analytics data', 'error');
        } finally {
            if (showLoading) setLoading(false);
            setRefreshing(false);
        }
    }, [timeRange, selectedRoute]);

    // Fetch AI Security Report
    const fetchAISecurityReport = useCallback(async () => {
        try {
            console.log('[AnalyticsPage] Generating AI security report...');
            setReportDialog({ open: true, loading: true });

            const endDate = new Date();
            const startDate = new Date(endDate.getTime() - 24 * 60 * 60 * 1000); // 24 hours ago

            // Call the security report endpoint
            const response = await fetch('/api/analytics/reports/security?' + new URLSearchParams({
                startDate: startDate.toISOString(),
                endDate: endDate.toISOString()
            }), {
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('token')}`
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const reportData = await response.json();
            setAiSecurityReport(reportData);
            setReportDialog({ open: true, loading: false });

            console.log('[AnalyticsPage] AI security report generated successfully');
            showNotification('AI security report generated successfully', 'success');

        } catch (error) {
            console.error('[AnalyticsPage] AI report error:', error);
            setReportDialog({ open: false });
            showNotification('Failed to generate AI security report: ' + error.message, 'error');
        }
    }, []);

    // Download AI Report
    const downloadAIReport = () => {
        if (!aiSecurityReport) return;

        try {
            const reportText = generateReportText(aiSecurityReport);
            const blob = new Blob([reportText], { type: 'text/plain' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `AI_Security_Report_${new Date().toISOString().split('T')[0]}.txt`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(url);

            showNotification('Security report downloaded successfully', 'success');
        } catch (error) {
            console.error('Download error:', error);
            showNotification('Failed to download report', 'error');
        }
    };

    // Generate Report Text for Download
    const generateReportText = (reportData) => {
        const timestamp = new Date().toLocaleString();

        let reportText = `
AI-ENHANCED SECURITY ANALYTICS REPORT
Generated: ${timestamp}
Analysis Period: ${reportData.reportPeriod?.start} to ${reportData.reportPeriod?.end}

========================================
EXECUTIVE SUMMARY
========================================
`;

        if (reportData.executiveSummary?.aiAnalysis?.structuredAnalysis) {
            const analysis = reportData.executiveSummary.aiAnalysis.structuredAnalysis;

            reportText += `
Security Posture Score: ${analysis.securityPostureScore || 'N/A'}/10
Risk Level: ${analysis.riskLevel || 'Unknown'}

Security Posture Analysis:
${analysis.securityPostureAnalysis || 'No analysis available'}

Key Threats:
${analysis.keyThreats || 'No key threats identified'}

Business Impact:
${analysis.businessImpact || 'No business impact analysis available'}

Immediate Actions Required:
${analysis.immediateActions || 'No immediate actions specified'}

Risk Analysis:
${analysis.riskAnalysis || 'No risk analysis available'}
`;
        } else if (reportData.executiveSummary?.aiAnalysis?.fallbackAnalysis) {
            const fallback = reportData.executiveSummary.aiAnalysis.fallbackAnalysis;
            reportText += `
${fallback.summary || 'No summary available'}
Risk Level: ${fallback.riskLevel || 'Unknown'}
`;
        }

        reportText += `

========================================
SECURITY METRICS
========================================
`;

        if (reportData.securityEventsAnalysis) {
            const metrics = reportData.securityEventsAnalysis;
            reportText += `
Total Requests: ${metrics.totalRequests || 0}
Total Rejections: ${metrics.totalRejections || 0}
Acceptance Rate: ${metrics.acceptanceRate?.toFixed(2) || 0}%
Average Response Time: ${metrics.avgResponseTime?.toFixed(0) || 0}ms
Unique IPs: ${metrics.uniqueIPs || 0}
`;
        }

        reportText += `

========================================
COMPLIANCE ANALYSIS
========================================
`;

        if (reportData.complianceAnalysis) {
            reportText += `Framework: ${reportData.complianceAnalysis.framework || 'N/A'}
Compliance Score: ${reportData.complianceAnalysis.complianceScore?.toFixed(1) || 0}%
Status: ${reportData.complianceAnalysis.complianceStatus || 'Unknown'}
`;
        }

        reportText += `

========================================
RECOMMENDATIONS
========================================
`;

        if (reportData.actionItems && Array.isArray(reportData.actionItems)) {
            reportData.actionItems.forEach((item, index) => {
                reportText += `
${index + 1}. [${item.priority || 'N/A'}] ${item.title || 'No title'}
   ${item.description || 'No description'}
`;
            });
        } else {
            reportText += `
No specific action items available.
`;
        }

        reportText += `

========================================
REPORT END
========================================

Note: This report was generated using AI-enhanced security analytics.
For technical support, please contact your security operations team.
`;

        return reportText;
    };

    // Event Handlers
    const handleTabChange = (event, newValue) => {
        setTabValue(newValue);
    };

    const handleRefresh = () => {
        fetchAnalyticsData(true);
    };

    const handleAlertClick = (alert) => {
        setAlertDetailDialog({ open: true, alert });
    };

    const handleTimeRangeChange = (event) => {
        setTimeRange(event.target.value);
    };

    const handleRouteChange = (event) => {
        setSelectedRoute(event.target.value);
    };

    // Auto-refresh Effect
    useEffect(() => {
        if (autoRefresh) {
            refreshInterval.current = setInterval(() => {
                fetchAnalyticsData(false);
            }, 30000); // Refresh every 30 seconds
        } else {
            if (refreshInterval.current) {
                clearInterval(refreshInterval.current);
            }
        }

        return () => {
            if (refreshInterval.current) {
                clearInterval(refreshInterval.current);
            }
        };
    }, [autoRefresh, fetchAnalyticsData]);

    // Initial Load Effect
    useEffect(() => {
        fetchAnalyticsData();
    }, [fetchAnalyticsData]);

    // Loading State
    if (loading) {
        return (
            <Box sx={{ p: 3 }}>
                <EnhancedLoading message="Loading AI-Enhanced Security Analytics" />
            </Box>
        );
    }

    // Error State
    if (error) {
        return (
            <Box sx={{ p: 3 }}>
                <EnhancedError
                    error={error}
                    onRetry={() => { setError(null); fetchAnalyticsData(); }}
                />
            </Box>
        );
    }

    return (
        <Box sx={{ p: 3, animation: `${fadeInUp} 0.6s ease-out` }}>
            {/* Enhanced Header */}
            <Box sx={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                mb: 4,
                flexWrap: 'wrap',
                gap: 2
            }}>
                <Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
                        <Typography variant="h4" component="h1" sx={{
                            fontWeight: 700,
                            background: 'linear-gradient(45deg, #FF914D, #FFB74D)',
                            backgroundClip: 'text',
                            WebkitBackgroundClip: 'text',
                            color: 'transparent'
                        }}>
                            Security Analytics
                        </Typography>
                        <Chip
                            icon={<AutoAwesomeIcon />}
                            label="AI-Enhanced"
                            sx={{
                                background: 'linear-gradient(45deg, rgba(255, 145, 77, 0.1), rgba(255, 193, 7, 0.1))',
                                border: '1px solid rgba(255, 145, 77, 0.3)',
                                color: '#FF914D',
                                fontWeight: 600
                            }}
                        />
                        {activeAlerts.length > 0 && (
                            <Badge badgeContent={activeAlerts.length} color="error">
                                <NotificationsActiveIcon sx={{ color: '#f44336' }} />
                            </Badge>
                        )}
                    </Box>
                    <Typography variant="subtitle1" color="text.secondary">
                        Real-time threat detection with comprehensive AI insights
                    </Typography>
                </Box>

                {/* Enhanced Controls */}
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
                    <FormControl size="small" sx={{ minWidth: 120 }}>
                        <InputLabel>Time Range</InputLabel>
                        <Select value={timeRange} onChange={handleTimeRangeChange} label="Time Range">
                            <MenuItem value="1h">Last Hour</MenuItem>
                            <MenuItem value="24h">Last 24 Hours</MenuItem>
                            <MenuItem value="7d">Last 7 Days</MenuItem>
                        </Select>
                    </FormControl>

                    <FormControlLabel
                        control={
                            <Switch
                                checked={autoRefresh}
                                onChange={(e) => setAutoRefresh(e.target.checked)}
                                color="warning"
                            />
                        }
                        label="Auto Refresh"
                    />

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
                        <RefreshIcon sx={{
                            animation: refreshing ? `${pulse} 1s infinite` : 'none'
                        }} />
                    </IconButton>
                </Box>
            </Box>

            {/* Enhanced Key Metrics Cards */}
            <Grid container spacing={3} sx={{ mb: 4 }}>
                <Grid item xs={12} sm={6} md={3}>
                    <MetricCard
                        color="#4caf50"
                        trend={calculateTrend(getSecurityHealthScore(), 75)}
                    >
                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                            <Typography variant="h6" fontWeight={600}>Security Health</Typography>
                            <ShieldIcon sx={{ fontSize: 32, color: '#4caf50', opacity: 0.8 }} />
                        </Box>
                        <Typography variant="h2" fontWeight={700} sx={{ color: '#4caf50', mb: 2 }}>
                            {getSecurityHealthScore().toFixed(0)}%
                        </Typography>
                        <LinearProgress
                            variant="determinate"
                            value={getSecurityHealthScore()}
                            sx={{
                                height: 8,
                                borderRadius: 4,
                                backgroundColor: alpha('#4caf50', 0.2),
                                '& .MuiLinearProgress-bar': {
                                    backgroundColor: '#4caf50',
                                    borderRadius: 4
                                }
                            }}
                        />
                    </MetricCard>
                </Grid>

                <Grid item xs={12} sm={6} md={3}>
                    <MetricCard
                        color="#f44336"
                        trend={calculateTrend(activeAlerts?.length || 0, 0)}
                    >
                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                            <Typography variant="h6" fontWeight={600}>Active Threats</Typography>
                            <WarningIcon sx={{ fontSize: 32, color: '#f44336', opacity: 0.8 }} />
                        </Box>
                        <Typography variant="h2" fontWeight={700} sx={{ color: '#f44336', mb: 1 }}>
                            {activeAlerts?.length || 0}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            {activeAlerts?.filter(a => a.severity === 'CRITICAL')?.length || 0} Critical
                        </Typography>
                    </MetricCard>
                </Grid>

                <Grid item xs={12} sm={6} md={3}>
                    <MetricCard color="#FF914D">
                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                            <Typography variant="h6" fontWeight={600}>Total Requests</Typography>
                            <TimelineIcon sx={{ fontSize: 32, color: '#FF914D', opacity: 0.8 }} />
                        </Box>
                        <Typography variant="h2" fontWeight={700} sx={{ color: '#FF914D', mb: 1 }}>
                            {dashboardData?.globalMetrics?.totalRequests?.toLocaleString() || 0}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            {dashboardData?.globalMetrics?.currentMinuteRequests || 0} this minute
                        </Typography>
                    </MetricCard>
                </Grid>

                <Grid item xs={12} sm={6} md={3}>
                    <MetricCard color="#9c27b0">
                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                            <Typography variant="h6" fontWeight={600}>AI Confidence</Typography>
                            <SmartToyIcon sx={{ fontSize: 32, color: '#9c27b0', opacity: 0.8 }} />
                        </Box>
                        <Typography variant="h2" fontWeight={700} sx={{ color: '#9c27b0', mb: 1 }}>
                            {((securityInsights?.confidenceLevel || 0.75) * 100).toFixed(0)}%
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            Analysis confidence
                        </Typography>
                    </MetricCard>
                </Grid>
            </Grid>

            {/* Enhanced Main Dashboard Tabs */}
            <StyledCard variant="elevated">
                <Box sx={{ p: 3 }}>
                    <Tabs
                        value={tabValue}
                        onChange={handleTabChange}
                        variant={isMobile ? "scrollable" : "fullWidth"}
                        scrollButtons={isMobile ? "auto" : false}
                        sx={{
                            mb: 3,
                            '& .MuiTabs-indicator': {
                                backgroundColor: '#FF914D',
                                height: 3,
                                borderRadius: '3px 3px 0 0',
                            }
                        }}
                    >
                        <InteractiveTab
                            icon={<TimelineIcon />}
                            iconPosition="start"
                            label="Threat Overview"
                            alertcount={activeAlerts?.length || 0}
                        />
                        <InteractiveTab
                            icon={<SmartToyIcon />}
                            iconPosition="start"
                            label="AI Insights"
                        />
                        <InteractiveTab
                            icon={<MapIcon />}
                            iconPosition="start"
                            label="Geographic Analysis"
                        />
                        <InteractiveTab
                            icon={<DescriptionIcon />}
                            iconPosition="start"
                            label="AI Security Report"
                        />
                        <InteractiveTab
                            icon={<GavelIcon />}
                            iconPosition="start"
                            label="Compliance"
                        />
                        <InteractiveTab
                            icon={<SpeedIcon />}
                            iconPosition="start"
                            label="Performance"
                        />
                    </Tabs>

                    {/* Tab Panel 0: Threat Overview */}
                    <TabPanel value={tabValue} index={0}>
                        <Stack spacing={4}>
                            {/* Active Alerts Section */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                    Active Security Alerts
                                </Typography>

                                {activeAlerts && activeAlerts.length > 0 ? (
                                    <Grid container spacing={2}>
                                        {activeAlerts.slice(0, 6).map((alert, index) => (
                                            <Grid item xs={12} md={6} key={alert.id || index}>
                                                <AICard
                                                    severity={alert.severity?.toLowerCase() || 'info'}
                                                    onClick={() => handleAlertClick(alert)}
                                                    sx={{ cursor: 'pointer' }}
                                                >
                                                    <CardContent>
                                                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                                                            <Typography variant="subtitle1" fontWeight={600} sx={{ flex: 1 }}>
                                                                {alert.title || 'Security Alert'}
                                                            </Typography>
                                                            <Chip
                                                                label={alert.severity || 'UNKNOWN'}
                                                                size="small"
                                                                sx={{
                                                                    backgroundColor: alpha(getThreatLevelColor(alert.severity), 0.1),
                                                                    color: getThreatLevelColor(alert.severity),
                                                                    fontWeight: 600,
                                                                    ml: 1
                                                                }}
                                                            />
                                                        </Box>
                                                        <Typography variant="body2" color="text.secondary" gutterBottom>
                                                            Source: {alert.sourceIp || 'Unknown'}
                                                        </Typography>
                                                        {alert.threatScore && (
                                                            <Typography variant="body2" color="text.secondary" gutterBottom>
                                                                Threat Score: {(alert.threatScore * 100).toFixed(1)}%
                                                            </Typography>
                                                        )}
                                                        <Typography variant="body2" color="text.secondary">
                                                            {formatTimestamp(alert.firstSeen || alert.createdAt)}
                                                        </Typography>
                                                    </CardContent>
                                                </AICard>
                                            </Grid>
                                        ))}
                                    </Grid>
                                ) : (
                                    <EmptyState
                                        title="No Active Threats Detected"
                                        description="Your security posture is currently stable with no active threats requiring attention."
                                        icon={CheckCircleOutlineIcon}
                                    />
                                )}
                            </Box>

                            {/* Global Metrics Overview */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                    Traffic Overview
                                </Typography>
                                <Grid container spacing={3}>
                                    <Grid item xs={12} md={4}>
                                        <GlassCard sx={{ textAlign: 'center', p: 3 }}>
                                            <Typography variant="h4" fontWeight={700} color="#FF914D" sx={{ mb: 1 }}>
                                                {dashboardData?.globalMetrics?.totalRequests?.toLocaleString() || 0}
                                            </Typography>
                                            <Typography variant="body1" color="text.secondary">
                                                Total Requests
                                            </Typography>
                                            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                                                {dashboardData?.globalMetrics?.currentMinuteRequests || 0} current minute
                                            </Typography>
                                        </GlassCard>
                                    </Grid>
                                    <Grid item xs={12} md={4}>
                                        <GlassCard sx={{ textAlign: 'center', p: 3 }}>
                                            <Typography variant="h4" fontWeight={700} color="#f44336" sx={{ mb: 1 }}>
                                                {dashboardData?.globalMetrics?.totalRejections?.toLocaleString() || 0}
                                            </Typography>
                                            <Typography variant="body1" color="text.secondary">
                                                Total Rejections
                                            </Typography>
                                            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                                                {dashboardData?.globalMetrics?.currentMinuteRejections || 0} current minute
                                            </Typography>
                                        </GlassCard>
                                    </Grid>
                                    <Grid item xs={12} md={4}>
                                        <GlassCard sx={{ textAlign: 'center', p: 3 }}>
                                            <Typography variant="h4" fontWeight={700} color="#4caf50" sx={{ mb: 1 }}>
                                                {(((dashboardData?.globalMetrics?.totalRequests || 0) - (dashboardData?.globalMetrics?.totalRejections || 0)) / Math.max(1, dashboardData?.globalMetrics?.totalRequests || 1) * 100).toFixed(1)}%
                                            </Typography>
                                            <Typography variant="body1" color="text.secondary">
                                                Success Rate
                                            </Typography>
                                            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                                                Last 24 hours
                                            </Typography>
                                        </GlassCard>
                                    </Grid>
                                </Grid>
                            </Box>

                            {/* Time Series Chart */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                    Request Timeline ({timeRange})
                                </Typography>
                                <GlassCard sx={{ p: 3 }}>
                                    {timeSeriesData && timeSeriesData.length > 0 ? (
                                        <ResponsiveContainer width="100%" height={350}>
                                            <ComposedChart data={timeSeriesData}>
                                                <CartesianGrid strokeDasharray="3 3" opacity={0.3} />
                                                <XAxis
                                                    dataKey="time"
                                                    tick={{ fontSize: 12 }}
                                                    tickFormatter={(value) => {
                                                        const date = new Date(value);
                                                        return date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
                                                    }}
                                                />
                                                <YAxis tick={{ fontSize: 12 }} />
                                                <RechartsTooltip
                                                    labelFormatter={(value) => new Date(value).toLocaleString()}
                                                    formatter={(value, name) => [value, name === 'total' ? 'Total Requests' : 'Rejected Requests']}
                                                />
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
                                                    fillOpacity={0.8}
                                                    name="Rejected Requests"
                                                />
                                            </ComposedChart>
                                        </ResponsiveContainer>
                                    ) : (
                                        <EmptyState
                                            title="No Time Series Data Available"
                                            description={`No request data found for the selected time range (${timeRange})`}
                                            icon={TimelineIcon}
                                        />
                                    )}
                                </GlassCard>
                            </Box>
                        </Stack>
                    </TabPanel>

                    {/* Tab Panel 1: AI Insights */}
                    <TabPanel value={tabValue} index={1}>
                        <Stack spacing={4}>
                            {/* AI Security Health Score */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                    AI Security Assessment
                                </Typography>
                                <Grid container spacing={3}>
                                    <Grid item xs={12} md={6}>
                                        <AICard severity="info">
                                            <CardContent sx={{ textAlign: 'center' }}>
                                                <SmartToyIcon sx={{ fontSize: 48, color: '#2196f3', mb: 2 }} />
                                                <Typography variant="h4" fontWeight={700} sx={{ color: '#2196f3', mb: 1 }}>
                                                    {getSecurityHealthScore().toFixed(1)}%
                                                </Typography>
                                                <Typography variant="h6" gutterBottom>
                                                    Overall Security Score
                                                </Typography>
                                                <Typography variant="body2" color="text.secondary">
                                                    AI Confidence: {((securityInsights?.confidenceLevel || 0.75) * 100).toFixed(0)}%
                                                </Typography>
                                            </CardContent>
                                        </AICard>
                                    </Grid>
                                    <Grid item xs={12} md={6}>
                                        <AICard severity="info">
                                            <CardContent>
                                                <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                                    <AutoAwesomeIcon sx={{ color: '#2196f3' }} />
                                                    AI Analysis Summary
                                                </Typography>
                                                <Typography variant="body2" color="text.secondary" paragraph>
                                                    Security posture appears {getSecurityHealthScore() > 80 ? 'strong' : getSecurityHealthScore() > 60 ? 'moderate' : 'concerning'} based on current metrics.
                                                </Typography>
                                                <Typography variant="body2" color="text.secondary" paragraph>
                                                    {activeAlerts?.length ? `${activeAlerts.length} active threats detected` : 'No active threats detected'}.
                                                </Typography>
                                                <Typography variant="body2" color="text.secondary">
                                                    Last analyzed: {formatTimestamp(securityInsights?.analysisTimestamp)}
                                                </Typography>
                                            </CardContent>
                                        </AICard>
                                    </Grid>
                                </Grid>
                            </Box>

                            {/* AI Recommendations */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                    AI Security Recommendations
                                </Typography>
                                <Stack spacing={2}>
                                    {securityInsights?.aiRecommendations && securityInsights.aiRecommendations.length > 0 ? (
                                        securityInsights.aiRecommendations.map((rec, index) => (
                                            <Alert
                                                key={index}
                                                severity={rec.priority === 'CRITICAL' ? 'error' :
                                                    rec.priority === 'HIGH' ? 'warning' : 'info'}
                                                sx={{
                                                    '& .MuiAlert-icon': { color: '#FF914D' },
                                                    borderRadius: 2
                                                }}
                                            >
                                                <AlertTitle>{rec.title}</AlertTitle>
                                                {rec.description}
                                                {rec.recommendedActions && rec.recommendedActions.length > 0 && (
                                                    <Box sx={{ mt: 2 }}>
                                                        <Typography variant="body2" fontWeight={600} gutterBottom>
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
                                        ))
                                    ) : (
                                        <Alert severity="success" sx={{ borderRadius: 2 }}>
                                            <AlertTitle>All Systems Optimal</AlertTitle>
                                            AI analysis indicates no immediate security recommendations at this time.
                                        </Alert>
                                    )}
                                </Stack>
                            </Box>

                            {/* Behavioral Anomalies */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                    Behavioral Anomalies Detected
                                </Typography>
                                {securityInsights?.behavioralAnomalies && securityInsights.behavioralAnomalies.length > 0 ? (
                                    <Grid container spacing={2}>
                                        {securityInsights.behavioralAnomalies.map((anomaly, index) => (
                                            <Grid item xs={12} md={6} key={index}>
                                                <AICard severity={anomaly.anomalyScore > 0.8 ? 'critical' : anomaly.anomalyScore > 0.6 ? 'high' : 'medium'}>
                                                    <CardContent>
                                                        <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                                                            {anomaly.clientIp}
                                                        </Typography>
                                                        <LinearProgress
                                                            variant="determinate"
                                                            value={anomaly.anomalyScore * 100}
                                                            sx={{
                                                                mb: 2,
                                                                height: 8,
                                                                borderRadius: 4,
                                                                backgroundColor: alpha('#FF914D', 0.2),
                                                                '& .MuiLinearProgress-bar': {
                                                                    backgroundColor: anomaly.anomalyScore > 0.8 ? '#d32f2f' : '#FF914D',
                                                                    borderRadius: 4
                                                                }
                                                            }}
                                                        />
                                                        <Typography variant="body2" color="text.secondary" gutterBottom>
                                                            Anomaly Score: {(anomaly.anomalyScore * 100).toFixed(1)}%
                                                        </Typography>
                                                        <Typography variant="body2" color="text.secondary" gutterBottom>
                                                            Events: {anomaly.eventCount}
                                                        </Typography>
                                                        {anomaly.suspiciousActivities && anomaly.suspiciousActivities.length > 0 && (
                                                            <Box sx={{ mt: 1 }}>
                                                                <Typography variant="caption" display="block" fontWeight={600}>
                                                                    Suspicious Activities:
                                                                </Typography>
                                                                {anomaly.suspiciousActivities.slice(0, 2).map((activity, actIndex) => (
                                                                    <Typography key={actIndex} variant="caption" display="block" color="text.secondary">
                                                                        • {activity}
                                                                    </Typography>
                                                                ))}
                                                            </Box>
                                                        )}
                                                    </CardContent>
                                                </AICard>
                                            </Grid>
                                        ))}
                                    </Grid>
                                ) : (
                                    <EmptyState
                                        title="No Behavioral Anomalies Detected"
                                        description="AI analysis shows all user behavior patterns are within normal parameters."
                                        icon={CheckCircleOutlineIcon}
                                    />
                                )}
                            </Box>

                            {/* AI Threat Predictions */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                    AI Threat Predictions (Next 6 Hours)
                                </Typography>
                                <GlassCard sx={{ p: 3 }}>
                                    {securityInsights?.threatPredictions?.predictions && securityInsights.threatPredictions.predictions.length > 0 ? (
                                        <ResponsiveContainer width="100%" height={300}>
                                            <LineChart data={securityInsights.threatPredictions.predictions}>
                                                <CartesianGrid strokeDasharray="3 3" opacity={0.3} />
                                                <XAxis
                                                    dataKey="hour"
                                                    tickFormatter={(value) => `+${value}h`}
                                                />
                                                <YAxis
                                                    domain={[0, 1]}
                                                    tickFormatter={(value) => `${(value * 100).toFixed(0)}%`}
                                                />
                                                <RechartsTooltip
                                                    formatter={(value) => [`${(value * 100).toFixed(1)}%`, 'Threat Likelihood']}
                                                    labelFormatter={(value) => `${value} hours from now`}
                                                />
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
                                        <EmptyState
                                            title="No Threat Prediction Data"
                                            description="AI threat prediction analysis is currently unavailable or no patterns detected."
                                            icon={SmartToyIcon}
                                        />
                                    )}
                                </GlassCard>
                            </Box>
                        </Stack>
                    </TabPanel>

                    {/* Tab Panel 2: Geographic Analysis */}
                    <TabPanel value={tabValue} index={2}>
                        <Stack spacing={4}>
                            {/* Geographic Threats Overview */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                    Geographic Threat Analysis
                                </Typography>

                                {geographicThreats && geographicThreats.length > 0 ? (
                                    <Grid container spacing={3}>
                                        {geographicThreats.map((threat, index) => (
                                            <Grid item xs={12} md={6} lg={4} key={index}>
                                                <AICard severity={threat.threatLevel?.toLowerCase() || 'low'}>
                                                    <CardContent>
                                                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                                                <PublicIcon sx={{ color: getThreatLevelColor(threat.threatLevel) }} />
                                                                <Typography variant="h6" fontWeight={600}>
                                                                    {threat.country}
                                                                </Typography>
                                                            </Box>
                                                            <Chip
                                                                label={threat.threatLevel || 'LOW'}
                                                                size="small"
                                                                sx={{
                                                                    backgroundColor: alpha(getThreatLevelColor(threat.threatLevel), 0.1),
                                                                    color: getThreatLevelColor(threat.threatLevel),
                                                                    fontWeight: 600
                                                                }}
                                                            />
                                                        </Box>

                                                        <Grid container spacing={2} sx={{ mb: 2 }}>
                                                            <Grid item xs={6}>
                                                                <Typography variant="body2" color="text.secondary">
                                                                    Unique IPs
                                                                </Typography>
                                                                <Typography variant="h6" fontWeight={600}>
                                                                    {threat.uniqueIPs}
                                                                </Typography>
                                                            </Grid>
                                                            <Grid item xs={6}>
                                                                <Typography variant="body2" color="text.secondary">
                                                                    Total Events
                                                                </Typography>
                                                                <Typography variant="h6" fontWeight={600}>
                                                                    {threat.totalEvents}
                                                                </Typography>
                                                            </Grid>
                                                            <Grid item xs={6}>
                                                                <Typography variant="body2" color="text.secondary">
                                                                    Rejections
                                                                </Typography>
                                                                <Typography variant="h6" fontWeight={600} color="#f44336">
                                                                    {threat.totalRejections}
                                                                </Typography>
                                                            </Grid>
                                                            <Grid item xs={6}>
                                                                <Typography variant="body2" color="text.secondary">
                                                                    Rejection Rate
                                                                </Typography>
                                                                <Typography variant="h6" fontWeight={600}>
                                                                    {(threat.rejectionRate * 100).toFixed(1)}%
                                                                </Typography>
                                                            </Grid>
                                                        </Grid>

                                                        {threat.cities && threat.cities.length > 0 && (
                                                            <Box>
                                                                <Typography variant="body2" color="text.secondary" gutterBottom>
                                                                    Top Cities:
                                                                </Typography>
                                                                <Typography variant="body2">
                                                                    {threat.cities.slice(0, 3).join(', ')}
                                                                </Typography>
                                                            </Box>
                                                        )}

                                                        {threat.suspiciousIPs > 0 && (
                                                            <Box sx={{ mt: 2 }}>
                                                                <Chip
                                                                    size="small"
                                                                    label={`${threat.suspiciousIPs} Suspicious IPs`}
                                                                    color="warning"
                                                                    variant="outlined"
                                                                />
                                                            </Box>
                                                        )}
                                                    </CardContent>
                                                </AICard>
                                            </Grid>
                                        ))}
                                    </Grid>
                                ) : (
                                    <EmptyState
                                        title="No Geographic Threats Detected"
                                        description="No suspicious geographic activity patterns have been identified in the current time period."
                                        icon={PublicIcon}
                                    />
                                )}
                            </Box>

                            {/* Geographic Distribution Chart */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                    Threat Distribution by Country
                                </Typography>
                                <GlassCard sx={{ p: 3 }}>
                                    {geographicThreats && geographicThreats.length > 0 ? (
                                        <ResponsiveContainer width="100%" height={400}>
                                            <BarChart data={geographicThreats.slice(0, 10)}>
                                                <CartesianGrid strokeDasharray="3 3" opacity={0.3} />
                                                <XAxis
                                                    dataKey="country"
                                                    angle={-45}
                                                    textAnchor="end"
                                                    height={100}
                                                    fontSize={12}
                                                />
                                                <YAxis />
                                                <RechartsTooltip
                                                    formatter={(value, name) => [
                                                        value,
                                                        name === 'totalRejections' ? 'Total Rejections' :
                                                            name === 'uniqueIPs' ? 'Unique IPs' : name
                                                    ]}
                                                />
                                                <Legend />
                                                <Bar
                                                    dataKey="totalRejections"
                                                    fill="#f44336"
                                                    name="Rejections"
                                                    radius={[4, 4, 0, 0]}
                                                />
                                                <Bar
                                                    dataKey="uniqueIPs"
                                                    fill="#FF914D"
                                                    name="Unique IPs"
                                                    radius={[4, 4, 0, 0]}
                                                />
                                            </BarChart>
                                        </ResponsiveContainer>
                                    ) : (
                                        <EmptyState
                                            title="No Geographic Data to Display"
                                            description="No geographic threat data is available for visualization."
                                            icon={MapIcon}
                                        />
                                    )}
                                </GlassCard>
                            </Box>

                            {/* Real-time Geographic Intelligence */}
                            {realTimeThreat?.realTimeMetrics && (
                                <Box>
                                    <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                        Real-time Geographic Intelligence
                                    </Typography>
                                    <Grid container spacing={3}>
                                        <Grid item xs={12} md={6}>
                                            <GlassCard sx={{ p: 3 }}>
                                                <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                                                    Current Threat Level
                                                </Typography>
                                                <Typography
                                                    variant="h4"
                                                    fontWeight={700}
                                                    sx={{
                                                        color: getThreatLevelColor(realTimeThreat.realTimeMetrics.currentThreatLevel),
                                                        mb: 1
                                                    }}
                                                >
                                                    {realTimeThreat.realTimeMetrics.currentThreatLevel || 'UNKNOWN'}
                                                </Typography>
                                                <Typography variant="body2" color="text.secondary">
                                                    Last updated: {formatTimestamp(realTimeThreat.timestamp)}
                                                </Typography>
                                            </GlassCard>
                                        </Grid>
                                        <Grid item xs={12} md={6}>
                                            <GlassCard sx={{ p: 3 }}>
                                                <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                                                    Attacks in Last Hour
                                                </Typography>
                                                <Typography variant="h4" fontWeight={700} sx={{ color: '#f44336', mb: 1 }}>
                                                    {realTimeThreat.realTimeMetrics.attacksInLastHour || 0}
                                                </Typography>
                                                <Typography variant="body2" color="text.secondary">
                                                    Geographic analysis enabled
                                                </Typography>
                                            </GlassCard>
                                        </Grid>
                                    </Grid>
                                </Box>
                            )}
                        </Stack>
                    </TabPanel>

                    {/* Tab Panel 3: AI Security Report */}
                    <TabPanel value={tabValue} index={3}>
                        <Stack spacing={4}>
                            {/* Report Generation Section */}
                            <Box>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                                    <Typography variant="h6" sx={{ fontWeight: 600, color: '#FF914D' }}>
                                        AI-Generated Security Report
                                    </Typography>
                                    <Button
                                        variant="contained"
                                        startIcon={<DescriptionIcon />}
                                        onClick={fetchAISecurityReport}
                                        sx={{
                                            backgroundColor: '#FF914D',
                                            '&:hover': { backgroundColor: '#FF7043' }
                                        }}
                                    >
                                        Generate Report
                                    </Button>
                                </Box>

                                <Alert severity="info" sx={{ mb: 3, borderRadius: 2 }}>
                                    <AlertTitle>AI-Powered Security Analysis</AlertTitle>
                                    This report is generated using advanced AI to provide comprehensive security insights,
                                    threat analysis, and actionable recommendations based on your current security posture.
                                </Alert>
                            </Box>

                            {/* Report Preview */}
                            {aiSecurityReport && (
                                <Box>
                                    <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                        Executive Summary
                                    </Typography>

                                    {aiSecurityReport.executiveSummary?.aiAnalysis ? (
                                        <Stack spacing={3}>
                                            {/* AI Analysis Results */}
                                            <AICard severity="info">
                                                <CardContent>
                                                    <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                                        <SmartToyIcon sx={{ color: '#2196f3' }} />
                                                        AI Security Assessment
                                                    </Typography>

                                                    {aiSecurityReport.executiveSummary.aiAnalysis.structuredAnalysis ? (
                                                        <Box>
                                                            <Typography variant="body1" paragraph>
                                                                <strong>Security Posture Score:</strong> {aiSecurityReport.executiveSummary.aiAnalysis.structuredAnalysis.securityPostureScore || 'N/A'}/10
                                                            </Typography>
                                                            <Typography variant="body1" paragraph>
                                                                <strong>Risk Level:</strong> {aiSecurityReport.executiveSummary.aiAnalysis.structuredAnalysis.riskLevel || 'Unknown'}
                                                            </Typography>

                                                            {aiSecurityReport.executiveSummary.aiAnalysis.structuredAnalysis.keyThreats && (
                                                                <Box sx={{ mt: 2 }}>
                                                                    <Typography variant="subtitle2" fontWeight={600} gutterBottom>
                                                                        Key Threats Identified:
                                                                    </Typography>
                                                                    <Typography variant="body2" color="text.secondary">
                                                                        {aiSecurityReport.executiveSummary.aiAnalysis.structuredAnalysis.keyThreats}
                                                                    </Typography>
                                                                </Box>
                                                            )}

                                                            {aiSecurityReport.executiveSummary.aiAnalysis.structuredAnalysis.immediateActions && (
                                                                <Box sx={{ mt: 2 }}>
                                                                    <Typography variant="subtitle2" fontWeight={600} gutterBottom>
                                                                        Immediate Actions Required:
                                                                    </Typography>
                                                                    <Typography variant="body2" color="text.secondary">
                                                                        {aiSecurityReport.executiveSummary.aiAnalysis.structuredAnalysis.immediateActions}
                                                                    </Typography>
                                                                </Box>
                                                            )}
                                                        </Box>
                                                    ) : (
                                                        <Box>
                                                            <Typography variant="body1" paragraph>
                                                                {aiSecurityReport.executiveSummary.aiAnalysis.fallbackAnalysis?.summary || 'AI analysis completed successfully.'}
                                                            </Typography>
                                                            <Typography variant="body2" color="text.secondary">
                                                                Risk Level: {aiSecurityReport.executiveSummary.aiAnalysis.fallbackAnalysis?.riskLevel || 'Unknown'}
                                                            </Typography>
                                                        </Box>
                                                    )}

                                                    <Typography variant="caption" display="block" sx={{ mt: 2 }}>
                                                        Generated by: {aiSecurityReport.executiveSummary.aiAnalysis.generatedBy || 'AI Analysis System'}
                                                    </Typography>
                                                </CardContent>
                                            </AICard>

                                            {/* Security Metrics Summary */}
                                            <GlassCard sx={{ p: 3 }}>
                                                <Typography variant="h6" gutterBottom>
                                                    Security Metrics Summary
                                                </Typography>
                                                <Grid container spacing={3}>
                                                    <Grid item xs={12} md={3}>
                                                        <Typography variant="body2" color="text.secondary">Security Events</Typography>
                                                        <Typography variant="h5" fontWeight={600}>{aiSecurityReport.executiveSummary.totalSecurityEvents || 0}</Typography>
                                                    </Grid>
                                                    <Grid item xs={12} md={3}>
                                                        <Typography variant="body2" color="text.secondary">Critical Events</Typography>
                                                        <Typography variant="h5" fontWeight={600} color="#f44336">{aiSecurityReport.executiveSummary.criticalEvents || 0}</Typography>
                                                    </Grid>
                                                    <Grid item xs={12} md={3}>
                                                        <Typography variant="body2" color="text.secondary">High Threat Alerts</Typography>
                                                        <Typography variant="h5" fontWeight={600} color="#FF914D">{aiSecurityReport.executiveSummary.highThreatAlerts || 0}</Typography>
                                                    </Grid>
                                                    <Grid item xs={12} md={3}>
                                                        <Typography variant="body2" color="text.secondary">Geographic Threats</Typography>
                                                        <Typography variant="h5" fontWeight={600}>{aiSecurityReport.executiveSummary.geographicThreatsCount || 0}</Typography>
                                                    </Grid>
                                                </Grid>
                                            </GlassCard>

                                            {/* Action Items */}
                                            {aiSecurityReport.actionItems && aiSecurityReport.actionItems.length > 0 && (
                                                <Box>
                                                    <Typography variant="h6" gutterBottom>
                                                        Priority Action Items
                                                    </Typography>
                                                    <Stack spacing={2}>
                                                        {aiSecurityReport.actionItems.map((item, index) => (
                                                            <Alert
                                                                key={index}
                                                                severity={item.priority === 'HIGH' ? 'warning' : 'info'}
                                                                sx={{ borderRadius: 2 }}
                                                            >
                                                                <AlertTitle>[{item.priority}] {item.title}</AlertTitle>
                                                                {item.description}
                                                            </Alert>
                                                        ))}
                                                    </Stack>
                                                </Box>
                                            )}
                                        </Stack>
                                    ) : (
                                        <Alert severity="warning" sx={{ borderRadius: 2 }}>
                                            <AlertTitle>Report Generation In Progress</AlertTitle>
                                            AI analysis is being processed. Please wait for the comprehensive report to be generated.
                                        </Alert>
                                    )}

                                    {/* Download Section */}
                                    <Box sx={{ mt: 4, textAlign: 'center' }}>
                                        <Button
                                            variant="outlined"
                                            startIcon={<DownloadIcon />}
                                            onClick={downloadAIReport}
                                            disabled={!aiSecurityReport}
                                            sx={{
                                                borderColor: '#FF914D',
                                                color: '#FF914D',
                                                '&:hover': {
                                                    borderColor: '#FF7043',
                                                    backgroundColor: alpha('#FF914D', 0.05)
                                                }
                                            }}
                                        >
                                            Download Full Report
                                        </Button>
                                    </Box>
                                </Box>
                            )}

                            {/* Report Generation Instructions */}
                            {!aiSecurityReport && (
                                <EmptyState
                                    title="Ready to Generate AI Security Report"
                                    description="Click 'Generate Report' to create a comprehensive AI-powered security analysis including threat assessment, risk analysis, and actionable recommendations."
                                    icon={DescriptionIcon}
                                />
                            )}
                        </Stack>
                    </TabPanel>

                    {/* Tab Panel 4: Compliance */}
                    <TabPanel value={tabValue} index={4}>
                        <Stack spacing={4}>
                            {/* Compliance Overview */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                    Compliance Framework Status
                                </Typography>

                                {complianceData?.frameworkStatuses && complianceData.frameworkStatuses.length > 0 ? (
                                    <Grid container spacing={3}>
                                        {complianceData.frameworkStatuses.map((framework, index) => (
                                            <Grid item xs={12} md={4} key={index}>
                                                <GlassCard sx={{ p: 3, textAlign: 'center' }}>
                                                    <Typography variant="h6" fontWeight={600} gutterBottom>
                                                        {framework.framework}
                                                    </Typography>
                                                    <Typography variant="h3" fontWeight={700} sx={{ color: '#FF914D', mb: 2 }}>
                                                        {framework.score?.toFixed(0) || 0}%
                                                    </Typography>
                                                    <Chip
                                                        label={framework.status}
                                                        color={framework.score >= 90 ? 'success' : framework.score >= 70 ? 'warning' : 'error'}
                                                        sx={{ mb: 2 }}
                                                    />
                                                    <Typography variant="body2" color="text.secondary">
                                                        {framework.criticalIssues || 0} critical issues
                                                    </Typography>
                                                    <Typography variant="caption" display="block" sx={{ mt: 1 }}>
                                                        Last assessment: {formatTimestamp(framework.lastAssessment)}
                                                    </Typography>
                                                </GlassCard>
                                            </Grid>
                                        ))}
                                    </Grid>
                                ) : (
                                    <EmptyState
                                        title="No Compliance Data Available"
                                        description="Compliance framework assessments are not currently available or configured."
                                        icon={GavelIcon}
                                    />
                                )}
                            </Box>

                            {/* Overall Compliance Metrics */}
                            {complianceData?.overallStatus && (
                                <Box>
                                    <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                        Overall Compliance Metrics
                                    </Typography>
                                    <Grid container spacing={3}>
                                        <Grid item xs={12} md={3}>
                                            <GlassCard sx={{ p: 3, textAlign: 'center' }}>
                                                <Typography variant="h3" fontWeight={700} color="#FF914D" sx={{ mb: 1 }}>
                                                    {(complianceData.overallStatus.averageComplianceScore || 0).toFixed(1)}%
                                                </Typography>
                                                <Typography variant="body1" color="text.secondary">
                                                    Average Score
                                                </Typography>
                                            </GlassCard>
                                        </Grid>
                                        <Grid item xs={12} md={3}>
                                            <GlassCard sx={{ p: 3, textAlign: 'center' }}>
                                                <Typography variant="h3" fontWeight={700} color="#f44336" sx={{ mb: 1 }}>
                                                    {complianceData.overallStatus.totalCriticalIssues || 0}
                                                </Typography>
                                                <Typography variant="body1" color="text.secondary">
                                                    Critical Issues
                                                </Typography>
                                            </GlassCard>
                                        </Grid>
                                        <Grid item xs={12} md={3}>
                                            <GlassCard sx={{ p: 3, textAlign: 'center' }}>
                                                <Typography variant="h3" fontWeight={700} color="#4caf50" sx={{ mb: 1 }}>
                                                    {complianceData.overallStatus.frameworkCount || 0}
                                                </Typography>
                                                <Typography variant="body1" color="text.secondary">
                                                    Frameworks
                                                </Typography>
                                            </GlassCard>
                                        </Grid>
                                        <Grid item xs={12} md={3}>
                                            <GlassCard sx={{ p: 3, textAlign: 'center' }}>
                                                <Chip
                                                    label={complianceData.overallStatus.overallStatus || 'UNKNOWN'}
                                                    color={
                                                        complianceData.overallStatus.overallStatus === 'EXCELLENT' ? 'success' :
                                                            complianceData.overallStatus.overallStatus === 'GOOD' ? 'warning' : 'error'
                                                    }
                                                    sx={{ fontSize: '1rem', p: 2, mb: 1 }}
                                                />
                                                <Typography variant="body1" color="text.secondary">
                                                    Overall Status
                                                </Typography>
                                            </GlassCard>
                                        </Grid>
                                    </Grid>
                                </Box>
                            )}

                            {/* Recent Compliance Events */}
                            {complianceData?.recentEvents && complianceData.recentEvents.length > 0 && (
                                <Box>
                                    <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                        Recent Compliance Events
                                    </Typography>
                                    <GlassCard sx={{ p: 0 }}>
                                        <TableContainer>
                                            <Table>
                                                <TableHead>
                                                    <TableRow>
                                                        <TableCell>Type</TableCell>
                                                        <TableCell>Severity</TableCell>
                                                        <TableCell>Title</TableCell>
                                                        <TableCell>Timestamp</TableCell>
                                                        <TableCell>Status</TableCell>
                                                    </TableRow>
                                                </TableHead>
                                                <TableBody>
                                                    {complianceData.recentEvents.slice(0, 10).map((event, index) => (
                                                        <TableRow key={index}>
                                                            <TableCell>{event.type}</TableCell>
                                                            <TableCell>
                                                                <Chip
                                                                    label={event.severity}
                                                                    size="small"
                                                                    color={event.severity === 'HIGH' ? 'error' : 'warning'}
                                                                />
                                                            </TableCell>
                                                            <TableCell>{event.title}</TableCell>
                                                            <TableCell>{formatTimestamp(event.timestamp)}</TableCell>
                                                            <TableCell>
                                                                <Chip
                                                                    label={event.status}
                                                                    size="small"
                                                                    variant="outlined"
                                                                />
                                                            </TableCell>
                                                        </TableRow>
                                                    ))}
                                                </TableBody>
                                            </Table>
                                        </TableContainer>
                                    </GlassCard>
                                </Box>
                            )}
                        </Stack>
                    </TabPanel>

                    {/* Tab Panel 5: Performance */}
                    <TabPanel value={tabValue} index={5}>
                        <Stack spacing={4}>
                            {/* Route Performance Overview */}
                            <Box>
                                <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                    Route Performance Overview
                                </Typography>

                                {dashboardData?.routeAnalytics && dashboardData.routeAnalytics.length > 0 ? (
                                    <Grid container spacing={3}>
                                        {dashboardData.routeAnalytics.map((route, index) => (
                                            <Grid item xs={12} md={6} key={route.routeId || index}>
                                                <GlassCard sx={{ p: 3 }}>
                                                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                                                        <Box>
                                                            <Typography variant="h6" fontWeight={600}>
                                                                {route.routeId}
                                                            </Typography>
                                                            <Typography variant="body2" color="text.secondary">
                                                                {route.totalRequests?.toLocaleString() || 0} requests • {((route.rejectionRate || 0) * 100).toFixed(1)}% rejected
                                                            </Typography>
                                                        </Box>
                                                        <Chip
                                                            label={route.securityStatus || 'NORMAL'}
                                                            size="small"
                                                            color={route.securityStatus === 'CRITICAL' ? 'error' :
                                                                route.securityStatus === 'HIGH_RISK' ? 'warning' : 'success'}
                                                        />
                                                    </Box>
                                                    <Grid container spacing={2}>
                                                        <Grid item xs={6}>
                                                            <Typography variant="body2" color="text.secondary">
                                                                Avg Response Time
                                                            </Typography>
                                                            <Typography variant="h6" color="#FF914D">
                                                                {route.averageResponseTime?.toFixed(0) || 0}ms
                                                            </Typography>
                                                        </Grid>
                                                        <Grid item xs={6}>
                                                            <Typography variant="body2" color="text.secondary">
                                                                Threat Score
                                                            </Typography>
                                                            <Typography variant="h6" color="#f44336">
                                                                {((route.threatScore || 0) * 100).toFixed(0)}%
                                                            </Typography>
                                                        </Grid>
                                                    </Grid>
                                                    {route.lastActivity && (
                                                        <Typography variant="caption" display="block" sx={{ mt: 2 }}>
                                                            Last activity: {formatTimestamp(route.lastActivity)}
                                                        </Typography>
                                                    )}
                                                </GlassCard>
                                            </Grid>
                                        ))}
                                    </Grid>
                                ) : (
                                    <EmptyState
                                        title="No Route Performance Data"
                                        description="No route-specific performance metrics are currently available."
                                        icon={SpeedIcon}
                                    />
                                )}
                            </Box>

                            {/* Performance Trends */}
                            {dashboardData?.performanceTrends && (
                                <Box>
                                    <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                        Performance Trends Analysis
                                    </Typography>
                                    <Grid container spacing={3}>
                                        <Grid item xs={12} md={6}>
                                            <GlassCard sx={{ p: 3 }}>
                                                <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                                                    Global Performance Trend
                                                </Typography>
                                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                                                    <Typography variant="h4" fontWeight={700} sx={{
                                                        color: dashboardData.performanceTrends.globalTrend === 'EXCELLENT' ? '#4caf50' :
                                                            dashboardData.performanceTrends.globalTrend === 'GOOD' ? '#FF914D' :
                                                                dashboardData.performanceTrends.globalTrend === 'FAIR' ? '#ff9800' : '#f44336'
                                                    }}>
                                                        {dashboardData.performanceTrends.globalTrend || 'UNKNOWN'}
                                                    </Typography>
                                                    {dashboardData.performanceTrends.globalTrend === 'EXCELLENT' && <TrendingUpIcon sx={{ color: '#4caf50', fontSize: 32 }} />}
                                                    {dashboardData.performanceTrends.globalTrend === 'POOR' && <TrendingDownIcon sx={{ color: '#f44336', fontSize: 32 }} />}
                                                </Box>
                                                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                                                    Overall system performance assessment
                                                </Typography>
                                            </GlassCard>
                                        </Grid>
                                        <Grid item xs={12} md={6}>
                                            <GlassCard sx={{ p: 3 }}>
                                                <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                                                    Route Trends Summary
                                                </Typography>
                                                {dashboardData.performanceTrends.routeTrends && Object.keys(dashboardData.performanceTrends.routeTrends).length > 0 ? (
                                                    <Box>
                                                        {Object.entries(dashboardData.performanceTrends.routeTrends).slice(0, 3).map(([routeId, trend], index) => (
                                                            <Box key={index} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                                                                <Typography variant="body2" color="text.secondary">
                                                                    {routeId}
                                                                </Typography>
                                                                <Chip
                                                                    label={trend}
                                                                    size="small"
                                                                    color={trend === 'IMPROVING' ? 'success' : trend === 'DEGRADING' ? 'error' : 'default'}
                                                                />
                                                            </Box>
                                                        ))}
                                                    </Box>
                                                ) : (
                                                    <Typography variant="body2" color="text.secondary">
                                                        No route trend data available
                                                    </Typography>
                                                )}
                                            </GlassCard>
                                        </Grid>
                                    </Grid>
                                </Box>
                            )}

                            {/* Alert Statistics Charts */}
                            {alertStatistics && (
                                <Box>
                                    <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                        Alert Performance Metrics
                                    </Typography>
                                    <Grid container spacing={3}>
                                        <Grid item xs={12} md={6}>
                                            <GlassCard sx={{ p: 3 }}>
                                                <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                                                    Alert Severity Distribution
                                                </Typography>
                                                {alertStatistics.severityDistribution && Object.keys(alertStatistics.severityDistribution).length > 0 ? (
                                                    <ResponsiveContainer width="100%" height={250}>
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
                                                    <EmptyState
                                                        title="No Alert Data"
                                                        description="No alert severity data available for analysis"
                                                        icon={WarningIcon}
                                                    />
                                                )}
                                            </GlassCard>
                                        </Grid>
                                        <Grid item xs={12} md={6}>
                                            <GlassCard sx={{ p: 3 }}>
                                                <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                                                    Alert Response Metrics
                                                </Typography>
                                                <Stack spacing={3}>
                                                    <Box>
                                                        <Typography variant="body2" color="text.secondary">
                                                            Total Open Alerts
                                                        </Typography>
                                                        <Typography variant="h4" fontWeight={700} color="#f44336">
                                                            {alertStatistics.totalOpenAlerts || 0}
                                                        </Typography>
                                                    </Box>
                                                    <Box>
                                                        <Typography variant="body2" color="text.secondary">
                                                            Resolution Rate
                                                        </Typography>
                                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                                                            <Typography variant="h4" fontWeight={700} color="#4caf50">
                                                                {(alertStatistics.resolutionRate || 0).toFixed(1)}%
                                                            </Typography>
                                                            <LinearProgress
                                                                variant="determinate"
                                                                value={alertStatistics.resolutionRate || 0}
                                                                sx={{
                                                                    flex: 1,
                                                                    height: 8,
                                                                    borderRadius: 4,
                                                                    backgroundColor: alpha('#4caf50', 0.2),
                                                                    '& .MuiLinearProgress-bar': {
                                                                        backgroundColor: '#4caf50',
                                                                        borderRadius: 4
                                                                    }
                                                                }}
                                                            />
                                                        </Box>
                                                    </Box>
                                                    <Box>
                                                        <Typography variant="body2" color="text.secondary">
                                                            Avg Response Time
                                                        </Typography>
                                                        <Typography variant="h4" fontWeight={700} color="#FF914D">
                                                            {(alertStatistics.avgResponseTimeMinutes || 0).toFixed(0)}m
                                                        </Typography>
                                                    </Box>
                                                </Stack>
                                            </GlassCard>
                                        </Grid>
                                    </Grid>
                                </Box>
                            )}

                            {/* Top Alert Sources */}
                            {alertStatistics?.topAlertSources && alertStatistics.topAlertSources.length > 0 && (
                                <Box>
                                    <Typography variant="h6" sx={{ mb: 3, fontWeight: 600, color: '#FF914D' }}>
                                        Top Alert Sources
                                    </Typography>
                                    <GlassCard sx={{ p: 0 }}>
                                        <TableContainer>
                                            <Table>
                                                <TableHead>
                                                    <TableRow>
                                                        <TableCell>IP Address</TableCell>
                                                        <TableCell align="right">Alert Count</TableCell>
                                                        <TableCell align="center">Risk Level</TableCell>
                                                    </TableRow>
                                                </TableHead>
                                                <TableBody>
                                                    {alertStatistics.topAlertSources.map((source, index) => (
                                                        <TableRow key={index}>
                                                            <TableCell component="th" scope="row">
                                                                {source.ip}
                                                            </TableCell>
                                                            <TableCell align="right">
                                                                <Typography variant="h6" fontWeight={600}>
                                                                    {source.alertCount}
                                                                </Typography>
                                                            </TableCell>
                                                            <TableCell align="center">
                                                                <Chip
                                                                    label={source.alertCount > 20 ? 'HIGH' : source.alertCount > 10 ? 'MEDIUM' : 'LOW'}
                                                                    size="small"
                                                                    color={source.alertCount > 20 ? 'error' : source.alertCount > 10 ? 'warning' : 'success'}
                                                                />
                                                            </TableCell>
                                                        </TableRow>
                                                    ))}
                                                </TableBody>
                                            </Table>
                                        </TableContainer>
                                    </GlassCard>
                                </Box>
                            )}
                        </Stack>
                    </TabPanel>
                </Box>
            </StyledCard>

            {/* Enhanced Alert Detail Dialog */}
            <Dialog
                open={alertDetailDialog.open}
                onClose={() => setAlertDetailDialog({ open: false, alert: null })}
                maxWidth="md"
                fullWidth
                PaperProps={{
                    sx: { borderRadius: 3 }
                }}
            >
                <DialogTitle>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <WarningIcon sx={{ color: getThreatLevelColor(alertDetailDialog.alert?.severity) }} />
                        <Typography variant="h6">Threat Alert Details</Typography>
                        <Chip
                            label={alertDetailDialog.alert?.severity || 'UNKNOWN'}
                            sx={{
                                backgroundColor: alpha(getThreatLevelColor(alertDetailDialog.alert?.severity), 0.1),
                                color: getThreatLevelColor(alertDetailDialog.alert?.severity),
                                fontWeight: 600,
                                ml: 'auto'
                            }}
                        />
                    </Box>
                </DialogTitle>
                <DialogContent>
                    {alertDetailDialog.alert && (
                        <Stack spacing={3} sx={{ mt: 1 }}>
                            <Box>
                                <Typography variant="h6" gutterBottom>{alertDetailDialog.alert.title}</Typography>
                                <Typography variant="body1" color="text.secondary" paragraph>
                                    {alertDetailDialog.alert.description}
                                </Typography>
                            </Box>

                            <Grid container spacing={3}>
                                <Grid item xs={12} md={6}>
                                    <Box>
                                        <Typography variant="subtitle2" color="text.secondary">Source IP</Typography>
                                        <Typography variant="h6">{alertDetailDialog.alert.sourceIp || 'Unknown'}</Typography>
                                    </Box>
                                </Grid>
                                <Grid item xs={12} md={6}>
                                    <Box>
                                        <Typography variant="subtitle2" color="text.secondary">Target Route</Typography>
                                        <Typography variant="h6">{alertDetailDialog.alert.targetRoute || 'N/A'}</Typography>
                                    </Box>
                                </Grid>
                                <Grid item xs={12} md={6}>
                                    <Box>
                                        <Typography variant="subtitle2" color="text.secondary">Threat Score</Typography>
                                        <Typography variant="h6" color="#f44336">
                                            {((alertDetailDialog.alert.threatScore || 0) * 100).toFixed(1)}%
                                        </Typography>
                                    </Box>
                                </Grid>
                                <Grid item xs={12} md={6}>
                                    <Box>
                                        <Typography variant="subtitle2" color="text.secondary">Confidence Level</Typography>
                                        <Typography variant="h6">
                                            {((alertDetailDialog.alert.confidence || 0) * 100).toFixed(1)}%
                                        </Typography>
                                    </Box>
                                </Grid>
                                <Grid item xs={12} md={6}>
                                    <Box>
                                        <Typography variant="subtitle2" color="text.secondary">First Detected</Typography>
                                        <Typography variant="body1">
                                            {formatTimestamp(alertDetailDialog.alert.firstSeen)}
                                        </Typography>
                                    </Box>
                                </Grid>
                                <Grid item xs={12} md={6}>
                                    <Box>
                                        <Typography variant="subtitle2" color="text.secondary">Last Seen</Typography>
                                        <Typography variant="body1">
                                            {formatTimestamp(alertDetailDialog.alert.lastSeen)}
                                        </Typography>
                                    </Box>
                                </Grid>
                            </Grid>

                            {alertDetailDialog.alert.eventCount && (
                                <Box>
                                    <Typography variant="subtitle2" color="text.secondary">Event Count</Typography>
                                    <Typography variant="h6">{alertDetailDialog.alert.eventCount}</Typography>
                                </Box>
                            )}
                        </Stack>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setAlertDetailDialog({ open: false, alert: null })}>
                        Close
                    </Button>
                    <Button
                        variant="contained"
                        sx={{ backgroundColor: '#FF914D', '&:hover': { backgroundColor: '#FF7043' } }}
                        onClick={() => {
                            // Here you would typically call an API to resolve the alert
                            showNotification(`Alert ${alertDetailDialog.alert?.id} marked as resolved`, 'success');
                            setAlertDetailDialog({ open: false, alert: null });
                        }}
                    >
                        Mark as Resolved
                    </Button>
                </DialogActions>
            </Dialog>

            {/* AI Security Report Dialog */}
            <Dialog
                open={reportDialog.open}
                onClose={() => setReportDialog({ open: false })}
                maxWidth="lg"
                fullWidth
                PaperProps={{
                    sx: {
                        borderRadius: 3,
                        maxHeight: '90vh'
                    }
                }}
            >
                <DialogTitle>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <DescriptionIcon sx={{ color: '#FF914D' }} />
                        <Typography variant="h6">AI Security Report Generation</Typography>
                        {reportDialog.loading && (
                            <CircularProgress size={20} sx={{ color: '#FF914D' }} />
                        )}
                    </Box>
                </DialogTitle>
                <DialogContent>
                    {reportDialog.loading ? (
                        <Box sx={{ textAlign: 'center', py: 4 }}>
                            <CircularProgress size={40} sx={{ color: '#FF914D', mb: 2 }} />
                            <Typography variant="h6">Generating AI Security Report...</Typography>
                            <Typography variant="body2" color="text.secondary">
                                Please wait while our AI analyzes your security data
                            </Typography>
                        </Box>
                    ) : (
                        <Box sx={{ py: 2 }}>
                            <Alert severity="info" sx={{ mb: 3, borderRadius: 2 }}>
                                <AlertTitle>Report Generated Successfully</AlertTitle>
                                Your AI-powered security report has been generated and is ready for download.
                            </Alert>
                            <Typography variant="body1" paragraph>
                                The comprehensive security analysis includes:
                            </Typography>
                            <ul>
                                <li>Executive Summary with AI Insights</li>
                                <li>Security Metrics and Trend Analysis</li>
                                <li>Threat Assessment and Risk Analysis</li>
                                <li>Compliance Status Report</li>
                                <li>Actionable Security Recommendations</li>
                                <li>Geographic Threat Intelligence</li>
                            </ul>
                        </Box>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setReportDialog({ open: false })}>
                        Close
                    </Button>
                    {!reportDialog.loading && (
                        <Button
                            variant="contained"
                            startIcon={<DownloadIcon />}
                            onClick={() => {
                                downloadAIReport();
                                setReportDialog({ open: false });
                            }}
                            sx={{ backgroundColor: '#FF914D', '&:hover': { backgroundColor: '#FF7043' } }}
                        >
                            Download Report
                        </Button>
                    )}
                </DialogActions>
            </Dialog>

            {/* Enhanced Notification Snackbar */}
            <Snackbar
                open={notification.open}
                autoHideDuration={6000}
                onClose={() => setNotification({ ...notification, open: false })}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
            >
                <Alert
                    onClose={() => setNotification({ ...notification, open: false })}
                    severity={notification.severity}
                    sx={{
                        borderRadius: 2,
                        minWidth: 300
                    }}
                >
                    {notification.message}
                </Alert>
            </Snackbar>

            {/* Floating Action Button for Quick Actions */}
            <Fab
                color="primary"
                sx={{
                    position: 'fixed',
                    bottom: 24,
                    right: 24,
                    backgroundColor: '#FF914D',
                    '&:hover': { backgroundColor: '#FF7043' }
                }}
                onClick={handleRefresh}
            >
                <RefreshIcon />
            </Fab>
        </Box>
    );
};

export default AnalyticsPage;