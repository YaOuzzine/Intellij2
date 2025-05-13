// gateway-admin-dashboard/src/pages/RateLimitPage.jsx
import React, { useState, useEffect, useRef } from 'react';
import {
  Typography, Box, Paper, Grid, Card, CardContent, CardHeader,
  Button, TextField, InputAdornment, CircularProgress, Alert,
  Dialog, DialogTitle, DialogContent, DialogActions,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Divider, Chip, IconButton, Tabs, Tab, FormControlLabel, Switch,
  Tooltip, Menu, MenuItem, Badge, Avatar, LinearProgress
} from '@mui/material';
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Settings as SettingsIcon,
  ArrowUpward as ArrowUpwardIcon,
  Warning as WarningIcon,
  Info as InfoIcon,
  MoreVert as MoreVertIcon,
  Timeline as TimelineIcon,
  Check as CheckIcon,
  Close as CloseIcon,
  BarChart as BarChartIcon,
  PieChart as PieChartIcon,
  ShowChart as LineChartIcon,
  Traffic as TrafficIcon,
  Block as BlockIcon,
  Speed as SpeedIcon
} from '@mui/icons-material';
import { styled } from '@mui/material/styles';
import { fetchGatewayRoutes, updateGatewayRoute } from '../services/dataService';
import apiClient from '../apiClient';
import {
  AreaChart, Area, BarChart, Bar, LineChart, Line, XAxis, YAxis,
  CartesianGrid, Tooltip as RechartsTooltip, Legend, ResponsiveContainer,
  PieChart, Pie, Cell, RadialBarChart, RadialBar, PolarAngleAxis
} from 'recharts';

// Custom styled components
const StyledCard = styled(Card)(({ theme }) => ({
  height: '100%',
  display: 'flex',
  flexDirection: 'column',
  transition: 'transform 0.3s, box-shadow 0.3s',
  borderRadius: '12px',
  '&:hover': {
    transform: 'translateY(-5px)',
    boxShadow: theme.shadows[6],
  },
  borderLeft: `4px solid ${theme.palette.primary.main}`
}));

const MetricValue = styled(Typography)(({ theme }) => ({
  fontSize: '2.5rem',
  fontWeight: 700,
  marginBottom: theme.spacing(1),
  background: theme.palette.mode === 'dark'
      ? 'linear-gradient(45deg, #FFFFFF, #E0E0E0)'
      : 'linear-gradient(45deg, #1976d2, #2196F3)',
  WebkitBackgroundClip: 'text',
  WebkitTextFillColor: 'transparent'
}));

const StatusBadge = styled(Badge)(({ theme, status }) => ({
  '& .MuiBadge-badge': {
    backgroundColor: getStatusColor(status),
    color: getStatusColor(status),
    boxShadow: `0 0 0 2px ${theme.palette.background.paper}`,
    '&::after': {
      position: 'absolute',
      top: 0,
      left: 0,
      width: '100%',
      height: '100%',
      borderRadius: '50%',
      animation: 'ripple 1.2s infinite ease-in-out',
      border: '1px solid currentColor',
      content: '""',
    },
  },
  '@keyframes ripple': {
    '0%': {
      transform: 'scale(.8)',
      opacity: 1,
    },
    '100%': {
      transform: 'scale(2.4)',
      opacity: 0,
    },
  },
}));

const PercentageChip = styled(Chip)(({ theme, value }) => {
  let color = theme.palette.success.main;
  let bgcolor = theme.palette.success.light + '30';

  if (value > 5) {
    color = theme.palette.warning.main;
    bgcolor = theme.palette.warning.light + '30';
  }
  if (value > 10) {
    color = theme.palette.error.main;
    bgcolor = theme.palette.error.light + '30';
  }

  return {
    backgroundColor: bgcolor,
    color: color,
    fontWeight: 'bold',
    borderRadius: '4px',
    padding: theme.spacing(0.5)
  };
});

const COLORS = [
  '#0088FE', '#00C49F', '#FFBB28', '#FF8042',
  '#8884d8', '#82ca9d', '#FF6B6B', '#4ECDC4',
  '#45B7D1', '#FFA07A'
];

const chartTheme = {
  textColor: '#6B7280',
  axisColor: '#E5E7EB',
  gridColor: '#F3F4F6',
  tooltipBg: '#1F2937',
  tooltipColor: '#F9FAFB'
};

// Helper functions
const getStatusColor = (status) => {
  switch (status) {
    case 'critical': return '#EF4444';
    case 'warning': return '#F59E0B';
    case 'healthy': return '#10B981';
    case 'disabled': return '#9CA3AF';
    default: return '#9CA3AF';
  }
};

const formatTimeWindow = (ms) => {
  if (ms < 1000) return `${ms}ms`;
  if (ms < 60000) return `${ms / 1000}s`;
  return `${ms / 60000}m`;
};

const formatDate = (timestamp) => {
  const date = new Date(timestamp);
  return `${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`;
};

