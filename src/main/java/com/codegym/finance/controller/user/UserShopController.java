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
        User user = userService.findByUsername(auth.getName());
        Icon icon = iconService.findById(id);

        if (icon == null || !icon.getActive()) {
            redirectAttributes.addFlashAttribute("error", "Icon này hiện không còn bán.");
            return "redirect:/user/shop";
        }

        // Kiểm tra xem đã sở hữu chưa
        if (iconService.existsByUserUsernameAndIconId(auth.getName(), id)) {
            redirectAttributes.addFlashAttribute("error", "Bạn đã sở hữu icon này rồi!");
            return "redirect:/user/shop";
        }

        java.math.BigDecimal currentBalance = (user.getBalance() != null) ? user.getBalance() : java.math.BigDecimal.ZERO;
        if (currentBalance.compareTo(icon.getPrice()) < 0) {
            redirectAttributes.addFlashAttribute("error", "Số dư của bạn không đủ. Vui lòng nạp thêm!");
            return "redirect:/user/deposit";
        }

        // Thực hiện trừ tiền
        user.setBalance(currentBalance.subtract(icon.getPrice()));
        userService.save(user);

        // Lưu vào kho đồ (UserIcon)
        com.codegym.finance.entity.icon.UserIcon userIcon = new com.codegym.finance.entity.icon.UserIcon();
        userIcon.setUser(user);
        userIcon.setIcon(icon);
        iconService.saveUserIcon(userIcon);
        
        redirectAttributes.addFlashAttribute("message", "Chúc mừng! Bạn đã mua thành công Icon '" + icon.getName() + "'.");
        return "redirect:/user/shop";
    }
}



