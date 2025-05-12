// demo 2/src/main/java/com/example/demo/Repository/AdminUserRepository.java
package com.example.demo.Repository;

import com.example.demo.Entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono; // If you need reactive repository methods

import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUsername(String username);

    // Example for reactive approach if needed elsewhere, though UserDetailsService is blocking
    // Mono<AdminUser> findByUsernameReactive(String username);
}