// gateway-admin/src/main/java/com/example/gateway_admin/Controller/UserController.java
package com.example.gateway_admin.Controller;

import com.example.gateway_admin.DTO.UserDTO;
import com.example.gateway_admin.Entities.User;
import com.example.gateway_admin.Repositories.UserRepository;
import com.example.gateway_admin.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @Autowired
    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public Mono<ResponseEntity<?>> getCurrentUserProfile() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    try {
                        String username = authentication.getName();
                        System.out.println("Authentication principal: " + authentication.getPrincipal());
                        System.out.println("Username from authentication: " + username);
                        System.out.println("Authorities: " + authentication.getAuthorities());

                        UserDTO userDTO = userService.getUserByUsername(username);
                        return Mono.just(ResponseEntity.ok(userDTO));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Failed to retrieve user profile");
                        errorResponse.put("message", e.getMessage());
                        errorResponse.put("type", e.getClass().getName());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                    }
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No authentication found", "message", "User not authenticated")));
    }

    /**
     * Update user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<UserDTO> updateProfile(@Valid @RequestBody UserDTO userDTO) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        UserDTO updatedUser = userService.updateUserProfile(username, userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Update password
     */
    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> updatePassword(@RequestBody PasswordChangeRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        try {
            userService.updatePassword(username, request.getCurrentPassword(), request.getNewPassword());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password updated successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Update security settings
     */
    @PutMapping("/security")
    public ResponseEntity<UserDTO> updateSecuritySettings(@RequestBody SecuritySettingsRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        UserDTO updatedUser = userService.updateSecuritySettings(
                username,
                request.getTwoFactorEnabled(),
                request.getSessionTimeoutMinutes(),
                request.getNotificationsEnabled()
        );

        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Get all users (Admin only)
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Requires administrator privileges");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorMap);
        }

        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Create new user (Admin only)
     */
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Requires administrator privileges");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorMap);
        }

        try {
            UserDTO newUser = userService.createUser(
                    request.getUsername(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getEmail(),
                    request.getRole()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMap);
        }
    }

    /**
     * Update user status (activate/deactivate) (Admin only)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Requires administrator privileges");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorMap);
        }

        Boolean active = request.get("active");
        if (active == null) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Status value required");
            return ResponseEntity.badRequest().body(errorMap);
        }

        try {
            UserDTO updatedUser = userService.updateUserStatus(id, active);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Failed to update user status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMap);
        }
    }

    /**
     * Delete user (Admin only)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Requires administrator privileges");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorMap);
        }

        try {
            userService.deleteUser(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Failed to delete user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMap);
        }
    }

    /**
     * Get user session info
     */
    @GetMapping("/session")
    public ResponseEntity<?> getUserSession() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMap);
        }

        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("username", username);
        sessionInfo.put("authorities", auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(java.util.stream.Collectors.toList()));
        sessionInfo.put("sessionTimeoutMinutes", user.getSessionTimeoutMinutes());
        sessionInfo.put("lastLogin", user.getLastLoginAt());

        return ResponseEntity.ok(sessionInfo);
    }

    /**
     * DTO for password change request
     */
    public static class PasswordChangeRequest {
        private String currentPassword;
        private String newPassword;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    /**
     * DTO for security settings request
     */
    public static class SecuritySettingsRequest {
        private Boolean twoFactorEnabled;
        private Integer sessionTimeoutMinutes;
        private Boolean notificationsEnabled;

        public Boolean getTwoFactorEnabled() {
            return twoFactorEnabled;
        }

        public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
            this.twoFactorEnabled = twoFactorEnabled;
        }

        public Integer getSessionTimeoutMinutes() {
            return sessionTimeoutMinutes;
        }

        public void setSessionTimeoutMinutes(Integer sessionTimeoutMinutes) {
            this.sessionTimeoutMinutes = sessionTimeoutMinutes;
        }

        public Boolean getNotificationsEnabled() {
            return notificationsEnabled;
        }

        public void setNotificationsEnabled(Boolean notificationsEnabled) {
            this.notificationsEnabled = notificationsEnabled;
        }
    }

    /**
     * DTO for creating a new user
     */
    public static class CreateUserRequest {
        private String username;
        private String password;
        private String firstName;
        private String lastName;
        private String email;
        private String role;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}