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
        User user = userService.findByUsername(auth.getName());
        java.math.BigDecimal currentBalance = (user.getBalance() != null) ? user.getBalance() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal price = java.math.BigDecimal.ZERO;
        String planName = "";
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = (user.getExpiryDate() != null && user.getExpiryDate().isAfter(now))
                ? user.getExpiryDate() : now;

        switch (plan) {
            case "weekly":
                price = java.math.BigDecimal.valueOf(29000);
                planName = "Gói Starter (Tuần)";
                expiryDate = expiryDate.plusDays(7);
                break;
            case "monthly":
                price = java.math.BigDecimal.valueOf(99000);
                planName = "Gói Pro (Tháng)";
                expiryDate = expiryDate.plusDays(30);
                break;
            case "yearly":
                price = java.math.BigDecimal.valueOf(799000);
                planName = "Gói Elite (Năm)";
                expiryDate = expiryDate.plusYears(1);
                break;
            default:
                ra.addFlashAttribute("error", "Gói nâng cấp không hợp lệ!");
                return "redirect:/user/premium";
        }

        if (currentBalance.compareTo(price) < 0) {
            ra.addFlashAttribute("error", "Số dư không đủ để mua " + planName + ". Vui lòng nạp thêm tiền!");
            return "redirect:/user/deposit";
        }

        // Trừ tiền và kích hoạt Premium
        user.setBalance(currentBalance.subtract(price));
        user.setPremium(true);
        user.setPremiumPlan(plan);
        user.setExpiryDate(expiryDate);
        userService.save(user);

        // Tạo giao dịch ghi lại doanh thu cho hệ thống
        com.codegym.finance.entity.transaction.Transaction t = new com.codegym.finance.entity.transaction.Transaction();
        t.setUser(user);
        t.setAmount(price);
        t.setType(com.codegym.finance.entity.transaction.TransactionType.EXPENSE);
        t.setDate(LocalDate.now());
        t.setDescription("Nâng cấp Premium: " + planName);
        
        transactionService.save(t, auth.getName());

        ra.addFlashAttribute("message", "Chúc mừng! Bạn đã trở thành thành viên PREMIUM với " + planName + ". Hiệu lực đến: " + 
                expiryDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        return "redirect:/user/dashboard";
    }
}
