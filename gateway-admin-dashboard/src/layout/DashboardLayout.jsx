// src/layout/DashboardLayout.jsx
import React, { useState, useEffect } from 'react';
import { Box, Typography, IconButton, Tooltip } from '@mui/material';
import { styled, keyframes } from '@mui/material/styles';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import HexagonOutlinedIcon from '@mui/icons-material/HexagonOutlined';
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import SpeedOutlinedIcon from '@mui/icons-material/SpeedOutlined';
import ComputerOutlinedIcon from '@mui/icons-material/ComputerOutlined';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import KeyboardArrowRightIcon from '@mui/icons-material/KeyboardArrowRight';
import { NavHighlightContext } from '../pages/DashboardPage';

const Sidebar = styled(Box)(({ theme }) => ({
  width: 260,
  backgroundColor: '#FFFFFF',
  height: '100vh',
  padding: theme.spacing(3),
  boxShadow: '0px 0px 10px rgba(0, 0, 0, 0.1)',
  display: 'flex',
  flexDirection: 'column'
}));

const SidebarHeader = styled(Box)({
  display: 'flex',
  alignItems: 'center',
  marginBottom: '2rem'
});

// Create a pulsating animation
const pulse = keyframes`
  0% {
    box-shadow: 0 0 0 0 rgba(255, 145, 77, 0.7);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(255, 145, 77, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(255, 145, 77, 0);
  }
`;

const NavItem = styled(Box)(({ theme, active, highlight }) => ({
  display: 'flex',
  alignItems: 'center',
  padding: theme.spacing(1.5),
  marginBottom: theme.spacing(1),
  borderRadius: theme.spacing(1),
  cursor: 'pointer',
  backgroundColor: active ? '#FF914D' : 'transparent',
  color: active ? '#FFFFFF' : 'black',
  boxShadow: highlight ? 'none' : 'none',
  animation: highlight ? `${pulse} 2s infinite` : 'none',
  transition: 'all 0.3s ease',
  '&:hover': {
    backgroundColor: active ? '#FF914D' : 'rgba(255, 145, 77, 0.1)',
  }
}));

const NavIcon = styled(Box)(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  marginRight: theme.spacing(1.5)
}));

const Content = styled(Box)({
  flexGrow: 1,
  backgroundColor: '#F5F7FA',
  height: '100vh',
  overflow: 'auto'
});

const DashboardLayout = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [highlightedNav, setHighlightedNav] = useState(null);

  // Updated navigation items (Gateway Routes removed)
  const navItems = [
    {
      id: 'DASHBOARD',
      label: 'Dashboard',
      icon: <DashboardOutlinedIcon />,
      path: '/dashboard'
    },
    {
      id: 'RATE_LIMITS',
      label: 'Rate Limits',
      icon: <SpeedOutlinedIcon />,
      path: '/rate-limits'
    },
    {
      id: 'IP_MANAGEMENT',
      label: 'IP Management',
      icon: <ComputerOutlinedIcon />,
      path: '/ip-management'
    },
    {
      id: 'SYSTEM_SETTINGS',
      label: 'System Settings',
      icon: <SettingsOutlinedIcon />,
      path: '/system-settings'
    }
  ];

  const handleNavClick = (path) => {
    navigate(path);
  };

  // Context value for controlling navigation highlights
  const contextValue = {
    updateNavHighlight: (navId) => {
      setHighlightedNav(navId);
    }
  };

  return (
      <NavHighlightContext.Provider value={contextValue}>
        <Box display="flex">
          <Sidebar>
            <SidebarHeader>
              <HexagonOutlinedIcon sx={{ fontSize: '2rem', mr: 1 }} />
              <Box>
                <Typography variant="h6" fontWeight="bold">
                  Dashboard
                </Typography>
                <Typography variant="caption" color="textSecondary">

                </Typography>
              </Box>
            </SidebarHeader>

            {navItems.map((item) => (
                <Tooltip
                    key={item.label}
                    title={item.id === highlightedNav ? "Configuration needed" : ""}
                    placement="right"
                    arrow
                    open={item.id === highlightedNav}
                >
                  <NavItem
                      active={location.pathname === item.path}
                      highlight={item.id === highlightedNav}
                      onClick={() => handleNavClick(item.path)}
                  >
                    <NavIcon>
                      {item.icon}
                    </NavIcon>
                    <Typography sx={{ flexGrow: 1 }}>
                      {item.label}
                    </Typography>
                    {item.label !== 'Dashboard' && (
                        <KeyboardArrowRightIcon fontSize="small" />
                    )}
                  </NavItem>
                </Tooltip>
            ))}
          </Sidebar>

          <Content>
            <Outlet />
          </Content>
        </Box>
      </NavHighlightContext.Provider>
  );
};

export default DashboardLayout;