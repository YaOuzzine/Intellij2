// src/pages/DashboardPage.jsx
import React, { useState, useEffect, useContext } from 'react';
import {
  Typography,
  Box,
  Grid,
  Paper,
  Snackbar,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControlLabel,
  Switch,
  CircularProgress,
  Select,
  MenuItem,
  InputLabel,
  FormControl
} from '@mui/material';
import PeopleOutlineIcon from '@mui/icons-material/PeopleOutline';
import ComputerIcon from '@mui/icons-material/Computer';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import NetworkCheckIcon from '@mui/icons-material/NetworkCheck';
import { styled } from '@mui/material/styles';

import RouteTable from '../components/RouteTable';
import {
  fetchGatewayRoutes,
  addGatewayRoute,
  updateGatewayRoute,
  deleteGatewayRoute
} from '../services/dataService';
import { fetchRoutesForIpManagement } from '../services/ipService';
import { useLocation, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { AuthContext } from '../context/AuthContext';

const MetricCard = styled(Paper)(({ theme }) => ({
  padding: theme.spacing(2),
  display: 'flex',
  alignItems: 'center',
  height: '100%',
  boxShadow: '0px 3px 6px rgba(0, 0, 0, 0.1)'
}));

// Create a context for managing the highlight state of IP Management nav item
export const NavHighlightContext = React.createContext();

const DashboardPage = () => {
  // Navigation and location
  const navigate = useNavigate();
  const location = useLocation();
  const highlightedRouteId = location.state?.routeId;
  const { updateNavHighlight } = useContext(NavHighlightContext) || {};

  // State for routes and metrics
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [metrics, setMetrics] = useState({
    totalRoutes: 0,
    withIpFilter: 0,
    withTokenValidation: 0,
    withRateLimit: 0
  });

  // State for request metrics
  const [minuteMetrics, setMinuteMetrics] = useState({
    requestsCurrentMinute: 0,
    requestsPreviousMinute: 0,
    rejectedCurrentMinute: 0,
    rejectedPreviousMinute: 0,
    increasePercentage: 0,
    rejectedIncreasePercentage: 0
  });
  const [requestCount, setRequestCount] = useState(0);
  const [rejectedCount, setRejectedCount] = useState(0);

  // State for notifications
  const [notification, setNotification] = useState({
    open: false,
    message: '',
    severity: 'success',
    action: null
  });

  // State for route dialog
  const [routeDialog, setRouteDialog] = useState({
    open: false,
    isEdit: false,
    data: {
      routeId: '',
      predicates: '',
      uri: '',
      withIpFilter: false,
      withToken: false,
      withRateLimit: false,
      rateLimit: {
        maxRequests: 10,
        timeWindowMs: 60000
      }
    }
  });

  // State for delete confirmation
  const [deleteConfirm, setDeleteConfirm] = useState({
    open: false,
    routeId: null
  });

  // Load routes and metrics
  const loadRoutes = async () => {
    try {
      setLoading(true);
      const data = await fetchGatewayRoutes();

      // If we have a highlighted route from navigation, make sure it appears in the data
      if (highlightedRouteId) {
        const highlightedRoute = data.find(r => r.id === parseInt(highlightedRouteId));
        if (highlightedRoute) {
          // You could add a property to highlight it in the UI
          highlightedRoute.highlighted = true;
        }
      }

      setRoutes(data);

      // Update metrics
      setMetrics({
        totalRoutes: data.length,
        withIpFilter: data.filter(r => r.withIpFilter).length,
        withTokenValidation: data.filter(r => r.withToken).length,
        withRateLimit: data.filter(r => r.withRateLimit).length
      });
    } catch (error) {
      console.error('Error fetching routes:', error);
      showNotification('Failed to load routes', 'error');
    } finally {
      setLoading(false);
    }
  };

  // Load request metrics
  const fetchRequestCounter = async () => {
    try {
      const response = await axios.get('http://localhost:9080/api/metrics/requests');
      const { requestCount, rejectedCount } = response.data;
      setRequestCount(requestCount);
      setRejectedCount(rejectedCount);
    } catch (error) {
      console.error('Error fetching request counter:', error);
    }
  };

  // Load minute metrics
  const fetchMinuteMetrics = async () => {
    try {
      const response = await axios.get('http://localhost:9080/api/metrics/minutely');
      const {
        requestsCurrentMinute,
        requestsPreviousMinute,
        rejectedCurrentMinute,
        rejectedPreviousMinute
      } = response.data;

      let increasePercentage = 0;
      if (requestsPreviousMinute > 0) {
        increasePercentage = Math.round(
            ((requestsCurrentMinute - requestsPreviousMinute) / requestsPreviousMinute) * 100
        );
      } else if (requestsCurrentMinute > 0) {
        increasePercentage = 100;
      }

      let rejectedIncreasePercentage = 0;
      if (rejectedPreviousMinute > 0) {
        rejectedIncreasePercentage = Math.round(
            ((rejectedCurrentMinute - rejectedPreviousMinute) / rejectedPreviousMinute) * 100
        );
      } else if (rejectedCurrentMinute > 0) {
        rejectedIncreasePercentage = 100;
      }

      setMinuteMetrics({
        requestsCurrentMinute,
        requestsPreviousMinute,
        rejectedCurrentMinute,
        rejectedPreviousMinute,
        increasePercentage,
        rejectedIncreasePercentage
      });
    } catch (error) {
      console.error('Error fetching minute metrics:', error);
    }
  };

  // Load data on mount
  useEffect(() => {
    loadRoutes();
    fetchRequestCounter();
    fetchMinuteMetrics();

    // Set up refresh intervals
    const intervalCounter = setInterval(fetchRequestCounter, 5000);
    const intervalMinute = setInterval(fetchMinuteMetrics, 5000);
    const routesInterval = setInterval(loadRoutes, 30000);

    return () => {
      clearInterval(intervalCounter);
      clearInterval(intervalMinute);
      clearInterval(routesInterval);
    };
  }, [highlightedRouteId]);

  // Show notification
  const showNotification = (message, severity = 'success', action = null) => {
    setNotification({
      open: true,
      message,
      severity,
      action
    });
  };

  // Close notification
  const handleCloseNotification = () => {
    setNotification(prev => ({ ...prev, open: false }));
  };

  // Route Dialog Handlers
  const handleOpenAddDialog = () => {
    setRouteDialog({
      open: true,
      isEdit: false,
      data: {
        routeId: '',
        predicates: '',
        uri: '',
        withIpFilter: false,
        withToken: false,
        withRateLimit: false,
        rateLimit: {
          maxRequests: 10,
          timeWindowMs: 60000
        }
      }
    });
  };

  const handleOpenEditDialog = (route) => {
    // Create a copy of the route object to avoid direct state mutation
    const routeData = {
      ...route,
      rateLimit: route.rateLimit ? { ...route.rateLimit } : { maxRequests: 10, timeWindowMs: 60000 }
    };

    setRouteDialog({
      open: true,
      isEdit: true,
      data: routeData
    });
  };

  const handleCloseRouteDialog = () => {
    setRouteDialog(prev => ({ ...prev, open: false }));
  };

  const handleRouteInputChange = (e) => {
    const { name, value, type, checked } = e.target;

    setRouteDialog(prev => ({
      ...prev,
      data: {
        ...prev.data,
        [name]: type === 'checkbox' ? checked : value
      }
    }));
  };

  const handleRateLimitChange = (e) => {
    const { name, value } = e.target;
    const numValue = parseInt(value, 10);

    setRouteDialog(prev => ({
      ...prev,
      data: {
        ...prev.data,
        rateLimit: {
          ...prev.data.rateLimit,
          [name]: isNaN(numValue) ? 0 : numValue
        }
      }
    }));
  };

  // Save route (add or update)
  const handleSaveRoute = async () => {
    try {
      setLoading(true);
      const { data, isEdit } = routeDialog;

      // Validate form
      if (!data.predicates || !data.uri) {
        showNotification('Path and URI are required fields', 'error');
        setLoading(false);
        return;
      }

      if (!data.predicates.endsWith('/**')) {
        // Add slash-star-star if not present
        data.predicates = data.predicates.endsWith('/')
            ? `${data.predicates}**`
            : `${data.predicates}/**`;
      }

      // Ensure URI has protocol
      if (!data.uri.startsWith('http://') && !data.uri.startsWith('https://')) {
        data.uri = `http://${data.uri}`;
      }

      if (isEdit) {
        await updateGatewayRoute(data.id, data);
        showNotification('Route updated successfully');
      } else {
        await addGatewayRoute(data);
        showNotification('New route created successfully');
      }

      handleCloseRouteDialog();
      await loadRoutes();
    } catch (error) {
      console.error('Error saving route:', error);
      showNotification(
          error.response?.data || 'Failed to save route',
          'error'
      );
    } finally {
      setLoading(false);
    }
  };

  // Delete route
  const handleDeleteRoute = async (routeId) => {
    setDeleteConfirm({
      open: true,
      routeId
    });
  };

  const confirmDelete = async () => {
    try {
      setLoading(true);
      await deleteGatewayRoute(deleteConfirm.routeId);
      showNotification('Route deleted successfully');

      setDeleteConfirm({
        open: false,
        routeId: null
      });

      await loadRoutes();
    } catch (error) {
      console.error('Error deleting route:', error);
      showNotification('Failed to delete route', 'error');
    } finally {
      setLoading(false);
    }
  };

  // Navigate to IP Management
  const goToIpManagement = (routeId) => {
    navigate('/ip-management', { state: { routeId } });
  };

  // Toggle security settings
  const handleToggleIpFilter = async (route) => {
    try {
      const updatedRoute = {
        ...route,
        withIpFilter: !route.withIpFilter
      };

      await updateGatewayRoute(route.id, updatedRoute);

      if (updatedRoute.withIpFilter) {
        // If enabling IP filtering, show notification with action and highlight nav
        showNotification(
            'IP filtering enabled. Add IP addresses to the whitelist for this route to take effect.',
            'info',
            <Button
                color="inherit"
                size="small"
                onClick={() => goToIpManagement(route.id)}
            >
              Manage IPs
            </Button>
        );

        // Highlight the IP Management nav item if context is available
        if (updateNavHighlight) {
          updateNavHighlight('IP_MANAGEMENT');
          // Reset highlight after 10 seconds
          setTimeout(() => updateNavHighlight(null), 10000);
        }
      } else {
        showNotification(`IP filtering disabled for route ${route.routeId || route.id}`);
      }

      await loadRoutes();
    } catch (error) {
      console.error('Error toggling IP filter:', error);
      showNotification('Failed to update route settings', 'error');
    }
  };

  const handleToggleTokenValidation = async (route) => {
    try {
      const updatedRoute = {
        ...route,
        withToken: !route.withToken
      };

      await updateGatewayRoute(route.id, updatedRoute);
      showNotification(`Token validation ${updatedRoute.withToken ? 'enabled' : 'disabled'}`);
      await loadRoutes();
    } catch (error) {
      console.error('Error toggling token validation:', error);
      showNotification('Failed to update route settings', 'error');
    }
  };

  const handleToggleRateLimit = async (route) => {
    try {
      const updatedRoute = {
        ...route,
        withRateLimit: !route.withRateLimit
      };

      if (updatedRoute.withRateLimit && !updatedRoute.rateLimit) {
        updatedRoute.rateLimit = {
          maxRequests: 10,
          timeWindowMs: 60000
        };
      }

      await updateGatewayRoute(route.id, updatedRoute);
      showNotification(`Rate limiting ${updatedRoute.withRateLimit ? 'enabled' : 'disabled'}`);
      await loadRoutes();
    } catch (error) {
      console.error('Error toggling rate limit:', error);
      showNotification('Failed to update route settings', 'error');
    }
  };

  return (
      <Box sx={{ p: 3 }}>
        {/* Header */}
        <Typography variant="h5" component="h1" sx={{ mb: 4 }}>
          Gateway Management Dashboard
        </Typography>

        {/* Metrics Cards */}
        <Grid container spacing={3} sx={{ mb: 4 }}>
          {/* Total Routes */}
          <Grid item xs={12} sm={6} md={3}>
            <MetricCard>
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <Box
                    sx={{
                      backgroundColor: '#e8f5e9',
                      borderRadius: '50%',
                      p: 1.5,
                      mr: 2
                    }}
                >
                  <NetworkCheckIcon />
                </Box>
                <Box>
                  <Typography color="textSecondary" variant="subtitle2">
                    Total Routes
                  </Typography>
                  <Typography variant="h4">
                    {metrics.totalRoutes}
                  </Typography>
                  <Typography variant="caption" color="textSecondary">
                    {metrics.withIpFilter} with IP filtering
                  </Typography>
                </Box>
              </Box>
            </MetricCard>
          </Grid>

          {/* Accepted Requests This Minute */}
          <Grid item xs={12} sm={6} md={3}>
            <MetricCard>
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <Box
                    sx={{
                      backgroundColor: '#e8f5e9',
                      borderRadius: '50%',
                      p: 1.5,
                      mr: 2
                    }}
                >
                  <ArrowUpwardIcon fontSize="small" />
                </Box>
                <Box>
                  <Typography color="textSecondary" variant="subtitle2">
                    Requests This Minute
                  </Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <Typography variant="h4" sx={{ mr: 1 }}>
                      {minuteMetrics.requestsCurrentMinute}
                    </Typography>
                    <Box
                        sx={{
                          display: 'flex',
                          alignItems: 'center',
                          backgroundColor:
                              minuteMetrics.increasePercentage >= 0
                                  ? '#e6f4ea'
                                  : '#fce8e6',
                          borderRadius: '4px',
                          px: 1,
                          py: 0.5
                        }}
                    >
                      <ArrowUpwardIcon
                          fontSize="small"
                          sx={{
                            color:
                                minuteMetrics.increasePercentage >= 0
                                    ? '#34a853'
                                    : '#ea4335',
                            mr: 0.5,
                            transform: minuteMetrics.increasePercentage < 0 ? 'rotate(180deg)' : 'none'
                          }}
                      />
                      <Typography
                          variant="body2"
                          sx={{
                            color:
                                minuteMetrics.increasePercentage >= 0
                                    ? '#34a853'
                                    : '#ea4335',
                            fontWeight: 500
                          }}
                      >
                        {Math.abs(minuteMetrics.increasePercentage)}%
                      </Typography>
                    </Box>
                  </Box>
                </Box>
              </Box>
            </MetricCard>
          </Grid>

          {/* Global Request Counter */}
          <Grid item xs={12} sm={6} md={3}>
            <MetricCard>
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <Box
                    sx={{
                      backgroundColor: '#e8f5e9',
                      borderRadius: '50%',
                      p: 1.5,
                      mr: 2
                    }}
                >
                  <ComputerIcon />
                </Box>
                <Box>
                  <Typography color="textSecondary" variant="subtitle2">
                    Total Requests
                  </Typography>
                  <Typography variant="h4">{requestCount}</Typography>
                </Box>
              </Box>
            </MetricCard>
          </Grid>

          {/* Rejected Requests This Minute */}
          <Grid item xs={12} sm={6} md={3}>
            <MetricCard>
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <Box
                    sx={{
                      backgroundColor: '#fce8e6',
                      borderRadius: '50%',
                      p: 1.5,
                      mr: 2
                    }}
                >
                  <ErrorOutlineIcon fontSize="small" />
                </Box>
                <Box>
                  <Typography color="textSecondary" variant="subtitle2">
                    Rejected This Minute
                  </Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <Typography variant="h4" sx={{ mr: 1 }}>
                      {minuteMetrics.rejectedCurrentMinute}
                    </Typography>
                    <Box
                        sx={{
                          display: 'flex',
                          alignItems: 'center',
                          backgroundColor:
                              minuteMetrics.rejectedIncreasePercentage >= 0
                                  ? '#fce8e6'
                                  : '#e6f4ea',
                          borderRadius: '4px',
                          px: 1,
                          py: 0.5
                        }}
                    >
                      <ArrowUpwardIcon
                          fontSize="small"
                          sx={{
                            color:
                                minuteMetrics.rejectedIncreasePercentage >= 0
                                    ? '#ea4335'
                                    : '#34a853',
                            mr: 0.5,
                            transform:
                                minuteMetrics.rejectedIncreasePercentage >= 0
                                    ? 'none'
                                    : 'rotate(180deg)'
                          }}
                      />
                      <Typography
                          variant="body2"
                          sx={{
                            color:
                                minuteMetrics.rejectedIncreasePercentage >= 0
                                    ? '#ea4335'
                                    : '#34a853',
                            fontWeight: 500
                          }}
                      >
                        {Math.abs(
                            minuteMetrics.rejectedIncreasePercentage
                        )}
                        %
                      </Typography>
                    </Box>
                  </Box>
                </Box>
              </Box>
            </MetricCard>
          </Grid>
        </Grid>

        {/* Routes Table */}
        <Box sx={{ mb: 3 }}>
          {loading && routes.length === 0 ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
                <CircularProgress />
              </Box>
          ) : (
              <RouteTable
                  routes={routes}
                  onAdd={handleOpenAddDialog}
                  onUpdate={handleOpenEditDialog}
                  onDelete={handleDeleteRoute}
                  onToggleIpFilter={handleToggleIpFilter}
                  onToggleTokenValidation={handleToggleTokenValidation}
                  onToggleRateLimit={handleToggleRateLimit}
                  onManageIps={goToIpManagement}
              />
          )}
        </Box>

        {/* Add/Edit Route Dialog */}
        <Dialog
            open={routeDialog.open}
            onClose={handleCloseRouteDialog}
            fullWidth
            maxWidth="md"
        >
          <DialogTitle>
            {routeDialog.isEdit ? 'Edit Route' : 'Add New Route'}
          </DialogTitle>
          <DialogContent dividers>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, py: 1 }}>
              <TextField
                  label="Route ID"
                  name="routeId"
                  value={routeDialog.data.routeId}
                  onChange={handleRouteInputChange}
                  fullWidth
                  placeholder="e.g., my-service-route"
                  helperText="A unique identifier for this route (optional)"
              />

              <TextField
                  label="Path"
                  name="predicates"
                  value={routeDialog.data.predicates}
                  onChange={handleRouteInputChange}
                  fullWidth
                  required
                  placeholder="e.g., /api/service/**"
                  helperText="The path predicate pattern, '/**' will be added automatically if missing"
              />

              <TextField
                  label="URI"
                  name="uri"
                  value={routeDialog.data.uri}
                  onChange={handleRouteInputChange}
                  fullWidth
                  required
                  placeholder="e.g., http://localhost:8080"
                  helperText="The destination URI where requests will be forwarded"
              />

              <Box sx={{ mt: 2 }}>
                <Typography variant="subtitle1" gutterBottom>
                  Security Settings
                </Typography>

                <Grid container spacing={2}>
                  <Grid item xs={12} md={4}>
                    <FormControlLabel
                        control={
                          <Switch
                              name="withIpFilter"
                              checked={routeDialog.data.withIpFilter}
                              onChange={handleRouteInputChange}
                              color="primary"
                          />
                        }
                        label="IP Filtering"
                    />
                  </Grid>

                  <Grid item xs={12} md={4}>
                    <FormControlLabel
                        control={
                          <Switch
                              name="withToken"
                              checked={routeDialog.data.withToken}
                              onChange={handleRouteInputChange}
                              color="primary"
                          />
                        }
                        label="Token Validation"
                    />
                  </Grid>

                  <Grid item xs={12} md={4}>
                    <FormControlLabel
                        control={
                          <Switch
                              name="withRateLimit"
                              checked={routeDialog.data.withRateLimit}
                              onChange={handleRouteInputChange}
                              color="primary"
                          />
                        }
                        label="Rate Limiting"
                    />
                  </Grid>
                </Grid>
              </Box>

              {routeDialog.data.withRateLimit && (
                  <Box sx={{ mt: 2, p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
                    <Typography variant="subtitle2" gutterBottom>
                      Rate Limit Settings
                    </Typography>

                    <Grid container spacing={2}>
                      <Grid item xs={12} md={6}>
                        <TextField
                            label="Max Requests"
                            name="maxRequests"
                            type="number"
                            value={routeDialog.data.rateLimit?.maxRequests || 10}
                            onChange={handleRateLimitChange}
                            fullWidth
                            InputProps={{ inputProps: { min: 1 } }}
                            helperText="Maximum number of requests allowed"
                        />
                      </Grid>

                      <Grid item xs={12} md={6}>
                        <TextField
                            label="Time Window (ms)"
                            name="timeWindowMs"
                            type="number"
                            value={routeDialog.data.rateLimit?.timeWindowMs || 60000}
                            onChange={handleRateLimitChange}
                            fullWidth
                            InputProps={{ inputProps: { min: 1000, step: 1000 } }}
                            helperText="Time window in milliseconds (e.g., 60000 = 1 minute)"
                        />
                      </Grid>
                    </Grid>
                  </Box>
              )}
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleCloseRouteDialog}>
              Cancel
            </Button>
            <Button
                onClick={handleSaveRoute}
                variant="contained"
                color="primary"
                disabled={loading}
            >
              {loading ? <CircularProgress size={24} /> : routeDialog.isEdit ? 'Update' : 'Create'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Delete Confirmation Dialog */}
        <Dialog
            open={deleteConfirm.open}
            onClose={() => setDeleteConfirm({ open: false, routeId: null })}
        >
          <DialogTitle>Confirm Deletion</DialogTitle>
          <DialogContent>
            <Typography>
              Are you sure you want to delete this route? This action cannot be undone.
            </Typography>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDeleteConfirm({ open: false, routeId: null })}>
              Cancel
            </Button>
            <Button
                onClick={confirmDelete}
                variant="contained"
                color="error"
                disabled={loading}
            >
              {loading ? <CircularProgress size={24} /> : 'Delete'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Notifications */}
        <Snackbar
            open={notification.open}
            autoHideDuration={6000}
            onClose={handleCloseNotification}
            anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
            action={notification.action}
        >
          <Alert
              onClose={handleCloseNotification}
              severity={notification.severity}
              sx={{ width: '100%' }}
              action={notification.action}
          >
            {notification.message}
          </Alert>
        </Snackbar>
      </Box>
  );
};

export default DashboardPage;