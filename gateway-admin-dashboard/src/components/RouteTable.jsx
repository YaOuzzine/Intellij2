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
  Collapse,
  alpha
} from '@mui/material';
import { styled, keyframes } from '@mui/material/styles';
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

// Define animations
const fadeIn = keyframes`
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
`;

const pulse = keyframes`
  0% { transform: scale(1); }
  50% { transform: scale(1.02); }
  100% { transform: scale(1); }
`;

const slideDown = keyframes`
  from { opacity: 0; max-height: 0; }
  to { opacity: 1; max-height: 800px; }
`;

// Styled components
const StyledTableContainer = styled(TableContainer)(({ theme }) => ({
  borderRadius: '12px',
  boxShadow: '0 8px 24px rgba(0, 0, 0, 0.1)',
  overflow: 'hidden',
  animation: `${fadeIn} 0.5s ease-out`,
  transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
  '&:hover': {
    boxShadow: '0 12px 28px rgba(0, 0, 0, 0.15)',
  },
}));

const StyledTableHead = styled(TableHead)(({ theme }) => ({
  background: `linear-gradient(145deg, ${theme.palette.background.default}, ${alpha(theme.palette.primary.main, 0.1)})`,
  '& .MuiTableCell-head': {
    color: theme.palette.text.primary,
    fontWeight: 600,
    borderBottom: `2px solid ${theme.palette.primary.main}`,
    padding: theme.spacing(1.5, 2),
  },
}));

const StyledTableRow = styled(TableRow)(({ theme, highlighted }) => ({
  transition: 'all 0.2s ease',
  position: 'relative',
  '&:hover': {
    backgroundColor: alpha(theme.palette.primary.main, 0.05),
    transform: 'translateY(-1px)',
    boxShadow: '0 3px 5px rgba(0, 0, 0, 0.05)',
  },
  ...(highlighted && {
    backgroundColor: alpha(theme.palette.primary.main, 0.08),
    animation: `${pulse} 2s 3`,
  }),
}));

const ActionButton = styled(Button)(({ theme, color = 'primary' }) => ({
  borderRadius: '8px',
  textTransform: 'none',
  boxShadow: 'none',
  transition: 'all 0.2s ease',
  '&:hover': {
    boxShadow: `0 4px 8px ${alpha(theme.palette[color].main, 0.25)}`,
    transform: 'translateY(-2px)',
  },
}));

const SearchTextField = styled(TextField)(({ theme }) => ({
  '& .MuiOutlinedInput-root': {
    borderRadius: '12px',
    transition: 'all 0.2s ease',
    backgroundColor: alpha(theme.palette.background.paper, 0.8),
    '&:hover': {
      backgroundColor: theme.palette.background.paper,
      boxShadow: '0 4px 8px rgba(0, 0, 0, 0.05)',
    },
    '&.Mui-focused': {
      boxShadow: `0 4px 16px ${alpha(theme.palette.primary.main, 0.15)}`,
    },
  },
}));

const ExpandButton = styled(IconButton)(({ theme, active }) => ({
  transition: 'transform 0.3s ease, background 0.2s ease',
  backgroundColor: active ? alpha(theme.palette.primary.main, 0.1) : 'transparent',
  '&:hover': {
    backgroundColor: alpha(theme.palette.primary.main, 0.2),
  },
}));

const SecurityIconButton = styled(IconButton)(({ theme, active }) => ({
  color: active ? theme.palette.primary.main : theme.palette.text.disabled,
  backgroundColor: active ? alpha(theme.palette.primary.main, 0.1) : 'transparent',
  transition: 'all 0.2s ease',
  '&:hover': {
    backgroundColor: alpha(theme.palette.primary.main, 0.2),
    transform: 'translateY(-2px)',
  },
}));

const ExpandedContent = styled(Box)(({ theme }) => ({
  animation: `${slideDown} 0.4s ease-out`,
  padding: theme.spacing(2, 3, 3),
  backgroundColor: alpha(theme.palette.background.paper, 0.5),
  borderBottomLeftRadius: '8px',
  borderBottomRightRadius: '8px',
  borderTop: `1px solid ${alpha(theme.palette.divider, 0.5)}`,
}));

