// gateway-admin-dashboard/src/pages/SystemSettingsPage.jsx
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
import apiClient from '../apiClient'; // Corrected import

// Icons
import EditIcon from '@mui/icons-material/Edit';
import LockIcon from '@mui/icons-material/Lock';
import SecurityIcon from '@mui/icons-material/Security';
import PersonIcon from '@mui/icons-material/Person';
import GroupIcon from '@mui/icons-material/Group';
import DeleteIcon from '@mui/icons-material/Delete';
import SettingsIcon from '@mui/icons-material/Settings'; // Keep if used elsewhere, not directly in tabs
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
// import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings'; // Keep if needed

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
  backgroundColor: theme.palette.primary.light, // Softer primary color
  color: theme.palette.primary.contrastText,
  boxShadow: theme.shadows[3],
  border: `4px solid ${theme.palette.background.paper}`,
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
  backgroundColor: role === 'ADMIN' // Check against 'ADMIN' as stored in DB
      ? theme.palette.error.main // More prominent color for Admin
      : theme.palette.info.main, // Use info color for User
  color: theme.palette.common.white,
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
        {value === index && <Box sx={{ p: 3, pt: 4 }}>{children}</Box>}
      </div>
  );
}

const SystemSettingsPage = () => {
  const navigate = useNavigate();
  const { logout, user: contextUser, updateUserProfile: updateUserContextProfile } = useContext(AuthContext);

  const [activeTab, setActiveTab] = useState(0);
  const [loading, setLoading] = useState(false);
  const [notification, setNotification] = useState({ open: false, message: '', severity: 'success' });

  const [profileData, setProfileData] = useState({
    firstName: '', lastName: '', email: '', jobTitle: '', department: ''
  });
  const [passwordData, setPasswordData] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [passwordVisibility, setPasswordVisibility] = useState({ currentPassword: false, newPassword: false, confirmPassword: false });
  const [profileImage, setProfileImage] = useState(null); // Will be URL
  const [openPictureDialog, setOpenPictureDialog] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [profileErrors, setProfileErrors] = useState({});
  const [passwordErrors, setPasswordErrors] = useState({});

  const [securitySettings, setSecuritySettings] = useState({
    twoFactorEnabled: false, sessionTimeoutMinutes: 30, notificationsEnabled: true
  });

  const [users, setUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [userSearchTerm, setUserSearchTerm] = useState('');
  const [openAddUserDialog, setOpenAddUserDialog] = useState(false);
  const [newUserData, setNewUserData] = useState({ username: '', firstName: '', lastName: '', email: '', password: '', role: 'USER' });
  const [newUserErrors, setNewUserErrors] = useState({});
  const [newUserPasswordVisible, setNewUserPasswordVisible] = useState(false);

  const [userActionsAnchorEl, setUserActionsAnchorEl] = useState(null);
  const [selectedUserId, setSelectedUserId] = useState(null);
  const [confirmLogoutDialog, setConfirmLogoutDialog] = useState(false);
  const [confirmDeleteDialog, setConfirmDeleteDialog] = useState(false);

  const showNotification = (message, severity = 'success') => {
    setNotification({ open: true, message, severity });
  };
  const handleCloseNotification = () => setNotification(prev => ({ ...prev, open: false }));

  const loadUserProfile = async () => {
    setLoading(true);
    try {
      const response = await apiClient.get('/user/profile');
      const userData = response.data;
      setProfileData({
        firstName: userData.firstName || '',
        lastName: userData.lastName || '',
        email: userData.email || '',
        jobTitle: userData.jobTitle || '',
        department: userData.department || ''
      });
      setSecuritySettings({
        twoFactorEnabled: userData.twoFactorEnabled || false,
        sessionTimeoutMinutes: userData.sessionTimeoutMinutes || 30,
        notificationsEnabled: userData.notificationsEnabled || true
      });
      setProfileImage(userData.profileImageUrl || null);
    } catch (error) {
      showNotification(error.response?.data?.message || 'Error loading user profile', 'error');
    } finally {
      setLoading(false);
    }
  };

  const loadUsers = async () => {
    if (!contextUser?.isAdmin) { // Check if current user is admin from context
      setUsers([]);
      setFilteredUsers([]);
      return;
    }
    setLoading(true);
    try {
      const response = await apiClient.get('/user/all');
      setUsers(response.data);
    } catch (error) {
      showNotification(error.response?.data?.message || 'Error loading users', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (contextUser) { // Ensure contextUser is loaded before fetching profile
      loadUserProfile();
      if (contextUser.isAdmin) { // Only load all users if current user is admin
        loadUsers();
      }
    }
  }, [contextUser]);


  useEffect(() => {
    const searchLower = userSearchTerm.toLowerCase();
    setFilteredUsers(
        users.filter(u =>
            u.username.toLowerCase().includes(searchLower) ||
            (u.firstName + " " + u.lastName).toLowerCase().includes(searchLower) ||
            u.email.toLowerCase().includes(searchLower) ||
            u.role.toLowerCase().includes(searchLower) ||
            u.status.toLowerCase().includes(searchLower)
        )
    );
  }, [userSearchTerm, users]);

  const handleTabChange = (event, newValue) => setActiveTab(newValue);
  const handleProfileChange = (e) => {
    const { name, value } = e.target;
    setProfileData(prev => ({ ...prev, [name]: value }));
    if (profileErrors[name]) setProfileErrors(prev => ({ ...prev, [name]: '' }));
  };
  const handlePasswordChange = (e) => {
    const { name, value } = e.target;
    setPasswordData(prev => ({ ...prev, [name]: value }));
    if (passwordErrors[name]) setPasswordErrors(prev => ({ ...prev, [name]: '' }));
  };
  const togglePasswordVisibility = (field) => setPasswordVisibility(prev => ({ ...prev, [field]: !prev[field] }));
  const handleSecurityChange = (e) => {
    const { name, value, checked, type } = e.target;
    setSecuritySettings(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : parseInt(value, 10) || 0 }));
  };
  const handleNewUserChange = (e) => {
    const { name, value } = e.target;
    setNewUserData(prev => ({ ...prev, [name]: value }));
    if (newUserErrors[name]) setNewUserErrors(prev => ({ ...prev, [name]: '' }));
  };
  const toggleNewUserPasswordVisibility = () => setNewUserPasswordVisible(prev => !prev);
  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setSelectedFile(file);
      const reader = new FileReader();
      reader.onloadend = () => setPreviewUrl(reader.result);
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

  const validateProfile = () => {
    const errors = {};
    if (!profileData.firstName.trim()) errors.firstName = 'First name is required';
    if (!profileData.lastName.trim()) errors.lastName = 'Last name is required';
    if (!profileData.email.trim()) errors.email = 'Email is required';
    else if (!/\S+@\S+\.\S+/.test(profileData.email)) errors.email = 'Invalid email format';
    setProfileErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    if (!validateProfile()) return;
    setLoading(true);
    try {
      const response = await apiClient.put('/user/profile', profileData);
      updateUserContextProfile(response.data); // Update context
      showNotification('Profile updated successfully');
    } catch (error) {
      showNotification(error.response?.data?.message || 'Failed to update profile', 'error');
    } finally {
      setLoading(false);
    }
  };

  const validatePassword = () => {
    const errors = {};
    if (!passwordData.currentPassword) errors.currentPassword = 'Current password is required';
    if (!passwordData.newPassword) errors.newPassword = 'New password is required';
    else if (passwordData.newPassword.length < 8) errors.newPassword = 'Password must be at least 8 characters';
    if (passwordData.newPassword !== passwordData.confirmPassword) errors.confirmPassword = 'Passwords do not match';
    setPasswordErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleUpdatePassword = async (e) => {
    e.preventDefault();
    if (!validatePassword()) return;
    setLoading(true);
    try {
      await apiClient.put('/user/password', passwordData);
      setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
      showNotification('Password updated successfully');
    } catch (error) {
      showNotification(error.response?.data?.message || 'Failed to update password', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateSecurity = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const response = await apiClient.put('/user/security', securitySettings);
      updateUserContextProfile(response.data); // Update context
      showNotification('Security settings updated successfully');
    } catch (error) {
      showNotification(error.response?.data?.message || 'Failed to update security settings', 'error');
    } finally {
      setLoading(false);
    }
  };

  const validateNewUser = () => {
    const errors = {};
    if (!newUserData.username.trim()) errors.username = 'Username is required';
    if (!newUserData.firstName.trim()) errors.firstName = 'First name is required';
    if (!newUserData.lastName.trim()) errors.lastName = 'Last name is required';
    if (!newUserData.email.trim()) errors.email = 'Email is required';
    else if (!/\S+@\S+\.\S+/.test(newUserData.email)) errors.email = 'Invalid email format';
    if (!newUserData.password.trim()) errors.password = 'Password is required';
    else if (newUserData.password.length < 8) errors.password = 'Password must be at least 8 characters';
    setNewUserErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleAddUser = async (e) => {
    e.preventDefault();
    if (!validateNewUser()) return;
    setLoading(true);
    try {
      const response = await apiClient.post('/user', newUserData);
      setUsers(prev => [...prev, response.data]);
      setNewUserData({ username: '', firstName: '', lastName: '', email: '', password: '', role: 'USER' });
      setOpenAddUserDialog(false);
      showNotification('User created successfully');
    } catch (error) {
      showNotification(error.response?.data?.message || 'Failed to create user', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteUser = async () => {
    if (!selectedUserId) return;
    // Prevent deleting the current user or a hardcoded admin ID if necessary
    if (selectedUserId === contextUser.id) {
      showNotification('You cannot delete your own account.', 'error');
      setConfirmDeleteDialog(false);
      handleCloseUserActionsMenu();
      return;
    }
    setLoading(true);
    try {
      await apiClient.delete(`/user/${selectedUserId}`);
      setUsers(prev => prev.filter(u => u.id !== selectedUserId));
      showNotification('User deleted successfully');
    } catch (error) {
      showNotification(error.response?.data?.message || 'Failed to delete user', 'error');
    } finally {
      setLoading(false);
      setConfirmDeleteDialog(false);
      handleCloseUserActionsMenu();
    }
  };

  const handleUpdateUserStatus = async (userIdToUpdate, currentStatus) => {
    if (userIdToUpdate === contextUser.id) {
      showNotification('You cannot change the status of your own account.', 'error');
      handleCloseUserActionsMenu();
      return;
    }
    const newActiveState = currentStatus === 'Active' ? false : true;
    setLoading(true);
    try {
      const response = await apiClient.patch(`/user/${userIdToUpdate}/status`, { active: newActiveState });
      setUsers(prev => prev.map(u => u.id === userIdToUpdate ? response.data : u));
      showNotification(`User status updated successfully`);
    } catch (error) {
      showNotification(error.response?.data?.message || 'Failed to update user status', 'error');
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
    setLoading(true);
    const formData = new FormData();
    formData.append('avatar', selectedFile); // Assuming backend expects 'avatar' field
    try {
      // This endpoint is not fully implemented in the backend plan, so this is a placeholder
      // const response = await apiClient.post('/user/avatar', formData);
      // setProfileImage(response.data.profileImageUrl); // Assuming API returns new URL
      // updateUserContextProfile({ profileImageUrl: response.data.profileImageUrl });

      // Simulate success for now
      await new Promise(resolve => setTimeout(resolve, 1000));
      setProfileImage(previewUrl); // Use local preview for now
      updateUserContextProfile({ profileImageUrl: previewUrl });


      setOpenPictureDialog(false);
      setSelectedFile(null);
      setPreviewUrl(null);
      showNotification('Profile picture updated (simulated)');
    } catch (error) {
      showNotification(error.response?.data?.message || 'Failed to upload profile picture', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  const getInitials = () => {
    if (profileData.firstName && profileData.lastName) {
      return `${profileData.firstName.charAt(0)}${profileData.lastName.charAt(0)}`.toUpperCase();
    }
    return contextUser?.username?.charAt(0).toUpperCase() || 'U';
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'Never';
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
  };

  if (loading && !profileData.email) { // Initial loading state for profile
    return <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh' }}><CircularProgress /></Box>;
  }

  return (
      <Box sx={{ p: { xs: 2, md: 3 } }}>
        <Typography variant="h5" component="h1" sx={{ mb: 3, fontWeight: 'bold' }}>
          System Settings
        </Typography>

        <Paper elevation={0} sx={{ borderRadius: 2, mb: 3, overflow: 'hidden', border: (theme) => `1px solid ${theme.palette.divider}` }}>
          <StyledTabs value={activeTab} onChange={handleTabChange} variant="scrollable" scrollButtons="auto" aria-label="Settings tabs">
            <StyledTab icon={<PersonIcon />} label="Profile" iconPosition="start" />
            <StyledTab icon={<LockIcon />} label="Password" iconPosition="start" />
            <StyledTab icon={<SecurityIcon />} label="Security" iconPosition="start" />
            {contextUser?.isAdmin && <StyledTab icon={<GroupIcon />} label="User Management" iconPosition="start" />}
          </StyledTabs>

          <TabPanel value={activeTab} index={0}>
            <Box component="form" onSubmit={handleUpdateProfile}>
              <Grid container spacing={4}>
                <Grid item xs={12} md={4}>
                  <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
                    <Box sx={{ position: 'relative', mb: 2 }}>
                      <ProfileAvatar src={profileImage || undefined}>{getInitials()}</ProfileAvatar>
                      <UploadButton variant="contained" onClick={() => setOpenPictureDialog(true)}><PhotoCameraIcon fontSize="small" /></UploadButton>
                    </Box>
                    <Typography variant="h6">{profileData.firstName} {profileData.lastName}</Typography>
                    <Typography variant="body2" color="text.secondary">{profileData.jobTitle || 'Job Title Not Set'}</Typography>
                    <Typography variant="body2" color="text.secondary">{profileData.department || 'Department Not Set'}</Typography>
                  </Box>
                </Grid>
                <Grid item xs={12} md={8}>
                  <Typography variant="h6" gutterBottom>Personal Information</Typography>
                  <Grid container spacing={2}>
                    <Grid item xs={12} sm={6}><TextField name="firstName" label="First Name" fullWidth value={profileData.firstName} onChange={handleProfileChange} error={!!profileErrors.firstName} helperText={profileErrors.firstName} /></Grid>
                    <Grid item xs={12} sm={6}><TextField name="lastName" label="Last Name" fullWidth value={profileData.lastName} onChange={handleProfileChange} error={!!profileErrors.lastName} helperText={profileErrors.lastName} /></Grid>
                    <Grid item xs={12}><TextField name="email" label="Email Address" fullWidth value={profileData.email} onChange={handleProfileChange} error={!!profileErrors.email} helperText={profileErrors.email} /></Grid>
                    <Grid item xs={12} sm={6}><TextField name="jobTitle" label="Job Title" fullWidth value={profileData.jobTitle} onChange={handleProfileChange} /></Grid>
                    <Grid item xs={12} sm={6}><TextField name="department" label="Department" fullWidth value={profileData.department} onChange={handleProfileChange} /></Grid>
                  </Grid>
                  <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
                    <Button type="submit" variant="contained" color="primary" disabled={loading} startIcon={loading ? <CircularProgress size={20} color="inherit"/> : <EditIcon />}>Update Profile</Button>
                  </Box>
                </Grid>
              </Grid>
            </Box>
          </TabPanel>

          <TabPanel value={activeTab} index={1}>
            <Box component="form" onSubmit={handleUpdatePassword} sx={{ maxWidth: 600, mx: 'auto' }}>
              <Typography variant="h6" gutterBottom>Change Password</Typography>
              <Grid container spacing={3}>
                <Grid item xs={12}><TextField name="currentPassword" label="Current Password" type={passwordVisibility.currentPassword ? 'text' : 'password'} fullWidth value={passwordData.currentPassword} onChange={handlePasswordChange} error={!!passwordErrors.currentPassword} helperText={passwordErrors.currentPassword} InputProps={{endAdornment: (<InputAdornment position="end"><IconButton onClick={() => togglePasswordVisibility('currentPassword')}>{passwordVisibility.currentPassword ? <VisibilityOffIcon /> : <VisibilityIcon />}</IconButton></InputAdornment>)}} /></Grid>
                <Grid item xs={12}><TextField name="newPassword" label="New Password" type={passwordVisibility.newPassword ? 'text' : 'password'} fullWidth value={passwordData.newPassword} onChange={handlePasswordChange} error={!!passwordErrors.newPassword} helperText={passwordErrors.newPassword || 'Min 8 characters'} InputProps={{endAdornment: (<InputAdornment position="end"><IconButton onClick={() => togglePasswordVisibility('newPassword')}>{passwordVisibility.newPassword ? <VisibilityOffIcon /> : <VisibilityIcon />}</IconButton></InputAdornment>)}} /></Grid>
                <Grid item xs={12}><TextField name="confirmPassword" label="Confirm New Password" type={passwordVisibility.confirmPassword ? 'text' : 'password'} fullWidth value={passwordData.confirmPassword} onChange={handlePasswordChange} error={!!passwordErrors.confirmPassword} helperText={passwordErrors.confirmPassword} InputProps={{endAdornment: (<InputAdornment position="end"><IconButton onClick={() => togglePasswordVisibility('confirmPassword')}>{passwordVisibility.confirmPassword ? <VisibilityOffIcon /> : <VisibilityIcon />}</IconButton></InputAdornment>)}} /></Grid>
                <Grid item xs={12}><Box sx={{ display: 'flex', justifyContent: 'flex-end' }}><Button type="submit" variant="contained" color="primary" disabled={loading} startIcon={loading ? <CircularProgress size={20} color="inherit"/> : <LockIcon />}>Update Password</Button></Box></Grid>
              </Grid>
            </Box>
          </TabPanel>

          <TabPanel value={activeTab} index={2}>
            <Box component="form" onSubmit={handleUpdateSecurity} sx={{ maxWidth: 800, mx: 'auto' }}>
              <Typography variant="h6" gutterBottom>Security Settings</Typography>
              <Grid container spacing={3}>
                <Grid item xs={12}><Card variant="outlined"><CardContent><Box sx={{ display: 'flex', alignItems: 'flex-start' }}><VerifiedUserIcon color="primary" sx={{ mr: 2, mt: 0.5 }} /><Box><Typography variant="subtitle1" fontWeight="medium">Two-Factor Authentication</Typography><Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>Enhance account security.</Typography><FormControlLabel control={<Switch name="twoFactorEnabled" checked={securitySettings.twoFactorEnabled} onChange={handleSecurityChange} color="primary" />} label={securitySettings.twoFactorEnabled ? "Enabled" : "Disabled"} /></Box></Box></CardContent></Card></Grid>
                <Grid item xs={12}><Card variant="outlined"><CardContent><Box sx={{ display: 'flex', alignItems: 'flex-start' }}><AccessTimeIcon color="primary" sx={{ mr: 2, mt: 0.5 }} /><Box sx={{ width: '100%' }}><Typography variant="subtitle1" fontWeight="medium">Session Timeout</Typography><Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>Log out after inactivity.</Typography><TextField name="sessionTimeoutMinutes" label="Timeout (minutes)" type="number" fullWidth value={securitySettings.sessionTimeoutMinutes} onChange={handleSecurityChange} InputProps={{ inputProps: { min: 5, max: 120 } }} sx={{maxWidth: 200}}/></Box></Box></CardContent></Card></Grid>
                <Grid item xs={12}><Card variant="outlined"><CardContent><Box sx={{ display: 'flex', alignItems: 'flex-start' }}><NotificationsIcon color="primary" sx={{ mr: 2, mt: 0.5 }} /><Box><Typography variant="subtitle1" fontWeight="medium">Email Notifications</Typography><Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>Receive security alerts.</Typography><FormControlLabel control={<Switch name="notificationsEnabled" checked={securitySettings.notificationsEnabled} onChange={handleSecurityChange} color="primary" />} label={securitySettings.notificationsEnabled ? "Enabled" : "Disabled"} /></Box></Box></CardContent></Card></Grid>
                <Grid item xs={12}><Box sx={{ display: 'flex', justifyContent: 'flex-end' }}><Button type="submit" variant="contained" color="primary" disabled={loading} startIcon={loading ? <CircularProgress size={20} color="inherit"/> : <SecurityIcon />}>Update Security</Button></Box></Grid>
              </Grid>
            </Box>
          </TabPanel>

          {contextUser?.isAdmin && (
              <TabPanel value={activeTab} index={3}>
                <Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                    <Typography variant="h6">User Management</Typography>
                    <Box sx={{ display: 'flex', gap: 2 }}>
                      <TextField placeholder="Search users..." variant="outlined" size="small" value={userSearchTerm} onChange={(e) => setUserSearchTerm(e.target.value)} InputProps={{startAdornment: (<InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment>)}} />
                      <Button variant="contained" color="primary" startIcon={<PersonIcon />} onClick={() => setOpenAddUserDialog(true)}>Add User</Button>
                    </Box>
                  </Box>
                  <TableContainer component={Paper} variant="outlined">
                    <Table>
                      <TableHead><TableRow><TableCell>Username</TableCell><TableCell>Full Name</TableCell><TableCell>Email</TableCell><TableCell>Role</TableCell><TableCell>Status</TableCell><TableCell>Last Login</TableCell><TableCell align="right">Actions</TableCell></TableRow></TableHead>
                      <TableBody>
                        {loading && filteredUsers.length === 0 ? <TableRow><TableCell colSpan={7} align="center" sx={{ py: 3 }}><CircularProgress size={24} sx={{ mr: 1 }} />Loading users...</TableCell></TableRow>
                            : filteredUsers.length === 0 ? <TableRow><TableCell colSpan={7} align="center" sx={{ py: 3 }}><Typography variant="body2" color="text.secondary">No users found</Typography></TableCell></TableRow>
                                : filteredUsers.map((u) => (
                                    <TableRow key={u.id} hover sx={{ opacity: u.status === 'Disabled' ? 0.7 : 1 }}>
                                      <TableCell>{u.username}</TableCell>
                                      <TableCell>{u.firstName} {u.lastName}</TableCell>
                                      <TableCell>{u.email}</TableCell>
                                      <TableCell><UserRoleChip label={u.role} role={u.role} size="small" /></TableCell>
                                      <TableCell><Chip icon={u.status === 'Active' ? <CheckCircleIcon /> : <BlockIcon />} label={u.status} color={u.status === 'Active' ? 'success' : 'default'} size="small" variant="outlined" /></TableCell>
                                      <TableCell>{formatDate(u.lastLogin)}</TableCell>
                                      <TableCell align="right"><IconButton onClick={(e) => handleOpenUserActionsMenu(e, u.id)} size="small"><MoreVertIcon /></IconButton></TableCell>
                                    </TableRow>
                                ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </Box>
              </TabPanel>
          )}
        </Paper>

        <Paper elevation={0} sx={{ p: 3, borderRadius: 2, border: (theme) => `1px solid ${theme.palette.divider}` }}>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Box><Typography variant="h6" fontWeight="medium">Account Management</Typography><Typography variant="body2" color="text.secondary">Log out of your account.</Typography></Box>
            <Button variant="outlined" color="error" startIcon={<LogoutIcon />} onClick={() => setConfirmLogoutDialog(true)}>Log Out</Button>
          </Box>
        </Paper>

        <Dialog open={openPictureDialog} onClose={() => setOpenPictureDialog(false)} maxWidth="xs" fullWidth>
          <DialogTitle>Update Profile Picture</DialogTitle>
          <DialogContent><Box sx={{ textAlign: 'center', my: 2 }}>{previewUrl ? <Avatar src={previewUrl} sx={{ width: 150, height: 150, mx: 'auto', mb: 2 }} /> : <Avatar sx={{ width: 150, height: 150, mx: 'auto', mb: 2, fontSize: 60 }}>{getInitials()}</Avatar>}<Button variant="contained" component="label">Select Image<input type="file" hidden accept="image/*" onChange={handleFileChange} /></Button><Typography variant="caption" display="block" sx={{ mt: 1 }}>Square image, 200x200 pixels recommended</Typography></Box></DialogContent>
          <DialogActions><Button onClick={() => setOpenPictureDialog(false)}>Cancel</Button><Button onClick={handleUploadPicture} variant="contained" color="primary" disabled={!selectedFile || loading}>{loading ? <CircularProgress size={24} color="inherit"/> : 'Upload'}</Button></DialogActions>
        </Dialog>

        <Dialog open={openAddUserDialog} onClose={() => setOpenAddUserDialog(false)} maxWidth="sm" fullWidth>
          <DialogTitle>Add New User</DialogTitle>
          <DialogContent><Box component="form" sx={{ mt: 2 }}><Grid container spacing={2}>
            <Grid item xs={12} sm={6}><TextField name="username" label="Username" fullWidth value={newUserData.username} onChange={handleNewUserChange} error={!!newUserErrors.username} helperText={newUserErrors.username} required /></Grid>
            <Grid item xs={12} sm={6}><FormControl fullWidth><InputLabel>Role</InputLabel><Select name="role" value={newUserData.role} onChange={handleNewUserChange} label="Role"><MenuItem value="USER">User</MenuItem><MenuItem value="ADMIN">Administrator</MenuItem></Select></FormControl></Grid>
            <Grid item xs={12} sm={6}><TextField name="firstName" label="First Name" fullWidth value={newUserData.firstName} onChange={handleNewUserChange} error={!!newUserErrors.firstName} helperText={newUserErrors.firstName} required /></Grid>
            <Grid item xs={12} sm={6}><TextField name="lastName" label="Last Name" fullWidth value={newUserData.lastName} onChange={handleNewUserChange} error={!!newUserErrors.lastName} helperText={newUserErrors.lastName} required /></Grid>
            <Grid item xs={12}><TextField name="email" label="Email Address" fullWidth value={newUserData.email} onChange={handleNewUserChange} error={!!newUserErrors.email} helperText={newUserErrors.email} required /></Grid>
            <Grid item xs={12}><TextField name="password" label="Password" type={newUserPasswordVisible ? 'text' : 'password'} fullWidth value={newUserData.password} onChange={handleNewUserChange} error={!!newUserErrors.password} helperText={newUserErrors.password || 'Min 8 characters'} required InputProps={{endAdornment: (<InputAdornment position="end"><IconButton onClick={toggleNewUserPasswordVisibility}>{newUserPasswordVisible ? <VisibilityOffIcon /> : <VisibilityIcon />}</IconButton></InputAdornment>)}} /></Grid>
          </Grid></Box></DialogContent>
          <DialogActions><Button onClick={() => setOpenAddUserDialog(false)}>Cancel</Button><Button onClick={handleAddUser} variant="contained" color="primary" disabled={loading}>{loading ? <CircularProgress size={24} color="inherit"/> : 'Create User'}</Button></DialogActions>
        </Dialog>

        <Menu id={`user-menu-${selectedUserId}`} anchorEl={userActionsAnchorEl} open={Boolean(userActionsAnchorEl)} onClose={handleCloseUserActionsMenu} anchorOrigin={{vertical: 'bottom', horizontal: 'right'}} transformOrigin={{vertical: 'top', horizontal: 'right'}}>
          {users.find(u => u.id === selectedUserId)?.status === 'Active' ?
              <MenuItem onClick={() => handleUpdateUserStatus(selectedUserId, 'Active')}><ListItemIcon><BlockIcon fontSize="small" /></ListItemIcon>Disable User</MenuItem> :
              <MenuItem onClick={() => handleUpdateUserStatus(selectedUserId, 'Disabled')}><ListItemIcon><CheckCircleIcon fontSize="small" /></ListItemIcon>Enable User</MenuItem>
          }
          <MenuItem onClick={() => { setConfirmDeleteDialog(true); }}><ListItemIcon><DeleteIcon fontSize="small" color="error" /></ListItemIcon><Typography color="error">Delete User</Typography></MenuItem>
        </Menu>

        <Dialog open={confirmLogoutDialog} onClose={() => setConfirmLogoutDialog(false)} maxWidth="xs" fullWidth>
          <DialogTitle>Confirm Logout</DialogTitle><DialogContent><Typography>Are you sure you want to log out?</Typography></DialogContent>
          <DialogActions><Button onClick={() => setConfirmLogoutDialog(false)}>Cancel</Button><Button onClick={handleLogout} variant="contained" color="primary">Logout</Button></DialogActions>
        </Dialog>

        <Dialog open={confirmDeleteDialog} onClose={() => setConfirmDeleteDialog(false)} maxWidth="xs" fullWidth>
          <DialogTitle>Confirm User Deletion</DialogTitle><DialogContent><Typography color="error" paragraph>Warning: This action cannot be undone.</Typography><Typography>Delete user {users.find(u=>u.id === selectedUserId)?.username || ''}?</Typography></DialogContent>
          <DialogActions><Button onClick={() => setConfirmDeleteDialog(false)}>Cancel</Button><Button onClick={handleDeleteUser} variant="contained" color="error" disabled={loading}>{loading ? <CircularProgress size={24} color="inherit"/> : 'Delete User'}</Button></DialogActions>
        </Dialog>

        <Snackbar open={notification.open} autoHideDuration={6000} onClose={handleCloseNotification} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
          <Alert onClose={handleCloseNotification} severity={notification.severity} sx={{ width: '100%' }} variant="filled" elevation={6}>{notification.message}</Alert>
        </Snackbar>
      </Box>
  );
};

export default SystemSettingsPage;