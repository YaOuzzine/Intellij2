// gateway-admin-dashboard/src/pages/LoginPage.jsx
import React, { useState, useContext, useEffect } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { Box, Button, TextField, Typography, Paper, CircularProgress, Alert } from '@mui/material';

const LoginPage = () => {
  const { login, isAuthenticated, isLoading: authLoading } = useContext(AuthContext);
  const navigate = useNavigate();
  const location = useLocation();

  const from = location.state?.from?.pathname || '/dashboard';

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (error) setError('');
  }, [username, password]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!username || !password) {
      setError('Username and password are required.');
      return;
    }

    setIsSubmitting(true);
    setError('');
    try {
      await login(username, password);
      navigate(from, { replace: true });
    } catch (err) {
      const errorMessage = err.response?.data?.message || err.response?.data?.error || 'Login failed. Please check your credentials.';
      setError(errorMessage);
      console.error('Login error:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (authLoading) {
    return (
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
          <CircularProgress />
        </Box>
    );
  }

  if (isAuthenticated) {
    return <Navigate to={from} replace />;
  }

  return (
      <Box
          sx={{
            display: 'flex',
            minHeight: '100vh',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: (theme) => theme.palette.grey[100]
          }}
      >
        <Paper
            elevation={6}
            sx={{
              p: 4,
              width: '100%',
              maxWidth: 400,
              borderRadius: 2,
              boxShadow: '0px 10px 25px rgba(0,0,0,0.1)'
            }}
        >
          <Typography variant="h4" component="h1" gutterBottom textAlign="center" fontWeight="bold" color="primary">
            Admin Login
          </Typography>
          <Typography variant="body2" color="text.secondary" textAlign="center" sx={{ mb: 3 }}>
            Access the Gateway Management Dashboard.
          </Typography>

          {error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {error}
              </Alert>
          )}

          <form onSubmit={handleSubmit}>
            <TextField
                label="Username"
                variant="outlined"
                fullWidth
                margin="normal"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={isSubmitting}
                autoFocus
                required
            />
            <TextField
                label="Password"
                variant="outlined"
                type="password"
                fullWidth
                margin="normal"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={isSubmitting}
                required
            />
            <Button
                type="submit"
                fullWidth
                variant="contained"
                color="primary"
                sx={{ mt: 3, mb: 2, py: 1.5, fontSize: '1rem' }}
                disabled={isSubmitting || authLoading}
            >
              {isSubmitting ? <CircularProgress size={24} color="inherit" /> : 'Sign In'}
            </Button>
          </form>
        </Paper>
      </Box>
  );
};

export default LoginPage;