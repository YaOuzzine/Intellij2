// gateway-admin/src/main/java/com/example/gateway_admin/Controller/UserController.java
package com.example.gateway_admin.Controller;

import com.example.gateway_admin.DTO.UserDTO;
import com.example.gateway_admin.Entities.User;
import com.example.gateway_admin.Repositories.UserRepository;
import com.example.gateway_admin.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
// Remove: import org.springframework.security.core.context.SecurityContextHolder;
// Add:
import org.springframework.security.core.Authentication; // For type hint if needed
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt; // Or your specific principal type
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono; // Import Mono

import java.security.Principal; // Can also use this
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * Get current user profile
     * MODIFIED to use @AuthenticationPrincipal
     */
    @GetMapping("/profile")
    public Mono<ResponseEntity<?>> getCurrentUserProfile(@AuthenticationPrincipal Jwt jwtPrincipal) {
        if (jwtPrincipal == null) {
            ResponseEntity<?> unauthorizedResponse = ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            return Mono.just(unauthorizedResponse);
        }
        String username = jwtPrincipal.getSubject();

        return Mono.fromCallable(() -> userService.getUserByUsername(username)) // Returns UserDTO
                .map(userDTO -> {
                    // Explicitly create ResponseEntity<?>
                    return (ResponseEntity<?>) ResponseEntity.ok(userDTO);
                })
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .defaultIfEmpty(ResponseEntity.notFound().build()) // Handle if userService returns null/empty Mono
                .onErrorResume(e -> { // Generic error handling
                    // Log error e
                    ResponseEntity<?> errorResponse = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Failed to fetch profile"));
                    return Mono.just(errorResponse);
                });
    }


    /**
     * Update user profile
     * MODIFIED to use @AuthenticationPrincipal and return Mono
     */
    @PutMapping("/profile")
    public Mono<ResponseEntity<UserDTO>> updateProfile(@AuthenticationPrincipal Jwt jwtPrincipal,
                                                       @Valid @RequestBody UserDTO userDTO) {
        if (jwtPrincipal == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        String username = jwtPrincipal.getSubject();
        // Assuming userService.updateUserProfile is blocking
        return Mono.fromCallable(() -> {
            UserDTO updatedUser = userService.updateUserProfile(username, userDTO);
            return ResponseEntity.ok(updatedUser);
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    /**
     * Update password
     * MODIFIED to use @AuthenticationPrincipal and return Mono
     */
    @PutMapping("/password")
    public Mono<ResponseEntity<?>> updatePassword(@AuthenticationPrincipal Jwt jwtPrincipal,
                                                  @RequestBody PasswordChangeRequest request) {
        if (jwtPrincipal == null) {
            ResponseEntity<?> unauthorizedResponse = ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            return Mono.just(unauthorizedResponse);
        }
        String username = jwtPrincipal.getSubject();

        return Mono.fromCallable(() -> {
            try {
                userService.updatePassword(username, request.getCurrentPassword(), request.getNewPassword());
                Map<String, String> responseBody = new HashMap<>();
                responseBody.put("message", "Password updated successfully");
                // Create and cast immediately
                return (ResponseEntity<?>) ResponseEntity.ok(responseBody);
            } catch (IllegalArgumentException e) {
                // Create and cast immediately
                return (ResponseEntity<?>) ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", e.getMessage()));
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    /**
     * Update security settings
     * MODIFIED to use @AuthenticationPrincipal and return Mono
     */
    @PutMapping("/security")
    public Mono<ResponseEntity<UserDTO>> updateSecuritySettings(@AuthenticationPrincipal Jwt jwtPrincipal,
                                                                @RequestBody SecuritySettingsRequest request) {
        if (jwtPrincipal == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        String username = jwtPrincipal.getSubject();

        return Mono.fromCallable(() -> {
            UserDTO updatedUser = userService.updateSecuritySettings(
                    username,
                    request.getTwoFactorEnabled(),
                    request.getSessionTimeoutMinutes(),
                    request.getNotificationsEnabled()
            );
            return ResponseEntity.ok(updatedUser);
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    /**
     * Get all users (Admin only)
     * MODIFIED to use @AuthenticationPrincipal and return Mono
     */
    @GetMapping("/all")
    public Mono<ResponseEntity<?>> getAllUsers(@AuthenticationPrincipal Jwt jwtPrincipal) {
        if (jwtPrincipal == null) {
            // Ensure this also matches Mono<ResponseEntity<?>>
            ResponseEntity<?> unauthorizedResponse = ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            return Mono.just(unauthorizedResponse);
        }
        List<String> roles = jwtPrincipal.getClaimAsStringList("roles");
        if (roles == null || !roles.contains("SCOPE_ADMIN")) {
            // Ensure this also matches Mono<ResponseEntity<?>>
            ResponseEntity<?> forbiddenResponse = ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Requires administrator privileges"));
            return Mono.just(forbiddenResponse);
        }

        return Mono.fromCallable(() -> {
            List<UserDTO> users = userService.getAllUsers();
            // Create the ResponseEntity and immediately cast it to ResponseEntity<?>
            // This ensures the lambda's return type is exactly ResponseEntity<?>
            return (ResponseEntity<?>) ResponseEntity.ok(users);
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    /**
     * Create new user (Admin only)
     * MODIFIED to use @AuthenticationPrincipal and return Mono
     */
    @PostMapping
    public Mono<ResponseEntity<?>> createUser(@AuthenticationPrincipal Jwt jwtPrincipal,
                                              @Valid @RequestBody CreateUserRequest request) {
        if (jwtPrincipal == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        List<String> roles = jwtPrincipal.getClaimAsStringList("roles");
        if (roles == null || !roles.contains("SCOPE_ADMIN")) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Requires administrator privileges")));
        }

        return Mono.fromCallable(() -> {
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
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", e.getMessage()));
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    /**
     * Update user status (activate/deactivate) (Admin only)
     * MODIFIED to use @AuthenticationPrincipal and return Mono
     */
    @PatchMapping("/{id}/status")
    public Mono<ResponseEntity<?>> updateUserStatus(@AuthenticationPrincipal Jwt jwtPrincipal,
                                                    @PathVariable Long id,
                                                    @RequestBody Map<String, Boolean> request) {
        if (jwtPrincipal == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        List<String> roles = jwtPrincipal.getClaimAsStringList("roles");
        if (roles == null || !roles.contains("SCOPE_ADMIN")) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Requires administrator privileges")));
        }

        Boolean active = request.get("active");
        if (active == null) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Status value required")));
        }

        return Mono.fromCallable(() -> {
            try {
                UserDTO updatedUser = userService.updateUserStatus(id, active);
                return ResponseEntity.ok(updatedUser);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to update user status: " + e.getMessage()));
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    /**
     * Delete user (Admin only)
     * MODIFIED to use @AuthenticationPrincipal and return Mono
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<?>> deleteUser(@AuthenticationPrincipal Jwt jwtPrincipal,
                                              @PathVariable Long id) {
        if (jwtPrincipal == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        List<String> roles = jwtPrincipal.getClaimAsStringList("roles");
        if (roles == null || !roles.contains("SCOPE_ADMIN")) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Requires administrator privileges")));
        }

        return Mono.fromCallable(() -> {
            try {
                userService.deleteUser(id);
                // Construct ResponseEntity with a type compatible with ResponseEntity<?>
                return (ResponseEntity<?>) ResponseEntity.ok(Map.of("message", "User deleted successfully"));
            } catch (Exception e) {
                return (ResponseEntity<?>) ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to delete user: " + e.getMessage()));
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    /**
     * Get user session info
     * MODIFIED to use @AuthenticationPrincipal and return Mono
     */
    @GetMapping("/session")
    public Mono<ResponseEntity<?>> getUserSession(@AuthenticationPrincipal Jwt jwtPrincipal) {
        if (jwtPrincipal == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        String username = jwtPrincipal.getSubject();
        List<String> authorities = jwtPrincipal.getClaimAsStringList("roles");

        return Mono.fromCallable(() -> {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                // Construct ResponseEntity with a type compatible with ResponseEntity<?>
                return (ResponseEntity<?>) ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }

            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("username", username);
            sessionInfo.put("authorities", authorities);
            sessionInfo.put("sessionTimeoutMinutes", user.getSessionTimeoutMinutes());
            sessionInfo.put("lastLogin", user.getLastLoginAt());

            // Construct ResponseEntity with a type compatible with ResponseEntity<?>
            return (ResponseEntity<?>) ResponseEntity.ok(sessionInfo);
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    // Inner DTO classes remain the same
    public static class PasswordChangeRequest {
        private String currentPassword;
        private String newPassword;
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class SecuritySettingsRequest {
        private Boolean twoFactorEnabled;
        private Integer sessionTimeoutMinutes;
        private Boolean notificationsEnabled;
        public Boolean getTwoFactorEnabled() { return twoFactorEnabled; }
        public void setTwoFactorEnabled(Boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }
        public Integer getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
        public void setSessionTimeoutMinutes(Integer sessionTimeoutMinutes) { this.sessionTimeoutMinutes = sessionTimeoutMinutes; }
        public Boolean getNotificationsEnabled() { return notificationsEnabled; }
        public void setNotificationsEnabled(Boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
    }

    public static class CreateUserRequest {
        private String username;
        private String password;
        private String firstName;
        private String lastName;
        private String email;
        private String role;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}