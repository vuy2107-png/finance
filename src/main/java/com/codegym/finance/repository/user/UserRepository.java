package com.codegym.finance.repository.user;

import com.codegym.finance.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    long countByPremium(Boolean premium);
    long countByActive(Boolean active);
    long countByCreatedAtAfter(java.time.LocalDateTime date);
    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
    long countByPremiumPlan(String plan);
    List<User> findTop5ByOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.Query("SELECT SUM(u.balance) FROM User u")
    Double sumAllBalances();
}