// Main component
const RateLimitPage = () => {
  // State management (same as original)
  const [routes, setRoutes] = useState([]);
  const [rateLimitMetrics, setRateLimitMetrics] = useState({
    routes: [],
    summary: {
      totalRequests: 0,
      totalRejections: 0,
      rejectionRate: 0
    }
  });
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedRouteHistory, setSelectedRouteHistory] = useState(null);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [tabValue, setTabValue] = useState(0);
  const [error, setError] = useState(null);
  const [refreshInterval, setRefreshInterval] = useState(5000);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const refreshTimerRef = useRef(null);
  const lastUpdatedRef = useRef(Date.now());
  const [lastUpdated, setLastUpdated] = useState('Just now');
  const [configDialogOpen, setConfigDialogOpen] = useState(false);
  const [selectedRoute, setSelectedRoute] = useState(null);
  const [rateLimitValues, setRateLimitValues] = useState({
    maxRequests: 10,
    timeWindowMs: 60000
  });
  const [menuAnchorEl, setMenuAnchorEl] = useState(null);
  const [visualizationMode, setVisualizationMode] = useState('all');

  // Data loading and effects (same as original)
  useEffect(() => {
    loadData();
    return () => {
      if (refreshTimerRef.current) {
        clearInterval(refreshTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    if (refreshTimerRef.current) {
      clearInterval(refreshTimerRef.current);
    }
    if (autoRefresh) {
      refreshTimerRef.current = setInterval(() => {
        loadData(false);
      }, refreshInterval);
    }
    return () => {
      if (refreshTimerRef.current) {
        clearInterval(refreshTimerRef.current);
      }
    };
  }, [autoRefresh, refreshInterval]);

  useEffect(() => {
    const timer = setInterval(() => {
      const now = Date.now();
      const diff = Math.floor((now - lastUpdatedRef.current) / 1000);
      if (diff < 60) {
        setLastUpdated(`${diff} second${diff !== 1 ? 's' : ''} ago`);
      } else if (diff < 3600) {
        const minutes = Math.floor(diff / 60);
        setLastUpdated(`${minutes} minute${minutes !== 1 ? 's' : ''} ago`);
      } else {
        const hours = Math.floor(diff / 3600);
        setLastUpdated(`${hours} hour${hours !== 1 ? 's' : ''} ago`);
      }
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // Data loading functions (same as original)
  const loadData = async (showLoadingIndicator = true) => {
    if (showLoadingIndicator) {
      setLoading(true);
    }
    setError(null);
    try {
      const routesData = await fetchGatewayRoutes();
      setRoutes(routesData);
      const metricsResponse = await apiClient.get('/metrics/ratelimit');
      setRateLimitMetrics(metricsResponse.data);
      lastUpdatedRef.current = Date.now();
      setLastUpdated('Just now');
      if (selectedRouteHistory) {
        await loadRouteHistory(selectedRouteHistory.routeId);
      }
    } catch (err) {
      console.error('Error loading data:', err);
      setError('Failed to load data. Please try again.');
    } finally {
      setLoading(showLoadingIndicator);
    }
  };

  const loadRouteHistory = async (routeId) => {
    setHistoryLoading(true);
    try {
      const response = await apiClient.get(`/metrics/ratelimit/${routeId}/history`);
      setSelectedRouteHistory(response.data);
    } catch (err) {
      console.error('Error loading route history:', err);
    } finally {
      setHistoryLoading(false);
    }
  };

  // Event handlers (same as original)
  const handleSearchChange = (e) => setSearchTerm(e.target.value);
  const handleOpenConfigDialog = (route) => {
    setSelectedRoute(route);
    setRateLimitValues({
      maxRequests: route.rateLimit?.maxRequests || 10,
      timeWindowMs: route.rateLimit?.timeWindowMs || 60000
    });
    setConfigDialogOpen(true);
  };
  const handleToggleRateLimit = async (route) => {
    try {
      const routeData = routes.find(r => r.id === route.id);
      if (!routeData) {
        console.error('Could not find route data for ID:', route.id);
        return;
      }
      const updatedRoute = {
        ...routeData,
        withRateLimit: !routeData.withRateLimit
      };
      if (updatedRoute.withRateLimit && !updatedRoute.rateLimit) {
        updatedRoute.rateLimit = { maxRequests: 10, timeWindowMs: 60000 };
      }
      await updateGatewayRoute(route.id, updatedRoute);
      await loadData();
    } catch (error) {
      console.error('Error toggling rate limit:', error);
      setError('Failed to update rate limit settings');
    }
  };
  const handleSaveRateLimit = async () => {
    try {
      const routeData = routes.find(r => r.id === selectedRoute.id);
      if (!routeData) {
        console.error('Could not find route data for ID:', selectedRoute.id);
        return;
      }
      const updatedRoute = {
        ...routeData,
        withRateLimit: true,
        rateLimit: {
          ...routeData.rateLimit,
          maxRequests: Number(rateLimitValues.maxRequests),
          timeWindowMs: Number(rateLimitValues.timeWindowMs)
        }
      };
      await updateGatewayRoute(selectedRoute.id, updatedRoute);
      setConfigDialogOpen(false);
      await loadData();
    } catch (error) {
      console.error('Error updating rate limit:', error);
      setError('Failed to update rate limit');
    }
  };
  const handleMenuOpen = (event) => setMenuAnchorEl(event.currentTarget);
  const handleMenuClose = () => setMenuAnchorEl(null);
  const handleVisualizationChange = (mode) => {
    setVisualizationMode(mode);
    handleMenuClose();
  };
  const handleViewHistory = (routeId) => {
    loadRouteHistory(routeId);
    setTabValue(1);
  };
  const handleTabChange = (event, newValue) => setTabValue(newValue);

  // Helper functions
  const getRouteStatus = (route) => {
    if (!route.withRateLimit) return 'disabled';
    if (route.rejectionRate > 10) return 'critical';
    if (route.rejectionRate > 5) return 'warning';
    return 'healthy';
  };

  const filteredRoutes = rateLimitMetrics.routes?.filter(route =>
      route.path.toLowerCase().includes(searchTerm.toLowerCase()) ||
      route.routeId.toLowerCase().includes(searchTerm.toLowerCase())
  ) || [];

  // Enhanced chart data preparation
  const getAggregateChartData = () => {
    let routesToShow = filteredRoutes;
    if (visualizationMode !== 'all') {
      routesToShow = filteredRoutes.filter(route => {
        const status = getRouteStatus(route);
        return visualizationMode === status;
      });
    }

    const topRoutes = [...routesToShow]
        .sort((a, b) => (b.totalRequests || 0) - (a.totalRequests || 0))
        .slice(0, 5);

    const pieData = topRoutes.map(route => ({
      name: route.routeId,
      value: route.totalRequests || 0,
      status: getRouteStatus(route)
    }));

    const barData = topRoutes.map(route => ({
      name: route.routeId,
      rejectionRate: route.rejectionRate || 0,
      requests: route.totalRequests || 0,
      rejections: route.totalRejections || 0
    }));

    return { pieData, barData };
  };

  // Custom Tooltip components
  const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
      return (
          <Paper sx={{
            p: 2,
            bgcolor: 'background.paper',
            boxShadow: 3,
            borderLeft: `4px solid ${payload[0].fill}`
          }}>
            <Typography variant="subtitle2" fontWeight="bold" gutterBottom>
              {label}
            </Typography>
            {payload.map((item, index) => (
                <Box key={index} sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <Box sx={{
                    width: 12,
                    height: 12,
                    bgcolor: item.fill,
                    borderRadius: '2px',
                    mr: 1
                  }} />
                  <Typography variant="body2">
                    {item.name}: <strong>{item.value}</strong>
                  </Typography>
                </Box>
            ))}
          </Paper>
      );
    }
    return null;
  };

  const RejectionRateTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      return (
          <Paper sx={{ p: 2, bgcolor: 'background.paper', boxShadow: 3 }}>
            <Typography variant="subtitle2" fontWeight="bold" gutterBottom>
              {label}
            </Typography>
            <Typography variant="body2">
              Requests: <strong>{data.requests}</strong>
            </Typography>
            <Typography variant="body2">
              Rejections: <strong>{data.rejections}</strong>
            </Typography>
            <Typography variant="body2">
              Rejection Rate: <strong>{data.rejectionRate.toFixed(2)}%</strong>
            </Typography>
          </Paper>
      );
    }
    return null;
  };

  // Enhanced chart rendering
  const renderCharts = () => {
    const { pieData, barData } = getAggregateChartData();

    return (
        <Grid container spacing={3}>
          {/* Traffic Distribution Radial Chart */}
          <Grid item xs={12} md={4}>
            <StyledCard>
              <CardHeader
                  title="Traffic Distribution"
                  subheader="Top 5 routes by request volume"
                  avatar={<PieChartIcon color="primary" />}
              />
              <CardContent sx={{ flexGrow: 1, minHeight: 300 }}>
                <ResponsiveContainer width="100%" height={300}>
                  <RadialBarChart
                      innerRadius="20%"
                      outerRadius="100%"
                      data={pieData.map(item => ({
                        ...item,
                        fill: getStatusColor(item.status)
                      }))}
                      startAngle={90}
                      endAngle={-270}
                  >
                    <PolarAngleAxis
                        type="number"
                        domain={[0, Math.max(...pieData.map(d => d.value))]}
                        angleAxisId={0}
                        tick={false}
                    />
                    <RadialBar
                        background
                        dataKey="value"
                        cornerRadius={4}
                        animationDuration={1500}
                    />
                    <RechartsTooltip content={<CustomTooltip />} />
                    <Legend
                        formatter={(value, entry, index) => (
                            <span style={{ color: chartTheme.textColor }}>
                        {pieData[index].name}
                      </span>
                        )}
                    />
                  </RadialBarChart>
                </ResponsiveContainer>
              </CardContent>
            </StyledCard>
          </Grid>

          {/* Rejection Rates Bar Chart */}
          <Grid item xs={12} md={4}>
            <StyledCard>
              <CardHeader
                  title="Rejection Rates"
                  subheader="Percentage of rejected requests"
                  avatar={<BarChartIcon color="primary" />}
              />
              <CardContent sx={{ flexGrow: 1, minHeight: 300 }}>
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={barData}>
                    <CartesianGrid strokeDasharray="3 3" stroke={chartTheme.gridColor} />
                    <XAxis
                        dataKey="name"
                        tick={{ fill: chartTheme.textColor }}
                        axisLine={{ stroke: chartTheme.axisColor }}
                    />
                    <YAxis
                        unit="%"
                        tick={{ fill: chartTheme.textColor }}
                        axisLine={{ stroke: chartTheme.axisColor }}
                    />
                    <RechartsTooltip content={<RejectionRateTooltip />} />
                    <Bar
                        dataKey="rejectionRate"
                        name="Rejection Rate"
                        radius={[4, 4, 0, 0]}
                        animationDuration={1500}
                    >
                      {barData.map((entry, index) => (
                          <Cell
                              key={`cell-${index}`}
                              fill={getStatusColor(getRouteStatus(entry))}
                          />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </CardContent>
            </StyledCard>
          </Grid>

          {/* Status Distribution Pie Chart - FIXED VERSION */}
          <Grid item xs={12} md={4}>
            <StyledCard>
              <CardHeader
                  title="Status Overview"
                  subheader="Distribution of route statuses"
                  avatar={<TrafficIcon color="primary" />}
              />
              <CardContent sx={{ flexGrow: 1, minHeight: 300 }}>
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie
                        data={[
                          { name: 'Healthy', value: filteredRoutes.filter(r => getRouteStatus(r) === 'healthy').length, id: 'healthy' },
                          { name: 'Warning', value: filteredRoutes.filter(r => getRouteStatus(r) === 'warning').length, id: 'warning' },
                          { name: 'Critical', value: filteredRoutes.filter(r => getRouteStatus(r) === 'critical').length, id: 'critical' },
                          { name: 'Disabled', value: filteredRoutes.filter(r => getRouteStatus(r) === 'disabled').length, id: 'disabled' }
                        ]}
                        cx="50%"
                        cy="50%"
                        labelLine={false}
                        outerRadius={80}
                        innerRadius={40}
                        paddingAngle={5}
                        dataKey="value"
                        nameKey="id"
                        label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                    >
                      {[
                        { status: 'healthy', key: 'cell-healthy' },
                        { status: 'warning', key: 'cell-warning' },
                        { status: 'critical', key: 'cell-critical' },
                        { status: 'disabled', key: 'cell-disabled' }
                      ].map(item => (
                          <Cell key={item.key} fill={getStatusColor(item.status)} />
                      ))}
                    </Pie>
                    <RechartsTooltip
                        formatter={(value, name, props) => [value, props.payload.name]}
                        contentStyle={{
                          backgroundColor: chartTheme.tooltipBg,
                          border: 'none',
                          borderRadius: '8px'
                        }}
                        itemStyle={{ color: chartTheme.tooltipColor }}
                    />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              </CardContent>
            </StyledCard>
          </Grid>
        </Grid>
    );
  };

  // Enhanced route history view
  const renderRouteHistory = () => {
    if (!selectedRouteHistory) {
      return (
          <Box sx={{
            textAlign: 'center',
            py: 10,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center'
          }}>
            <TimelineIcon sx={{ fontSize: 60, color: 'text.disabled', mb: 2 }} />
            <Typography variant="subtitle1" color="textSecondary">
              Select a route to view detailed history
            </Typography>
            <Typography variant="body2" color="textSecondary" sx={{ mt: 1 }}>
              Click on a route's timeline icon to view its historical data
            </Typography>
          </Box>
      );
    }

    const { routeId, path, history, maxRequests, timeWindowMs } = selectedRouteHistory;
    const historyData = history.map(point => ({
      timestamp: point.timestamp,
      time: formatDate(point.timestamp),
      requests: point.requests,
      rejections: point.rejections,
      accepted: point.requests - point.rejections,
      rejectionRate: point.requests > 0 ? (point.rejections / point.requests) * 100 : 0
    }));

    const currentRate = historyData[historyData.length - 1]?.rejectionRate || 0;
    const status = currentRate > 10 ? 'critical' : currentRate > 5 ? 'warning' : 'healthy';

    return (
        <Box>
          <Box sx={{
            mb: 3,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            bgcolor: 'background.paper',
            p: 3,
            borderRadius: '12px',
            boxShadow: 1
          }}>
            <Box>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                <StatusBadge
                    overlap="circular"
                    anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                    variant="dot"
                    status={status}
                    sx={{ mr: 2 }}
                >
                  <Avatar sx={{ bgcolor: getStatusColor(status) }}>
                    {status === 'healthy' ? <CheckIcon /> :
                        status === 'warning' ? <WarningIcon /> : <BlockIcon />}
                  </Avatar>
                </StatusBadge>
                <Box>
                  <Typography variant="h6" fontWeight="bold">
                    {path}
                  </Typography>
                  <Typography variant="body2" color="textSecondary">
                    Route ID: {routeId}
                  </Typography>
                </Box>
              </Box>
              {maxRequests && (
                  <Box sx={{ mt: 2 }}>
                    <Typography variant="body2" sx={{ display: 'flex', alignItems: 'center' }}>
                      <SpeedIcon color="action" sx={{ mr: 1, fontSize: '1rem' }} />
                      Current limit: {maxRequests} requests per {formatTimeWindow(timeWindowMs)}
                    </Typography>
                  </Box>
              )}
            </Box>
            <Button
                startIcon={<RefreshIcon />}
                variant="contained"
                onClick={() => loadRouteHistory(routeId)}
                disabled={historyLoading}
                sx={{ borderRadius: '8px' }}
            >
              {historyLoading ? 'Loading...' : 'Refresh Data'}
            </Button>
          </Box>

          {/* Request vs Rejection Over Time */}
          <StyledCard sx={{ mb: 3 }}>
            <CardHeader
                title="Traffic Over Time"
                avatar={<LineChartIcon color="primary" />}
            />
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={historyData}>
                  <defs>
                    <linearGradient id="acceptedGradient" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#10B981" stopOpacity={0.8}/>
                      <stop offset="95%" stopColor="#10B981" stopOpacity={0}/>
                    </linearGradient>
                    <linearGradient id="rejectedGradient" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#EF4444" stopOpacity={0.8}/>
                      <stop offset="95%" stopColor="#EF4444" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke={chartTheme.gridColor} />
                  <XAxis
                      dataKey="time"
                      tick={{ fill: chartTheme.textColor }}
                      axisLine={{ stroke: chartTheme.axisColor }}
                  />
                  <YAxis
                      tick={{ fill: chartTheme.textColor }}
                      axisLine={{ stroke: chartTheme.axisColor }}
                  />
                  <RechartsTooltip
                      content={<CustomTooltip />}
                      contentStyle={{
                        backgroundColor: chartTheme.tooltipBg,
                        border: 'none',
                        borderRadius: '8px'
                      }}
                      itemStyle={{ color: chartTheme.tooltipColor }}
                  />
                  <Legend />
                  <Area
                      type="monotone"
                      dataKey="accepted"
                      stackId="1"
                      stroke="#10B981"
                      fillOpacity={1}
                      fill="url(#acceptedGradient)"
                      name="Accepted"
                  />
                  <Area
                      type="monotone"
                      dataKey="rejections"
                      stackId="1"
                      stroke="#EF4444"
                      fillOpacity={1}
                      fill="url(#rejectedGradient)"
                      name="Rejected"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </CardContent>
          </StyledCard>

          {/* Rejection Rate Over Time */}
          <StyledCard>
            <CardHeader
                title="Rejection Rate Trend"
                avatar={<LineChartIcon color="primary" />}
                action={
                  <PercentageChip
                      value={currentRate}
                      label={`Current: ${currentRate.toFixed(2)}%`}
                      size="medium"
                      sx={{ mr: 2 }}
                  />
                }
            />
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={historyData}>
                  <CartesianGrid strokeDasharray="3 3" stroke={chartTheme.gridColor} />
                  <XAxis
                      dataKey="time"
                      tick={{ fill: chartTheme.textColor }}
                      axisLine={{ stroke: chartTheme.axisColor }}
                  />
                  <YAxis
                      unit="%"
                      tick={{ fill: chartTheme.textColor }}
                      axisLine={{ stroke: chartTheme.axisColor }}
                  />
                  <RechartsTooltip
                      formatter={(value) => [`${value.toFixed(2)}%`, 'Rejection Rate']}
                      labelFormatter={(label) => `Time: ${label}`}
                      contentStyle={{
                        backgroundColor: chartTheme.tooltipBg,
                        border: 'none',
                        borderRadius: '8px'
                      }}
                      itemStyle={{ color: chartTheme.tooltipColor }}
                  />
                  <Line
                      type="monotone"
                      dataKey="rejectionRate"
                      stroke="#F59E0B"
                      strokeWidth={2}
                      dot={{ r: 2 }}
                      activeDot={{ r: 6, strokeWidth: 2 }}
                      isAnimationActive={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            </CardContent>
          </StyledCard>
        </Box>
    );
  };

  return (
      <Box sx={{ p: 3 }}>
        {/* Header and controls */}
        <Box sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          mb: 3,
          flexWrap: 'wrap',
          gap: 2
        }}>
          <Box>
            <Typography variant="h4" component="h1" fontWeight="bold">
              Rate Limit Dashboard
            </Typography>
            <Typography variant="caption" color="textSecondary">
              Monitor and manage API rate limiting configurations
            </Typography>
          </Box>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <FormControlLabel
                control={
                  <Switch
                      checked={autoRefresh}
                      onChange={(e) => setAutoRefresh(e.target.checked)}
                      color="primary"
                      size="small"
                  />
                }
                label="Auto-refresh"
                labelPlacement="start"
                sx={{ mr: 0 }}
            />

            <Button
                variant="contained"
                startIcon={<RefreshIcon />}
                onClick={() => loadData()}
                disabled={loading}
                sx={{ borderRadius: '8px' }}
            >
              Refresh
            </Button>

            <Button
                variant="outlined"
                startIcon={<BarChartIcon />}
                onClick={handleMenuOpen}
                sx={{ borderRadius: '8px' }}
            >
              Filter
            </Button>

            <Menu
                anchorEl={menuAnchorEl}
                open={Boolean(menuAnchorEl)}
                onClose={handleMenuClose}
            >
              <MenuItem
                  onClick={() => handleVisualizationChange('all')}
                  selected={visualizationMode === 'all'}
                  sx={{ minWidth: '180px' }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <Box sx={{ width: 12, height: 12, bgcolor: 'text.primary', borderRadius: '2px', mr: 1.5 }} />
                  All Routes
                </Box>
              </MenuItem>
              <MenuItem
                  onClick={() => handleVisualizationChange('healthy')}
                  selected={visualizationMode === 'healthy'}
                  sx={{ minWidth: '180px' }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <Box sx={{ width: 12, height: 12, bgcolor: getStatusColor('healthy'), borderRadius: '2px', mr: 1.5 }} />
                  Healthy
                </Box>
              </MenuItem>
              <MenuItem
                  onClick={() => handleVisualizationChange('warning')}
                  selected={visualizationMode === 'warning'}
                  sx={{ minWidth: '180px' }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <Box sx={{ width: 12, height: 12, bgcolor: getStatusColor('warning'), borderRadius: '2px', mr: 1.5 }} />
                  Warning
                </Box>
              </MenuItem>
              <MenuItem
                  onClick={() => handleVisualizationChange('critical')}
                  selected={visualizationMode === 'critical'}
                  sx={{ minWidth: '180px' }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <Box sx={{ width: 12, height: 12, bgcolor: getStatusColor('critical'), borderRadius: '2px', mr: 1.5 }} />
                  Critical
                </Box>
              </MenuItem>
              <MenuItem
                  onClick={() => handleVisualizationChange('disabled')}
                  selected={visualizationMode === 'disabled'}
                  sx={{ minWidth: '180px' }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <Box sx={{ width: 12, height: 12, bgcolor: getStatusColor('disabled'), borderRadius: '2px', mr: 1.5 }} />
                  Disabled
                </Box>
              </MenuItem>
            </Menu>
          </Box>
        </Box>

        <Box sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          mb: 3,
          flexWrap: 'wrap',
          gap: 2
        }}>
          <Typography variant="caption" color="textSecondary">
            Last updated: {lastUpdated}
          </Typography>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Box sx={{
              width: 8,
              height: 8,
              borderRadius: '50%',
              bgcolor: getStatusColor('healthy')
            }} />
            <Typography variant="caption" color="textSecondary">Healthy</Typography>

            <Box sx={{
              width: 8,
              height: 8,
              borderRadius: '50%',
              bgcolor: getStatusColor('warning'),
              ml: 1
            }} />
            <Typography variant="caption" color="textSecondary">Warning</Typography>

            <Box sx={{
              width: 8,
              height: 8,
              borderRadius: '50%',
              bgcolor: getStatusColor('critical'),
              ml: 1
            }} />
            <Typography variant="caption" color="textSecondary">Critical</Typography>

            <Box sx={{
              width: 8,
              height: 8,
              borderRadius: '50%',
              bgcolor: getStatusColor('disabled'),
              ml: 1
            }} />
            <Typography variant="caption" color="textSecondary">Disabled</Typography>
          </Box>
        </Box>

        {/* Error message */}
        {error && (
            <Alert
                severity="error"
                sx={{ mb: 3, borderRadius: '8px' }}
                onClose={() => setError(null)}
            >
              {error}
            </Alert>
        )}

        {/* Summary metrics */}
        <Grid container spacing={3} sx={{ mb: 3 }}>
          <Grid item xs={12} md={4}>
            <StyledCard>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <TrafficIcon color="primary" sx={{ mr: 1 }} />
                  <Typography variant="subtitle1" color="textSecondary">
                    Total Traffic
                  </Typography>
                </Box>
                <MetricValue>
                  {rateLimitMetrics.summary?.totalRequests.toLocaleString() || '0'}
                </MetricValue>
                <Typography variant="body2" color="textSecondary">
                  Total requests processed
                </Typography>
                <LinearProgress
                    variant="determinate"
                    value={100}
                    sx={{
                      mt: 2,
                      height: 4,
                      borderRadius: 2,
                      bgcolor: 'action.selected',
                      '& .MuiLinearProgress-bar': {
                        bgcolor: 'primary.main'
                      }
                    }}
                />
              </CardContent>
            </StyledCard>
          </Grid>

          <Grid item xs={12} md={4}>
            <StyledCard>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <BlockIcon color="primary" sx={{ mr: 1 }} />
                  <Typography variant="subtitle1" color="textSecondary">
                    Rate Limited
                  </Typography>
                </Box>
                <MetricValue>
                  {rateLimitMetrics.summary?.totalRejections.toLocaleString() || '0'}
                </MetricValue>
                <Typography variant="body2" color="textSecondary">
                  Requests rejected due to rate limits
                </Typography>
                <LinearProgress
                    variant="determinate"
                    value={Math.min(100, rateLimitMetrics.summary?.rejectionRate || 0)}
                    sx={{
                      mt: 2,
                      height: 4,
                      borderRadius: 2,
                      bgcolor: 'action.selected',
                      '& .MuiLinearProgress-bar': {
                        bgcolor: 'error.main'
                      }
                    }}
                />
              </CardContent>
            </StyledCard>
          </Grid>

          <Grid item xs={12} md={4}>
            <StyledCard>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <SpeedIcon color="primary" sx={{ mr: 1 }} />
                  <Typography variant="subtitle1" color="textSecondary">
                    Rejection Rate
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <MetricValue>
                    {rateLimitMetrics.summary?.rejectionRate.toLocaleString() || '0'}%
                  </MetricValue>

                  {rateLimitMetrics.summary?.rejectionRate > 0 && (
                      <PercentageChip
                          value={rateLimitMetrics.summary?.rejectionRate}
                          label={
                            rateLimitMetrics.summary?.rejectionRate > 10
                                ? 'High'
                                : rateLimitMetrics.summary?.rejectionRate > 5
                                    ? 'Moderate'
                                    : 'Low'
                          }
                          size="small"
                          sx={{ ml: 1 }}
                      />
                  )}
                </Box>
                <Typography variant="body2" color="textSecondary">
                  Percentage of rejected requests
                </Typography>
                <LinearProgress
                    variant="determinate"
                    value={Math.min(100, rateLimitMetrics.summary?.rejectionRate || 0)}
                    sx={{
                      mt: 2,
                      height: 4,
                      borderRadius: 2,
                      bgcolor: 'action.selected',
                      '& .MuiLinearProgress-bar': {
                        bgcolor: rateLimitMetrics.summary?.rejectionRate > 10
                            ? 'error.main'
                            : rateLimitMetrics.summary?.rejectionRate > 5
                                ? 'warning.main'
                                : 'success.main'
                      }
                    }}
                />
              </CardContent>
            </StyledCard>
          </Grid>
        </Grid>

        {/* Tabs for Dashboard and Route Details */}
        <Paper sx={{ mb: 3, borderRadius: '12px', overflow: 'hidden' }}>
          <Tabs
              value={tabValue}
              onChange={handleTabChange}
              indicatorColor="primary"
              textColor="primary"
              variant="fullWidth"
          >
            <Tab
                label="Dashboard Overview"
                icon={<BarChartIcon fontSize="small" />}
                iconPosition="start"
            />
            <Tab
                label="Route Analytics"
                icon={<TimelineIcon fontSize="small" />}
                iconPosition="start"
            />
          </Tabs>
        </Paper>

        {/* Dashboard Tab */}
        {tabValue === 0 && (
            <Box>
              {/* Search bar */}
              <Box sx={{ mb: 3 }}>
                <TextField
                    placeholder="Search routes by path or ID..."
                    variant="outlined"
                    size="small"
                    fullWidth
                    value={searchTerm}
                    onChange={handleSearchChange}
                    InputProps={{
                      startAdornment: (
                          <InputAdornment position="start">
                            <SearchIcon />
                          </InputAdornment>
                      ),
                      sx: { borderRadius: '8px' }
                    }}
                />
              </Box>

              {/* Charts */}
              {renderCharts()}

              {/* Routes Table */}
              <TableContainer
                  component={Paper}
                  sx={{
                    mt: 3,
                    borderRadius: '12px',
                    boxShadow: 2
                  }}
              >
                <Table size="small">
                  <TableHead>
                    <TableRow sx={{ bgcolor: 'action.hover' }}>
                      <TableCell sx={{ fontWeight: 'bold' }}>Status</TableCell>
                      <TableCell sx={{ fontWeight: 'bold' }}>Route Path</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 'bold' }}>Limit</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 'bold' }}>Requests</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 'bold' }}>Rejected</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 'bold' }}>Rejection Rate</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 'bold' }}>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {loading ? (
                        <TableRow>
                          <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                            <CircularProgress size={24} />
                            <Typography variant="body2" sx={{ mt: 1 }}>
                              Loading route data...
                            </Typography>
                          </TableCell>
                        </TableRow>
                    ) : filteredRoutes.length === 0 ? (
                        <TableRow>
                          <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                            <Box sx={{
                              display: 'flex',
                              flexDirection: 'column',
                              alignItems: 'center',
                              color: 'text.secondary'
                            }}>
                              <SearchIcon sx={{ fontSize: 48, mb: 1 }} />
                              <Typography variant="body1">
                                No matching routes found
                              </Typography>
                              <Typography variant="body2">
                                Try adjusting your search query
                              </Typography>
                            </Box>
                          </TableCell>
                        </TableRow>
                    ) : (
                        filteredRoutes.map((route) => {
                          const status = getRouteStatus(route);

                          return (
                              <TableRow
                                  key={route.id}
                                  hover
                                  sx={{
                                    '&:last-child td': { borderBottom: 0 },
                                    '&:hover': {
                                      bgcolor: 'action.hover'
                                    }
                                  }}
                              >
                                <TableCell>
                                  <Box sx={{ display: 'flex', alignItems: 'center' }}>
                                    <StatusBadge
                                        overlap="circular"
                                        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                                        variant="dot"
                                        status={status}
                                        sx={{ mr: 1 }}
                                    >
                                      <Box sx={{
                                        width: 24,
                                        height: 24,
                                        borderRadius: '4px',
                                        bgcolor: `${getStatusColor(status)}20`,
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'center'
                                      }}>
                                        {status === 'disabled' ? <CloseIcon fontSize="small" sx={{ color: getStatusColor(status) }} /> :
                                            status === 'critical' ? <WarningIcon fontSize="small" sx={{ color: getStatusColor(status) }} /> :
                                                status === 'warning' ? <WarningIcon fontSize="small" sx={{ color: getStatusColor(status) }} /> :
                                                    <CheckIcon fontSize="small" sx={{ color: getStatusColor(status) }} />}
                                      </Box>
                                    </StatusBadge>
                                    <Typography variant="body2">
                                      {status === 'disabled' ? 'Disabled' :
                                          status === 'critical' ? 'Critical' :
                                              status === 'warning' ? 'Warning' : 'Healthy'}
                                    </Typography>
                                  </Box>
                                </TableCell>
                                <TableCell>
                                  <Tooltip title={`Route ID: ${route.routeId}`}>
                                    <Box component="span" sx={{ cursor: 'help' }}>
                                      <Typography variant="body2" noWrap>
                                        {route.path}
                                      </Typography>
                                    </Box>
                                  </Tooltip>
                                </TableCell>
                                <TableCell align="right">
                                  {route.withRateLimit && route.rateLimit ?
                                      <Chip
                                          label={`${route.rateLimit.maxRequests}/${formatTimeWindow(route.rateLimit.timeWindowMs)}`}
                                          size="small"
                                          variant="outlined"
                                          sx={{ borderRadius: '4px' }}
                                      /> :
                                      <Chip
                                          label="N/A"
                                          size="small"
                                          variant="outlined"
                                          sx={{ borderRadius: '4px' }}
                                      />
                                  }
                                </TableCell>
                                <TableCell align="right">
                                  <Typography variant="body2">
                                    {route.totalRequests?.toLocaleString() || '0'}
                                  </Typography>
                                </TableCell>
                                <TableCell align="right">
                                  <Typography variant="body2" color="error.main">
                                    {route.totalRejections?.toLocaleString() || '0'}
                                  </Typography>
                                </TableCell>
                                <TableCell align="right">
                                  <PercentageChip
                                      value={route.rejectionRate || 0}
                                      label={`${route.rejectionRate?.toFixed(2) || 0}%`}
                                      size="small"
                                  />
                                </TableCell>
                                <TableCell align="right">
                                  <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
                                    <Tooltip title="View detailed analytics">
                                      <IconButton
                                          size="small"
                                          onClick={() => handleViewHistory(route.routeId)}
                                          sx={{
                                            bgcolor: 'primary.light',
                                            '&:hover': { bgcolor: 'primary.main', color: 'white' }
                                          }}
                                      >
                                        <TimelineIcon fontSize="small" />
                                      </IconButton>
                                    </Tooltip>

                                    <Tooltip title={route.withRateLimit ? "Configure rate limit" : "Enable rate limiting"}>
                                      <IconButton
                                          size="small"
                                          onClick={() => route.withRateLimit ?
                                              handleOpenConfigDialog(route) :
                                              handleToggleRateLimit(route)}
                                          sx={{
                                            bgcolor: route.withRateLimit ? 'primary.light' : 'action.selected',
                                            '&:hover': {
                                              bgcolor: route.withRateLimit ? 'primary.main' : 'action.hover',
                                              color: route.withRateLimit ? 'white' : 'inherit'
                                            }
                                          }}
                                      >
                                        <SettingsIcon fontSize="small" />
                                      </IconButton>
                                    </Tooltip>

                                    {route.withRateLimit && (
                                        <Tooltip title="Disable rate limiting">
                                          <IconButton
                                              size="small"
                                              onClick={() => handleToggleRateLimit(route)}
                                              sx={{
                                                bgcolor: 'error.light',
                                                '&:hover': { bgcolor: 'error.main', color: 'white' }
                                              }}
                                          >
                                            <CloseIcon fontSize="small" />
                                          </IconButton>
                                        </Tooltip>
                                    )}
                                  </Box>
                                </TableCell>
                              </TableRow>
                          );
                        })
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
        )}

        {/* Route History Tab */}
        {tabValue === 1 && renderRouteHistory()}

        {/* Rate Limit Configuration Dialog */}
        <Dialog
            open={configDialogOpen}
            onClose={() => setConfigDialogOpen(false)}
            maxWidth="sm"
            fullWidth
            PaperProps={{ sx: { borderRadius: '12px' } }}
        >
          <DialogTitle sx={{
            bgcolor: 'primary.main',
            color: 'primary.contrastText',
            display: 'flex',
            alignItems: 'center'
          }}>
            <SettingsIcon sx={{ mr: 1 }} />
            Configure Rate Limit
          </DialogTitle>
          <DialogContent sx={{ pt: 3 }}>
            <Box>
              <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                {selectedRoute?.path}
              </Typography>
              <Typography variant="body2" color="textSecondary" gutterBottom>
                Route ID: {selectedRoute?.routeId}
              </Typography>

              <Box sx={{ mt: 3, mb: 2 }}>
                <TextField
                    label="Maximum Requests"
                    type="number"
                    fullWidth
                    margin="normal"
                    value={rateLimitValues.maxRequests}
                    onChange={(e) => setRateLimitValues({
                      ...rateLimitValues,
                      maxRequests: Math.max(1, Number(e.target.value))
                    })}
                    InputProps={{
                      inputProps: { min: 1 },
                      sx: { borderRadius: '8px' }
                    }}
                    helperText="Maximum number of requests allowed within the time window"
                    variant="outlined"
                />

                <TextField
                    label="Time Window (milliseconds)"
                    type="number"
                    fullWidth
                    margin="normal"
                    value={rateLimitValues.timeWindowMs}
                    onChange={(e) => setRateLimitValues({
                      ...rateLimitValues,
                      timeWindowMs: Math.max(1000, Number(e.target.value))
                    })}
                    InputProps={{
                      inputProps: { min: 1000, step: 1000 },
                      sx: { borderRadius: '8px' }
                    }}
                    helperText="Time window in milliseconds (e.g., 60000 = 1 minute)"
                    variant="outlined"
                />

                <Box sx={{
                  mt: 3,
                  p: 2,
                  bgcolor: 'action.selected',
                  borderRadius: '8px'
                }}>
                  <Typography variant="body2" fontWeight="bold">
                    New Rate Limit:
                  </Typography>
                  <Typography variant="body2">
                    {rateLimitValues.maxRequests} requests per {formatTimeWindow(rateLimitValues.timeWindowMs)}
                  </Typography>
                </Box>
              </Box>
            </Box>
          </DialogContent>
          <DialogActions sx={{ p: 2 }}>
            <Button
                onClick={() => setConfigDialogOpen(false)}
                variant="outlined"
                sx={{ borderRadius: '8px' }}
            >
              Cancel
            </Button>
            <Button
                onClick={handleSaveRateLimit}
                variant="contained"
                color="primary"
                sx={{ borderRadius: '8px' }}
            >
              Save Configuration
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
  );
};

export default RateLimitPage;