// src/components/SyncButton.jsx
import React, { useState } from 'react';
import { Button, Snackbar, Alert, CircularProgress } from '@mui/material'; // Added CircularProgress
import SyncIcon from '@mui/icons-material/Sync'; // Added SyncIcon
import axios from 'axios';

function SyncButton() {
    const [loading, setLoading] = useState(false);
    const [showAlert, setShowAlert] = useState(false);
    const [alertMessage, setAlertMessage] = useState('');
    const [alertType, setAlertType] = useState('success'); // 'success' | 'error' | 'info' | 'warning'

    const handleSync = async () => {
        if (loading) return;

        setLoading(true);
        try {
            // Simulate API call delay for demo if needed
            // await new Promise(resolve => setTimeout(resolve, 1500));
            const response = await axios.post('/api/sync/routes');
            console.log('Sync response:', response.data);
            setAlertMessage(response.data?.message || 'Routes synchronized successfully!');
            setAlertType('success');
        } catch (error) {
            console.error('Sync error:', error);
            setAlertMessage(error.response?.data?.message || 'Failed to synchronize routes.');
            setAlertType('error');
        } finally {
            setLoading(false);
            setShowAlert(true);
        }
    };

    const handleCloseAlert = (event, reason) => {
        if (reason === 'clickaway') {
            return;
        }
        setShowAlert(false);
    };

    return (
        <>
            <Button
                variant="contained"
                color="secondary" // Or primary, depending on desired emphasis
                onClick={handleSync}
                disabled={loading}
                startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <SyncIcon />}
                sx={{ minWidth: 120 }} // Prevents layout shift when text changes
            >
                {loading ? 'Syncing...' : 'Sync Now'}
            </Button>

            <Snackbar
                open={showAlert}
                autoHideDuration={6000}
                onClose={handleCloseAlert}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
            >
                <Alert
                    onClose={handleCloseAlert}
                    severity={alertType}
                    sx={{ width: '100%' }}
                    elevation={6}
                >
                    {alertMessage}
                </Alert>
            </Snackbar>
        </>
    );
}

export default SyncButton;