// src/context/AuthContext.jsx
import React, { createContext, useState, useEffect } from 'react';
import axios from 'axios';
import apiClient from '../apiClient';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [user, setUser] = useState(null);

  // Check if user is already authenticated on component mount
  useEffect(() => {
    const token = localStorage.getItem('token');
    const userData = localStorage.getItem('userData');
    
    if (token) {
      apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;
      setIsAuthenticated(true);
      
      // If we have user data in localStorage, parse and use it
      if (userData) {
        try {
          setUser(JSON.parse(userData));
        } catch (error) {
          console.error('Error parsing user data from localStorage', error);
        }
      } else {
        // If we have a token but no user data, try to fetch the user profile
        fetchUserProfile(token);
      }
    }
    setIsLoading(false);
  }, []);

  const fetchUserProfile = async (token) => {
    try {
      // In a real app, you would have an endpoint to fetch the user profile
      // const response = await apiClient.get('/api/user/profile');
      // const userData = response.data;
      
      // For now, we'll use hardcoded data
      const userData = {
        id: '123',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john.doe@example.com',
        jobTitle: 'System Administrator',
        department: 'IT Operations',
        twoFactorEnabled: false,
        sessionTimeout: 30,
        notificationsEnabled: true
      };
      
      setUser(userData);
      localStorage.setItem('userData', JSON.stringify(userData));
    } catch (error) {
      console.error('Error fetching user profile', error);
    }
  };

  const login = async (username, password) => {
    try {
      // Try both approaches - Basic auth and form submission
      let response;
      try {
        // First try with Basic Auth
        response = await axios.get("http://localhost:8081/api/auth/login", {
          auth: {
            username,
            password,
          },
        });
      } catch (err) {
        // If that fails, try with form submission
        const formData = new URLSearchParams();
        formData.append('username', username);
        formData.append('password', password);
        
        response = await axios.post("http://localhost:9080/login", formData, {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
        });
      }
      
      if (response.status === 200) {
        // Store token if provided in the response
        if (response.data.token) {
          localStorage.setItem('token', response.data.token);
          apiClient.defaults.headers.common['Authorization'] = `Bearer ${response.data.token}`;
        }
        
        // For demo purposes, create a fake user object
        const userData = {
          id: '123',
          firstName: 'John',
          lastName: 'Doe',
          email: 'john.doe@example.com',
          jobTitle: 'System Administrator',
          department: 'IT Operations',
          twoFactorEnabled: false,
          sessionTimeout: 30,
          notificationsEnabled: true
        };
        
        setUser(userData);
        localStorage.setItem('userData', JSON.stringify(userData));
        setIsAuthenticated(true);
        return true;
      }
      return false;
    } catch (err) {
      console.error("Login failed:", err);
      return false;
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userData');
    delete apiClient.defaults.headers.common['Authorization'];
    setIsAuthenticated(false);
    setUser(null);
  };

  const updateUserProfile = (updatedProfile) => {
    const updatedUser = { ...user, ...updatedProfile };
    setUser(updatedUser);
    localStorage.setItem('userData', JSON.stringify(updatedUser));
  };

  return (
    <AuthContext.Provider value={{ 
      isAuthenticated, 
      isLoading, 
      login, 
      logout, 
      user, 
      updateUserProfile 
    }}>
      {children}
    </AuthContext.Provider>
  );
};