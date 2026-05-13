package com.codegym.finance.config;

import com.codegym.finance.entity.user.Role;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Tạo tài khoản Admin mặc định nếu chưa tồn tại
        String adminUsername = "highmode";
        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode("vuduy21"))
                    .fullName("System Administrator")
                    .email("admin@financepro.com")
                    .role(Role.ADMIN)
                    .active(true)
                    .premium(true)
                    .expiryDate(java.time.LocalDateTime.now().plusYears(100))
                    .hasSeenTour(true)
                    .build();
            
            userRepository.save(admin);
            System.out.println(">>> Đã tạo tài khoản Admin mặc định: " + adminUsername);
        }
    }
}
