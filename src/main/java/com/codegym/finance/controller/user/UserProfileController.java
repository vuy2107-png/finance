package com.codegym.finance.controller.user;

import com.codegym.finance.entity.user.User;
import com.codegym.finance.service.user.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user/profile")
public class UserProfileController {

    @Autowired
    private IUserService userService;

    @GetMapping
    public String showProfile(Authentication auth, Model model) {
        User user = userService.findByUsername(auth.getName());
        model.addAttribute("user", user);
        return "user/profile";
    }

    @PostMapping("/update")
    public String updateProfile(@ModelAttribute("user") User userUpdates,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        try {
            userService.updateProfile(auth.getName(), userUpdates);
            redirectAttributes.addFlashAttribute("message", "Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/user/profile";
    }

    @PostMapping("/api/toggle-suggest-limit")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> toggleSuggestLimit(Authentication auth, @RequestParam boolean enabled) {
        User currentUser = userService.findByUsername(auth.getName());
        currentUser.setAutoSuggestDailyLimit(enabled);
        userService.save(currentUser);
        return org.springframework.http.ResponseEntity.ok().build();
    }
}
