// src/pages/IPManagementPage.jsx
import React, { useState, useEffect } from 'react';
import {
  Typography,
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Button,
  Pagination,
  TextField,
  InputAdornment,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  CircularProgress,
  Alert,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Chip,
  Snackbar,
  IconButton,
  Grid,
  Card,
  CardContent,
  CardActions,
  Divider
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import NetworkCheckIcon from '@mui/icons-material/NetworkCheck';
import CloseIcon from '@mui/icons-material/Close';
import { styled } from '@mui/material/styles';
import {
  fetchIpAddresses,
  fetchRoutesForIpManagement,
  addIpAddress,
  updateIpAddress,
  deleteIpAddress,
  deleteAllIpsForRoute
} from '../services/ipService';
import { useLocation } from 'react-router-dom';

const TableContainerStyled = styled(TableContainer)(({ theme }) => ({
  marginTop: theme.spacing(2)
}));

const ActionButton = styled(Button)(({ theme }) => ({
  textTransform: 'none',
  marginRight: theme.spacing(1)
}));

const ITEMS_PER_PAGE = 5;

const IPManagementPage = () => {
  const location = useLocation();
  const highlightedRouteId = location.state?.routeId;

  // State for IP lists, loading, and errors
  const [ips, setIps] = useState([]);
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // UI state
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const [viewMode, setViewMode] = useState('table'); // 'table', 'cards'
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });

  // Dialogs state
  const [openAdd, setOpenAdd] = useState(false);
  const [addForm, setAddForm] = useState({
    ip: '',
    gatewayRoute: { id: '' }
  });
  const [addFormErrors, setAddFormErrors] = useState({});

  const [openUpdate, setOpenUpdate] = useState(false);
  const [updateForm, setUpdateForm] = useState({
    id: null,
    ip: '',
    gatewayRoute: { id: null }
  });
  const [updateFormErrors, setUpdateFormErrors] = useState({});

  const [openDeleteConfirm, setOpenDeleteConfirm] = useState(false);
  const [itemToDelete, setItemToDelete] = useState(null);

  //-----------------------------------
  // Load IPs and routes
  //-----------------------------------
  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      // Add logging to see what's happening
      console.log("Attempting to fetch IP addresses...");

      // Try fetching IPs first, to isolate the issue
      const ipData = await fetchIpAddresses();
      console.log("Successfully fetched IP addresses:", ipData);

      console.log("Attempting to fetch routes...");
      const routeData = await fetchRoutesForIpManagement();
      console.log("Successfully fetched routes:", routeData);

      setIps(ipData);
      setRoutes(routeData);

      // Rest of your function...
    } catch (err) {
      console.error('Error details:', err);
      if (err.response) {
        console.error('Response status:', err.response.status);
        console.error('Response data:', err.response.data);
      }
      setError('Failed to load data. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [highlightedRouteId]);

  //-----------------------------------
  // Notification handling
  //-----------------------------------
  const showNotification = (message, severity = 'success') => {
    setNotification({
      open: true,
      message,
      severity
    });
  };

  const handleCloseNotification = () => {
    setNotification(prev => ({ ...prev, open: false }));
  };

  //-----------------------------------
  // Filter and pagination
  //-----------------------------------
  const filtered = ips.filter(ip =>
      `${ip.id} ${ip.gatewayRouteId} ${ip.predicate || ''} ${ip.ip}`
          .toLowerCase()
          .includes(search.toLowerCase())
  );

  const totalPages = Math.max(1, Math.ceil(filtered.length / ITEMS_PER_PAGE));
  const paginated = filtered.slice((page - 1) * ITEMS_PER_PAGE, page * ITEMS_PER_PAGE);

  //-----------------------------------
  // Form validation
  //-----------------------------------
  const validateAddForm = () => {
    const errors = {};
    if (!addForm.ip.trim()) {
      errors.ip = 'IP address is required';
    } else if (!isValidIp(addForm.ip)) {
      errors.ip = 'Invalid IP address format (use x.x.x.x)';
    }

    if (!addForm.gatewayRoute?.id) {
      errors.routeId = 'Route is required';
    }

    setAddFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const validateUpdateForm = () => {
    const errors = {};
    if (!updateForm.ip.trim()) {
      errors.ip = 'IP address is required';
    } else if (!isValidIp(updateForm.ip)) {
      errors.ip = 'Invalid IP address format (use x.x.x.x)';
    }

    setUpdateFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const isValidIp = (ip) => {
    const ipPattern = /^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$/;
    if (!ipPattern.test(ip)) return false;

    const octets = ip.split('.');
    return octets.every(octet => {
      const num = parseInt(octet);
      return num >= 0 && num <= 255;
    });
  };

  //-----------------------------------
  // CRUD operations
  //-----------------------------------
  const handleAdd = async () => {
    if (!validateAddForm()) return;

    setLoading(true);
    try {
      await addIpAddress({
        ip: addForm.ip,
        gatewayRoute: { id: addForm.gatewayRoute.id }
      });

      setOpenAdd(false);
      setAddForm({ ip: '', gatewayRoute: { id: '' } });
      showNotification('IP address added successfully');
      await loadData();
    } catch (err) {
      let errorMessage = 'Failed to add IP address';

      if (err.response?.data) {
        errorMessage = typeof err.response.data === 'string'
            ? err.response.data
            : JSON.stringify(err.response.data);
      }

      showNotification(errorMessage, 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdate = async () => {
    if (!validateUpdateForm()) return;

    setLoading(true);
    try {
      await updateIpAddress(updateForm.id, {
        ip: updateForm.ip,
        gatewayRoute: { id: updateForm.gatewayRoute.id }
      });

      setOpenUpdate(false);
      showNotification('IP address updated successfully');
      await loadData();
    } catch (err) {
      let errorMessage = 'Failed to update IP address';

      if (err.response?.data) {
        errorMessage = typeof err.response.data === 'string'
            ? err.response.data
            : JSON.stringify(err.response.data);
      }

      showNotification(errorMessage, 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!itemToDelete) return;

    setLoading(true);
    try {
      await deleteIpAddress(itemToDelete.id, itemToDelete.gatewayRouteId);
      showNotification('IP address deleted successfully');
      setOpenDeleteConfirm(false);
      setItemToDelete(null);
      await loadData();
    } catch (err) {
      showNotification('Failed to delete IP address', 'error');
    } finally {
      setLoading(false);
    }
  };

  const confirmDelete = (item) => {
    setItemToDelete(item);
    setOpenDeleteConfirm(true);
  };

  //-----------------------------------
  // Handle route with multiple IPs
  //-----------------------------------
  const getRouteIps = (routeId) => {
    return ips.filter(ip => ip.gatewayRouteId === routeId);
  };

  const handleDeleteAllIpsForRoute = async (routeId) => {
    if (!window.confirm('Are you sure you want to delete all IPs for this route? This will also disable IP filtering for the route.')) {
      return;
    }

    setLoading(true);
    try {
      await deleteAllIpsForRoute(routeId);
      showNotification('All IP addresses for this route deleted successfully');
      await loadData();
    } catch (err) {
      showNotification('Failed to delete IP addresses', 'error');
    } finally {
      setLoading(false);
    }
  };

  //-----------------------------------
  // Route grouping view
  //-----------------------------------
  const renderRouteCards = () => {
    // Get unique routes with their IPs
    const routeMap = {};

    // Filter routes that have IP filtering enabled or routes that are highlighted
    const filteredRoutes = routes.filter(route =>
        route.withIpFilter || (highlightedRouteId && route.id === parseInt(highlightedRouteId))
    );

    // Group IPs by route
    filteredRoutes.forEach(route => {
      routeMap[route.id] = {
        ...route,
        ips: ips.filter(ip => ip.gatewayRouteId === route.id)
      };
    });

    if (Object.keys(routeMap).length === 0) {
      return (
          <Box sx={{ textAlign: 'center', mt: 4 }}>
            <Typography variant="subtitle1" color="textSecondary">
              No routes with IP filtering enabled
            </Typography>
            <Button
                variant="contained"
                color="primary"
                sx={{ mt: 2 }}
                onClick={() => setViewMode('table')}
            >
              Switch to Table View
            </Button>
          </Box>
      );
    }

    return (
        <Grid container spacing={3}>
          {Object.values(routeMap).map(route => (
              <Grid item xs={12} md={6} lg={4} key={route.id}>
                <Card
                    sx={{
                      border: highlightedRouteId && route.id === parseInt(highlightedRouteId)
                          ? '2px solid #FF914D'
                          : 'none'
                    }}
                >
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      {route.predicate}
                    </Typography>
                    <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                      {route.uri}
                    </Typography>
                    <Typography variant="body2" sx={{ mb: 2 }}>
                      Route ID: {route.routeId || route.id}
                    </Typography>

                    <Divider sx={{ mb: 2 }} />

                    <Typography variant="subtitle2" gutterBottom>
                      Allowed IP Addresses ({route.ips.length}):
                    </Typography>

                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 2 }}>
                      {route.ips.length > 0 ? (
                          route.ips.map(ip => (
                              <Chip
                                  key={ip.id}
                                  label={ip.ip}
                                  onDelete={() => confirmDelete(ip)}
                                  size="small"
                              />
                          ))
                      ) : (
                          <Typography variant="body2" color="textSecondary">
                            No IP addresses configured
                          </Typography>
                      )}
                    </Box>
                  </CardContent>
                  <CardActions>
                    <Button
                        size="small"
                        startIcon={<AddIcon />}
                        onClick={() => {
                          setAddForm({ ip: '', gatewayRoute: { id: route.id } });
                          setAddFormErrors({});
                          setOpenAdd(true);
                        }}
                    >
                      Add IP
                    </Button>
                    <Button
                        size="small"
                        color="error"
                        startIcon={<DeleteIcon />}
                        onClick={() => handleDeleteAllIpsForRoute(route.id)}
                        disabled={route.ips.length === 0}
                    >
                      Remove All
                    </Button>
                  </CardActions>
                </Card>
              </Grid>
          ))}
        </Grid>
    );
  };

  //-----------------------------------
  // Table view
  //-----------------------------------
  const renderTable = () => (
      <>
        <TableContainerStyled component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>IP&nbsp;ID</TableCell>
                <TableCell>IP&nbsp;Address</TableCell>
                <TableCell>Route&nbsp;Path</TableCell>
                <TableCell>Route&nbsp;URI</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center">
                      <CircularProgress size={24} />
                    </TableCell>
                  </TableRow>
              ) : paginated.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center">
                      No IP addresses found
                    </TableCell>
                  </TableRow>
              ) : (
                  paginated.map(ip => (
                      <TableRow key={ip.id}>
                        <TableCell>{ip.id}</TableCell>
                        <TableCell>{ip.ip}</TableCell>
                        <TableCell>{ip.predicate || 'Unknown'}</TableCell>
                        <TableCell>{ip.routeUri || 'Unknown'}</TableCell>
                        <TableCell align="right">
                          <ActionButton
                              size="small"
                              variant="contained"
                              color="primary"
                              startIcon={<EditIcon />}
                              onClick={() => {
                                setUpdateForm({
                                  id: ip.id,
                                  ip: ip.ip,
                                  gatewayRoute: { id: ip.gatewayRouteId }
                                });
                                setUpdateFormErrors({});
                                setOpenUpdate(true);
                              }}
                          >
                            Edit
                          </ActionButton>
                          <ActionButton
                              size="small"
                              variant="contained"
                              color="error"
                              startIcon={<DeleteIcon />}
                              onClick={() => confirmDelete(ip)}
                          >
                            Delete
                          </ActionButton>
                        </TableCell>
                      </TableRow>
                  ))
              )}
            </TableBody>
          </Table>
        </TableContainerStyled>

        <Pagination
            page={page}
            count={totalPages}
            onChange={(_, v) => setPage(v)}
            sx={{ mt: 2 }}
        />
      </>
  );

  //-----------------------------------
  // Main render
  //-----------------------------------
  return (
      <Box sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h5">IP Address Management</Typography>
          <Box sx={{ display: 'flex', gap: 2 }}>
            <Button
                variant={viewMode === 'table' ? 'contained' : 'outlined'}
                onClick={() => setViewMode('table')}
            >
              Table View
            </Button>
            <Button
                variant={viewMode === 'cards' ? 'contained' : 'outlined'}
                onClick={() => setViewMode('cards')}
            >
              Route View
            </Button>
          </Box>
        </Box>

        {error && (
            <Alert severity="error" sx={{ mb: 3 }}>
              {error}
            </Alert>
        )}

        {/* Controls for table view */}
        {viewMode === 'table' && (
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <TextField
                  placeholder="Search IPs, routes..."
                  size="small"
                  value={search}
                  onChange={e => { setSearch(e.target.value); setPage(1); }}
                  InputProps={{
                    startAdornment: (
                        <InputAdornment position="start">
                          <SearchIcon />
                        </InputAdornment>
                    )
                  }}
                  sx={{ width: 280 }}
              />
              <Button
                  variant="contained"
                  color="primary"
                  startIcon={<AddIcon />}
                  onClick={() => {
                    setAddForm({ ip: '', gatewayRoute: { id: '' } });
                    setAddFormErrors({});
                    setOpenAdd(true);
                  }}
              >
                Add New IP
              </Button>
            </Box>
        )}

        {/* Main content */}
        {viewMode === 'table' ? renderTable() : renderRouteCards()}

        {/* Add IP Dialog */}
        <Dialog
            open={openAdd}
            onClose={() => setOpenAdd(false)}
            fullWidth
            maxWidth="sm"
        >
          <DialogTitle>
            Add New IP Address
            <IconButton
                aria-label="close"
                onClick={() => setOpenAdd(false)}
                sx={{ position: 'absolute', right: 8, top: 8 }}
            >
              <CloseIcon />
            </IconButton>
          </DialogTitle>
          <DialogContent dividers>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, py: 1 }}>
              <TextField
                  label="IP Address"
                  fullWidth
                  value={addForm.ip}
                  onChange={e => setAddForm(prev => ({
                    ...prev,
                    ip: e.target.value
                  }))}
                  error={!!addFormErrors.ip}
                  helperText={addFormErrors.ip}
                  placeholder="e.g., 192.168.1.1"
              />

              <FormControl fullWidth error={!!addFormErrors.routeId}>
                <InputLabel id="route-select-label">Route</InputLabel>
                <Select
                    labelId="route-select-label"
                    value={addForm.gatewayRoute.id}
                    label="Route"
                    onChange={e => setAddForm(prev => ({
                      ...prev,
                      gatewayRoute: { id: e.target.value }
                    }))}
                >
                  <MenuItem value="">
                    <em>Select a route</em>
                  </MenuItem>
                  {routes.map(route => (
                      <MenuItem key={route.id} value={route.id}>
                        {route.predicate} ({route.uri})
                      </MenuItem>
                  ))}
                </Select>
                {addFormErrors.routeId && (
                    <Typography variant="caption" color="error">
                      {addFormErrors.routeId}
                    </Typography>
                )}
              </FormControl>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenAdd(false)}>
              Cancel
            </Button>
            <Button
                onClick={handleAdd}
                variant="contained"
                color="primary"
                disabled={loading}
            >
              {loading ? <CircularProgress size={24} /> : 'Add IP Address'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Update IP Dialog */}
        <Dialog
            open={openUpdate}
            onClose={() => setOpenUpdate(false)}
            fullWidth
            maxWidth="sm"
        >
          <DialogTitle>
            Update IP Address
            <IconButton
                aria-label="close"
                onClick={() => setOpenUpdate(false)}
                sx={{ position: 'absolute', right: 8, top: 8 }}
            >
              <CloseIcon />
            </IconButton>
          </DialogTitle>
          <DialogContent dividers>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, py: 1 }}>
              <TextField
                  label="IP Address"
                  fullWidth
                  value={updateForm.ip}
                  onChange={e => setUpdateForm(prev => ({
                    ...prev,
                    ip: e.target.value
                  }))}
                  error={!!updateFormErrors.ip}
                  helperText={updateFormErrors.ip}
                  placeholder="e.g., 192.168.1.1"
              />

              <FormControl fullWidth>
                <InputLabel id="update-route-select-label">Route</InputLabel>
                <Select
                    labelId="update-route-select-label"
                    value={updateForm.gatewayRoute.id}
                    label="Route"
                    onChange={e => setUpdateForm(prev => ({
                      ...prev,
                      gatewayRoute: { id: e.target.value }
                    }))}
                >
                  {routes.map(route => (
                      <MenuItem key={route.id} value={route.id}>
                        {route.predicate} ({route.uri})
                      </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenUpdate(false)}>
              Cancel
            </Button>
            <Button
                onClick={handleUpdate}
                variant="contained"
                color="primary"
                disabled={loading}
            >
              {loading ? <CircularProgress size={24} /> : 'Update IP Address'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Delete Confirmation Dialog */}
        <Dialog
            open={openDeleteConfirm}
            onClose={() => setOpenDeleteConfirm(false)}
        >
          <DialogTitle>Confirm Deletion</DialogTitle>
          <DialogContent>
            <Typography>
              Are you sure you want to delete the IP address: <strong>{itemToDelete?.ip}</strong>?
            </Typography>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenDeleteConfirm(false)}>
              Cancel
            </Button>
            <Button
                onClick={handleDelete}
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
        >
          <Alert
              onClose={handleCloseNotification}
              severity={notification.severity}
              sx={{ width: '100%' }}
          >
            {notification.message}
          </Alert>
        </Snackbar>
      </Box>
  );
};

export default IPManagementPage;