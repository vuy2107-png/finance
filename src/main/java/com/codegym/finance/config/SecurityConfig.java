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

    @Autowired
    private CustomLogoutHandler logoutHandler;

    // Cấu hình phân quyền + login
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) 
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/register", "/login", "/css/**", "/js/**", "/images/**", "/webjars/**", "/test-savings").permitAll()
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN") 
                        .requestMatchers("/admin/**").hasRole("ADMIN") 
                        .anyRequest().authenticated() 
                )
                .formLogin(form -> form
                        .loginPage("/login") 
                        .successHandler(successHandler)
                        .permitAll() 
                )
                .logout(logout -> logout
                        .addLogoutHandler(logoutHandler)
                        .logoutSuccessUrl("/login") 
                )
                .sessionManagement(session -> session
                        .maximumSessions(-1) // Không giới hạn số thiết bị đăng nhập đồng thời
                        .sessionRegistry(sessionRegistry())
                );

        return http.build();
    }

    @Bean
    public org.springframework.security.core.session.SessionRegistry sessionRegistry() {
        return new org.springframework.security.core.session.SessionRegistryImpl();
    }

    @Bean
    public org.springframework.security.web.session.HttpSessionEventPublisher httpSessionEventPublisher() {
        return new org.springframework.security.web.session.HttpSessionEventPublisher();
    }

    // C?u hnh m ha m?t kh?u

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

