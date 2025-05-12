package com.example.gateway_admin.Services;

import com.example.gateway_admin.DTO.UserDTO;
import com.example.gateway_admin.Entities.User;
import com.example.gateway_admin.Repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Get all users
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get user by username
     */
    @Transactional(readOnly = true)
    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));
        return convertToDTO(user);
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        return convertToDTO(user);
    }

    /**
     * Create a new user
     */
    @Transactional
    public UserDTO createUser(String username, String password, String firstName,
                              String lastName, String email, String role) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setRole(role != null && role.equalsIgnoreCase("Administrator") ? "ADMIN" : "USER");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setTwoFactorEnabled(false);
        user.setSessionTimeoutMinutes(30);
        user.setNotificationsEnabled(true);

        // Save user
        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserDTO updateUserProfile(String username, UserDTO userDTO) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));

        // Check email uniqueness if changed
        if (!user.getEmail().equals(userDTO.getEmail()) &&
                userRepository.existsByEmail(userDTO.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        // Update fields
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());

        if (userDTO.getJobTitle() != null) {
            user.setJobTitle(userDTO.getJobTitle());
        }

        if (userDTO.getDepartment() != null) {
            user.setDepartment(userDTO.getDepartment());
        }

        user.setUpdatedAt(LocalDateTime.now());

        // Save user
        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    /**
     * Update password
     */
    @Transactional
    public void updatePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Validate new password
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Update security settings
     */
    @Transactional
    public UserDTO updateSecuritySettings(String username, Boolean twoFactorEnabled,
                                          Integer sessionTimeoutMinutes, Boolean notificationsEnabled) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));

        if (twoFactorEnabled != null) {
            user.setTwoFactorEnabled(twoFactorEnabled);
        }

        if (sessionTimeoutMinutes != null) {
            // Ensure timeout is between reasonable bounds
            sessionTimeoutMinutes = Math.max(5, Math.min(120, sessionTimeoutMinutes));
            user.setSessionTimeoutMinutes(sessionTimeoutMinutes);
        }

        if (notificationsEnabled != null) {
            user.setNotificationsEnabled(notificationsEnabled);
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    /**
     * Record user login
     */
    @Transactional
    public void recordUserLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    /**
     * Update user status (active/inactive)
     */
    @Transactional
    public UserDTO updateUserStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // Don't allow disabling of system admin
        if (user.getUsername().equals("admin") && !active) {
            throw new IllegalArgumentException("Cannot disable the system administrator account");
        }

        // Set status in database - we'll use a boolean field internally
        // but expose it as a string status in the DTO
        user.setActive(active);
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    /**
     * Delete a user
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // Don't allow deleting system admin
        if (user.getUsername().equals("admin")) {
            throw new IllegalArgumentException("Cannot delete the system administrator account");
        }

        userRepository.delete(user);
    }

    /**
     * Convert User entity to UserDTO
     */
    public UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setJobTitle(user.getJobTitle());
        dto.setDepartment(user.getDepartment());
        dto.setProfileImageUrl(user.getProfileImageUrl());
        dto.setTwoFactorEnabled(user.getTwoFactorEnabled());
        dto.setSessionTimeoutMinutes(user.getSessionTimeoutMinutes());
        dto.setNotificationsEnabled(user.getNotificationsEnabled());

        // Convert internal role format to display format
        if (user.getRole() != null && user.getRole().equals("ADMIN")) {
            dto.setRole("Administrator");
        } else {
            dto.setRole("User");
        }

        // Convert active flag to status string
        dto.setStatus(user.isActive() ? "Active" : "Disabled");

        // Add last login time
        dto.setLastLogin(user.getLastLoginAt());

        return dto;
    }
}