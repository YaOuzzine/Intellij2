import React, { useState, useEffect } from 'react';
import {
  Typography, Box, Paper, Table, TableBody, TableCell, TableContainer, TableHead,
  TableRow, Button, TextField, InputAdornment, Chip, Pagination,
  Dialog, DialogTitle, DialogContent, DialogActions, Alert
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { fetchGatewayRoutes, updateGatewayRoute } from '../services/dataService';

const RateLimitDashboard = () => {
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filteredRoutes, setFilteredRoutes] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(5);
  const [notification, setNotification] = useState({ open: false, message: '', type: 'info' });

  // Rate limit update dialog
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [currentRoute, setCurrentRoute] = useState(null);
  const [rateLimitValues, setRateLimitValues] = useState({
    maxRequests: 10,
    timeWindowMs: 60000
  });

  useEffect(() => {
    loadRoutes();
  }, []);

  useEffect(() => {
    filterRoutes();
  }, [routes, searchTerm]);

  const loadRoutes = async () => {
    setLoading(true);
    try {
      const data = await fetchGatewayRoutes();
      setRoutes(data);
    } catch (error) {
      console.error('Error loading routes:', error);
      showNotification('Failed to load routes data', 'error');
    } finally {
      setLoading(false);
    }
  };

  const filterRoutes = () => {
    let filtered = routes;

    if (searchTerm) {
      filtered = routes.filter(route =>
          `${route.predicates} ${route.routeId} ${route.uri}`
              .toLowerCase()
              .includes(searchTerm.toLowerCase())
      );
    }

    setFilteredRoutes(filtered);
  };

  const showNotification = (message, type = 'success') => {
    setNotification({ open: true, message, type });
    setTimeout(() => {
      setNotification({ ...notification, open: false });
    }, 5000);
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
    setCurrentPage(1);
  };

  const handlePageChange = (event, value) => {
    setCurrentPage(value);
  };

  const handleEditRateLimit = (route) => {
    setCurrentRoute(route);
    setRateLimitValues({
      maxRequests: route.rateLimit?.maxRequests || 10,
      timeWindowMs: route.rateLimit?.timeWindowMs || 60000
    });
    setEditDialogOpen(true);
  };

  const handleToggleRateLimit = async (route) => {
    try {
      const updatedRoute = {
        ...route,
        withRateLimit: !route.withRateLimit
      };

      // If enabling rate limiting and no rate limit exists, create default one
      if (updatedRoute.withRateLimit && !updatedRoute.rateLimit) {
        updatedRoute.rateLimit = { maxRequests: 10, timeWindowMs: 60000 };
      }

      await updateGatewayRoute(route.id, updatedRoute);
      showNotification(`Rate limiting ${updatedRoute.withRateLimit ? 'enabled' : 'disabled'}`);
      await loadRoutes();
    } catch (error) {
      console.error('Error toggling rate limit:', error);
      showNotification('Failed to update rate limit settings', 'error');
    }
  };

  const handleSaveRateLimit = async () => {
    try {
      const updatedRoute = {
        ...currentRoute,
        withRateLimit: true,
        rateLimit: {
          ...currentRoute.rateLimit,
          maxRequests: Number(rateLimitValues.maxRequests),
          timeWindowMs: Number(rateLimitValues.timeWindowMs)
        }
      };

      await updateGatewayRoute(currentRoute.id, updatedRoute);
      showNotification('Rate limit updated successfully');
      setEditDialogOpen(false);
      await loadRoutes();
    } catch (error) {
      console.error('Error updating rate limit:', error);
      showNotification('Failed to update rate limit', 'error');
    }
  };

  const paginatedRoutes = filteredRoutes.slice(
      (currentPage - 1) * pageSize,
      currentPage * pageSize
  );

  const totalPages = Math.ceil(filteredRoutes.length / pageSize);

  return (
      <Box sx={{ p: 3 }}>
        <Typography variant="h5" component="h1" gutterBottom>
          Rate Limit Management
        </Typography>

        {notification.open && (
            <Alert
                severity={notification.type}
                sx={{ mb: 2 }}
                onClose={() => setNotification({ ...notification, open: false })}
            >
              {notification.message}
            </Alert>
        )}

        <Box sx={{ display: 'flex', mb: 3, justifyContent: 'space-between' }}>
          <TextField
              placeholder="Search routes..."
              variant="outlined"
              size="small"
              value={searchTerm}
              onChange={handleSearchChange}
              InputProps={{
                startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon />
                    </InputAdornment>
                ),
              }}
              sx={{ width: 300 }}
          />
          <Button
              variant="contained"
              color="primary"
              onClick={loadRoutes}
          >
            Refresh
          </Button>
        </Box>

        <Paper sx={{ width: '100%', overflow: 'hidden' }}>
          <TableContainer sx={{ maxHeight: 'calc(100vh - 260px)' }}>
            <Table stickyHeader>
              <TableHead>
                <TableRow>
                  <TableCell>Route Path</TableCell>
                  <TableCell>Target URI</TableCell>
                  <TableCell align="center">Status</TableCell>
                  <TableCell align="center">Max Requests</TableCell>
                  <TableCell align="center">Time Window</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center">Loading...</TableCell>
                    </TableRow>
                ) : paginatedRoutes.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center">No routes found</TableCell>
                    </TableRow>
                ) : (
                    paginatedRoutes.map((route) => (
                        <TableRow key={route.id} hover>
                          <TableCell>{route.predicates}</TableCell>
                          <TableCell>{route.uri}</TableCell>
                          <TableCell align="center">
                            <Chip
                                label={route.withRateLimit ? "Enabled" : "Disabled"}
                                color={route.withRateLimit ? "success" : "default"}
                                size="small"
                            />
                          </TableCell>
                          <TableCell align="center">
                            {route.rateLimit?.maxRequests || '-'}
                          </TableCell>
                          <TableCell align="center">
                            {route.rateLimit?.timeWindowMs
                                ? `${route.rateLimit.timeWindowMs / 1000}s`
                                : '-'
                            }
                          </TableCell>
                          <TableCell align="right">
                            <Button
                                variant="outlined"
                                size="small"
                                sx={{ mr: 1 }}
                                onClick={() => handleToggleRateLimit(route)}
                            >
                              {route.withRateLimit ? 'Disable' : 'Enable'}
                            </Button>
                            <Button
                                variant="contained"
                                size="small"
                                color="primary"
                                disabled={!route.withRateLimit}
                                onClick={() => handleEditRateLimit(route)}
                            >
                              Configure
                            </Button>
                          </TableCell>
                        </TableRow>
                    ))
                )}
              </TableBody>
            </Table>
          </TableContainer>

          {totalPages > 1 && (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
                <Pagination
                    count={totalPages}
                    page={currentPage}
                    onChange={handlePageChange}
                    color="primary"
                />
              </Box>
          )}
        </Paper>

        {/* Edit Rate Limit Dialog */}
        <Dialog
            open={editDialogOpen}
            onClose={() => setEditDialogOpen(false)}
            maxWidth="sm"
            fullWidth
        >
          <DialogTitle>Configure Rate Limit</DialogTitle>
          <DialogContent>
            <Box sx={{ pt: 2 }}>
              <Typography variant="subtitle1" gutterBottom>
                Route: {currentRoute?.predicates}
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
                This will allow {rateLimitValues.maxRequests} requests per {rateLimitValues.timeWindowMs / 1000} seconds.
              </Typography>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setEditDialogOpen(false)}>Cancel</Button>
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

export default RateLimitDashboard;