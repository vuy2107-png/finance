package com.codegym.finance.controller.user;

import com.codegym.finance.entity.user.User;
import com.codegym.finance.service.transaction.ITransactionService;
import com.codegym.finance.service.user.IUserService;
import com.codegym.finance.service.wallet.IWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/user/premium")
public class PremiumController {

    @Autowired
    private IUserService userService;

    @Autowired
    private ITransactionService transactionService;

    @Autowired
    private IWalletService walletService;

    @GetMapping
    public String showPricingPage(Authentication auth, Model model) {
        User user = userService.findByUsername(auth.getName());
        model.addAttribute("user", user);
        return "user/premium/pricing";
    }

    @PostMapping("/buy")
    public String buyPremium(@RequestParam String plan, Authentication auth, RedirectAttributes ra) {
        try {
            userService.buyPremium(auth.getName(), plan);
            User user = userService.findByUsername(auth.getName());
            String planName = "";
            switch (plan) {
                case "weekly": planName = "Gói Starter (Tuần)"; break;
                case "monthly": planName = "Gói Pro (Tháng)"; break;
                case "yearly": planName = "Gói Elite (Năm)"; break;
            }
            String formattedExpiry = user.getExpiryDate() != null 
                ? user.getExpiryDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
                : "";
            ra.addFlashAttribute("message", "Chúc mừng! Bạn đã trở thành thành viên PREMIUM với " + planName + ". Hiệu lực đến: " + formattedExpiry);
            return "redirect:/user/dashboard";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
            if (e.getMessage().contains("Số dư không đủ")) {
                return "redirect:/user/deposit";
            }
            return "redirect:/user/premium";
        }
    }
}
