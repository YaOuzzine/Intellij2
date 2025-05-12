package com.example.gateway_admin.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for User entities.
 * Used to transfer user data between the frontend and backend
 * without exposing sensitive information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String jobTitle;
    private String department;
    private String profileImageUrl;
    private Boolean twoFactorEnabled;
    private Integer sessionTimeoutMinutes;
    private Boolean notificationsEnabled;
    private String role;
    private String status;
    private LocalDateTime lastLogin;

    /**
     * Get user's full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Check if user is an administrator
     */
    public boolean isAdmin() {
        return "Administrator".equals(role);
    }

    /**
     * Check if user account is active
     */
    public boolean isActive() {
        return "Active".equals(status);
    }

    /**
     * Format last login date for display
     */
    public String getFormattedLastLogin() {
        if (lastLogin == null) {
            return "Never";
        }

        return lastLogin.toString();
    }
}