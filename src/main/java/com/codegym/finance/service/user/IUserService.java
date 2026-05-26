package com.codegym.finance.service.user;

import com.codegym.finance.entity.user.User;
import java.util.List;

public interface IUserService {
    void save(User user);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    User findByUsername(String username);
    List<User> findAll();
    void toggleStatus(Long userId);
    void togglePremium(Long userId);
    void activateTrial(String username);
    void markTourAsSeen(String username);
    User findById(Long userId);
    java.time.LocalDate getEffectiveDate(String username);
    void updateTestDate(String username, java.time.LocalDate testDate);
    List<User> findRecentUsers();
}
