// gateway-admin/src/main/java/com/example/gateway_admin/Services/UserService.java
package com.example.gateway_admin.Services;

import com.example.gateway_admin.Controller.UserController.UserProfileRequest;
import com.example.gateway_admin.Controller.UserController.UpdateUserRequest;
import com.example.gateway_admin.DTO.UserDTO;
import com.example.gateway_admin.Entities.User;
import com.example.gateway_admin.Repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

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
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
        return convertToDTO(user);
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
     * Update user profile
     */
    @Transactional
    public UserDTO updateUserProfile(String username, UserProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));

        // Update fields
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        // Check if email is changing and if it's available
        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use by another account");
        }
        user.setEmail(request.getEmail());

        // Save and return updated user
        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    /**
     * Update profile image
     */
    @Transactional
    public UserDTO updateProfileImage(String username, String imageUrl) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));

        user.setProfileImageUrl(imageUrl);
        User savedUser = userRepository.save(user);

        return convertToDTO(savedUser);
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
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        logger.info("Password updated for user: {}", username);
    }

    /**
     * Create new user
     */
    @Transactional
    public UserDTO createUser(String username, String password, String firstName,
                              String lastName, String email, String role) {
        // Validate inputs
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Valid email is required");
        }

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

        // Set role (default to USER if not specified or invalid)
        String normalizedRole = "USER";
        if (role != null && "ADMIN".equalsIgnoreCase(role)) {
            normalizedRole = "ADMIN";
        }
        user.setRole(normalizedRole);

        // Set default values
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        // Save and return
        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    /**
     * Update user
     */
    @Transactional
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        // Update basic info
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        // Check if email is changing and if it's available
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already in use by another account");
            }
            user.setEmail(request.getEmail());
        }

        // Update role if provided
        if (request.getRole() != null) {
            String normalizedRole = "USER";
            if ("ADMIN".equalsIgnoreCase(request.getRole())) {
                normalizedRole = "ADMIN";
            }
            user.setRole(normalizedRole);
        }

        // Save and return
        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    /**
     * Update user status (active/inactive)
     */
    @Transactional
    public UserDTO updateUserStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        // Update active status
        user.setActive(active);

        // Save and return
        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    /**
     * Delete user
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        // Check if it's the primary admin account
        if (user.getUsername().equals("admin")) {
            throw new IllegalArgumentException("Cannot delete the primary administrator account");
        }

        userRepository.delete(user);
        logger.info("User deleted: ID={}, username={}", id, user.getUsername());
    }

    /**
     * Check if a user ID matches a username
     */
    @Transactional(readOnly = true)
    public boolean isUserWithId(Long id, String username) {
        User user = userRepository.findById(id).orElse(null);
        return user != null && user.getUsername().equals(username);
    }

    /**
     * Convert Entity to DTO
     */
    public UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setProfileImageUrl(user.getProfileImageUrl());
        dto.setRole(user.getRole());
        dto.setStatus(user.isActive() ? "Active" : "Disabled");
        dto.setLastLogin(user.getLastLoginAt());
        dto.setIsAdmin(user.isAdmin());

        return dto;
    }
}