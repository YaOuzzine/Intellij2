// src/components/RouteTable.jsx
import React, { useState } from 'react';
import {
  Box,
  Typography,
  TextField,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Pagination,
  Button,
  Chip,
  Switch,
  FormControlLabel,
  Badge,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Collapse
} from '@mui/material';
import NetworkCheckIcon from '@mui/icons-material/NetworkCheck';
import SearchIcon from '@mui/icons-material/Search';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import SecurityIcon from '@mui/icons-material/Security';
import SpeedIcon from '@mui/icons-material/Speed';
import { useNavigate } from 'react-router-dom';

export default function RouteTable({
                                     routes,
                                     onUpdate,
                                     onDelete,
                                     onAdd,
                                     onToggleIpFilter,
                                     onToggleTokenValidation,
                                     onToggleRateLimit
                                   }) {
  // Route search and pagination
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 5;

  // IP integration
  const [openIpDialog, setOpenIpDialog] = useState(false);
  const [selectedRoute, setSelectedRoute] = useState(null);
  const [expandedRows, setExpandedRows] = useState({});
  const navigate = useNavigate();

  // Handle search
  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
    setCurrentPage(1);
  };

  // Filter by search term
  const filteredRoutes = routes.filter((route) => {
    const routeData = `${route.routeId} ${route.predicates} ${route.uri}`.toLowerCase();
    return routeData.includes(searchTerm.toLowerCase());
  });

  // Page calculation
  const totalPages = Math.ceil(filteredRoutes.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const currentItems = filteredRoutes.slice(startIndex, startIndex + itemsPerPage);

  // Row expansion toggle
  const toggleRowExpansion = (id) => {
    setExpandedRows(prev => ({
      ...prev,
      [id]: !prev[id]
    }));
  };

  // Open dialog with route IP details
  const handleOpenIpManager = (route) => {
    setSelectedRoute(route);
    setOpenIpDialog(true);
  };

  // Navigate to IP page for a specific route
  const handleManageIps = (routeId) => {
    setOpenIpDialog(false);
    navigate('/ip-management', { state: { routeId } });
  };

  // Toggle direct access to IP settings
  const handleDirectIpSetup = (route) => {
    onToggleIpFilter(route);
  };

  return (
      <Box>
        {/* Header and Search */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6" fontWeight="bold">
            Gateway Routes
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <TextField
                size="small"
                variant="outlined"
                placeholder="Search routes..."
                value={searchTerm}
                onChange={handleSearchChange}
                InputProps={{
                  startAdornment: <SearchIcon sx={{ mr: 1, color: 'text.secondary' }} />
                }}
                sx={{ mr: 2, width: 250 }}
            />
            <Button
                variant="contained"
                color="primary"
                startIcon={<AddIcon />}
                onClick={onAdd}
            >
              Add Route
            </Button>
          </Box>
        </Box>

        {/* Routes Table */}
        <TableContainer component={Paper} elevation={1}>
          <Table>
            <TableHead sx={{ bgcolor: 'background.default' }}>
              <TableRow>
                <TableCell width="40px" /> {/* For expand/collapse */}
                <TableCell>Route ID</TableCell>
                <TableCell>Path</TableCell>
                <TableCell>URI</TableCell>
                <TableCell align="center">Security</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {currentItems.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} align="center" sx={{ py: 3 }}>
                      <Typography variant="subtitle1" color="text.secondary">
                        No routes found
                      </Typography>
                    </TableCell>
                  </TableRow>
              ) : (
                  currentItems.map((route) => (
                      <React.Fragment key={route.id}>
                        <TableRow
                            hover
                            sx={{
                              '&:hover': { bgcolor: 'rgba(0, 0, 0, 0.04)' },
                              cursor: 'pointer'
                            }}
                        >
                          <TableCell>
                            <IconButton
                                size="small"
                                onClick={() => toggleRowExpansion(route.id)}
                            >
                              {expandedRows[route.id] ? <KeyboardArrowUpIcon /> : <KeyboardArrowDownIcon />}
                            </IconButton>
                          </TableCell>
                          <TableCell>{route.routeId || `route-${route.id}`}</TableCell>
                          <TableCell>{route.predicates}</TableCell>
                          <TableCell>{route.uri}</TableCell>
                          <TableCell align="center">
                            <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1 }}>
                              <Tooltip title={`IP Filtering: ${route.withIpFilter ? 'Enabled' : 'Disabled'}`}>
                                <Badge
                                    badgeContent={route.allowedIps?.length || 0}
                                    color={route.withIpFilter ? "success" : "default"}
                                    overlap="circular"
                                    max={99}
                                >
                                  <IconButton
                                      size="small"
                                      color={route.withIpFilter ? "primary" : "default"}
                                      onClick={() => handleDirectIpSetup(route)}
                                  >
                                    <NetworkCheckIcon />
                                  </IconButton>
                                </Badge>
                              </Tooltip>

                              <Tooltip title={`Token Validation: ${route.withToken ? 'Enabled' : 'Disabled'}`}>
                                <IconButton
                                    size="small"
                                    color={route.withToken ? "primary" : "default"}
                                    onClick={() => onToggleTokenValidation(route)}
                                >
                                  <SecurityIcon />
                                </IconButton>
                              </Tooltip>

                              <Tooltip title={`Rate Limiting: ${route.withRateLimit ? 'Enabled' : 'Disabled'}`}>
                                <IconButton
                                    size="small"
                                    color={route.withRateLimit ? "primary" : "default"}
                                    onClick={() => onToggleRateLimit(route)}
                                >
                                  <SpeedIcon />
                                </IconButton>
                              </Tooltip>
                            </Box>
                          </TableCell>
                          <TableCell align="right">
                            <Button
                                variant="outlined"
                                color="primary"
                                size="small"
                                startIcon={<EditIcon />}
                                sx={{ mr: 1, textTransform: 'none' }}
                                onClick={() => onUpdate(route)}
                            >
                              Edit
                            </Button>
                            <Button
                                variant="outlined"
                                color="error"
                                size="small"
                                startIcon={<DeleteIcon />}
                                sx={{ textTransform: 'none' }}
                                onClick={() => onDelete(route.id)}
                            >
                              Delete
                            </Button>
                          </TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={6}>
                            <Collapse in={expandedRows[route.id]} timeout="auto" unmountOnExit>
                              <Box sx={{ m: 2 }}>
                                <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                  <Typography variant="subtitle1" gutterBottom component="div">
                                    Security Settings
                                  </Typography>
                                  <Button
                                      variant="contained"
                                      size="small"
                                      color="primary"
                                      startIcon={<NetworkCheckIcon />}
                                      sx={{ textTransform: 'none' }}
                                      onClick={() => handleOpenIpManager(route)}
                                  >
                                    Manage IP Whitelist
                                  </Button>
                                </Box>

                                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
                                  <FormControlLabel
                                      control={
                                        <Switch
                                            checked={route.withIpFilter || false}
                                            onChange={() => onToggleIpFilter(route)}
                                            color="primary"
                                        />
                                      }
                                      label="IP Filtering"
                                  />

                                  <FormControlLabel
                                      control={
                                        <Switch
                                            checked={route.withToken || false}
                                            onChange={() => onToggleTokenValidation(route)}
                                            color="primary"
                                        />
                                      }
                                      label="Token Validation"
                                  />

                                  <FormControlLabel
                                      control={
                                        <Switch
                                            checked={route.withRateLimit || false}
                                            onChange={() => onToggleRateLimit(route)}
                                            color="primary"
                                        />
                                      }
                                      label="Rate Limiting"
                                  />
                                </Box>

                                {route.withIpFilter && (
                                    <Box sx={{ mt: 2 }}>
                                      <Typography variant="subtitle2" gutterBottom>
                                        Allowed IP Addresses:
                                      </Typography>
                                      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                                        {route.allowedIps && route.allowedIps.length > 0 ? (
                                            route.allowedIps.map(ip => (
                                                <Chip
                                                    key={ip.id}
                                                    label={ip.ip}
                                                    variant="outlined"
                                                    size="small"
                                                />
                                            ))
                                        ) : (
                                            <Typography variant="body2" color="text.secondary">
                                              No IP addresses configured
                                            </Typography>
                                        )}
                                      </Box>
                                    </Box>
                                )}

                                {route.withRateLimit && route.rateLimit && (
                                    <Box sx={{ mt: 2 }}>
                                      <Typography variant="subtitle2" gutterBottom>
                                        Rate Limit Settings:
                                      </Typography>
                                      <Typography variant="body2">
                                        Max Requests: {route.rateLimit.maxRequests || 'Not set'} requests per{' '}
                                        {route.rateLimit.timeWindowMs ? (route.rateLimit.timeWindowMs / 1000) : 'N/A'} seconds
                                      </Typography>
                                    </Box>
                                )}
                              </Box>
                            </Collapse>
                          </TableCell>
                        </TableRow>
                      </React.Fragment>
                  ))
              )}
            </TableBody>
          </Table>
        </TableContainer>

        {/* Pagination */}
        {totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
              <Pagination
                  count={totalPages}
                  page={currentPage}
                  onChange={(e, page) => setCurrentPage(page)}
                  color="primary"
                  shape="rounded"
              />
            </Box>
        )}

        {/* IP Management Dialog */}
        <Dialog
            open={openIpDialog}
            onClose={() => setOpenIpDialog(false)}
            maxWidth="sm"
            fullWidth
        >
          <DialogTitle>
            IP Filtering for {selectedRoute?.predicates}
          </DialogTitle>
          <DialogContent>
            <Typography variant="body1" paragraph>
              IP filtering {selectedRoute?.withIpFilter ? 'is enabled' : 'is disabled'} for this route.
            </Typography>

            {selectedRoute?.withIpFilter && (
                <>
                  <Typography variant="body2" paragraph>
                    Current allowed IPs: {selectedRoute?.allowedIps?.length || 0}
                  </Typography>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 2 }}>
                    {selectedRoute?.allowedIps?.map(ip => (
                        <Chip
                            key={ip.id}
                            label={ip.ip}
                            variant="outlined"
                        />
                    ))}
                  </Box>
                </>
            )}

            <Typography>
              For detailed IP management, you can go to the dedicated IP Management page.
            </Typography>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenIpDialog(false)}>
              Close
            </Button>
            <Button
                variant="contained"
                color="primary"
                onClick={() => handleManageIps(selectedRoute?.id)}
            >
              Manage IP Addresses
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
  );
}