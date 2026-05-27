package com.codegym.finance.controller.user;
import com.codegym.finance.entity.icon.UserIcon;

import com.codegym.finance.entity.icon.Icon;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.service.icon.IIconService;
import com.codegym.finance.service.user.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/user/shop")
public class UserShopController {

    @Autowired
    private IIconService iconService;

    @Autowired
    private IUserService userService;

    @GetMapping
    public String list(Authentication auth, Model model) {
        User user = userService.findByUsername(auth.getName());
        List<Icon> icons = iconService.findAllActive();
        
        model.addAttribute("user", user);
        model.addAttribute("icons", icons);
        return "user/shop/list";
    }

    @PostMapping("/buy/{id}")
    public String buy(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            iconService.buyIcon(id, auth.getName());
            Icon icon = iconService.findById(id);
            String iconName = icon != null ? icon.getName() : "";
            redirectAttributes.addFlashAttribute("message", "Chúc mừng! Bạn đã mua thành công Icon '" + iconName + "'.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            if (e.getMessage().contains("Số dư của bạn không đủ")) {
                return "redirect:/user/deposit";
            }
        }
        return "redirect:/user/shop";
    }
}



