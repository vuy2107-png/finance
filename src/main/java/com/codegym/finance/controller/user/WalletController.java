package com.codegym.finance.controller.user;
import com.codegym.finance.entity.category.Category;
import com.codegym.finance.entity.budget.Budget;

import com.codegym.finance.entity.user.User;
import com.codegym.finance.entity.wallet.Wallet;
import com.codegym.finance.entity.transaction.Transaction;
import com.codegym.finance.service.budget.IBudgetService;
import com.codegym.finance.service.category.ICategoryService;
import com.codegym.finance.service.user.IUserService;
import com.codegym.finance.service.wallet.IWalletService;
import com.codegym.finance.service.transaction.ITransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import org.springframework.format.annotation.DateTimeFormat;
import com.codegym.finance.entity.transaction.TransactionType;

@Controller
@RequestMapping("/user/wallets")
public class WalletController {

    @Autowired
    private IWalletService walletService;

    @Autowired
    private IUserService userService;

    @Autowired
    private ITransactionService transactionService;

    @Autowired
    private IBudgetService budgetService;

    @Autowired
    private ICategoryService categoryService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 6, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth, Model model) {
        
        Page<Wallet> walletPage = walletService.searchWallets(auth.getName(), keyword, pageable);
        
        model.addAttribute("wallets", walletPage.getContent());
        model.addAttribute("allWallets", walletService.findByUsername(auth.getName())); // For modal selector
        model.addAttribute("page", walletPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("newWallet", new Wallet());
        
        // For budget modal
        model.addAttribute("categories", categoryService.findByUserName(auth.getName()));
        LocalDate now = LocalDate.now();
        model.addAttribute("currentMonth", now.getMonthValue());
        model.addAttribute("currentYear", now.getYear());
        
        return "user/wallet/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                         @RequestParam(required = false) String keyword,
                         @PageableDefault(size = 10, sort = {"date", "id"}, direction = Sort.Direction.DESC) Pageable pageable,
                         Authentication auth, Model model) {
        String username = auth.getName();
        Wallet wallet = walletService.findById(id, username);
        if (wallet == null) return "redirect:/user/wallets";
        
        model.addAttribute("wallet", wallet);
        
        // Lấy hạn mức thực tế cho ngày đang xem (mặc định là ngày hiện tại/giả lập)
        LocalDate effectiveDate = userService.getEffectiveDate(username);
        model.addAttribute("effectiveLimit", budgetService.getDailyLimitForWallet(username, id, effectiveDate));
        
        // Sử dụng bộ lọc giao dịch nhưng fix cứng walletId
        Page<Transaction> transactionPage = transactionService.filterTransactions(username, startDate, endDate, id, null, keyword, pageable);
        
        // Nhóm giao dịch theo ngày
        Map<LocalDate, List<Transaction>> groupedTransactions = transactionPage.getContent().stream()
                .collect(Collectors.groupingBy(
                        Transaction::getDate,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        model.addAttribute("groupedTransactions", groupedTransactions);
        model.addAttribute("page", transactionPage);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("keyword", keyword);
        
        LocalDate now = LocalDate.now();
        model.addAttribute("currentMonth", now.getMonthValue());
        model.addAttribute("currentYear", now.getYear());
        model.addAttribute("allWallets", walletService.findByUsername(username));
        model.addAttribute("categories", categoryService.findByUserName(username));
        
        return "user/wallet/detail";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute Wallet wallet, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            walletService.createWallet(wallet, auth.getName());
            redirectAttributes.addFlashAttribute("message", "Đã tạo ví mới thành công!");
            return "redirect:/user/dashboard";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            if (e.getMessage().contains("Premium")) {
                return "redirect:/user/premium";
            }
            return "redirect:/user/wallets";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            walletService.delete(id, auth.getName());
            redirectAttributes.addFlashAttribute("message", "Đã xóa ví thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa ví: " + e.getMessage());
        }
        return "redirect:/user/wallets";
    }

    @PostMapping("/batch-fund")
    public String batchFund(@RequestParam java.util.Map<String, String> allParams, Authentication auth, RedirectAttributes redirectAttributes) {
        String username = auth.getName();
        java.util.Map<Long, java.math.BigDecimal> amounts = new java.util.HashMap<>();
        java.util.Map<Long, String> descriptions = new java.util.HashMap<>();
        
        for (String key : allParams.keySet()) {
            if (key.startsWith("amounts[")) {
                String walletIdStr = key.substring(8, key.length() - 1);
                try {
                    Long walletId = Long.parseLong(walletIdStr);
                    String amountStr = allParams.get(key);
                    if (amountStr != null && !amountStr.trim().isEmpty()) {
                        java.math.BigDecimal amount = new java.math.BigDecimal(amountStr.trim());
                        amounts.put(walletId, amount);
                        descriptions.put(walletId, allParams.get("descriptions[" + walletId + "]"));
                    }
                } catch (NumberFormatException e) {
                    // Skip if invalid number format
                }
            }
        }
        
        try {
            int count = transactionService.batchFund(amounts, descriptions, username);
            if (count > 0) {
                redirectAttributes.addFlashAttribute("message", "Đã cấp vốn thành công cho " + count + " ví!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/user/dashboard";
    }

    @PostMapping("/fund")
    public String fund(@RequestParam Long walletId, 
                       @RequestParam java.math.BigDecimal amount, 
                       @RequestParam String description, 
                       Authentication auth, 
                       RedirectAttributes redirectAttributes) {
        try {
            transactionService.fund(walletId, amount, description, auth.getName());
            Wallet wallet = walletService.findById(walletId, auth.getName());
            String walletName = wallet != null ? wallet.getName() : "";
            redirectAttributes.addFlashAttribute("message", "Đã cấp vốn thành công cho ví " + walletName + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/user/wallets/" + walletId;
    }

    @PostMapping("/save-daily-limit")
    @ResponseBody
    public ResponseEntity<Void> saveDailyLimit(@RequestParam Long walletId, 
                                              @RequestParam java.math.BigDecimal dailyLimit, 
                                              Authentication auth) {
        budgetService.saveDailyLimit(walletId, dailyLimit, auth.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/budgets")
    @ResponseBody
    public ResponseEntity<Map<Long, java.math.BigDecimal>> getBudgets(@PathVariable Long id, 
                                                      @RequestParam int month, 
                                                      @RequestParam int year, 
                                                      Authentication auth) {
        Map<Long, java.math.BigDecimal> budgets = budgetService.getBudgetMapByMonth(auth.getName(), month, year, id);
        return ResponseEntity.ok(budgets);
    }

    @GetMapping("/{id}/effective-limit")
    @ResponseBody
    public java.math.BigDecimal getEffectiveLimit(@PathVariable Long id, @RequestParam String date, Authentication auth) {
        LocalDate targetDate = LocalDate.parse(date);
        return budgetService.getDailyLimitForWallet(auth.getName(), id, targetDate);
    }
}
