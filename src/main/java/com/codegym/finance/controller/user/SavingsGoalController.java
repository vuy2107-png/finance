package com.codegym.finance.controller.user;
import com.codegym.finance.entity.wallet.Wallet;
import com.codegym.finance.entity.user.User;

import com.codegym.finance.entity.savings.SavingsGoal;
import com.codegym.finance.service.savings.ISavingsGoalService;
import com.codegym.finance.service.wallet.IWalletService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user/savings")
public class SavingsGoalController {

    @Autowired
    private ISavingsGoalService savingsGoalService;

    @Autowired
    private IWalletService walletService;

    @GetMapping
    public String listGoals(Model model, Authentication auth) {
        String username = auth.getName();
        model.addAttribute("goals", savingsGoalService.findByUserName(username));
        model.addAttribute("wallets", walletService.findByUsername(username));
        model.addAttribute("newGoal", new SavingsGoal());
        return "user/savings/list";
    }

    @PostMapping("/create")
    public String createGoal(@Valid @ModelAttribute("newGoal") SavingsGoal goal, 
                             BindingResult result, 
                             Authentication auth, 
                             RedirectAttributes redirectAttributes,
                             Model model) {
        String username = auth.getName();
        
        if (result.hasErrors()) {
            model.addAttribute("goals", savingsGoalService.findByUserName(username));
            model.addAttribute("wallets", walletService.findByUsername(username));
            return "user/savings/list";
        }
        
        try {
            savingsGoalService.save(goal, username);
            redirectAttributes.addFlashAttribute("message", "Tạo mục tiêu tiết kiệm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/user/savings";
    }

    @PostMapping("/add-funds")
    public String addFunds(@RequestParam("goalId") Long goalId,
                           @RequestParam("amount") java.math.BigDecimal amount,
                           @RequestParam("walletId") Long walletId,
                           Authentication auth,
                           RedirectAttributes redirectAttributes) {
        String username = auth.getName();
        try {
            savingsGoalService.addFunds(goalId, amount, walletId, username);
            redirectAttributes.addFlashAttribute("message", "Nạp tiền vào quỹ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi nạp tiền: " + e.getMessage());
        }
        return "redirect:/user/savings";
    }

    @PostMapping("/delete/{id}")
    public String deleteGoal(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        String username = auth.getName();
        try {
            savingsGoalService.delete(id, username);
            redirectAttributes.addFlashAttribute("message", "Đã xóa mục tiêu.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa mục tiêu: " + e.getMessage());
        }
        return "redirect:/user/savings";
    }
}
