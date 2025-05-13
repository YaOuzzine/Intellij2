// src/pages/SystemSettingsPage.jsx
import React, { useState, useEffect, useContext } from 'react';
import {
  Typography,
  Box,
  Paper,
  Button,
  TextField,
  Grid,
  Avatar,
  Tab,
  Tabs,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Snackbar,
  Alert,
  CircularProgress,
  Table,
  TableContainer,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  Chip,
  InputAdornment,
  IconButton,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Divider,
  Card,
  CardContent,
  CardActions
} from '@mui/material';
import { styled } from '@mui/material/styles';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import apiClient from '../apiClient';

// Icons
import EditIcon from '@mui/icons-material/Edit';
import PersonIcon from '@mui/icons-material/Person';
import GroupIcon from '@mui/icons-material/Group';
import DeleteIcon from '@mui/icons-material/Delete';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import SearchIcon from '@mui/icons-material/Search';
import PhotoCameraIcon from '@mui/icons-material/PhotoCamera';
import LogoutIcon from '@mui/icons-material/Logout';
import SaveIcon from '@mui/icons-material/Save';
import AddIcon from '@mui/icons-material/Add';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import BlockIcon from '@mui/icons-material/Block';

// Styled components
const StyledTabs = styled(Tabs)(({ theme }) => ({
  borderBottom: `1px solid ${theme.palette.divider}`,
  '& .MuiTabs-indicator': {
    backgroundColor: theme.palette.primary.main,
    height: 3
  },
}));

const StyledTab = styled(Tab)(({ theme }) => ({
  textTransform: 'none',
  fontWeight: theme.typography.fontWeightRegular,
  fontSize: theme.typography.pxToRem(15),
  marginRight: theme.spacing(1),
  minHeight: 48,
  '&.Mui-selected': {
    fontWeight: theme.typography.fontWeightMedium,
  },
}));

const ProfileAvatar = styled(Avatar)(({ theme }) => ({
  width: 120,
  height: 120,
  fontSize: 48,
  backgroundColor: theme.palette.primary.light,
  color: theme.palette.primary.contrastText,
  boxShadow: theme.shadows[3],
  border: `4px solid ${theme.palette.background.paper}`,
  margin: '0 auto 20px auto'
}));

const UploadButton = styled(IconButton)(({ theme }) => ({
  position: 'absolute',
  bottom: 10,
  right: '50%',
  transform: 'translateX(60px)',
  backgroundColor: theme.palette.background.paper,
  color: theme.palette.primary.main,
  boxShadow: theme.shadows[2],
  '&:hover': {
    backgroundColor: theme.palette.grey[200],
  },
}));

const UserRoleChip = styled(Chip)(({ theme, role }) => ({
  backgroundColor: role === 'ADMIN'
      ? theme.palette.error.main
      : theme.palette.info.main,
  color: '#fff',
  fontWeight: 'bold'
}));

