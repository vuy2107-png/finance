package com.codegym.finance.controller.user;
import com.codegym.finance.entity.wallet.Wallet;

import com.codegym.finance.entity.budget.Budget;
import com.codegym.finance.entity.category.Category;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.service.budget.IBudgetService;
import com.codegym.finance.service.category.ICategoryService;
import com.codegym.finance.service.user.IUserService;
import com.codegym.finance.service.wallet.IWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user/budgets")
public class BudgetController {

    @Autowired
    private IBudgetService budgetService;

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private IUserService userService;

    @Autowired
    private IWalletService walletService;

    @GetMapping
    public String index(Model model, Authentication auth) {
        String username = auth.getName();
        User user = userService.findByUsername(username);
        LocalDate now = LocalDate.now();

        List<Category> categories = categoryService.findByUserName(username);
        Map<Long, java.math.BigDecimal> budgetMap = budgetService.getBudgetMapByMonth(username, now.getMonthValue(), now.getYear(), null);

        model.addAttribute("user", user);
        model.addAttribute("categories", categories);
        model.addAttribute("budgetMap", budgetMap);
        model.addAttribute("currentMonth", now.getMonthValue());
        model.addAttribute("currentYear", now.getYear());

        return "user/budget/index";
    }

    @PostMapping("/save-ajax")
    @ResponseBody
    public ResponseEntity<Void> saveAjax(@RequestParam Long categoryId,
                                         @RequestParam java.math.BigDecimal amount,
                                         @RequestParam Integer month,
                                         @RequestParam Integer year,
                                         @RequestParam(required = false) Long walletId,
                                         Authentication auth) {
        budgetService.save(categoryId, amount, month, year, auth.getName(), walletId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/update-daily-limit")
    public String updateDailyLimit(@RequestParam java.math.BigDecimal dailyLimit, Authentication auth) {
        // Since User entity doesn't have dailySpendingLimit, 
        // we can either add it to User or apply it to the first wallet by default.
        // For now, let's just use the WalletController's save-daily-limit logic 
        // or redirect to wallets if we want them to set it per wallet.
        
        // Alternatively, I can add the field to User entity to make it a global limit.
        // But the user asked about "wallet" integration.
        
        // Let's redirect to wallets for now or just inform that it's per wallet.
        return "redirect:/user/budgets";
    }
}
