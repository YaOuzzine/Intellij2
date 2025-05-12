// demo 2/src/main/java/com/example/demo/Service/ReactiveAdminUserDetailsService.java
package com.example.demo.Service;

import com.example.demo.Entity.AdminUser;
import com.example.demo.Repository.AdminUserRepository;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ReactiveAdminUserDetailsService implements ReactiveUserDetailsService {

    private final AdminUserRepository adminUserRepository;

    public ReactiveAdminUserDetailsService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        // JPA is blocking, so operations need to be offloaded to a bounded elastic scheduler
        return Mono.fromCallable(() -> adminUserRepository.findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username)))
                .subscribeOn(Schedulers.boundedElastic())
                .cast(UserDetails.class);
    }
}