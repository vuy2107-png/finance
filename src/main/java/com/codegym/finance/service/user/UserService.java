package com.codegym.finance.service.user;
import com.codegym.finance.entity.transaction.Transaction;

import com.codegym.finance.entity.user.User;
import com.codegym.finance.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService implements IUserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void save(User user) {
        userRepository.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User findById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @Override
    @Transactional
    public void toggleStatus(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            // Handle null safely
            boolean currentStatus = user.getActive() != null && user.getActive();
            user.setActive(!currentStatus);
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public void togglePremium(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            // Handle null safely
            boolean currentPremium = user.getPremium() != null && user.getPremium();
            user.setPremium(!currentPremium);
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public void activateTrial(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            user.setPremium(true);
            user.setPremiumPlan("trial");
            user.setExpiryDate(java.time.LocalDateTime.now().plusDays(7));
            user.setHasSeenTour(true);
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public void markTourAsSeen(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            user.setHasSeenTour(true);
            userRepository.save(user);
        }
    }

    @Override
    public java.time.LocalDate getEffectiveDate(String username) {
        User user = findByUsername(username);
        if (user != null && user.getTestDate() != null) {
            return user.getTestDate();
        }
        return java.time.LocalDate.now();
    }

    @Override
    @Transactional
    public void updateTestDate(String username, java.time.LocalDate testDate) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            user.setTestDate(testDate);
            userRepository.save(user);
        }
    }

    @Override
    public List<User> findRecentUsers() {
        return userRepository.findTop5ByOrderByCreatedAtDesc();
    }
}
