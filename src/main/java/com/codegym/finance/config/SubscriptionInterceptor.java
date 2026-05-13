package com.codegym.finance.config;

import com.codegym.finance.entity.user.User;
import com.codegym.finance.service.user.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Component
public class SubscriptionInterceptor implements HandlerInterceptor {

    @Autowired
    private IUserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String uri = request.getRequestURI();
            
            // Bỏ qua các trang không cần check hoặc trang xử lý hết hạn
            if (uri.startsWith("/upgrade") || uri.startsWith("/logout") || uri.startsWith("/css") || 
                uri.startsWith("/js") || uri.startsWith("/images") || uri.startsWith("/api/user/activate-trial") ||
                uri.startsWith("/user/premium") || uri.startsWith("/user/deposit") ||
                uri.startsWith("/admin")) { // Admin không bị check trial theo cách này hoặc có logic riêng
                return true;
            }

            User user = userService.findByUsername(auth.getName());
            if (user != null && user.getExpiryDate() != null) {
                if (LocalDateTime.now().isAfter(user.getExpiryDate())) {
                    // Nếu đã hết hạn, chuyển hướng về trang thông báo hết hạn
                    response.sendRedirect("/upgrade");
                    return false;
                }
            }
        }
        
        return true;
    }
}
