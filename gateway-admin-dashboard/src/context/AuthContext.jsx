// gateway-admin-dashboard/src/context/AuthContext.jsx
import React, { createContext, useState, useEffect, useCallback } from 'react';
import apiClient from '../apiClient'; // Use the configured apiClient

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true); // Start with loading true

  // gateway-admin-dashboard/src/context/AuthContext.jsx
  const fetchUserProfile = useCallback(async () => {
    try {
      // Make sure the token is properly set before making this request
      const token = localStorage.getItem('token');

      if (!token) {
        throw new Error('No authentication token found');
      }

      // Explicitly set the header for this request
      const response = await apiClient.get('/user/profile', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      setUser(response.data);
      localStorage.setItem('userData', JSON.stringify(response.data));
      setIsAuthenticated(true);
    } catch (error) {
      console.error('Failed to fetch user profile:', error);
      // If fetching profile fails, token might be invalid
      logout(); // Clear invalid state
    }
  }, []);


  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;
      // Try to fetch user profile to validate token and get user data
      fetchUserProfile().finally(() => setIsLoading(false));
    } else {
      setIsLoading(false); // No token, not loading
    }
  }, [fetchUserProfile]);

  // gateway-admin-dashboard/src/context/AuthContext.jsx - Line ~40
  const login = async (username, password) => {
    try {
      const response = await apiClient.post('/auth/login', { username, password });

      if (response.data && response.data.token) {
        const { token } = response.data;
        localStorage.setItem('token', token);
        apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        await fetchUserProfile();
        return true;
      }
      return false;
    } catch (error) {
      console.error("Login failed:", error.response?.data?.message || error.message);
      throw error; // Re-throw to be caught by LoginPage
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userData');
    delete apiClient.defaults.headers.common['Authorization'];
    setIsAuthenticated(false);
    setUser(null);
    // Optionally redirect to login page
    // window.location.href = '/login';
  };

  const updateUserContextProfile = (updatedProfileData) => {
    setUser(prevUser => ({ ...prevUser, ...updatedProfileData }));
    localStorage.setItem('userData', JSON.stringify({ ...user, ...updatedProfileData }));
  };


  return (
      <AuthContext.Provider value={{
        isAuthenticated,
        user,
        login,
        logout,
        isLoading,
        updateUserProfile: updateUserContextProfile // Renamed for clarity
      }}>
        {children}
      </AuthContext.Provider>
  );
};