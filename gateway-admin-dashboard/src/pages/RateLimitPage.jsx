// gateway-admin-dashboard/src/pages/RateLimitPage.jsx
import React, { useState, useEffect, useRef } from 'react';
import {
  Typography, Box, Paper, Grid, Card, CardContent, CardHeader,
  Button, TextField, InputAdornment, CircularProgress, Alert,
  Dialog, DialogTitle, DialogContent, DialogActions,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Divider, Chip, IconButton, Tabs, Tab, FormControlLabel, Switch,
  Tooltip, Menu, MenuItem
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
  Close as CloseIcon
} from '@mui/icons-material';
import { styled } from '@mui/material/styles';
import { fetchGatewayRoutes, updateGatewayRoute } from '../services/dataService';
import apiClient from '../apiClient';
import {
  AreaChart, Area, BarChart, Bar, LineChart, Line, XAxis, YAxis,
  CartesianGrid, Tooltip as RechartsTooltip, Legend, ResponsiveContainer,
  PieChart, Pie, Cell
} from 'recharts';

// Styled components
const StyledCard = styled(Card)(({ theme }) => ({
  height: '100%',
  display: 'flex',
  flexDirection: 'column',
  transition: 'transform 0.3s, box-shadow 0.3s',
  '&:hover': {
    transform: 'translateY(-4px)',
    boxShadow: theme.shadows[4],
  },
}));

const MetricValue = styled(Typography)(({ theme }) => ({
  fontSize: '2rem',
  fontWeight: 500,
  marginBottom: theme.spacing(1),
}));

const PercentageChip = styled(Chip)(({ theme, value }) => {
  let color = theme.palette.success.main;
  let bgcolor = theme.palette.success.light;

  if (value > 5) {
    color = theme.palette.warning.main;
    bgcolor = theme.palette.warning.light;
  }
  if (value > 10) {
    color = theme.palette.error.main;
    bgcolor = theme.palette.error.light;
  }

  return {
    backgroundColor: bgcolor,
    color: color,
    fontWeight: 'bold',
  };
});

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8', '#82ca9d'];

