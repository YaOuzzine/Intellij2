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
  TableContainer,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  Chip,
  InputAdornment,
  IconButton,
  Divider,
  Switch,
  FormControlLabel,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  Card,
  CardContent,
  Tooltip,
  ListItemIcon,
  Menu
} from '@mui/material';
import { styled } from '@mui/material/styles';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

// Icons
import EditIcon from '@mui/icons-material/Edit';
import LockIcon from '@mui/icons-material/Lock';
import SecurityIcon from '@mui/icons-material/Security';
import PersonIcon from '@mui/icons-material/Person';
import GroupIcon from '@mui/icons-material/Group';
import DeleteIcon from '@mui/icons-material/Delete';
import SettingsIcon from '@mui/icons-material/Settings';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import SearchIcon from '@mui/icons-material/Search';
import PhotoCameraIcon from '@mui/icons-material/PhotoCamera';
import LogoutIcon from '@mui/icons-material/Logout';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import NotificationsIcon from '@mui/icons-material/Notifications';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import BlockIcon from '@mui/icons-material/Block';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';

import apiClient from '../apiClient';

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
  backgroundColor: theme.palette.primary.main,
  boxShadow: theme.shadows[3],
  border: `4px solid white`,
}));

const UploadButton = styled(Button)(({ theme }) => ({
  position: 'absolute',
  bottom: 0,
  right: 0,
  minWidth: 'auto',
  width: 36,
  height: 36,
  borderRadius: '50%',
  padding: 0,
  backgroundColor: theme.palette.common.white,
  color: theme.palette.grey[800],
  boxShadow: theme.shadows[2],
  '&:hover': {
    backgroundColor: theme.palette.grey[200],
  },
}));

