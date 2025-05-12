// src/components/RouteList.jsx
import React from 'react';
import { Table, TableHead, TableBody, TableRow, TableCell, Button } from '@mui/material';
import { apiClient } from '../services/api.js';

const RouteList = ({ routes, onRoutesChange }) => {
  const handleDelete = async (routeId) => {
    if (!window.confirm(`Are you sure you want to delete route "${routeId}"?`)) {
      return;
    }
    try {
      await apiClient.delete(`/routes/${routeId}`);
      // Remove the deleted route from state
      onRoutesChange(routes.filter(route => route.id !== routeId));
    } catch (err) {
      console.error('Failed to delete route', err);
      alert('Failed to delete route. Please check the console for details.');
    }
  };

  return (
    <Table size="small">
      <TableHead>
        <TableRow>
          <TableCell>ID</TableCell>
          <TableCell>Destination URI</TableCell>
          <TableCell>Predicates</TableCell>
          <TableCell>Filters</TableCell>
          <TableCell>Actions</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {routes.map(route => (
          <TableRow key={route.id}>
            <TableCell>{route.id}</TableCell>
            <TableCell>{route.uri}</TableCell>
            <TableCell>{route.predicates?.join(', ')}</TableCell>
            <TableCell>{route.filters?.join(', ')}</TableCell>
            <TableCell>
              <Button
                variant="outlined"
                size="small"
                color="secondary"
                onClick={() => handleDelete(route.id)}
              >
                Delete
              </Button>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
};

export default RouteList;
