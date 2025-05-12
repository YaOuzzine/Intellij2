// demo 2/src/main/java/com/example/demo/Controller/AuthController.java
package com.example.demo.Controller;

import com.example.demo.Config.JwtUtil;
import com.example.demo.Entity.AdminUser; // Import AdminUser
import com.example.demo.Repository.AdminUserRepository; // Import AdminUserRepository
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final ReactiveAuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AdminUserRepository adminUserRepository; // For updating lock status
    private final PasswordEncoder passwordEncoder; // For direct password check if not using AuthManager for lock logic

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    public AuthController(ReactiveAuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          AdminUserRepository adminUserRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, String>>> login(@RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());

        return Mono.fromCallable(() -> adminUserRepository.findByUsername(loginRequest.getUsername()))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .flatMap(optionalUser -> {
                    if (optionalUser.isEmpty()) {
                        log.warn("User not found: {}", loginRequest.getUsername());
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error", "Invalid credentials", "message", "User not found or password incorrect.")));
                    }
                    AdminUser user = optionalUser.get();

                    if (!user.isEnabled()) {
                        log.warn("Login failed for {}: Account disabled", loginRequest.getUsername());
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error", "Account disabled", "message", "Your account is currently disabled.")));
                    }

                    if (!user.isAccountNonLocked()) {
                        log.warn("Login failed for {}: Account locked", loginRequest.getUsername());
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error", "Account locked", "message", "Your account is locked. Please try again later.")));
                    }

                    // Use ReactiveAuthenticationManager for the actual authentication
                    Authentication authToken = new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());
                    return authenticationManager.authenticate(authToken)
                            .flatMap(authentication -> {
                                // Reset failed attempts on successful login
                                user.setFailedLoginAttempts(0);
                                user.setAccountLockedUntil(null);
                                adminUserRepository.save(user); // Save changes
                                log.info("User {} authenticated successfully", loginRequest.getUsername());
                                String jwt = jwtUtil.generateToken(authentication);
                                return Mono.just(ResponseEntity.ok(Map.of("token", jwt)));
                            });
                })
                .onErrorResume(BadCredentialsException.class, e -> {
                    // Handle incorrect password - increment failed attempts
                    return Mono.fromCallable(() -> adminUserRepository.findByUsername(loginRequest.getUsername()))
                            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                            .flatMap(optionalUser -> {
                                if (optionalUser.isPresent()) {
                                    AdminUser user = optionalUser.get();
                                    user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                                    if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                                        user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
                                        log.warn("User {} account locked due to too many failed login attempts.", loginRequest.getUsername());
                                    }
                                    adminUserRepository.save(user);
                                }
                                log.warn("Login failed for {}: {}", loginRequest.getUsername(), e.getMessage());
                                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of("error", "Invalid credentials", "message", "User not found or password incorrect.")));
                            });
                })
                .onErrorResume(e -> { // Catch other errors
                    log.error("Authentication error for user {}: {}", loginRequest.getUsername(), e.getMessage(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Authentication failed", "message", "An internal error occurred.")));
                });
    }
}

class LoginRequest { // Keep this DTO or make it a record
    private String username;
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}