// Main component
const RateLimitPage = () => {
  // State for routes and metrics
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

  // Dialog states
  const [configDialogOpen, setConfigDialogOpen] = useState(false);
  const [selectedRoute, setSelectedRoute] = useState(null);
  const [rateLimitValues, setRateLimitValues] = useState({
    maxRequests: 10,
    timeWindowMs: 60000
  });

  // Menu state
  const [menuAnchorEl, setMenuAnchorEl] = useState(null);
  const [visualizationMode, setVisualizationMode] = useState('all');

  // Load data on mount
  useEffect(() => {
    loadData();

    return () => {
      if (refreshTimerRef.current) {
        clearInterval(refreshTimerRef.current);
      }
    };
  }, []);

  // Handle auto-refresh
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

  // Update last updated time
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

  // Load routes and rate limit metrics
  const loadData = async (showLoadingIndicator = true) => {
    if (showLoadingIndicator) {
      setLoading(true);
    }
    setError(null);

    try {
      // Load gateway routes
      const routesData = await fetchGatewayRoutes();
      setRoutes(routesData);

      // Change this line from axios.get to apiClient.get
      const metricsResponse = await apiClient.get('/metrics/ratelimit');
      setRateLimitMetrics(metricsResponse.data);

      lastUpdatedRef.current = Date.now();
      setLastUpdated('Just now');

      // If we were viewing a route's history, refresh that too
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

  // Load detailed history for a route
  const loadRouteHistory = async (routeId) => {
    setHistoryLoading(true);

    try {
      // Change this line from axios.get to apiClient.get
      const response = await apiClient.get(`/metrics/ratelimit/${routeId}/history`);
      setSelectedRouteHistory(response.data);
    } catch (err) {
      console.error('Error loading route history:', err);
    } finally {
      setHistoryLoading(false);
    }
  };

  // Handle search
  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  };

  // Filter routes based on search term
  const filteredRoutes = rateLimitMetrics.routes?.filter(route =>
      route.path.toLowerCase().includes(searchTerm.toLowerCase()) ||
      route.routeId.toLowerCase().includes(searchTerm.toLowerCase())
  ) || [];

  // Open config dialog
  const handleOpenConfigDialog = (route) => {
    setSelectedRoute(route);
    setRateLimitValues({
      maxRequests: route.rateLimit?.maxRequests || 10,
      timeWindowMs: route.rateLimit?.timeWindowMs || 60000
    });
    setConfigDialogOpen(true);
  };

  // Toggle rate limit
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

      // If enabling rate limiting and no rate limit exists, create default one
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

  // Save rate limit configuration
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

  // Open menu for visualizations
  const handleMenuOpen = (event) => {
    setMenuAnchorEl(event.currentTarget);
  };

  // Close visualization menu
  const handleMenuClose = () => {
    setMenuAnchorEl(null);
  };

  // Change visualization mode
  const handleVisualizationChange = (mode) => {
    setVisualizationMode(mode);
    handleMenuClose();
  };

  // View route history
  const handleViewHistory = (routeId) => {
    loadRouteHistory(routeId);
    setTabValue(1);
  };

  // Format time window display
  const formatTimeWindow = (ms) => {
    if (ms < 1000) return `${ms}ms`;
    if (ms < 60000) return `${ms / 1000}s`;
    return `${ms / 60000}m`;
  };

  // Format date for charts
  const formatDate = (timestamp) => {
    const date = new Date(timestamp);
    return `${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`;
  };

  // Tab change handler
  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };

  // Get visual status for route
  const getRouteStatus = (route) => {
    if (!route.withRateLimit) return 'disabled';
    if (route.rejectionRate > 10) return 'critical';
    if (route.rejectionRate > 5) return 'warning';
    return 'healthy';
  };

  // Get color for status
  const getStatusColor = (status) => {
    switch (status) {
      case 'critical': return '#f44336';
      case 'warning': return '#ff9800';
      case 'healthy': return '#4caf50';
      case 'disabled': return '#9e9e9e';
      default: return '#9e9e9e';
    }
  };

  // Prepare aggregate chart data
  const getAggregateChartData = () => {
    // If specific routes selected, filter the data
    let routesToShow = filteredRoutes;

    if (visualizationMode !== 'all') {
      routesToShow = filteredRoutes.filter(route => {
        const status = getRouteStatus(route);
        return visualizationMode === status;
      });
    }

    // Get top 5 routes by traffic
    const topRoutes = [...routesToShow]
        .sort((a, b) => (b.totalRequests || 0) - (a.totalRequests || 0))
        .slice(0, 5);

    // Prepare pie chart data
    const pieData = topRoutes.map(route => ({
      name: route.routeId,
      value: route.totalRequests || 0
    }));

    // Prepare bar chart data for rejection rates
    const barData = topRoutes.map(route => ({
      name: route.routeId,
      rejectionRate: route.rejectionRate || 0
    }));

    return { pieData, barData };
  };

  // Render charts based on visualization mode
  const renderCharts = () => {
    const { pieData, barData } = getAggregateChartData();

    return (
        <Grid container spacing={3}>
          {/* Traffic Distribution Pie Chart */}
          <Grid item xs={12} md={6}>
            <StyledCard>
              <CardHeader
                  title="Traffic Distribution"
                  subheader="Top 5 routes by request volume"
              />
              <CardContent sx={{ flexGrow: 1, minHeight: 300 }}>
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie
                        data={pieData}
                        cx="50%"
                        cy="50%"
                        labelLine={false}
                        outerRadius={80}
                        fill="#8884d8"
                        dataKey="value"
                        label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                    >
                      {pieData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <RechartsTooltip formatter={(value) => `${value} requests`} />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              </CardContent>
            </StyledCard>
          </Grid>

          {/* Rejection Rates Bar Chart */}
          <Grid item xs={12} md={6}>
            <StyledCard>
              <CardHeader
                  title="Rejection Rates"
                  subheader="Percentage of rejected requests"
              />
              <CardContent sx={{ flexGrow: 1, minHeight: 300 }}>
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={barData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" />
                    <YAxis unit="%" />
                    <RechartsTooltip formatter={(value) => `${value}%`} />
                    <Bar
                        dataKey="rejectionRate"
                        fill="#8884d8"
                        background={{ fill: '#eee' }}
                        isAnimationActive={true}
                    >
                      {barData.map((entry, index) => {
                        const { rejectionRate } = entry;
                        let color = '#4caf50';

                        if (rejectionRate > 5) color = '#ff9800';
                        if (rejectionRate > 10) color = '#f44336';

                        return <Cell key={`cell-${index}`} fill={color} />;
                      })}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </CardContent>
            </StyledCard>
          </Grid>
        </Grid>
    );
  };

  // Render the route history detail view
  const renderRouteHistory = () => {
    if (!selectedRouteHistory) {
      return (
          <Box sx={{ textAlign: 'center', py: 10 }}>
            <Typography variant="subtitle1" color="textSecondary">
              Select a route to view detailed history
            </Typography>
          </Box>
      );
    }

    const { routeId, path, history, maxRequests, timeWindowMs } = selectedRouteHistory;

    // Format history data for charts
    const historyData = history.map(point => ({
      timestamp: point.timestamp,
      time: formatDate(point.timestamp),
      requests: point.requests,
      rejections: point.rejections,
      accepted: point.requests - point.rejections
    }));

    return (
        <Box>
          <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Box>
              <Typography variant="h6">
                {path}
              </Typography>
              <Typography variant="subtitle2" color="textSecondary">
                Route ID: {routeId}
              </Typography>
              {maxRequests && (
                  <Typography variant="body2">
                    Limit: {maxRequests} requests per {formatTimeWindow(timeWindowMs)}
                  </Typography>
              )}
            </Box>
            <Button
                startIcon={<RefreshIcon />}
                variant="outlined"
                onClick={() => loadRouteHistory(routeId)}
                disabled={historyLoading}
            >
              {historyLoading ? 'Loading...' : 'Refresh'}
            </Button>
          </Box>

          {/* Request vs Rejection Over Time */}
          <StyledCard sx={{ mb: 3 }}>
            <CardHeader title="Traffic Over Time" />
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={historyData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis />
                  <RechartsTooltip
                      formatter={(value, name) => [value, name === 'accepted' ? 'Accepted Requests' : 'Rejected Requests']}
                  />
                  <Legend />
                  <Area
                      type="monotone"
                      dataKey="accepted"
                      stackId="1"
                      stroke="#4caf50"
                      fill="#4caf50"
                      fillOpacity={0.6}
                      name="Accepted"
                  />
                  <Area
                      type="monotone"
                      dataKey="rejections"
                      stackId="1"
                      stroke="#f44336"
                      fill="#f44336"
                      fillOpacity={0.6}
                      name="Rejected"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </CardContent>
          </StyledCard>

          {/* Rejection Rate Over Time */}
          <StyledCard>
            <CardHeader title="Rejection Rate Over Time" />
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={historyData.map(point => ({
                  ...point,
                  rejectionRate: point.requests > 0
                      ? (point.rejections / point.requests) * 100
                      : 0
                }))}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis unit="%" />
                  <RechartsTooltip formatter={(value) => `${value.toFixed(2)}%`} />
                  <Line
                      type="monotone"
                      dataKey="rejectionRate"
                      stroke="#ff9800"
                      dot={{ r: 4 }}
                      activeDot={{ r: 6 }}
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
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h5" component="h1">
            Rate Limit Management
          </Typography>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <FormControlLabel
                control={
                  <Switch
                      checked={autoRefresh}
                      onChange={(e) => setAutoRefresh(e.target.checked)}
                      color="primary"
                  />
                }
                label="Auto-refresh"
            />

            <Button
                variant="outlined"
                startIcon={<RefreshIcon />}
                onClick={() => loadData()}
                disabled={loading}
            >
              Refresh
            </Button>

            <IconButton
                aria-label="visualization options"
                onClick={handleMenuOpen}
            >
              <MoreVertIcon />
            </IconButton>

            <Menu
                anchorEl={menuAnchorEl}
                open={Boolean(menuAnchorEl)}
                onClose={handleMenuClose}
            >
              <MenuItem
                  onClick={() => handleVisualizationChange('all')}
                  selected={visualizationMode === 'all'}
              >
                All Routes
              </MenuItem>
              <MenuItem
                  onClick={() => handleVisualizationChange('healthy')}
                  selected={visualizationMode === 'healthy'}
              >
                <Box component="span" sx={{ color: getStatusColor('healthy'), mr: 1 }}>●</Box> Healthy
              </MenuItem>
              <MenuItem
                  onClick={() => handleVisualizationChange('warning')}
                  selected={visualizationMode === 'warning'}
              >
                <Box component="span" sx={{ color: getStatusColor('warning'), mr: 1 }}>●</Box> Warning
              </MenuItem>
              <MenuItem
                  onClick={() => handleVisualizationChange('critical')}
                  selected={visualizationMode === 'critical'}
              >
                <Box component="span" sx={{ color: getStatusColor('critical'), mr: 1 }}>●</Box> Critical
              </MenuItem>
              <MenuItem
                  onClick={() => handleVisualizationChange('disabled')}
                  selected={visualizationMode === 'disabled'}
              >
                <Box component="span" sx={{ color: getStatusColor('disabled'), mr: 1 }}>●</Box> Disabled
              </MenuItem>
            </Menu>
          </Box>
        </Box>

        <Typography variant="caption" color="textSecondary" sx={{ display: 'block', mb: 3 }}>
          Last updated: {lastUpdated}
        </Typography>

        {/* Error message */}
        {error && (
            <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
              {error}
            </Alert>
        )}

        {/* Summary metrics */}
        <Grid container spacing={3} sx={{ mb: 3 }}>
          <Grid item xs={12} md={4}>
            <StyledCard>
              <CardContent>
                <Typography variant="subtitle1" color="textSecondary" gutterBottom>
                  Total Traffic
                </Typography>
                <MetricValue>
                  {rateLimitMetrics.summary?.totalRequests.toLocaleString() || '0'}
                </MetricValue>
                <Typography variant="body2" color="textSecondary">
                  Total requests processed
                </Typography>
              </CardContent>
            </StyledCard>
          </Grid>

          <Grid item xs={12} md={4}>
            <StyledCard>
              <CardContent>
                <Typography variant="subtitle1" color="textSecondary" gutterBottom>
                  Rate Limited
                </Typography>
                <MetricValue>
                  {rateLimitMetrics.summary?.totalRejections.toLocaleString() || '0'}
                </MetricValue>
                <Typography variant="body2" color="textSecondary">
                  Requests rejected due to rate limits
                </Typography>
              </CardContent>
            </StyledCard>
          </Grid>

          <Grid item xs={12} md={4}>
            <StyledCard>
              <CardContent>
                <Typography variant="subtitle1" color="textSecondary" gutterBottom>
                  Rejection Rate
                </Typography>
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
              </CardContent>
            </StyledCard>
          </Grid>
        </Grid>

        {/* Tabs for Dashboard and Route Details */}
        <Paper sx={{ mb: 3 }}>
          <Tabs
              value={tabValue}
              onChange={handleTabChange}
              indicatorColor="primary"
              textColor="primary"
          >
            <Tab label="Dashboard" />
            <Tab label="Route History" />
          </Tabs>
        </Paper>

        {/* Dashboard Tab */}
        {tabValue === 0 && (
            <Box>
              {/* Search bar */}
              <Box sx={{ mb: 3 }}>
                <TextField
                    placeholder="Search routes..."
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
                    }}
                />
              </Box>

              {/* Charts */}
              {renderCharts()}

              {/* Routes Table */}
              <TableContainer component={Paper} sx={{ mt: 3 }}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Status</TableCell>
                      <TableCell>Route Path</TableCell>
                      <TableCell align="right">Limit</TableCell>
                      <TableCell align="right">Requests</TableCell>
                      <TableCell align="right">Rejected</TableCell>
                      <TableCell align="right">Rejection Rate</TableCell>
                      <TableCell align="right">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {loading ? (
                        <TableRow>
                          <TableCell colSpan={7} align="center">
                            <CircularProgress size={24} />
                          </TableCell>
                        </TableRow>
                    ) : filteredRoutes.length === 0 ? (
                        <TableRow>
                          <TableCell colSpan={7} align="center">
                            No rate limited routes found
                          </TableCell>
                        </TableRow>
                    ) : (
                        filteredRoutes.map((route) => {
                          const status = getRouteStatus(route);

                          return (
                              <TableRow key={route.id} hover>
                                <TableCell>
                                  <Box
                                      sx={{
                                        width: 12,
                                        height: 12,
                                        borderRadius: '50%',
                                        backgroundColor: getStatusColor(status),
                                        display: 'inline-block',
                                        mr: 1
                                      }}
                                  />
                                  {status === 'disabled' ? 'Disabled' :
                                      status === 'critical' ? 'Critical' :
                                          status === 'warning' ? 'Warning' : 'Healthy'}
                                </TableCell>
                                <TableCell>
                                  <Tooltip title={`Route ID: ${route.routeId}`}>
                                    <Box component="span" sx={{ cursor: 'help' }}>
                                      {route.path}
                                    </Box>
                                  </Tooltip>
                                </TableCell>
                                <TableCell align="right">
                                  {route.withRateLimit && route.rateLimit ?
                                      `${route.rateLimit.maxRequests}/${formatTimeWindow(route.rateLimit.timeWindowMs)}` :
                                      'N/A'}
                                </TableCell>
                                <TableCell align="right">
                                  {route.totalRequests || 0}
                                </TableCell>
                                <TableCell align="right">
                                  {route.totalRejections || 0}
                                </TableCell>
                                <TableCell align="right">
                                  <PercentageChip
                                      value={route.rejectionRate || 0}
                                      label={`${route.rejectionRate?.toFixed(2) || 0}%`}
                                      size="small"
                                  />
                                </TableCell>
                                <TableCell align="right">
                                  <Tooltip title="View detailed history">
                                    <IconButton
                                        size="small"
                                        onClick={() => handleViewHistory(route.routeId)}
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
                                        color={route.withRateLimit ? "primary" : "default"}
                                    >
                                      <SettingsIcon fontSize="small" />
                                    </IconButton>
                                  </Tooltip>

                                  {route.withRateLimit && (
                                      <Tooltip title="Disable rate limiting">
                                        <IconButton
                                            size="small"
                                            onClick={() => handleToggleRateLimit(route)}
                                            color="warning"
                                        >
                                          <CloseIcon fontSize="small" />
                                        </IconButton>
                                      </Tooltip>
                                  )}
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
        >
          <DialogTitle>Configure Rate Limit</DialogTitle>
          <DialogContent>
            <Box sx={{ pt: 2 }}>
              <Typography variant="subtitle1" gutterBottom>
                {selectedRoute?.path}
              </Typography>

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
                    inputProps: { min: 1 }
                  }}
                  helperText="Maximum number of requests allowed within the time window"
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
                    inputProps: { min: 1000, step: 1000 }
                  }}
                  helperText="Time window in milliseconds (e.g., 60000 = 1 minute)"
              />

              <Typography variant="body2" color="textSecondary" sx={{ mt: 2 }}>
                This will allow {rateLimitValues.maxRequests} requests per {formatTimeWindow(rateLimitValues.timeWindowMs)}.
              </Typography>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setConfigDialogOpen(false)}>
              Cancel
            </Button>
            <Button
                onClick={handleSaveRateLimit}
                variant="contained"
                color="primary"
            >
              Save Changes
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
  );
};

export default RateLimitPage;