// demo 2/src/main/java/com/example/demo/Service/ReactiveAdminUserDetailsService.java
package com.example.demo.Service;

import com.example.demo.Entity.AdminUser;
import com.example.demo.Repository.AdminUserRepository;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional; // Import Optional

@Service
public class ReactiveAdminUserDetailsService implements ReactiveUserDetailsService {

    // Declare the logger instance
    private static final Logger log = LoggerFactory.getLogger(ReactiveAdminUserDetailsService.class);

    private final AdminUserRepository adminUserRepository;

    public ReactiveAdminUserDetailsService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        log.info("ReactiveAdminUserDetailsService: Attempting to find user: {}", username);
        return Mono.fromCallable(() -> {
                    Optional<AdminUser> userOptional = adminUserRepository.findByUsername(username);
                    if (userOptional.isEmpty()) {
                        log.warn("ReactiveAdminUserDetailsService: User {} not found in admin.users table", username);
                        throw new UsernameNotFoundException("User not found: " + username);
                    }
                    AdminUser user = userOptional.get();
                    log.info("ReactiveAdminUserDetailsService: User {} found. Active: {}, Role: {}, Locked: {}",
                            username, user.isEnabled(), user.getRole(), !user.isAccountNonLocked());
                    return user; // AdminUser implements UserDetails
                })
                .subscribeOn(Schedulers.boundedElastic())
                .cast(UserDetails.class); // Ensure it's cast to UserDetails for the contract
    }
}