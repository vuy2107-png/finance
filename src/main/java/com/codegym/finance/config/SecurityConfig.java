package com.codegym.finance.config;
import com.codegym.finance.entity.user.User;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private CustomAuthenticationSuccessHandler successHandler;

    // C?u hnh phn quy?n + login
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // T?t CSRF d? d? dng test b?ng Postman
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/register", "/login", "/css/**", "/js/**", "/images/**", "/webjars/**", "/test-savings").permitAll()// Cho php truy c?p vo tinh v login/register
                        .requestMatchers("/user/**").hasRole("USER") // Ch? cho php ngu?i dng c vai tr USER truy c?p vo cc URL b?t d?u b?ng /user/
                        .requestMatchers("/admin/**").hasRole("ADMIN") // Ch? cho php ngu?i dng c vai tr ADMIN truy c?p vo cc URL b?t d?u b?ng /admin/
                        .anyRequest().authenticated() // Yu c?u xc th?c cho t?t c? cc yu c?u khc
                )
                .formLogin(form -> form
                        .loginPage("/login") // Ch? d?nh trang login ty ch?nh
                        .successHandler(successHandler)
                        .permitAll() // Cho php t?t c? m?i ngu?i truy c?p vo
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login") // URL d? th?c hi?n logout
                );

        return http.build();
    }

    // C?u hnh m ha m?t kh?u

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