// Tab Panel component
function TabPanel(props) {
  const { children, value, index, ...other } = props;
  return (
      <div
          role="tabpanel"
          hidden={value !== index}
          id={`settings-tabpanel-${index}`}
          aria-labelledby={`settings-tab-${index}`}
          {...other}
      >
        {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
      </div>
  );
}

const SystemSettingsPage = () => {
  const navigate = useNavigate();
  const { logout, user: contextUser, updateUserProfile } = useContext(AuthContext);

  // Tab state
  const [activeTab, setActiveTab] = useState(0);

  // Loading and notification states
  const [loading, setLoading] = useState(false);
  const [notification, setNotification] = useState({
    open: false,
    message: '',
    severity: 'success'
  });

  // Profile data state
  const [profileData, setProfileData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    username: ''
  });

  const [profileErrors, setProfileErrors] = useState({});

  // Password state
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });

  const [passwordErrors, setPasswordErrors] = useState({});
  const [passwordVisible, setPasswordVisible] = useState(false);

  // Profile picture state
  const [profileImage, setProfileImage] = useState(null);
  const [openPictureDialog, setOpenPictureDialog] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);

  // User management state (Admin only)
  const [users, setUsers] = useState([]);
  const [userSearchTerm, setUserSearchTerm] = useState('');
  const [filteredUsers, setFilteredUsers] = useState([]);

  // Add/Edit user dialog
  const [userDialog, setUserDialog] = useState({
    open: false,
    isEdit: false,
    data: {
      id: null,
      username: '',
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      role: 'USER'
    },
    errors: {}
  });

  // Delete confirmation dialog
  const [deleteDialog, setDeleteDialog] = useState({
    open: false,
    userId: null
  });

  // Logout confirmation dialog
  const [logoutDialog, setLogoutDialog] = useState(false);

  // Notification functions
  const showNotification = (message, severity = 'success') => {
    setNotification({ open: true, message, severity });
  };

  const handleCloseNotification = () => {
    setNotification(prev => ({ ...prev, open: false }));
  };

  // Load user profile data
  const loadUserProfile = async () => {
    setLoading(true);
    try {
      const response = await apiClient.get('/user/profile');
      const userData = response.data;

      setProfileData({
        firstName: userData.firstName || '',
        lastName: userData.lastName || '',
        email: userData.email || '',
        username: userData.username || ''
      });

      setProfileImage(userData.profileImageUrl || null);
    } catch (error) {
      console.error('Error loading profile:', error);
      showNotification(
          error.response?.data?.message || 'Unable to load your profile',
          'error'
      );
    } finally {
      setLoading(false);
    }
  };

  // gateway-admin-dashboard/src/pages/SystemSettingsPage.jsx

  const loadUsers = async () => {
    if (!contextUser?.isAdmin) return;

    console.log('SystemSettingsPage: Loading users for admin user', contextUser);
    setLoading(true);
    try {
      // Use the same pattern that's succeeding for user/profile
      console.log('SystemSettingsPage: Making API request to /user/all');
      const response = await apiClient.get('/user/all'); // Match the working pattern
      console.log('SystemSettingsPage: Received user data:', response.data);
      setUsers(response.data);
      setFilteredUsers(response.data);
    } catch (error) {
      // Error logging remains the same
      console.error('SystemSettingsPage: Error loading users:', error);
      if (error.response) {
        console.error('SystemSettingsPage: Error response status:', error.response.status);
        console.error('SystemSettingsPage: Error response data:', error.response.data);
      } else if (error.request) {
        console.error('SystemSettingsPage: No response received:', error.request);
      } else {
        console.error('SystemSettingsPage: Error message:', error.message);
      }
      showNotification('Unable to load users', 'error');
    } finally {
      setLoading(false);
    }
  };

  // Initial data loading
  useEffect(() => {
    loadUserProfile();
    if (contextUser?.isAdmin) {
      loadUsers();
    }
  }, [contextUser]);

  // Filter users when search term changes
  useEffect(() => {
    if (!users.length) return;

    const searchLower = userSearchTerm.toLowerCase();
    setFilteredUsers(
        users.filter(user =>
            user.username?.toLowerCase().includes(searchLower) ||
            `${user.firstName} ${user.lastName}`.toLowerCase().includes(searchLower) ||
            user.email?.toLowerCase().includes(searchLower) ||
            user.role?.toLowerCase().includes(searchLower)
        )
    );
  }, [userSearchTerm, users]);

  // Tab change handler
  const handleTabChange = (event, newValue) => {
    setActiveTab(newValue);
  };

  // Profile form handlers
  const handleProfileChange = (e) => {
    const { name, value } = e.target;
    setProfileData(prev => ({ ...prev, [name]: value }));
    if (profileErrors[name]) {
      setProfileErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  // Password form handlers
  const handlePasswordChange = (e) => {
    const { name, value } = e.target;
    setPasswordData(prev => ({ ...prev, [name]: value }));
    if (passwordErrors[name]) {
      setPasswordErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const togglePasswordVisibility = () => {
    setPasswordVisible(!passwordVisible);
  };

  // File handlers
  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setSelectedFile(file);

      const reader = new FileReader();
      reader.onloadend = () => {
        setPreviewUrl(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  // User dialog handlers
  const handleUserDialogChange = (e) => {
    const { name, value } = e.target;
    setUserDialog(prev => ({
      ...prev,
      data: {
        ...prev.data,
        [name]: value
      }
    }));

    // Clear error when field is updated
    if (userDialog.errors[name]) {
      setUserDialog(prev => ({
        ...prev,
        errors: {
          ...prev.errors,
          [name]: ''
        }
      }));
    }
  };

  const openAddUserDialog = () => {
    setUserDialog({
      open: true,
      isEdit: false,
      data: {
        id: null,
        username: '',
        firstName: '',
        lastName: '',
        email: '',
        password: '',
        role: 'USER'
      },
      errors: {}
    });
  };

  const openEditUserDialog = (user) => {
    setUserDialog({
      open: true,
      isEdit: true,
      data: {
        id: user.id,
        username: user.username,
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email,
        password: '',
        role: user.role
      },
      errors: {}
    });
  };

  const closeUserDialog = () => {
    setUserDialog(prev => ({ ...prev, open: false }));
  };

  // Form validation
  const validateProfile = () => {
    const errors = {};
    if (!profileData.firstName.trim()) errors.firstName = 'First name is required';
    if (!profileData.lastName.trim()) errors.lastName = 'Last name is required';
    if (!profileData.email.trim()) {
      errors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(profileData.email)) {
      errors.email = 'Invalid email format';
    }

    setProfileErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const validatePassword = () => {
    const errors = {};
    if (!passwordData.currentPassword) {
      errors.currentPassword = 'Current password is required';
    }

    if (!passwordData.newPassword) {
      errors.newPassword = 'New password is required';
    } else if (passwordData.newPassword.length < 8) {
      errors.newPassword = 'Password must be at least 8 characters';
    }

    if (passwordData.newPassword !== passwordData.confirmPassword) {
      errors.confirmPassword = 'Passwords do not match';
    }

    setPasswordErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const validateUserForm = () => {
    const { data } = userDialog;
    const errors = {};

    if (!data.username.trim()) errors.username = 'Username is required';
    if (!data.firstName.trim()) errors.firstName = 'First name is required';
    if (!data.lastName.trim()) errors.lastName = 'Last name is required';

    if (!data.email.trim()) {
      errors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(data.email)) {
      errors.email = 'Invalid email format';
    }

    // Only validate password for new users
    if (!userDialog.isEdit && !data.password.trim()) {
      errors.password = 'Password is required';
    } else if (!userDialog.isEdit && data.password.length < 8) {
      errors.password = 'Password must be at least 8 characters';
    }

    setUserDialog(prev => ({ ...prev, errors }));
    return Object.keys(errors).length === 0;
  };

  // Form submission handlers
  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    if (!validateProfile()) return;

    setLoading(true);
    try {
      const response = await apiClient.put('/user/profile', profileData);
      updateUserProfile(response.data);
      showNotification('Profile updated successfully');
    } catch (error) {
      console.error('Error updating profile:', error);
      showNotification(
          error.response?.data?.message || 'Failed to update profile',
          'error'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleUpdatePassword = async (e) => {
    e.preventDefault();
    if (!validatePassword()) return;

    setLoading(true);
    try {
      await apiClient.put('/user/password', {
        currentPassword: passwordData.currentPassword,
        newPassword: passwordData.newPassword
      });

      setPasswordData({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      });

      showNotification('Password updated successfully');
    } catch (error) {
      console.error('Error updating password:', error);
      showNotification(
          error.response?.data?.message || 'Failed to update password',
          'error'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleUploadPicture = async () => {
    if (!selectedFile) {
      setOpenPictureDialog(false);
      return;
    }

    setLoading(true);

    try {
      // Create FormData for file upload
      const formData = new FormData();
      formData.append('profileImage', selectedFile);

      // Add detailed logging to help diagnose the issue
      console.log('File being uploaded:', selectedFile.name, selectedFile.type, selectedFile.size);

      // boundary for multipart/form-data
      const response = await apiClient.post('/user/profile-image', formData, {
        headers: {
          'Content-Type': undefined // Let browser set correct content type with boundary
        }
      });

      setProfileImage(response.data.profileImageUrl);
      updateUserProfile({ profileImageUrl: response.data.profileImageUrl });

      setOpenPictureDialog(false);
      setSelectedFile(null);
      setPreviewUrl(null);

      showNotification('Profile picture uploaded successfully');
    } catch (error) {
      console.error('Error uploading profile picture:', error);
      showNotification(
          error.response?.data?.message || 'Failed to upload profile picture',
          'error'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleSaveUser = async () => {
    if (!validateUserForm()) return;

    setLoading(true);
    try {
      const { data, isEdit } = userDialog;

      if (isEdit) {
        // Update existing user
        const response = await apiClient.put(`/user/${data.id}`, data);
        setUsers(prev =>
            prev.map(user => user.id === data.id ? response.data : user)
        );
        showNotification('User updated successfully');
      } else {
        // Create new user
        const response = await apiClient.post('/user', data);
        setUsers(prev => [...prev, response.data]);
        showNotification('User created successfully');
      }

      closeUserDialog();
    } catch (error) {
      console.error('Error saving user:', error);
      showNotification(
          error.response?.data?.message || 'Failed to save user',
          'error'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteUser = async () => {
    if (!deleteDialog.userId) return;

    setLoading(true);
    try {
      await apiClient.delete(`/user/${deleteDialog.userId}`);
      setUsers(prev => prev.filter(user => user.id !== deleteDialog.userId));
      setDeleteDialog({ open: false, userId: null });
      showNotification('User deleted successfully');
    } catch (error) {
      console.error('Error deleting user:', error);
      showNotification(
          error.response?.data?.message || 'Failed to delete user',
          'error'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleToggleUserStatus = async (userId, currentStatus) => {
    const newStatus = currentStatus === 'Active' ? false : true;

    setLoading(true);
    try {
      const response = await apiClient.patch(`/user/${userId}/status`, {
        active: newStatus
      });

      setUsers(prev =>
          prev.map(user => user.id === userId ? response.data : user)
      );

      showNotification('User status updated successfully');
    } catch (error) {
      console.error('Error updating user status:', error);
      showNotification(
          error.response?.data?.message || 'Failed to update user status',
          'error'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  // Helper functions
  const getInitials = (firstName, lastName) => {
    return `${firstName?.charAt(0) || ''}${lastName?.charAt(0) || ''}`.toUpperCase();
  };

  // If loading profile data initially
  if (loading && !profileData.email) {
    return (
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh' }}>
          <CircularProgress />
        </Box>
    );
  }

  return (
      <Box sx={{ p: 3 }}>
        <Typography variant="h5" gutterBottom sx={{ fontWeight: 'bold', mb: 3 }}>
          Settings
        </Typography>

        <Paper sx={{ mb: 4, borderRadius: 2, overflow: 'hidden' }}>
          <StyledTabs
              value={activeTab}
              onChange={handleTabChange}
              aria-label="settings tabs"
          >
            <StyledTab
                icon={<PersonIcon />}
                label="Profile"
                iconPosition="start"
            />
            {contextUser?.isAdmin && (
                <StyledTab
                    icon={<GroupIcon />}
                    label="User Management"
                    iconPosition="start"
                />
            )}
          </StyledTabs>

          {/* Profile Tab */}
          <TabPanel value={activeTab} index={0}>
            <Grid container spacing={4}>
              {/* Profile Picture Section */}
              <Grid item xs={12} md={4}>
                <Box sx={{ textAlign: 'center', position: 'relative' }}>
                  <ProfileAvatar src={profileImage}>
                    {getInitials(profileData.firstName, profileData.lastName)}
                  </ProfileAvatar>
                  <UploadButton
                      onClick={() => setOpenPictureDialog(true)}
                      color="primary"
                      aria-label="change profile picture"
                  >
                    <PhotoCameraIcon />
                  </UploadButton>
                  <Typography variant="subtitle1" fontWeight="medium">
                    {profileData.username}
                  </Typography>
                  {contextUser?.isAdmin && (
                      <Chip
                          label="Administrator"
                          color="error"
                          size="small"
                          sx={{ mt: 1 }}
                      />
                  )}
                </Box>

                {/* Password Change Card */}
                <Card sx={{ mt: 4, borderRadius: 2 }}>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Change Password
                    </Typography>
                    <TextField
                        name="currentPassword"
                        label="Current Password"
                        type={passwordVisible ? 'text' : 'password'}
                        fullWidth
                        margin="normal"
                        value={passwordData.currentPassword}
                        onChange={handlePasswordChange}
                        error={!!passwordErrors.currentPassword}
                        helperText={passwordErrors.currentPassword}
                        InputProps={{
                          endAdornment: (
                              <InputAdornment position="end">
                                <IconButton
                                    onClick={togglePasswordVisibility}
                                    edge="end"
                                >
                                  {passwordVisible ? <VisibilityOffIcon /> : <VisibilityIcon />}
                                </IconButton>
                              </InputAdornment>
                          )
                        }}
                    />
                    <TextField
                        name="newPassword"
                        label="New Password"
                        type={passwordVisible ? 'text' : 'password'}
                        fullWidth
                        margin="normal"
                        value={passwordData.newPassword}
                        onChange={handlePasswordChange}
                        error={!!passwordErrors.newPassword}
                        helperText={passwordErrors.newPassword || 'Minimum 8 characters'}
                    />
                    <TextField
                        name="confirmPassword"
                        label="Confirm New Password"
                        type={passwordVisible ? 'text' : 'password'}
                        fullWidth
                        margin="normal"
                        value={passwordData.confirmPassword}
                        onChange={handlePasswordChange}
                        error={!!passwordErrors.confirmPassword}
                        helperText={passwordErrors.confirmPassword}
                    />
                  </CardContent>
                  <CardActions sx={{ justifyContent: 'flex-end', p: 2 }}>
                    <Button
                        variant="contained"
                        color="primary"
                        onClick={handleUpdatePassword}
                        disabled={loading}
                    >
                      {loading ? <CircularProgress size={24} /> : 'Update Password'}
                    </Button>
                  </CardActions>
                </Card>
              </Grid>

              {/* Profile Information Form */}
              <Grid item xs={12} md={8}>
                <Card sx={{ borderRadius: 2 }}>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Personal Information
                    </Typography>
                    <Grid container spacing={2}>
                      <Grid item xs={12} sm={6}>
                        <TextField
                            name="firstName"
                            label="First Name"
                            fullWidth
                            value={profileData.firstName}
                            onChange={handleProfileChange}
                            error={!!profileErrors.firstName}
                            helperText={profileErrors.firstName}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <TextField
                            name="lastName"
                            label="Last Name"
                            fullWidth
                            value={profileData.lastName}
                            onChange={handleProfileChange}
                            error={!!profileErrors.lastName}
                            helperText={profileErrors.lastName}
                        />
                      </Grid>
                      <Grid item xs={12}>
                        <TextField
                            name="email"
                            label="Email Address"
                            fullWidth
                            value={profileData.email}
                            onChange={handleProfileChange}
                            error={!!profileErrors.email}
                            helperText={profileErrors.email}
                        />
                      </Grid>
                    </Grid>
                  </CardContent>
                  <CardActions sx={{ justifyContent: 'flex-end', p: 2 }}>
                    <Button
                        variant="contained"
                        color="primary"
                        startIcon={<SaveIcon />}
                        onClick={handleUpdateProfile}
                        disabled={loading}
                    >
                      {loading ? <CircularProgress size={24} /> : 'Save Changes'}
                    </Button>
                  </CardActions>
                </Card>

                {/* Logout Card */}
                <Card sx={{ mt: 4, borderRadius: 2 }}>
                  <CardContent>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Box>
                        <Typography variant="h6">Account Management</Typography>
                        <Typography variant="body2" color="text.secondary">
                          Log out of your account
                        </Typography>
                      </Box>
                      <Button
                          variant="outlined"
                          color="error"
                          startIcon={<LogoutIcon />}
                          onClick={() => setLogoutDialog(true)}
                      >
                        Log Out
                      </Button>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>
          </TabPanel>

          {/* User Management Tab (Admin Only) */}
          {contextUser?.isAdmin && (
              <TabPanel value={activeTab} index={1}>
                <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="h6">User Management</Typography>
                  <Box sx={{ display: 'flex', gap: 2 }}>
                    <TextField
                        placeholder="Search users..."
                        size="small"
                        value={userSearchTerm}
                        onChange={(e) => setUserSearchTerm(e.target.value)}
                        InputProps={{
                          startAdornment: (
                              <InputAdornment position="start">
                                <SearchIcon fontSize="small" />
                              </InputAdornment>
                          )
                        }}
                    />
                    <Button
                        variant="contained"
                        color="primary"
                        startIcon={<AddIcon />}
                        onClick={openAddUserDialog}
                    >
                      Add User
                    </Button>
                  </Box>
                </Box>

                <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 2 }}>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Username</TableCell>
                        <TableCell>Name</TableCell>
                        <TableCell>Email</TableCell>
                        <TableCell>Role</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell align="right">Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {loading && filteredUsers.length === 0 ? (
                          <TableRow>
                            <TableCell colSpan={6} align="center" sx={{ py: 3 }}>
                              <CircularProgress size={24} />
                            </TableCell>
                          </TableRow>
                      ) : filteredUsers.length === 0 ? (
                          <TableRow>
                            <TableCell colSpan={6} align="center" sx={{ py: 3 }}>
                              <Typography variant="body2" color="text.secondary">
                                No users found
                              </Typography>
                            </TableCell>
                          </TableRow>
                      ) : (
                          filteredUsers.map(user => (
                              <TableRow key={user.id} hover>
                                <TableCell>{user.username}</TableCell>
                                <TableCell>{user.firstName} {user.lastName}</TableCell>
                                <TableCell>{user.email}</TableCell>
                                <TableCell>
                                  <UserRoleChip
                                      label={user.role}
                                      role={user.role}
                                      size="small"
                                  />
                                </TableCell>
                                <TableCell>
                                  <Chip
                                      icon={user.status === 'Active' ? <CheckCircleIcon /> : <BlockIcon />}
                                      label={user.status}
                                      color={user.status === 'Active' ? 'success' : 'default'}
                                      size="small"
                                      variant="outlined"
                                      onClick={() => handleToggleUserStatus(user.id, user.status)}
                                  />
                                </TableCell>
                                <TableCell align="right">
                                  <IconButton
                                      color="primary"
                                      onClick={() => openEditUserDialog(user)}
                                      size="small"
                                      sx={{ mr: 1 }}
                                  >
                                    <EditIcon fontSize="small" />
                                  </IconButton>
                                  <IconButton
                                      color="error"
                                      onClick={() => setDeleteDialog({ open: true, userId: user.id })}
                                      size="small"
                                      disabled={user.id === contextUser.id}
                                  >
                                    <DeleteIcon fontSize="small" />
                                  </IconButton>
                                </TableCell>
                              </TableRow>
                          ))
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              </TabPanel>
          )}
        </Paper>

        {/* Profile Picture Dialog */}
        <Dialog
            open={openPictureDialog}
            onClose={() => setOpenPictureDialog(false)}
            maxWidth="xs"
            fullWidth
        >
          <DialogTitle>Update Profile Picture</DialogTitle>
          <DialogContent>
            <Box sx={{ textAlign: 'center', my: 2 }}>
              {previewUrl ? (
                  <Avatar
                      src={previewUrl}
                      sx={{ width: 150, height: 150, mx: 'auto', mb: 2 }}
                  />
              ) : (
                  <Avatar
                      src={profileImage}
                      sx={{ width: 150, height: 150, mx: 'auto', mb: 2, fontSize: 60 }}
                  >
                    {getInitials(profileData.firstName, profileData.lastName)}
                  </Avatar>
              )}
              <Button
                  variant="contained"
                  component="label"
                  sx={{ mt: 2 }}
              >
                Select Image
                <input
                    type="file"
                    hidden
                    accept="image/*"
                    onChange={handleFileChange}
                />
              </Button>
              <Typography variant="caption" display="block" sx={{ mt: 1 }}>
                For best results, use a square image
              </Typography>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenPictureDialog(false)}>
              Cancel
            </Button>
            <Button
                variant="contained"
                color="primary"
                onClick={handleUploadPicture}
                disabled={!selectedFile || loading}
            >
              {loading ? <CircularProgress size={24} /> : 'Upload'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Add/Edit User Dialog */}
        <Dialog
            open={userDialog.open}
            onClose={closeUserDialog}
            maxWidth="md"
            fullWidth
        >
          <DialogTitle>
            {userDialog.isEdit ? 'Edit User' : 'Add New User'}
          </DialogTitle>
          <DialogContent dividers>
            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12} sm={6}>
                <TextField
                    name="username"
                    label="Username"
                    fullWidth
                    value={userDialog.data.username}
                    onChange={handleUserDialogChange}
                    error={!!userDialog.errors.username}
                    helperText={userDialog.errors.username}
                    disabled={userDialog.isEdit}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <FormControl fullWidth>
                  <InputLabel>Role</InputLabel>
                  <Select
                      name="role"
                      value={userDialog.data.role}
                      onChange={handleUserDialogChange}
                      label="Role"
                  >
                    <MenuItem value="USER">User</MenuItem>
                    <MenuItem value="ADMIN">Administrator</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                    name="firstName"
                    label="First Name"
                    fullWidth
                    value={userDialog.data.firstName}
                    onChange={handleUserDialogChange}
                    error={!!userDialog.errors.firstName}
                    helperText={userDialog.errors.firstName}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                    name="lastName"
                    label="Last Name"
                    fullWidth
                    value={userDialog.data.lastName}
                    onChange={handleUserDialogChange}
                    error={!!userDialog.errors.lastName}
                    helperText={userDialog.errors.lastName}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                    name="email"
                    label="Email Address"
                    fullWidth
                    value={userDialog.data.email}
                    onChange={handleUserDialogChange}
                    error={!!userDialog.errors.email}
                    helperText={userDialog.errors.email}
                />
              </Grid>
              {!userDialog.isEdit && (
                  <Grid item xs={12}>
                    <TextField
                        name="password"
                        label="Password"
                        type={passwordVisible ? 'text' : 'password'}
                        fullWidth
                        value={userDialog.data.password}
                        onChange={handleUserDialogChange}
                        error={!!userDialog.errors.password}
                        helperText={userDialog.errors.password || 'Minimum 8 characters'}
                        InputProps={{
                          endAdornment: (
                              <InputAdornment position="end">
                                <IconButton
                                    onClick={togglePasswordVisibility}
                                    edge="end"
                                >
                                  {passwordVisible ? <VisibilityOffIcon /> : <VisibilityIcon />}
                                </IconButton>
                              </InputAdornment>
                          )
                        }}
                    />
                  </Grid>
              )}
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={closeUserDialog}>
              Cancel
            </Button>
            <Button
                variant="contained"
                color="primary"
                onClick={handleSaveUser}
                disabled={loading}
            >
              {loading ? <CircularProgress size={24} /> : (userDialog.isEdit ? 'Update' : 'Create')}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Delete User Dialog */}
        <Dialog
            open={deleteDialog.open}
            onClose={() => setDeleteDialog({ open: false, userId: null })}
            maxWidth="xs"
            fullWidth
        >
          <DialogTitle>Confirm Deletion</DialogTitle>
          <DialogContent>
            <Typography>
              Are you sure you want to delete this user? This action cannot be undone.
            </Typography>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDeleteDialog({ open: false, userId: null })}>
              Cancel
            </Button>
            <Button
                variant="contained"
                color="error"
                onClick={handleDeleteUser}
                disabled={loading}
            >
              {loading ? <CircularProgress size={24} /> : 'Delete'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Logout Confirmation Dialog */}
        <Dialog
            open={logoutDialog}
            onClose={() => setLogoutDialog(false)}
            maxWidth="xs"
            fullWidth
        >
          <DialogTitle>Confirm Logout</DialogTitle>
          <DialogContent>
            <Typography>
              Are you sure you want to log out of your account?
            </Typography>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setLogoutDialog(false)}>
              Cancel
            </Button>
            <Button
                variant="contained"
                color="primary"
                onClick={handleLogout}
            >
              Logout
            </Button>
          </DialogActions>
        </Dialog>

        {/* Notifications */}
        <Snackbar
            open={notification.open}
            autoHideDuration={5000}
            onClose={handleCloseNotification}
            anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        >
          <Alert
              onClose={handleCloseNotification}
              severity={notification.severity}
              sx={{ width: '100%' }}
              elevation={6}
              variant="filled"
          >
            {notification.message}
          </Alert>
        </Snackbar>
      </Box>
  );
};

export default SystemSettingsPage;