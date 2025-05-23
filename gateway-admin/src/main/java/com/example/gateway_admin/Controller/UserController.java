// gateway-admin/src/main/java/com/example/gateway_admin/Controller/UserController.java
package com.example.gateway_admin.Controller;

import com.example.gateway_admin.DTO.UserDTO;
import com.example.gateway_admin.Services.StorageService;
import com.example.gateway_admin.Services.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final StorageService storageService;

    @Autowired
    public UserController(UserService userService, StorageService storageService) {
        this.userService = userService;
        this.storageService = storageService;
    }

    @GetMapping("/profile")
    public Mono<ResponseEntity<?>> getCurrentUserProfile() {
        logger.info("Received request to /api/user/profile");
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    try {
                        logger.info("Authentication found: {}", authentication);
                        logger.info("Authentication type: {}", authentication.getClass().getName());
                        logger.info("Authentication principal type: {}", authentication.getPrincipal().getClass().getName());
                        logger.info("Authentication principal: {}", authentication.getPrincipal());
                        logger.info("Username from authentication: {}", authentication.getName());
                        logger.info("Authorities: {}", authentication.getAuthorities());

                        String username = authentication.getName();
                        UserDTO userDTO = userService.getUserByUsername(username);
                        logger.info("Successfully retrieved user profile for: {}", username);
                        return Mono.just(ResponseEntity.ok(userDTO));
                    } catch (Exception e) {
                        logger.error("Error retrieving user profile: {}", e.getMessage(), e);
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Failed to retrieve user profile");
                        errorResponse.put("message", e.getMessage());
                        errorResponse.put("type", e.getClass().getName());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                    }
                })
                .doOnError(e -> logger.error("Error processing authentication context: {}", e.getMessage(), e))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No authentication found", "message", "User not authenticated")));
    }

    /**
     * Update user profile information - CONVERTED TO REACTIVE
     */
    @PutMapping("/profile")
    public Mono<ResponseEntity<?>> updateProfile(@Valid @RequestBody UserProfileRequest request) {
        logger.info("Received PUT request to /api/user/profile");
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    try {
                        logger.info("Authentication found in PUT: {}", authentication);
                        logger.info("Username from authentication in PUT: {}", authentication.getName());
                        logger.info("Authorities in PUT: {}", authentication.getAuthorities());

                        String username = authentication.getName();
                        logger.info("Updating profile for user: {}", username);

                        UserDTO updatedUser = userService.updateUserProfile(username, request);
                        logger.info("Profile successfully updated for user: {}", username);
                        return Mono.just(ResponseEntity.ok(updatedUser));
                    } catch (Exception e) {
                        logger.error("Error updating user profile: {}", e.getMessage(), e);
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Failed to update user profile");
                        errorResponse.put("message", e.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                    }
                })
                .doOnError(e -> logger.error("Error processing authentication context in PUT: {}", e.getMessage(), e))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required")));
    }

    /**
     * Upload profile picture
     */
    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfilePicture(@RequestParam("profileImage") MultipartFile file) {
        logger.info("Received file upload request with name: {}, size: {}, content type: {}",
                file != null ? file.getOriginalFilename() : "null",
                file != null ? file.getSize() : 0,
                file != null ? file.getContentType() : "null");
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            String username = auth.getName();
            logger.info("Uploading profile picture for user: {}", username);

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is empty", "message", "Please select a file to upload"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid file type", "message", "Only image files are allowed"));
            }

            // Save the file and update the user profile
            String imageUrl = storageService.storeProfileImage(file, username);
            UserDTO updatedUser = userService.updateProfileImage(username, imageUrl);

            return ResponseEntity.ok(Map.of(
                    "profileImageUrl", imageUrl,
                    "message", "Profile picture uploaded successfully"
            ));
        } catch (Exception e) {
            logger.error("Error uploading profile picture: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to upload profile picture");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Update password
     */
    @PutMapping("/password")
    public Mono<ResponseEntity<?>> updatePassword(@Valid @RequestBody PasswordChangeRequest request) {
        logger.info("Received PUT request to /api/user/password");
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    try {
                        logger.info("Authentication found in updatePassword: {}", authentication);
                        logger.info("Username from authentication in updatePassword: {}", authentication.getName());
                        logger.info("Authorities in updatePassword: {}", authentication.getAuthorities());

                        String username = authentication.getName();
                        logger.info("Updating password for user: {}", username);

                        userService.updatePassword(username, request.getCurrentPassword(), request.getNewPassword());

                        // Use explicit type cast to ResponseEntity<?>
                        return Mono.just((ResponseEntity<?>) ResponseEntity.ok(Map.of(
                                "message", "Password updated successfully"
                        )));
                    } catch (IllegalArgumentException e) {
                        logger.warn("Password update failed: {}", e.getMessage());
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Invalid password");
                        errorResponse.put("message", e.getMessage());
                        // Use explicit type cast to ResponseEntity<?>
                        return Mono.just((ResponseEntity<?>) ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
                    } catch (Exception e) {
                        logger.error("Error updating password: {}", e.getMessage(), e);
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Failed to update password");
                        errorResponse.put("message", e.getMessage());
                        // Use explicit type cast to ResponseEntity<?>
                        return Mono.just((ResponseEntity<?>) ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                    }
                })
                .doOnError(e -> logger.error("Error processing authentication context in updatePassword: {}", e.getMessage(), e))
                // Use explicit type cast here too
                .defaultIfEmpty((ResponseEntity<?>) ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required")));
    }

    /**
     * Get all users (Admin only) - FIXED to be fully reactive
     */
    @GetMapping("/all")
    public Mono<ResponseEntity<?>> getAllUsers() {
        logger.info("Received request to get all users");

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    logger.info("Authentication retrieved in getAllUsers: {}", auth);
                    // Check if user has admin role
                    boolean isAdmin = auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("SCOPE_ADMIN") ||
                                    a.getAuthority().equals("ADMIN"));

                    if (!isAdmin) {
                        logger.warn("Access denied - user {} does not have admin role", auth.getName());
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "Access denied", "message", "Admin role required")));
                    }

                    try {
                        // Get all users from the service
                        List<UserDTO> users = userService.getAllUsers();
                        logger.info("Successfully retrieved {} users", users.size());
                        return Mono.just(ResponseEntity.ok(users));
                    } catch (Exception e) {
                        logger.error("Error retrieving all users: {}", e.getMessage(), e);
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Failed to retrieve users");
                        errorResponse.put("message", e.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                    }
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required")));
    }

    /**
     * Get user by ID (Admin only)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            // Check if user has admin role or is requesting their own profile
            if (!hasAdminRole(auth) && !isOwnProfile(auth, id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied", "message", "You can only view your own profile"));
            }

            UserDTO user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Error retrieving user {}: {}", id, e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve user");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Create new user (Admin only) - CONVERTED TO REACTIVE
     */
    @PostMapping
    public Mono<ResponseEntity<?>> createUser(@Valid @RequestBody CreateUserRequest request) {
        logger.info("Received POST request to /api/user to create a new user");
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    try {
                        logger.info("Authentication found in createUser: {}", auth);
                        logger.info("Username from authentication in createUser: {}", auth.getName());
                        logger.info("Authorities in createUser: {}", auth.getAuthorities());

                        // Check if user has admin role
                        boolean isAdmin = auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("SCOPE_ADMIN") ||
                                        a.getAuthority().equals("ADMIN"));

                        if (!isAdmin) {
                            logger.warn("Access denied for user {} - admin role required", auth.getName());
                            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body(Map.of("error", "Access denied", "message", "Admin role required")));
                        }

                        UserDTO newUser = userService.createUser(
                                request.getUsername(),
                                request.getPassword(),
                                request.getFirstName(),
                                request.getLastName(),
                                request.getEmail(),
                                request.getRole()
                        );

                        logger.info("User created successfully: {}", newUser.getUsername());
                        return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(newUser));
                    } catch (IllegalArgumentException e) {
                        logger.warn("User creation failed: {}", e.getMessage());
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Invalid user data");
                        errorResponse.put("message", e.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
                    } catch (Exception e) {
                        logger.error("Error creating user: {}", e.getMessage(), e);
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Failed to create user");
                        errorResponse.put("message", e.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                    }
                })
                .doOnError(e -> logger.error("Error processing authentication context in createUser: {}", e.getMessage(), e))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required")));
    }

    /**
     * Update user (Admin only)
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<?>> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    try {
                        // Check if user has admin role or is updating their own profile
                        if (!hasAdminRole(auth) && !isOwnProfile(auth, id)) {
                            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body(Map.of("error", "Access denied", "message", "You can only update your own profile")));
                        }

                        // Only admin can change roles
                        if (request.getRole() != null && !hasAdminRole(auth)) {
                            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body(Map.of("error", "Access denied", "message", "Only admins can change user roles")));
                        }

                        UserDTO updatedUser = userService.updateUser(id, request);
                        return Mono.just(ResponseEntity.ok(updatedUser));
                    } catch (IllegalArgumentException e) {
                        logger.warn("User update failed for ID {}: {}", id, e.getMessage());
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Invalid user data");
                        errorResponse.put("message", e.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
                    } catch (Exception e) {
                        logger.error("Error updating user {}: {}", id, e.getMessage(), e);
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Failed to update user");
                        errorResponse.put("message", e.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                    }
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required")));
    }

    /**
     * Update user status (Admin only)
     */
    @PatchMapping("/{id}/status")
    public Mono<ResponseEntity<?>> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    try {
                        // Check if user has admin role
                        if (!hasAdminRole(auth)) {
                            return Mono.just((ResponseEntity<?>) ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body(Map.of("error", "Access denied", "message", "Admin role required")));
                        }

                        // Get the active status from the request
                        Boolean active = request.get("active");
                        if (active == null) {
                            return Mono.just((ResponseEntity<?>) ResponseEntity.badRequest()
                                    .body(Map.of("error", "Missing data", "message", "Active status is required")));
                        }

                        // Prevent admin from deactivating their own account
                        String adminUsername = auth.getName();
                        if (userService.isUserWithId(id, adminUsername) && !active) {
                            return Mono.just((ResponseEntity<?>) ResponseEntity.badRequest()
                                    .body(Map.of("error", "Invalid operation", "message", "You cannot deactivate your own account")));
                        }

                        UserDTO updatedUser = userService.updateUserStatus(id, active);
                        return Mono.just((ResponseEntity<?>) ResponseEntity.ok(updatedUser));
                    } catch (Exception e) {
                        logger.error("Error updating user status for ID {}: {}", id, e.getMessage(), e);
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Failed to update user status");
                        errorResponse.put("message", e.getMessage());
                        return Mono.just((ResponseEntity<?>) ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                    }
                })
                .defaultIfEmpty((ResponseEntity<?>) ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required")));
    }

    /**
     * Delete user (Admin only)
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<?>> deleteUser(@PathVariable Long id) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    try {
                        // Check if user has admin role
                        if (!hasAdminRole(auth)) {
                            return Mono.just((ResponseEntity<?>) ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body(Map.of("error", "Access denied", "message", "Admin role required")));
                        }

                        // Prevent admin from deleting their own account
                        String adminUsername = auth.getName();
                        if (userService.isUserWithId(id, adminUsername)) {
                            return Mono.just((ResponseEntity<?>) ResponseEntity.badRequest()
                                    .body(Map.of("error", "Invalid operation", "message", "You cannot delete your own account")));
                        }

                        userService.deleteUser(id);
                        return Mono.just((ResponseEntity<?>) ResponseEntity.ok(Map.of(
                                "message", "User deleted successfully"
                        )));
                    } catch (Exception e) {
                        logger.error("Error deleting user {}: {}", id, e.getMessage(), e);
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Failed to delete user");
                        errorResponse.put("message", e.getMessage());
                        return Mono.just((ResponseEntity<?>) ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                    }
                })
                .defaultIfEmpty((ResponseEntity<?>) ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required")));
    }

    // Helper methods for role and identity checks

    private boolean hasAdminRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SCOPE_ADMIN") ||
                        a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean isOwnProfile(Authentication auth, Long userId) {
        try {
            String username = auth.getName();
            return userService.isUserWithId(userId, username);
        } catch (Exception e) {
            return false;
        }
    }

    // Request/Response classes

    public static class UserProfileRequest {
        private String firstName;
        private String lastName;
        private String email;

        // Getters and setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class PasswordChangeRequest {
        private String currentPassword;
        private String newPassword;

        // Getters and setters
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class CreateUserRequest {
        private String username;
        private String password;
        private String firstName;
        private String lastName;
        private String email;
        private String role;

        // Getters and setters
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

    public static class UpdateUserRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String role;

        // Getters and setters
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