const StatusChip = styled(Chip)(({ theme, status }) => ({
  fontWeight: 600,
  borderRadius: '16px',
  backgroundColor: status === 'active' ? alpha(theme.palette.success.light, 0.2) : alpha(theme.palette.grey[400], 0.2),
  color: status === 'active' ? theme.palette.success.dark : theme.palette.grey[700],
  border: status === 'active' ? `1px solid ${alpha(theme.palette.success.main, 0.3)}` : `1px solid ${alpha(theme.palette.grey[500], 0.3)}`,
  '.MuiChip-label': {
    padding: '0 12px',
  },
}));

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
        <Box sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          mb: 3,
          marginTop: '100px',
          animation: `${fadeIn} 0.4s ease-out`
        }}>
          <Typography variant="h6" fontWeight="bold" sx={{
            color: 'primary.main',
            position: 'relative',
            '&:after': {
              content: '""',
              position: 'absolute',
              bottom: -4,
              left: 0,
              width: 40,
              height: 3,
              backgroundColor: 'primary.main',
              borderRadius: 4
            }
          }}>
            Gateway Routes
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <SearchTextField
                size="small"
                variant="outlined"
                placeholder="Search routes..."
                value={searchTerm}
                onChange={handleSearchChange}
                InputProps={{
                  startAdornment: <SearchIcon sx={{ mr: 1, color: 'text.secondary' }} />
                }}
            />
            <ActionButton
                variant="contained"
                color="primary"
                startIcon={<AddIcon />}
                onClick={onAdd}
                sx={{
                  py: 1,
                  px: 2,
                  fontWeight: 600
                }}
            >
              Add Route
            </ActionButton>
          </Box>
        </Box>

        {/* Routes Table */}
        <StyledTableContainer component={Paper} elevation={0}>
          <Table>
            <StyledTableHead>
              <TableRow>
                <TableCell width="40px" /> {/* For expand/collapse */}
                <TableCell>Route ID</TableCell>
                <TableCell>Path</TableCell>
                <TableCell>URI</TableCell>
                <TableCell align="center">Security</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </StyledTableHead>
            <TableBody>
              {currentItems.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} align="center" sx={{ py: 4 }}>
                      <Box sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        py: 6,
                        opacity: 0.7
                      }}>
                        <SearchIcon sx={{ fontSize: 48, mb: 2, color: 'text.secondary' }} />
                        <Typography variant="subtitle1" color="text.secondary" fontWeight={500}>
                          No routes found
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          Try adjusting your search criteria
                        </Typography>
                      </Box>
                    </TableCell>
                  </TableRow>
              ) : (
                  currentItems.map((route) => (
                      <React.Fragment key={route.id}>
                        <StyledTableRow
                            highlighted={route.highlighted}
                            sx={{
                              cursor: 'pointer'
                            }}
                        >
                          <TableCell>
                            <ExpandButton
                                size="small"
                                onClick={() => toggleRowExpansion(route.id)}
                                active={expandedRows[route.id]}
                            >
                              {expandedRows[route.id] ? <KeyboardArrowUpIcon /> : <KeyboardArrowDownIcon />}
                            </ExpandButton>
                          </TableCell>
                          <TableCell>
                            <Typography variant="body2" fontWeight={500}>
                              {route.routeId || `route-${route.id}`}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            <Tooltip title={route.predicates} arrow placement="top">
                              <Typography
                                  variant="body2"
                                  sx={{
                                    maxWidth: 180,
                                    overflow: 'hidden',
                                    textOverflow: 'ellipsis',
                                    whiteSpace: 'nowrap'
                                  }}
                              >
                                {route.predicates}
                              </Typography>
                            </Tooltip>
                          </TableCell>
                          <TableCell>
                            <Tooltip title={route.uri} arrow placement="top">
                              <Typography
                                  variant="body2"
                                  sx={{
                                    maxWidth: 180,
                                    overflow: 'hidden',
                                    textOverflow: 'ellipsis',
                                    whiteSpace: 'nowrap'
                                  }}
                              >
                                {route.uri}
                              </Typography>
                            </Tooltip>
                          </TableCell>
                          <TableCell align="center">
                            <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1 }}>
                              <Tooltip title={`IP Filtering: ${route.withIpFilter ? 'Enabled' : 'Disabled'}`} arrow>
                                <Badge
                                    badgeContent={route.allowedIps?.length || 0}
                                    color={route.withIpFilter ? "success" : "default"}
                                    overlap="circular"
                                    max={99}
                                    sx={{
                                      '& .MuiBadge-badge': {
                                        fontWeight: 'bold',
                                        fontSize: '0.7rem'
                                      }
                                    }}
                                >
                                  <SecurityIconButton
                                      size="small"
                                      active={route.withIpFilter}
                                      onClick={() => handleDirectIpSetup(route)}
                                  >
                                    <NetworkCheckIcon />
                                  </SecurityIconButton>
                                </Badge>
                              </Tooltip>

                              <Tooltip title={`Token Validation: ${route.withToken ? 'Enabled' : 'Disabled'}`} arrow>
                                <SecurityIconButton
                                    size="small"
                                    active={route.withToken}
                                    onClick={() => onToggleTokenValidation(route)}
                                >
                                  <SecurityIcon />
                                </SecurityIconButton>
                              </Tooltip>

                              <Tooltip title={`Rate Limiting: ${route.withRateLimit ? 'Enabled' : 'Disabled'}`} arrow>
                                <SecurityIconButton
                                    size="small"
                                    active={route.withRateLimit}
                                    onClick={() => onToggleRateLimit(route)}
                                >
                                  <SpeedIcon />
                                </SecurityIconButton>
                              </Tooltip>
                            </Box>
                          </TableCell>
                          <TableCell align="right">
                            <ActionButton
                                variant="outlined"
                                color="primary"
                                size="small"
                                startIcon={<EditIcon />}
                                sx={{ mr: 1 }}
                                onClick={() => onUpdate(route)}
                            >
                              Edit
                            </ActionButton>
                            <ActionButton
                                variant="outlined"
                                color="error"
                                size="small"
                                startIcon={<DeleteIcon />}
                                onClick={() => onDelete(route.id)}
                            >
                              Delete
                            </ActionButton>
                          </TableCell>
                        </StyledTableRow>
                        <TableRow>
                          <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={6}>
                            <Collapse in={expandedRows[route.id]} timeout="auto" unmountOnExit>
                              <ExpandedContent>
                                <Box sx={{
                                  mb: 2,
                                  display: 'flex',
                                  justifyContent: 'space-between',
                                  alignItems: 'center'
                                }}>
                                  <Typography variant="subtitle1" fontWeight="bold" color="primary.main">
                                    Security Settings
                                  </Typography>
                                  <ActionButton
                                      variant="contained"
                                      size="small"
                                      color="primary"
                                      startIcon={<NetworkCheckIcon />}
                                      onClick={() => handleOpenIpManager(route)}
                                  >
                                    Manage IP Whitelist
                                  </ActionButton>
                                </Box>

                                <Paper sx={{ p: 2, mb: 2, borderRadius: '8px', bgcolor: alpha('#f5f5f5', 0.5) }}>
                                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
                                    <FormControlLabel
                                        control={
                                          <Switch
                                              checked={route.withIpFilter || false}
                                              onChange={() => onToggleIpFilter(route)}
                                              color="primary"
                                          />
                                        }
                                        label={
                                          <Box sx={{ display: 'flex', alignItems: 'center' }}>
                                            <NetworkCheckIcon fontSize="small" sx={{ mr: 0.5 }} />
                                            <Typography fontWeight={500}>IP Filtering</Typography>
                                          </Box>
                                        }
                                    />

                                    <FormControlLabel
                                        control={
                                          <Switch
                                              checked={route.withToken || false}
                                              onChange={() => onToggleTokenValidation(route)}
                                              color="primary"
                                          />
                                        }
                                        label={
                                          <Box sx={{ display: 'flex', alignItems: 'center' }}>
                                            <SecurityIcon fontSize="small" sx={{ mr: 0.5 }} />
                                            <Typography fontWeight={500}>Token Validation</Typography>
                                          </Box>
                                        }
                                    />

                                    <FormControlLabel
                                        control={
                                          <Switch
                                              checked={route.withRateLimit || false}
                                              onChange={() => onToggleRateLimit(route)}
                                              color="primary"
                                          />
                                        }
                                        label={
                                          <Box sx={{ display: 'flex', alignItems: 'center' }}>
                                            <SpeedIcon fontSize="small" sx={{ mr: 0.5 }} />
                                            <Typography fontWeight={500}>Rate Limiting</Typography>
                                          </Box>
                                        }
                                    />
                                  </Box>
                                </Paper>

                                {route.withIpFilter && (
                                    <Box sx={{ mt: 2 }}>
                                      <Typography variant="subtitle2" fontWeight="bold" gutterBottom>
                                        Allowed IP Addresses:
                                      </Typography>
                                      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                                        {route.allowedIps && route.allowedIps.length > 0 ? (
                                            route.allowedIps.map(ip => (
                                                <StatusChip
                                                    key={ip.id}
                                                    label={ip.ip}
                                                    status="active"
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
                                      <Typography variant="subtitle2" fontWeight="bold" gutterBottom>
                                        Rate Limit Settings:
                                      </Typography>
                                      <Paper sx={{ p: 2, borderRadius: '8px', bgcolor: alpha('#fff9c4', 0.3), border: '1px dashed #ffc107' }}>
                                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                                          <SpeedIcon sx={{ mr: 1, color: 'warning.main' }} />
                                          <Typography variant="body2" fontWeight={500}>
                                            {route.rateLimit.maxRequests || 'Not set'} requests per{' '}
                                            {route.rateLimit.timeWindowMs ? (route.rateLimit.timeWindowMs / 1000) : 'N/A'} seconds
                                          </Typography>
                                        </Box>
                                      </Paper>
                                    </Box>
                                )}
                              </ExpandedContent>
                            </Collapse>
                          </TableCell>
                        </TableRow>
                      </React.Fragment>
                  ))
              )}
            </TableBody>
          </Table>
        </StyledTableContainer>

        {/* Pagination */}
        {totalPages > 1 && (
            <Box sx={{
              display: 'flex',
              justifyContent: 'center',
              mt: 3,
              animation: `${fadeIn} 0.6s ease-out`
            }}>
              <Pagination
                  count={totalPages}
                  page={currentPage}
                  onChange={(e, page) => setCurrentPage(page)}
                  color="primary"
                  shape="rounded"
                  sx={{
                    '& .MuiPaginationItem-root': {
                      borderRadius: '8px',
                      '&.Mui-selected': {
                        fontWeight: 'bold',
                        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.15)'
                      }
                    }
                  }}
              />
            </Box>
        )}

        {/* IP Management Dialog */}
        <Dialog
            open={openIpDialog}
            onClose={() => setOpenIpDialog(false)}
            maxWidth="sm"
            fullWidth
            PaperProps={{
              sx: {
                borderRadius: '12px',
                boxShadow: '0 12px 32px rgba(0, 0, 0, 0.2)',
                background: 'linear-gradient(145deg, #ffffff, #f8f9fa)'
              }
            }}
        >
          <DialogTitle sx={{
            borderBottom: '1px solid',
            borderColor: 'divider',
            pb: 2,
            fontWeight: 'bold'
          }}>
            IP Filtering for {selectedRoute?.predicates}
          </DialogTitle>
          <DialogContent sx={{ py: 3 }}>
            <Typography variant="body1" paragraph fontWeight={500}>
              IP filtering {selectedRoute?.withIpFilter ? 'is enabled' : 'is disabled'} for this route.
            </Typography>

            {selectedRoute?.withIpFilter && (
                <>
                  <Typography variant="body2" paragraph>
                    Current allowed IPs: {selectedRoute?.allowedIps?.length || 0}
                  </Typography>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 2 }}>
                    {selectedRoute?.allowedIps?.map(ip => (
                        <StatusChip
                            key={ip.id}
                            label={ip.ip}
                            status="active"
                            size="small"
                        />
                    ))}
                  </Box>
                </>
            )}

            <Box sx={{
              mt: 2,
              p: 2,
              bgcolor: alpha('#e3f2fd', 0.5),
              borderRadius: '8px',
              border: '1px dashed',
              borderColor: 'primary.light'
            }}>
              <Typography variant="body2" color="primary.main">
                For detailed IP management, you can go to the dedicated IP Management page.
              </Typography>
            </Box>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 3 }}>
            <Button
                onClick={() => setOpenIpDialog(false)}
                variant="outlined"
                sx={{
                  borderRadius: '8px',
                  textTransform: 'none'
                }}
            >
              Close
            </Button>
            <ActionButton
                variant="contained"
                color="primary"
                onClick={() => handleManageIps(selectedRoute?.id)}
                sx={{
                  borderRadius: '8px',
                  textTransform: 'none',
                  fontWeight: 600
                }}
            >
              Manage IP Addresses
            </ActionButton>
          </DialogActions>
        </Dialog>
      </Box>
  );
}