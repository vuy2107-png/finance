package com.codegym.finance.controller.auth;

import com.codegym.finance.entity.user.User;
import com.codegym.finance.entity.user.Role;
import com.codegym.finance.service.user.IUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private IUserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/register")
    public String showForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") User user,
                           BindingResult result,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        // Chuẩn hóa dữ liệu đầu vào
        if (user.getUsername() != null) {
            user.setUsername(user.getUsername().trim().toLowerCase());
        }
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().trim().toLowerCase());
        }

        // 0. Kiểm tra độ dài tối thiểu của username sau khi trim
        if (user.getUsername() != null && user.getUsername().length() < 6) {
            result.rejectValue("username", "error.user", "Tên đăng nhập không được ít hơn 6 ký tự");
        }

        // 1. Check trùng username
        if (userService.existsByUsername(user.getUsername())) {
            result.rejectValue("username", "error.user", "Tên đăng nhập này đã được sử dụng");
        }

        // 2. Check trùng email
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            if (userService.existsByEmail(user.getEmail())) {
                result.rejectValue("email", "error.user", "Email này đã được đăng ký tài khoản");
            }
        }

        // 3. Check password confirm
        if (user.getPassword() != null && user.getPassword().length() < 6) {
            result.rejectValue("password", "error.user", "Mật khẩu không được ít hơn 6 ký tự");
        }
        if (user.getPassword() != null && !user.getPassword().equals(user.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.user", "Mật khẩu xác nhận không khớp");
        }

        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setRole(Role.USER);
            user.setActive(true);
            user.setPremium(false); // Mặc định là tài khoản thường
            user.setExpiryDate(null);
            user.setHasSeenTour(false);

            userService.save(user);

            redirectAttributes.addFlashAttribute("message", "Đăng ký thành công! Hãy đăng nhập để bắt đầu.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra trong quá trình đăng ký. Vui lòng thử lại.");
            return "auth/register";
        }
    }

    @GetMapping("/upgrade")
    public String showUpgradePage() {
        return "auth/upgrade";
    }
}
