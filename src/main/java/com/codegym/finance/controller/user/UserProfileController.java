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
        User currentUser = userService.findByUsername(auth.getName());
        
        // Update only profile fields
        currentUser.setFullName(userUpdates.getFullName());
        currentUser.setEmail(userUpdates.getEmail());
        currentUser.setPhoneNumber(userUpdates.getPhoneNumber());
        currentUser.setAddress(userUpdates.getAddress());
        currentUser.setGender(userUpdates.getGender());
        currentUser.setDateOfBirth(userUpdates.getDateOfBirth());
        currentUser.setTestDate(userUpdates.getTestDate());
        // Avatar would normally be handled as a file upload, but for now we'll just keep it
        
        userService.save(currentUser);
        
        redirectAttributes.addFlashAttribute("message", "Cập nhật hồ sơ thành công!");
        return "redirect:/user/profile";
    }
}