const UserRoleChip = styled(Chip)(({ theme, role }) => ({
  backgroundColor: role === 'Administrator'
      ? theme.palette.error.light
      : theme.palette.primary.light,
  color: 'white',
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
  const { logout, user, updateUserProfile } = useContext(AuthContext);

  // State for active tab
  const [activeTab, setActiveTab] = useState(0);
  const [loading, setLoading] = useState(false);

  // Notification system
  const [notification, setNotification] = useState({
    open: false,
    message: '',
    severity: 'success'
  });

  // Profile form state
  const [profileData, setProfileData] = useState({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
    email: user?.email || '',
    jobTitle: user?.jobTitle || '',
    department: user?.department || ''
  });

  // Password change form state with visibility toggles
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [passwordVisibility, setPasswordVisibility] = useState({
    currentPassword: false,
    newPassword: false,
    confirmPassword: false
  });

  // Profile picture state
  const [profileImage, setProfileImage] = useState(user?.profileImage || null);
  const [openPictureDialog, setOpenPictureDialog] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);

  // Form validation states
  const [profileErrors, setProfileErrors] = useState({});
  const [passwordErrors, setPasswordErrors] = useState({});

  // Security settings state
  const [securitySettings, setSecuritySettings] = useState({
    twoFactorEnabled: user?.twoFactorEnabled || false,
    sessionTimeout: user?.sessionTimeout || 30,
    notificationsEnabled: user?.notificationsEnabled || true
  });

  // User management state
  const [users, setUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [userSearchTerm, setUserSearchTerm] = useState('');
  const [openAddUserDialog, setOpenAddUserDialog] = useState(false);
  const [newUserData, setNewUserData] = useState({
    username: '',
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    role: 'User'
  });
  const [newUserErrors, setNewUserErrors] = useState({});
  const [newUserPasswordVisible, setNewUserPasswordVisible] = useState(false);

  // User actions menu
  const [userActionsAnchorEl, setUserActionsAnchorEl] = useState(null);
  const [selectedUserId, setSelectedUserId] = useState(null);

  // Confirmation dialogs
  const [confirmLogoutDialog, setConfirmLogoutDialog] = useState(false);
  const [confirmDeleteDialog, setConfirmDeleteDialog] = useState(false);

  // Load user data
  useEffect(() => {
    loadUserProfile();
    loadUsers();
  }, []);

  // Filter users when search term changes
  useEffect(() => {
    if (userSearchTerm) {
      const searchLower = userSearchTerm.toLowerCase();
      setFilteredUsers(users.filter(user =>
          user.username.toLowerCase().includes(searchLower) ||
          user.role.toLowerCase().includes(searchLower) ||
          user.status.toLowerCase().includes(searchLower)
      ));
    } else {
      setFilteredUsers(users);
    }
  }, [userSearchTerm, users]);

  // Load user profile data
  const loadUserProfile = async () => {
    try {
      setLoading(true);

      // In a real app, this would fetch from an API
      const response = await apiClient.get('/api/user/profile');
      const userData = response.data;

      setProfileData({
        firstName: userData.firstName,
        lastName: userData.lastName,
        email: userData.email,
        jobTitle: userData.jobTitle,
        department: userData.department
      });

      setSecuritySettings({
        twoFactorEnabled: userData.twoFactorEnabled,
        sessionTimeout: userData.sessionTimeout,
        notificationsEnabled: userData.notificationsEnabled
      });
    } catch (error) {
      showNotification('Error loading user profile', 'error');
    } finally {
      setLoading(false);
    }
  };

  // Load users for user management tab
  const loadUsers = async () => {
    try {
      setLoading(true);

      // Here's the API call that's failing
      const response = await apiClient.get('/api/user/all');
      const usersData = response.data;

      setUsers(usersData);
      setFilteredUsers(usersData);
    } catch (error) {
      console.error('Error loading users:', error);
      showNotification('Error loading users', 'error');
    } finally {
      setLoading(false);
    }
  };

  // Event Handlers
  const handleTabChange = (event, newValue) => {
    setActiveTab(newValue);
  };

  const handleProfileChange = (e) => {
    const { name, value } = e.target;
    setProfileData(prev => ({ ...prev, [name]: value }));

    // Clear error for field when typing
    if (profileErrors[name]) {
      setProfileErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const handlePasswordChange = (e) => {
    const { name, value } = e.target;
    setPasswordData(prev => ({ ...prev, [name]: value }));

    // Clear error for field when typing
    if (passwordErrors[name]) {
      setPasswordErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const togglePasswordVisibility = (field) => {
    setPasswordVisibility(prev => ({
      ...prev,
      [field]: !prev[field]
    }));
  };

  const handleSecurityChange = (e) => {
    const { name, value, checked } = e.target;
    setSecuritySettings(prev => ({
      ...prev,
      [name]: e.target.type === 'checkbox' ? checked : value
    }));
  };

  const handleNewUserChange = (e) => {
    const { name, value } = e.target;
    setNewUserData(prev => ({ ...prev, [name]: value }));

    // Clear error for field when typing
    if (newUserErrors[name]) {
      setNewUserErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const toggleNewUserPasswordVisibility = () => {
    setNewUserPasswordVisible(prev => !prev);
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setSelectedFile(file);

      // Create preview
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreviewUrl(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleOpenUserActionsMenu = (event, userId) => {
    setUserActionsAnchorEl(event.currentTarget);
    setSelectedUserId(userId);
  };

  const handleCloseUserActionsMenu = () => {
    setUserActionsAnchorEl(null);
    setSelectedUserId(null);
  };

  // Notification system
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

  // Form submission handlers
  const handleUpdateProfile = async (e) => {
    e.preventDefault();

    // Validation
    const errors = {};
    if (!profileData.firstName.trim()) errors.firstName = 'First name is required';
    if (!profileData.lastName.trim()) errors.lastName = 'Last name is required';
    if (!profileData.email.trim()) errors.email = 'Email is required';
    if (profileData.email && !profileData.email.includes('@')) errors.email = 'Valid email is required';

    if (Object.keys(errors).length > 0) {
      setProfileErrors(errors);
      return;
    }

    // Submit form
    try {
      setLoading(true);

      await apiClient.put('/api/user/profile', profileData);

      // Update context
      if (updateUserProfile) {
        updateUserProfile(profileData);
      }

      showNotification('Profile updated successfully');
    } catch (error) {
      showNotification('Failed to update profile', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdatePassword = async (e) => {
    e.preventDefault();

    // Validation
    const errors = {};
    if (!passwordData.currentPassword) errors.currentPassword = 'Current password is required';
    if (!passwordData.newPassword) errors.newPassword = 'New password is required';
    if (!passwordData.confirmPassword) errors.confirmPassword = 'Please confirm your password';
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      errors.confirmPassword = 'Passwords do not match';
    }
    if (passwordData.newPassword && passwordData.newPassword.length < 8) {
      errors.newPassword = 'Password must be at least 8 characters';
    }

    if (Object.keys(errors).length > 0) {
      setPasswordErrors(errors);
      return;
    }

    // Submit form
    try {
      setLoading(true);


      await apiClient.put('/api/user/password', passwordData);

      // Simulating API call
      await new Promise(resolve => setTimeout(resolve, 800));

      // Reset form
      setPasswordData({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      });

      showNotification('Password updated successfully');
    } catch (error) {
      showNotification('Failed to update password', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateSecurity = async (e) => {
    e.preventDefault();

    try {
      setLoading(true);

      await apiClient.put('/api/user/security', securitySettings);



      showNotification('Security settings updated successfully');
    } catch (error) {
      showNotification('Failed to update security settings', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleAddUser = async (e) => {
    e.preventDefault();

    // Validation
    const errors = {};
    if (!newUserData.username.trim()) errors.username = 'Username is required';
    if (!newUserData.firstName.trim()) errors.firstName = 'First name is required';
    if (!newUserData.lastName.trim()) errors.lastName = 'Last name is required';
    if (!newUserData.email.trim()) errors.email = 'Email is required';
    if (newUserData.email && !newUserData.email.includes('@')) errors.email = 'Valid email is required';
    if (!newUserData.password.trim()) errors.password = 'Password is required';
    if (newUserData.password && newUserData.password.length < 8) errors.password = 'Password must be at least 8 characters';

    if (Object.keys(errors).length > 0) {
      setNewUserErrors(errors);
      return;
    }

    try {
      setLoading(true);


      await apiClient.post('/api/user', newUserData);

      // Simulating API call
      await new Promise(resolve => setTimeout(resolve, 800));

      // Add the new user to the list with a generated ID
      const newUser = {
        id: Math.max(...users.map(u => u.id)) + 1,
        username: newUserData.username,
        firstName: newUserData.firstName,
        lastName: newUserData.lastName,
        email: newUserData.email,
        role: newUserData.role,
        status: 'Active',
        lastLogin: new Date().toISOString()
      };

      setUsers(prev => [...prev, newUser]);

      // Reset form and close dialog
      setNewUserData({
        username: '',
        firstName: '',
        lastName: '',
        email: '',
        password: '',
        role: 'User'
      });
      setOpenAddUserDialog(false);

      showNotification('User created successfully');
    } catch (error) {
      showNotification('Failed to create user', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteUser = async () => {
    if (!selectedUserId) return;

    if (selectedUserId === 5) {
      showNotification('Cannot delete the primary admin user', 'error');
      setConfirmDeleteDialog(false);
      handleCloseUserActionsMenu();
      return;
    }

    try {
      setLoading(true);


      await apiClient.delete(`/api/user/${userId}`);

      // Simulating API call
      await new Promise(resolve => setTimeout(resolve, 800));

      // Remove the user from the list
      setUsers(prev => prev.filter(user => user.id !== selectedUserId));

      showNotification('User deleted successfully');
    } catch (error) {
      showNotification('Failed to delete user', 'error');
    } finally {
      setLoading(false);
      setConfirmDeleteDialog(false);
      handleCloseUserActionsMenu();
    }
  };

  const handleUpdateUserStatus = async (userId, currentStatus) => {
    if (userId === 5) {
      showNotification('Cannot modify the primary admin user', 'error');
      handleCloseUserActionsMenu();
      return;
    }

    const newStatus = currentStatus === 'Active' ? 'Disabled' : 'Active';

    try {
      setLoading(true);


      await apiClient.patch(`/api/user/${userId}/status`, { active: true/false });

      // Update the user status in the list
      setUsers(prev => prev.map(user =>
          user.id === userId ? { ...user, status: newStatus } : user
      ));

      showNotification(`User ${newStatus === 'Active' ? 'activated' : 'disabled'} successfully`);
    } catch (error) {
      showNotification('Failed to update user status', 'error');
    } finally {
      setLoading(false);
      handleCloseUserActionsMenu();
    }
  };

  const handleUploadPicture = async () => {
    if (!selectedFile) {
      setOpenPictureDialog(false);
      return;
    }

    try {
      setLoading(true);

      // In a real app, you would upload the file:
      // const formData = new FormData();
      // formData.append('file', selectedFile);
      // await apiClient.post('/api/user/avatar', formData);

      // Simulating API call
      await new Promise(resolve => setTimeout(resolve, 800));

      // Update profile image with preview URL
      setProfileImage(previewUrl);
      setOpenPictureDialog(false);
      showNotification('Profile picture updated successfully');
    } catch (error) {
      showNotification('Failed to upload profile picture', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  // Helper functions
  const getInitials = () => {
    return `${profileData.firstName.charAt(0)}${profileData.lastName.charAt(0)}`;
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'Never';

    const date = new Date(dateString);
    return new Intl.DateTimeFormat('en-US', {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(date);
  };

  return (
      <Box sx={{ p: 3 }}>
        <Typography variant="h5" component="h1" sx={{ mb: 3, fontWeight: 'bold' }}>
          System Settings
        </Typography>

        <Paper elevation={2} sx={{ borderRadius: 2, mb: 3, overflow: 'hidden' }}>
          <StyledTabs
              value={activeTab}
              onChange={handleTabChange}
              variant="fullWidth"
              aria-label="Settings tabs"
          >
            <StyledTab
                icon={<PersonIcon sx={{ mr: 1 }} />}
                label="Profile"
                iconPosition="start"
            />
            <StyledTab
                icon={<LockIcon sx={{ mr: 1 }} />}
                label="Password"
                iconPosition="start"
            />
            <StyledTab
                icon={<SecurityIcon sx={{ mr: 1 }} />}
                label="Security"
                iconPosition="start"
            />
            <StyledTab
                icon={<GroupIcon sx={{ mr: 1 }} />}
                label="User Management"
                iconPosition="start"
            />
          </StyledTabs>

          {/* Profile Tab */}
          <TabPanel value={activeTab} index={0}>
            <Box component="form" onSubmit={handleUpdateProfile}>
              <Grid container spacing={4}>
                <Grid item xs={12} md={4} sx={{ position: 'relative' }}>
                  <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                    <Box sx={{ position: 'relative', mb: 2 }}>
                      <ProfileAvatar>
                        {getInitials()}
                      </ProfileAvatar>
                      <UploadButton
                          variant="contained"
                          onClick={() => setOpenPictureDialog(true)}
                      >
                        <PhotoCameraIcon fontSize="small" />
                      </UploadButton>
                    </Box>

                    <Typography variant="h6" gutterBottom>
                      {profileData.firstName} {profileData.lastName}
                    </Typography>

                    <Typography variant="body2" color="text.secondary" gutterBottom>
                      {profileData.jobTitle}
                    </Typography>

                    <Typography variant="body2" color="text.secondary">
                      {profileData.department}
                    </Typography>
                  </Box>
                </Grid>

                <Grid item xs={12} md={8}>
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
                          variant="outlined"
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
                          variant="outlined"
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
                          variant="outlined"
                      />
                    </Grid>

                    <Grid item xs={12} sm={6}>
                      <TextField
                          name="jobTitle"
                          label="Job Title"
                          fullWidth
                          value={profileData.jobTitle}
                          onChange={handleProfileChange}
                          variant="outlined"
                      />
                    </Grid>

                    <Grid item xs={12} sm={6}>
                      <TextField
                          name="department"
                          label="Department"
                          fullWidth
                          value={profileData.department}
                          onChange={handleProfileChange}
                          variant="outlined"
                      />
                    </Grid>
                  </Grid>

                  <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
                    <Button
                        type="submit"
                        variant="contained"
                        color="primary"
                        disabled={loading}
                        startIcon={loading ? <CircularProgress size={20} /> : <EditIcon />}
                    >
                      {loading ? 'Updating...' : 'Update Profile'}
                    </Button>
                  </Box>
                </Grid>
              </Grid>
            </Box>
          </TabPanel>

          {/* Password Tab */}
          <TabPanel value={activeTab} index={1}>
            <Box component="form" onSubmit={handleUpdatePassword} sx={{ maxWidth: 600, mx: 'auto' }}>
              <Typography variant="h6" gutterBottom>
                Change Password
              </Typography>

              <Typography variant="body2" color="text.secondary" paragraph>
                To change your password, please enter your current password followed by your new password.
              </Typography>

              <Grid container spacing={3}>
                <Grid item xs={12}>
                  <TextField
                      name="currentPassword"
                      label="Current Password"
                      type={passwordVisibility.currentPassword ? 'text' : 'password'}
                      fullWidth
                      value={passwordData.currentPassword}
                      onChange={handlePasswordChange}
                      error={!!passwordErrors.currentPassword}
                      helperText={passwordErrors.currentPassword}
                      InputProps={{
                        endAdornment: (
                            <InputAdornment position="end">
                              <IconButton
                                  aria-label="toggle current password visibility"
                                  onClick={() => togglePasswordVisibility('currentPassword')}
                                  edge="end"
                              >
                                {passwordVisibility.currentPassword ? <VisibilityOffIcon /> : <VisibilityIcon />}
                              </IconButton>
                            </InputAdornment>
                        ),
                      }}
                  />
                </Grid>

                <Grid item xs={12}>
                  <TextField
                      name="newPassword"
                      label="New Password"
                      type={passwordVisibility.newPassword ? 'text' : 'password'}
                      fullWidth
                      value={passwordData.newPassword}
                      onChange={handlePasswordChange}
                      error={!!passwordErrors.newPassword}
                      helperText={passwordErrors.newPassword || 'Password must be at least 8 characters long'}
                      InputProps={{
                        endAdornment: (
                            <InputAdornment position="end">
                              <IconButton
                                  aria-label="toggle new password visibility"
                                  onClick={() => togglePasswordVisibility('newPassword')}
                                  edge="end"
                              >
                                {passwordVisibility.newPassword ? <VisibilityOffIcon /> : <VisibilityIcon />}
                              </IconButton>
                            </InputAdornment>
                        ),
                      }}
                  />
                </Grid>

                <Grid item xs={12}>
                  <TextField
                      name="confirmPassword"
                      label="Confirm New Password"
                      type={passwordVisibility.confirmPassword ? 'text' : 'password'}
                      fullWidth
                      value={passwordData.confirmPassword}
                      onChange={handlePasswordChange}
                      error={!!passwordErrors.confirmPassword}
                      helperText={passwordErrors.confirmPassword}
                      InputProps={{
                        endAdornment: (
                            <InputAdornment position="end">
                              <IconButton
                                  aria-label="toggle confirm password visibility"
                                  onClick={() => togglePasswordVisibility('confirmPassword')}
                                  edge="end"
                              >
                                {passwordVisibility.confirmPassword ? <VisibilityOffIcon /> : <VisibilityIcon />}
                              </IconButton>
                            </InputAdornment>
                        ),
                      }}
                  />
                </Grid>

                <Grid item xs={12}>
                  <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                    <Button
                        type="submit"
                        variant="contained"
                        color="primary"
                        disabled={loading}
                        startIcon={loading ? <CircularProgress size={20} /> : <LockIcon />}
                    >
                      {loading ? 'Updating...' : 'Update Password'}
                    </Button>
                  </Box>
                </Grid>
              </Grid>

              <Box sx={{ mt: 4 }}>
                <Typography variant="h6" gutterBottom>
                  Password Requirements
                </Typography>

                <Typography variant="body2" color="text.secondary">
                  • Minimum 8 characters<br />
                  • Include at least one uppercase letter<br />
                  • Include at least one lowercase letter<br />
                  • Include at least one number<br />
                  • Include at least one special character
                </Typography>
              </Box>
            </Box>
          </TabPanel>

          {/* Security Tab */}
          <TabPanel value={activeTab} index={2}>
            <Box component="form" onSubmit={handleUpdateSecurity} sx={{ maxWidth: 800, mx: 'auto' }}>
              <Typography variant="h6" gutterBottom>
                Security Settings
              </Typography>

              <Grid container spacing={3}>
                <Grid item xs={12}>
                  <Card variant="outlined" sx={{ mb: 3 }}>
                    <CardContent>
                      <Box sx={{ display: 'flex', alignItems: 'flex-start', mb: 2 }}>
                        <VerifiedUserIcon sx={{ color: 'primary.main', mr: 2, mt: 0.5 }} />
                        <Box>
                          <Typography variant="subtitle1" fontWeight="medium">
                            Two-Factor Authentication
                          </Typography>
                          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                            Add an extra layer of security to your account by requiring a verification code in addition to your password.
                          </Typography>
                          <FormControlLabel
                              control={
                                <Switch
                                    name="twoFactorEnabled"
                                    checked={securitySettings.twoFactorEnabled}
                                    onChange={handleSecurityChange}
                                    color="primary"
                                />
                              }
                              label={securitySettings.twoFactorEnabled ? "Enabled" : "Disabled"}
                          />
                        </Box>
                      </Box>
                    </CardContent>
                  </Card>

                  <Card variant="outlined" sx={{ mb: 3 }}>
                    <CardContent>
                      <Box sx={{ display: 'flex', alignItems: 'flex-start', mb: 2 }}>
                        <AccessTimeIcon sx={{ color: 'primary.main', mr: 2, mt: 0.5 }} />
                        <Box sx={{ width: '100%' }}>
                          <Typography variant="subtitle1" fontWeight="medium">
                            Session Timeout
                          </Typography>
                          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                            Automatically log out after a period of inactivity for security.
                          </Typography>
                          <Grid container alignItems="center" spacing={2}>
                            <Grid item xs={12} sm={8} md={6}>
                              <TextField
                                  name="sessionTimeout"
                                  label="Timeout (minutes)"
                                  type="number"
                                  fullWidth
                                  value={securitySettings.sessionTimeout}
                                  onChange={handleSecurityChange}
                                  InputProps={{ inputProps: { min: 5, max: 120 } }}
                              />
                            </Grid>
                            <Grid item>
                              <Typography variant="body2" color="text.secondary">
                                {securitySettings.sessionTimeout < 15 && "Short timeout for high security"}
                                {securitySettings.sessionTimeout >= 15 && securitySettings.sessionTimeout < 45 && "Balanced security"}
                                {securitySettings.sessionTimeout >= 45 && "Extended session"}
                              </Typography>
                            </Grid>
                          </Grid>
                        </Box>
                      </Box>
                    </CardContent>
                  </Card>

                  <Card variant="outlined">
                    <CardContent>
                      <Box sx={{ display: 'flex', alignItems: 'flex-start', mb: 2 }}>
                        <NotificationsIcon sx={{ color: 'primary.main', mr: 2, mt: 0.5 }} />
                        <Box>
                          <Typography variant="subtitle1" fontWeight="medium">
                            Email Notifications
                          </Typography>
                          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                            Receive email notifications for important security events like password changes or login attempts.
                          </Typography>
                          <FormControlLabel
                              control={
                                <Switch
                                    name="notificationsEnabled"
                                    checked={securitySettings.notificationsEnabled}
                                    onChange={handleSecurityChange}
                                    color="primary"
                                />
                              }
                              label={securitySettings.notificationsEnabled ? "Enabled" : "Disabled"}
                          />
                        </Box>
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>

                <Grid item xs={12}>
                  <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                    <Button
                        type="submit"
                        variant="contained"
                        color="primary"
                        disabled={loading}
                        startIcon={loading ? <CircularProgress size={20} /> : <SecurityIcon />}
                    >
                      {loading ? 'Updating...' : 'Update Security Settings'}
                    </Button>
                  </Box>
                </Grid>
              </Grid>
            </Box>
          </TabPanel>

          {/* User Management Tab */}
          <TabPanel value={activeTab} index={3}>
            <Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                <Typography variant="h6" gutterBottom>
                  User Management
                </Typography>

                <Box sx={{ display: 'flex', gap: 2 }}>
                  <TextField
                      placeholder="Search users..."
                      variant="outlined"
                      size="small"
                      value={userSearchTerm}
                      onChange={(e) => setUserSearchTerm(e.target.value)}
                      InputProps={{
                        startAdornment: (
                            <InputAdornment position="start">
                              <SearchIcon fontSize="small" />
                            </InputAdornment>
                        ),
                      }}
                  />

                  <Button
                      variant="contained"
                      color="primary"
                      startIcon={<PersonIcon />}
                      onClick={() => setOpenAddUserDialog(true)}
                  >
                    Add User
                  </Button>
                </Box>
              </Box>

              <TableContainer component={Paper} variant="outlined">
                <Table sx={{ minWidth: 650 }}>
                  <TableHead>
                    <TableRow>
                      <TableCell>Username</TableCell>
                      <TableCell>Full Name</TableCell>
                      <TableCell>Email</TableCell>
                      <TableCell>Role</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Last Login</TableCell>
                      <TableCell align="right">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {loading && filteredUsers.length === 0 ? (
                        <TableRow>
                          <TableCell colSpan={7} align="center" sx={{ py: 3 }}>
                            <CircularProgress size={24} sx={{ mr: 1 }} />
                            Loading users...
                          </TableCell>
                        </TableRow>
                    ) : filteredUsers.length === 0 ? (
                        <TableRow>
                          <TableCell colSpan={7} align="center" sx={{ py: 3 }}>
                            <Typography variant="body2" color="text.secondary">
                              No users found matching your search
                            </Typography>
                          </TableCell>
                        </TableRow>
                    ) : (
                        filteredUsers.map((user) => (
                            <TableRow
                                key={user.id}
                                hover
                                sx={{
                                  opacity: user.status === 'Disabled' ? 0.7 : 1,
                                  '&:last-child td, &:last-child th': { border: 0 }
                                }}
                            >
                              <TableCell component="th" scope="row">
                                {user.username}
                              </TableCell>
                              <TableCell>
                                {user.firstName} {user.lastName}
                              </TableCell>
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
                                />
                              </TableCell>
                              <TableCell>{formatDate(user.lastLogin)}</TableCell>
                              <TableCell align="right">
                                <IconButton
                                    aria-label="User actions"
                                    aria-controls={`user-menu-${user.id}`}
                                    aria-haspopup="true"
                                    onClick={(e) => handleOpenUserActionsMenu(e, user.id)}
                                    size="small"
                                >
                                  <MoreVertIcon />
                                </IconButton>
                              </TableCell>
                            </TableRow>
                        ))
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          </TabPanel>
        </Paper>

        {/* Logout Section */}
        <Paper elevation={2} sx={{ p: 3, borderRadius: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Box>
              <Typography variant="h6" fontWeight="medium">
                Account Management
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Log out of your account or manage your account sessions
              </Typography>
            </Box>

            <Button
                variant="outlined"
                color="error"
                startIcon={<LogoutIcon />}
                onClick={() => setConfirmLogoutDialog(true)}
            >
              Log Out
            </Button>
          </Box>
        </Paper>

        {/* Profile Picture Dialog */}
        <Dialog
            open={openPictureDialog}
            onClose={() => setOpenPictureDialog(false)}
            maxWidth="sm"
            fullWidth
        >
          <DialogTitle>Update Profile Picture</DialogTitle>
          <DialogContent>
            <Box sx={{ textAlign: 'center', my: 2 }}>
              {previewUrl ? (
                  <Avatar
                      src={previewUrl}
                      alt="Preview"
                      sx={{ width: 150, height: 150, mx: 'auto', mb: 2 }}
                  />
              ) : (
                  <Avatar
                      sx={{ width: 150, height: 150, mx: 'auto', mb: 2, fontSize: 60 }}
                  >
                    {getInitials()}
                  </Avatar>
              )}

              <Button
                  variant="contained"
                  component="label"
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
                Recommended: Square image, at least 200x200 pixels
              </Typography>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenPictureDialog(false)}>
              Cancel
            </Button>
            <Button
                onClick={handleUploadPicture}
                variant="contained"
                color="primary"
                disabled={!selectedFile || loading}
            >
              {loading ? <CircularProgress size={24} /> : 'Upload'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Add User Dialog */}
        <Dialog
            open={openAddUserDialog}
            onClose={() => setOpenAddUserDialog(false)}
            maxWidth="md"
            fullWidth
        >
          <DialogTitle>Add New User</DialogTitle>
          <DialogContent>
            <Box component="form" sx={{ mt: 2 }}>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={6}>
                  <TextField
                      name="username"
                      label="Username"
                      fullWidth
                      value={newUserData.username}
                      onChange={handleNewUserChange}
                      error={!!newUserErrors.username}
                      helperText={newUserErrors.username}
                      required
                  />
                </Grid>

                <Grid item xs={12} sm={6}>
                  <FormControl fullWidth>
                    <InputLabel id="role-select-label">Role</InputLabel>
                    <Select
                        labelId="role-select-label"
                        name="role"
                        value={newUserData.role}
                        onChange={handleNewUserChange}
                        label="Role"
                    >
                      <MenuItem value="User">User</MenuItem>
                      <MenuItem value="Administrator">Administrator</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>

                <Grid item xs={12} sm={6}>
                  <TextField
                      name="firstName"
                      label="First Name"
                      fullWidth
                      value={newUserData.firstName}
                      onChange={handleNewUserChange}
                      error={!!newUserErrors.firstName}
                      helperText={newUserErrors.firstName}
                      required
                  />
                </Grid>

                <Grid item xs={12} sm={6}>
                  <TextField
                      name="lastName"
                      label="Last Name"
                      fullWidth
                      value={newUserData.lastName}
                      onChange={handleNewUserChange}
                      error={!!newUserErrors.lastName}
                      helperText={newUserErrors.lastName}
                      required
                  />
                </Grid>

                <Grid item xs={12}>
                  <TextField
                      name="email"
                      label="Email Address"
                      fullWidth
                      value={newUserData.email}
                      onChange={handleNewUserChange}
                      error={!!newUserErrors.email}
                      helperText={newUserErrors.email}
                      required
                  />
                </Grid>

                <Grid item xs={12}>
                  <TextField
                      name="password"
                      label="Password"
                      type={newUserPasswordVisible ? 'text' : 'password'}
                      fullWidth
                      value={newUserData.password}
                      onChange={handleNewUserChange}
                      error={!!newUserErrors.password}
                      helperText={newUserErrors.password || 'Password must be at least 8 characters long'}
                      required
                      InputProps={{
                        endAdornment: (
                            <InputAdornment position="end">
                              <IconButton
                                  aria-label="toggle password visibility"
                                  onClick={toggleNewUserPasswordVisibility}
                                  edge="end"
                              >
                                {newUserPasswordVisible ? <VisibilityOffIcon /> : <VisibilityIcon />}
                              </IconButton>
                            </InputAdornment>
                        ),
                      }}
                  />
                </Grid>
              </Grid>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenAddUserDialog(false)}>
              Cancel
            </Button>
            <Button
                onClick={handleAddUser}
                variant="contained"
                color="primary"
                disabled={loading}
            >
              {loading ? <CircularProgress size={24} /> : 'Create User'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* User Actions Menu */}
        <Menu
            id={`user-menu-${selectedUserId}`}
            anchorEl={userActionsAnchorEl}
            open={Boolean(userActionsAnchorEl)}
            onClose={handleCloseUserActionsMenu}
            anchorOrigin={{
              vertical: 'bottom',
              horizontal: 'right',
            }}
            transformOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
        >
          {users.find(u => u.id === selectedUserId)?.status === 'Active' ? (
              <MenuItem onClick={() => handleUpdateUserStatus(selectedUserId, 'Active')}>
                <ListItemIcon>
                  <BlockIcon fontSize="small" />
                </ListItemIcon>
                Disable User
              </MenuItem>
          ) : (
              <MenuItem onClick={() => handleUpdateUserStatus(selectedUserId, 'Disabled')}>
                <ListItemIcon>
                  <CheckCircleIcon fontSize="small" />
                </ListItemIcon>
                Enable User
              </MenuItem>
          )}

          <MenuItem onClick={() => {
            setConfirmDeleteDialog(true);
            handleCloseUserActionsMenu();
          }}
          >
            <ListItemIcon>
              <DeleteIcon fontSize="small" color="error" />
            </ListItemIcon>
            <Typography color="error">Delete User</Typography>
          </MenuItem>
        </Menu>

        {/* Confirm Logout Dialog */}
        <Dialog
            open={confirmLogoutDialog}
            onClose={() => setConfirmLogoutDialog(false)}
            maxWidth="xs"
            fullWidth
        >
          <DialogTitle>Confirm Logout</DialogTitle>
          <DialogContent>
            <Typography>
              Are you sure you want to log out? Any unsaved changes will be lost.
            </Typography>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setConfirmLogoutDialog(false)}>
              Cancel
            </Button>
            <Button
                onClick={handleLogout}
                variant="contained"
                color="primary"
            >
              Logout
            </Button>
          </DialogActions>
        </Dialog>

        {/* Confirm Delete User Dialog */}
        <Dialog
            open={confirmDeleteDialog}
            onClose={() => setConfirmDeleteDialog(false)}
            maxWidth="xs"
            fullWidth
        >
          <DialogTitle>Confirm User Deletion</DialogTitle>
          <DialogContent>
            <Typography color="error" paragraph>
              Warning: This action cannot be undone.
            </Typography>
            <Typography>
              Are you sure you want to permanently delete this user? All associated data will be removed from the system.
            </Typography>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setConfirmDeleteDialog(false)}>
              Cancel
            </Button>
            <Button
                onClick={handleDeleteUser}
                variant="contained"
                color="error"
                disabled={loading}
            >
              {loading ? <CircularProgress size={24} /> : 'Delete User'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Notification snackbar */}
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
              variant="filled"
              elevation={6}
          >
            {notification.message}
          </Alert>
        </Snackbar>
      </Box>
  );
};

export default SystemSettingsPage;