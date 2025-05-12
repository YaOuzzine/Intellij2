// gateway-admin/src/main/java/com/example/gateway_admin/Services/UserService.java
package com.example.gateway_admin.Services;

import com.example.gateway_admin.DTO.UserDTO;
import com.example.gateway_admin.Entities.User;
import com.example.gateway_admin.Repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;


    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));
        if (user.isAccountLocked()) {
            throw new IllegalStateException("User account is locked.");
        }
        return convertToDTO(user);
    }

    @Transactional(readOnly = true)
    public User findUserEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }


    @Transactional
    public UserDTO createUser(String username, String password, String firstName,
                              String lastName, String email, String role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // Hash password
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setRole(role != null && "ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "USER");
        user.setActive(true);
        user.setTwoFactorEnabled(false);
        user.setSessionTimeoutMinutes(30);
        user.setNotificationsEnabled(true);

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    @Transactional
    public UserDTO updateUserProfile(String username, UserDTO userDTO) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));

        if (!user.getEmail().equals(userDTO.getEmail()) &&
                userRepository.existsByEmail(userDTO.getEmail())) {
            throw new IllegalArgumentException("Email already in use by another account.");
        }

        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setJobTitle(userDTO.getJobTitle());
        user.setDepartment(userDTO.getDepartment());
        // Profile image URL update would be handled separately

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    @Transactional
    public void updatePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect current password.");
        }
        if (newPassword.length() < 8) { // Basic validation
            throw new IllegalArgumentException("New password must be at least 8 characters long.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public UserDTO updateSecuritySettings(String username, Boolean twoFactorEnabled,
                                          Integer sessionTimeoutMinutes, Boolean notificationsEnabled) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));

        if (twoFactorEnabled != null) user.setTwoFactorEnabled(twoFactorEnabled);
        if (sessionTimeoutMinutes != null) user.setSessionTimeoutMinutes(Math.max(5, Math.min(sessionTimeoutMinutes, 24 * 60))); // 5 min to 1 day
        if (notificationsEnabled != null) user.setNotificationsEnabled(notificationsEnabled);

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }


    @Transactional
    public void recordLoginSuccess(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    @Transactional
    public void recordLoginFailure(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            }
            userRepository.save(user);
        });
    }

    @Transactional
    public UserDTO updateUserStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (user.getUsername().equals("admin") && !active) { // Assuming "admin" is a superuser
            throw new IllegalArgumentException("Cannot deactivate the primary administrator account.");
        }
        user.setActive(active);
        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        if (user.getUsername().equals("admin")) {
            throw new IllegalArgumentException("Cannot delete the primary administrator account.");
        }
        userRepository.deleteById(userId);
    }

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
        dto.setRole(user.getRole());
        dto.setStatus(user.isActive() ? "Active" : "Disabled");
        dto.setLastLogin(user.getLastLoginAt());
        return dto;
    }
}