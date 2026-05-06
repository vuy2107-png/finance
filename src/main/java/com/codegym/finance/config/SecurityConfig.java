package com.codegym.finance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Cấu hình phân quyền + login
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Tắt CSRF để dễ dàng test bằng Postman
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/register", "/login", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()// Cho phép truy cập vào tĩnh và login/register
                        .requestMatchers("/user/**").hasRole("USER") // Chỉ cho phép người dùng có vai trò USER truy cập vào các URL bắt đầu bằng /user/
                        .requestMatchers("/admin/**").hasRole("ADMIN") // Chỉ cho phép người dùng có vai trò ADMIN truy cập vào các URL bắt đầu bằng /admin/
                        .anyRequest().authenticated() // Yêu cầu xác thực cho tất cả các yêu cầu khác
                )
                .formLogin(form -> form
                        .loginPage("/login") // Chỉ định trang login tùy chỉnh
                        .defaultSuccessUrl("/user/dashboard", true) // Chuyển hướng sau khi đăng nhập thành công
                        .permitAll() // Cho phép tất cả mọi người truy cập vào
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login") // URL để thực hiện logout
                );

        return http.build();
    }

    // Cấu hình mã hóa mật khẩu

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
