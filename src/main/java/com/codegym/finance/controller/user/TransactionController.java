package com.codegym.finance.controller.user;
import com.codegym.finance.entity.category.Category;

import com.codegym.finance.entity.budget.Budget;
import com.codegym.finance.entity.transaction.Transaction;
import com.codegym.finance.entity.transaction.TransactionType;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.entity.wallet.Wallet;
import com.codegym.finance.service.budget.BudgetAlertDTO;
import com.codegym.finance.service.budget.IBudgetService;
import com.codegym.finance.service.category.ICategoryService;
import com.codegym.finance.service.user.IUserService;
import com.codegym.finance.service.transaction.ITransactionService;
import com.codegym.finance.service.wallet.IWalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.codegym.finance.exception.SpendingLimitException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
public class TransactionController {

    @Autowired
    private ITransactionService transactionService;

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private IBudgetService budgetService;

    @Autowired
    private IWalletService walletService;

    @Autowired
    private IUserService userService;

    @Autowired
    private com.codegym.finance.repository.transaction.TransactionRepository transactionRepository;

    // DASHBOARD
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth, 
                           @RequestParam(required = false) String testReport,
                           @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        String username = auth.getName();
        try {
            User currentUser = userService.findByUsername(username);
            model.addAttribute("user", currentUser);
            model.addAttribute("showWelcomeTour", currentUser.getHasSeenTour() != null && !currentUser.getHasSeenTour());

            // Morning Report Logic: Hiện báo cáo nếu chưa xem trong ngày hôm nay VÀ tài khoản đã được tạo trước ngày hôm nay
            LocalDate now = userService.getEffectiveDate(username);
            boolean isFirstLoginToday = currentUser.getLastReportDate() == null || currentUser.getLastReportDate().isBefore(now);
            boolean hasHistoryBeforeToday = currentUser.getCreatedAt() != null && currentUser.getCreatedAt().toLocalDate().isBefore(now);
            
            boolean forceReport = "true".equals(testReport) || date != null;
            if (forceReport || (isFirstLoginToday && hasHistoryBeforeToday)) {
                LocalDate reportDate = (date != null) ? date : now.minusDays(1);
                Map<String, Object> yesterdayReport = new HashMap<>();
                yesterdayReport.put("date", reportDate);
                
                List<Map<String, Object>> overspentCategories = new ArrayList<>();
                List<Map<String, Object>> goodCategories = new ArrayList<>();
                
                // 1. Kiểm tra hạn mức ngày của Ví
                double totalDaySpent = 0;
                double totalDayLimit = 0;
                boolean isDailyOverspent = false;
                List<Wallet> userWallets = walletService.findByUsername(username);
                for (Wallet w : userWallets) {
                    Double spentInWallet = transactionRepository.sumByUserAndWalletAndTypeAndDate(currentUser, w, TransactionType.EXPENSE, reportDate);
                    if (spentInWallet == null) spentInWallet = 0.0;
                    totalDaySpent += spentInWallet;
                    Double histLimit = budgetService.getDailyLimitForWallet(username, w.getId(), reportDate);
                    if (histLimit != null && histLimit > 0) {
                        totalDayLimit += histLimit;
                        if (spentInWallet > histLimit) {
                            isDailyOverspent = true;
                        }
                    }
                }
                yesterdayReport.put("totalSpent", totalDaySpent);
                yesterdayReport.put("isDailyOverspent", isDailyOverspent);

                // 2. Kiểm tra ngân sách danh mục
                List<Budget> yesterdayBudgets = budgetService.getBudgetsByMonth(username, reportDate.getMonthValue(), reportDate.getYear(), null);
                for (Budget b : yesterdayBudgets) {
                    if (b.getCategory() == null || b.getCategory().getType() != TransactionType.EXPENSE) continue;
                    
                    Double spentOnDate;
                    if (b.getWallet() != null) {
                        spentOnDate = transactionRepository.sumByUserAndCategoryAndWalletAndDate(currentUser, b.getCategory(), b.getWallet(), reportDate);
                    } else {
                        spentOnDate = transactionRepository.sumByUserAndCategoryAndDate(currentUser, b.getCategory(), reportDate);
                    }
                    if (spentOnDate == null) spentOnDate = 0.0;
                    
                    if (spentOnDate > b.getAmount()) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("name", b.getCategory().getName());
                        item.put("spent", spentOnDate);
                        item.put("limit", b.getAmount());
                        item.put("wallet", b.getWallet() != null ? b.getWallet().getName() : "Chung");
                        overspentCategories.add(item);
                    } else if (spentOnDate > 0) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("name", b.getCategory().getName());
                        item.put("spent", spentOnDate);
                        goodCategories.add(item);
                    }
                }
                
                yesterdayReport.put("overspent", overspentCategories);
                yesterdayReport.put("good", goodCategories);
                Double totalSpentOnDate = transactionRepository.sumByUserAndTypeAndDate(currentUser, TransactionType.EXPENSE, reportDate);
                yesterdayReport.put("totalSpent", totalSpentOnDate != null ? totalSpentOnDate : 0.0);
                
                model.addAttribute("morningReport", yesterdayReport);
                model.addAttribute("showMorningReport", true);
                
                if (!forceReport) {
                    currentUser.setLastReportDate(now);
                    userService.save(currentUser);
                }
            }
            
            // Core Metrics
            double monthlyIncome = transactionService.getThisMonthIncome(username);
            double monthlyExpense = transactionService.getThisMonthExpense(username);
            
            model.addAttribute("totalBalance", transactionService.getBalance(username)); // Tổng tài sản thực tế
            model.addAttribute("monthlyIncome", monthlyIncome);
            model.addAttribute("monthlyExpense", monthlyExpense);
            model.addAttribute("monthlyBalance", monthlyIncome - monthlyExpense); // Số dư tháng này
            
            // Daily Metrics (New)
            model.addAttribute("todayIncome", transactionService.getTodayIncome(username));
            model.addAttribute("todayExpense", transactionService.getTodayExpense(username));
            
            // Monthly Trend (Chart)
            model.addAttribute("monthlyStats", transactionService.getMonthlyTrend(username));
            
            // Weekly Trend (New Chart Data)
            model.addAttribute("weeklyStats", transactionService.getLast7DaysTrend(username));
            
            // Category Statistics (Doughnut)
            model.addAttribute("categoryStats", transactionService.getCategorySummary(username));
            
            // Recent Transactions
            List<Transaction> allTransactions = transactionService.findByUserName(username);
            model.addAttribute("recentTransactions", allTransactions.stream().limit(5).collect(Collectors.toList()));

            // Budgeting Data restructed by wallet
            List<Wallet> userWallets = walletService.findByUsername(username);
            List<Map<String, Object>> walletLimits = new ArrayList<>();
            
            for (Wallet w : userWallets) {
                Map<String, Object> walletData = new HashMap<>();
                walletData.put("walletName", w.getName());
                walletData.put("id", w.getId());
                
                // 1. Daily Limit for this wallet (Use historical/effective limit for the simulated date)
                Double effectiveLimit = budgetService.getDailyLimitForWallet(username, w.getId(), now);
                boolean isSuggested = false;
                
                // Cảnh báo chi tiêu vượt quá tiến trình thời gian của tháng
                int currentDay = now.getDayOfMonth();
                java.time.YearMonth yearMonthObject = java.time.YearMonth.of(now.getYear(), now.getMonthValue());
                int daysInMonth = yearMonthObject.lengthOfMonth();
                double spentThisMonth = transactionService.getThisMonthExpenseForWallet(username, w.getId());
                double initialBalance = w.getBalance() + spentThisMonth; // xấp xỉ số tiền đầu tháng
                if (initialBalance > 0) {
                    double spentPercent = (spentThisMonth / initialBalance) * 100;
                    double timePercent = ((double) currentDay / daysInMonth) * 100;
                    if (Double.isNaN(spentPercent) || Double.isInfinite(spentPercent)) {
                        spentPercent = 0.0;
                    }
                    if (Double.isNaN(timePercent) || Double.isInfinite(timePercent)) {
                        timePercent = 0.0;
                    }
                    if (spentPercent > timePercent) {
                        walletData.put("overspentWarning", true);
                        walletData.put("spentPercent", spentPercent);
                        walletData.put("timePercent", timePercent);
                    }
                }
                
                if (Boolean.TRUE.equals(currentUser.getAutoSuggestDailyLimit())) {
                    // Tính số ngày còn lại trong tháng
                    int remainingDays = daysInMonth - currentDay + 1;
                    
                    if (remainingDays > 0) {
                        double suggested = w.getBalance() / remainingDays;
                        effectiveLimit = suggested > 0 ? suggested : 0.0;
                        isSuggested = true;
                        walletData.put("isSuggested", true);
                    }
                }

                if (effectiveLimit != null && effectiveLimit > 0) {
                    double todayWalletExp = transactionService.getTodayExpenseForWallet(username, w.getId());
                    walletData.put("spent", todayWalletExp);
                    walletData.put("limit", effectiveLimit);
                    double percent = (todayWalletExp / effectiveLimit) * 100;
                    if (Double.isNaN(percent) || Double.isInfinite(percent)) {
                        percent = 0.0;
                    }
                    walletData.put("percent", percent);
                    walletData.put("status", percent > 100 ? "DANGER" : (percent > 80 ? "WARNING" : "SUCCESS"));
                }
                
                // 2. Category Budgets for this specific wallet
                List<Budget> wBudgets = budgetService.getBudgetsByMonth(username, now.getMonthValue(), now.getYear(), w.getId());
                List<Map<String, Object>> catBudgets = new ArrayList<>();
                for (Budget b : wBudgets) {
                    if (b.getCategory() == null || b.getCategory().getType() != TransactionType.EXPENSE) continue;
                    Map<String, Object> cb = new HashMap<>();
                    cb.put("categoryName", b.getCategory().getName());
                    BudgetAlertDTO alert = budgetService.checkDailyCategoryBudgetAlert(
                            username, b.getCategory().getId(), now.getMonthValue(), now.getYear(), w.getId());
                    cb.put("spentAmount", alert.getSpentAmount());
                    cb.put("limitAmount", b.getAmount());
                    cb.put("percent", alert.getPercentage());
                    catBudgets.add(cb);
                }
                walletData.put("categoryBudgets", catBudgets);
                
                if (walletData.get("limit") != null || !catBudgets.isEmpty()) {
                    walletLimits.add(walletData);
                }
            }
            model.addAttribute("dailyLimits", walletLimits);

            // 3. Global Budgets (No wallet assigned)
            List<Budget> globalBudgets = budgetService.getBudgetsByMonth(username, now.getMonthValue(), now.getYear(), null);
            List<Map<String, Object>> globalBudgetStatus = new ArrayList<>();
            for (Budget b : globalBudgets) {
                if (b.getWallet() != null) continue; // Already handled in wallet loop
                if (b.getCategory() == null || b.getCategory().getType() != TransactionType.EXPENSE) continue;
                
                Map<String, Object> status = new HashMap<>();
                status.put("categoryName", b.getCategory().getName());
                BudgetAlertDTO alert = budgetService.checkDailyCategoryBudgetAlert(
                        username, b.getCategory().getId(), now.getMonthValue(), now.getYear(), null);
                status.put("spentAmount", alert.getSpentAmount());
                status.put("limitAmount", b.getAmount());
                status.put("percent", alert.getPercentage());
                globalBudgetStatus.add(status);
            }
            model.addAttribute("globalBudgets", globalBudgetStatus);
            model.addAttribute("budgetMap", budgetService.getBudgetMapByMonth(username, now.getMonthValue(), now.getYear(), null));

            // Objects for Sidebar/Forms
            model.addAttribute("categories", categoryService.findByUserName(username));
            model.addAttribute("wallets", walletService.findByUsername(username));
            model.addAttribute("transaction", new Transaction());
        } catch (Exception e) {
            model.addAttribute("totalBalance", 0.0);
            model.addAttribute("monthlyIncome", 0.0);
            model.addAttribute("monthlyExpense", 0.0);
            model.addAttribute("monthlyStats", new HashMap<>());
            model.addAttribute("categoryStats", new HashMap<>());
            model.addAttribute("recentTransactions", new ArrayList<>());
            model.addAttribute("budgetStatus", new ArrayList<>());
            System.err.println("Dashboard error: " + e.getMessage());
        }
        return "user/dashboard";
    }

    // TRANSACTIONS LIST
    @GetMapping("/transactions")
    public String list(Model model, Authentication auth,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                       @RequestParam(required = false) Long walletId,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) String keyword,
                       @PageableDefault(size = 10, sort = {"date", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        String username = auth.getName();
        
        Page<Transaction> transactionPage = transactionService.filterTransactions(username, startDate, endDate, walletId, categoryId, keyword, pageable);
        
        // Group transactions by date for intuitive UI
        Map<LocalDate, List<Transaction>> groupedTransactions = transactionPage.getContent().stream()
                .collect(Collectors.groupingBy(
                    Transaction::getDate, 
                    LinkedHashMap::new,
                    Collectors.toList()
                ));
        
        model.addAttribute("groupedTransactions", groupedTransactions);
        model.addAttribute("page", transactionPage);
        model.addAttribute("categories", categoryService.findByUserName(username));
        model.addAttribute("wallets", walletService.findByUsername(username));
        
        // Preserve filter values in the UI
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("walletId", walletId);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("keyword", keyword);
        
        return "user/transaction/list";
    }

    @GetMapping("/transactions/create")
    public String showCreateForm(@RequestParam(required = false) Long walletId, Model model, Authentication auth) {
        String username = auth.getName();
        Transaction transaction = new Transaction();
        transaction.setDate(LocalDate.now());
        
        if (walletId != null) {
            try {
                transaction.setWallet(walletService.findById(walletId, username));
            } catch (Exception e) {
                // Ignore if wallet not found or doesn't belong to user
            }
        }
        
        model.addAttribute("transaction", transaction);
        model.addAttribute("categories", categoryService.findByUserName(username));
        model.addAttribute("wallets", walletService.findByUsername(username));
        return "user/transaction/create";
    }

    @GetMapping("/transactions/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, Authentication auth) {
        String username = auth.getName();
        model.addAttribute("transaction", transactionService.findById(id, username));
        model.addAttribute("categories", categoryService.findByUserName(username));
        model.addAttribute("wallets", walletService.findByUsername(username));
        return "user/transaction/edit";
    }

    // CREATE SAVE
    @PostMapping("/transactions/create")
    public Object create(@Valid @ModelAttribute Transaction transaction,
                         @RequestParam(required = false) String categoryName,
                         BindingResult result, Model model, Authentication auth,
                         RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {
        if (result.hasErrors()) {
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                return "redirect:/user/transactions/error-ajax";
            }
            model.addAttribute("categories", categoryService.findByUserName(auth.getName()));
            model.addAttribute("wallets", walletService.findByUsername(auth.getName()));
            return "user/transaction/create";
        }
        try {
            // Kiểm tra khóa tháng (không được thêm tháng trước hoặc sau)
            // Kiểm tra ngày tương lai (Sử dụng ngày giả lập nếu có)
            LocalDate today = userService.getEffectiveDate(auth.getName());
            if (transaction.getDate().isAfter(today)) {
                throw new Exception("Không thể thêm giao dịch cho ngày trong tương lai! (Hôm nay là " + 
                    today.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")");
            }

            if (categoryName != null && !categoryName.trim().isEmpty()) {
                transaction.setCategory(categoryService.getOrCreateCategory(categoryName, transaction.getType(), auth.getName()));
            }
            transactionService.save(transaction, auth.getName());
            
            if (transaction.getCategory() != null) {
                Long walletId = (transaction.getWallet() != null) ? transaction.getWallet().getId() : null;
                BudgetAlertDTO alert = budgetService.checkBudgetAlert(
                        auth.getName(), 
                        transaction.getCategory().getId(), 
                        transaction.getDate().getMonthValue(), 
                        transaction.getDate().getYear(),
                        walletId
                );
                
                // Also check daily limit
                BudgetAlertDTO dailyAlert = budgetService.checkDailyLimitAlert(auth.getName(), walletId);
                
                if (dailyAlert.isAlert()) {
                    redirectAttributes.addFlashAttribute("budgetAlert", dailyAlert);
                    redirectAttributes.addFlashAttribute("message", dailyAlert.getMessage());
                } else if (alert.isAlert()) {
                    redirectAttributes.addFlashAttribute("budgetAlert", alert);
                    redirectAttributes.addFlashAttribute("message", alert.getMessage());
                } else {
                    redirectAttributes.addFlashAttribute("message", "Tuyệt vời! Bạn vẫn đang quản lý ngân sách rất tốt. ✅");
                }
            } else {
                redirectAttributes.addFlashAttribute("message", "Đã thêm giao dịch thành công! ✅");
            }
        } catch (SpendingLimitException e) {
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.findByUserName(auth.getName()));
            model.addAttribute("wallets", walletService.findByUsername(auth.getName()));
            return "user/transaction/create";
        } catch (Exception e) {
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
            }
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.findByUserName(auth.getName()));
            model.addAttribute("wallets", walletService.findByUsername(auth.getName()));
            return "user/transaction/create";
        }
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/user/transactions");
    }

    @GetMapping("/transactions/error-ajax")
    @ResponseBody
    public org.springframework.http.ResponseEntity<String> errorAjax() {
        return org.springframework.http.ResponseEntity.badRequest().body("Validation failed");
    }

    // UPDATE
    @PostMapping("/transactions/edit")
    public String update(@Valid @ModelAttribute Transaction transaction,
                         @RequestParam(required = false) String categoryName,
                         BindingResult result, Model model, Authentication auth,
                         RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findByUserName(auth.getName()));
            model.addAttribute("wallets", walletService.findByUsername(auth.getName()));
            return "user/transaction/edit";
        }

        // Fetch existing transaction to check date and lock status
        Transaction existing = transactionService.findById(transaction.getId(), auth.getName());
        if (existing == null) {
            redirectAttributes.addFlashAttribute("error", "Giao dịch không tồn tại.");
            return "redirect:/user/transactions";
        }

        // Kiểm tra ngày tương lai
        LocalDate today = userService.getEffectiveDate(auth.getName());
        if (transaction.getDate().isAfter(today)) {
            redirectAttributes.addFlashAttribute("error", "Không thể sửa giao dịch thành ngày trong tương lai!");
            return "redirect:/user/transactions";
        }

        try {
            // Xử lý danh mục "viết tay"
            if (categoryName != null && !categoryName.trim().isEmpty()) {
                transaction.setCategory(categoryService.getOrCreateCategory(categoryName, transaction.getType(), auth.getName()));
            }
            
            transactionService.update(transaction, auth.getName());
            
            // Check budget for update
            if (transaction.getCategory() != null) {
                Long walletId = (transaction.getWallet() != null) ? transaction.getWallet().getId() : null;
                BudgetAlertDTO alert = budgetService.checkBudgetAlert(
                        auth.getName(), 
                        transaction.getCategory().getId(), 
                        transaction.getDate().getMonthValue(), 
                        transaction.getDate().getYear(),
                        walletId
                );
                
                // Also check daily limit
                BudgetAlertDTO dailyAlert = budgetService.checkDailyLimitAlert(auth.getName(), walletId);
                
                if (dailyAlert.isAlert()) {
                    redirectAttributes.addFlashAttribute("budgetAlert", dailyAlert);
                    redirectAttributes.addFlashAttribute("message", "Cập nhật thành công. " + dailyAlert.getMessage());
                } else if (alert.isAlert()) {
                    redirectAttributes.addFlashAttribute("budgetAlert", alert);
                    redirectAttributes.addFlashAttribute("message", "Cập nhật thành công. " + alert.getMessage());
                } else {
                    redirectAttributes.addFlashAttribute("message", "Đã cập nhật giao dịch thành công! Bạn vẫn đang quản lý ngân sách rất tốt. ✅");
                }
            } else {
                redirectAttributes.addFlashAttribute("message", "Đã cập nhật giao dịch thành công! ✅");
            }
        } catch (SpendingLimitException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.findByUserName(auth.getName()));
            model.addAttribute("wallets", walletService.findByUsername(auth.getName()));
            return "user/transaction/edit";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.findByUserName(auth.getName()));
            model.addAttribute("wallets", walletService.findByUsername(auth.getName()));
            return "user/transaction/edit";
        }
        
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/user/transactions");
    }

    @PostMapping("/transactions/delete")
    public String delete(@RequestParam Long id, Authentication auth, RedirectAttributes ra) {
        Transaction transaction = transactionService.findById(id, auth.getName());
        if (transaction == null) {
            ra.addFlashAttribute("error", "Giao dịch không tồn tại.");
            return "redirect:/user/transactions";
        }

        // Kiểm tra khóa tháng
        java.time.LocalDate today = java.time.LocalDate.now();
        if (transaction.getDate().getMonthValue() != today.getMonthValue() || 
            transaction.getDate().getYear() != today.getYear()) {
            ra.addFlashAttribute("error", "Không thể xóa giao dịch từ các tháng trước.");
            return "redirect:/user/transactions";
        }

        try {
            transactionService.delete(id, auth.getName());
            ra.addFlashAttribute("message", "Đã xóa giao dịch thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/user/transactions";
    }

    @PostMapping("/update-ajax")
    public String updateAjax(@ModelAttribute Transaction transaction, Authentication auth, 
                             RedirectAttributes ra, HttpServletRequest request, Model model) {
        Transaction existing = transactionService.findById(transaction.getId(), auth.getName());
        if (existing == null) {
            return "redirect:/user/transactions/error-ajax";
        }

        // Kiểm tra ngày tương lai
        LocalDate today = userService.getEffectiveDate(auth.getName());
        if (transaction.getDate().isAfter(today)) {
            return "redirect:/user/transactions/error-ajax";
        }

        try {
            transactionService.update(transaction, auth.getName());
            return "redirect:/user/transactions";
        } catch (Exception e) {
            return "redirect:/user/transactions/error-ajax";
        }
    }

    @GetMapping("/api/transactions/{id}")
    @ResponseBody
    public ResponseEntity<?> getTransaction(@PathVariable Long id, Authentication auth) {
        try {
            Transaction t = transactionService.findById(id, auth.getName());
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("amount", t.getAmount());
            map.put("type", t.getType().name());
            map.put("date", t.getDate().toString());
            map.put("description", t.getDescription());
            if (t.getCategory() != null) {
                map.put("categoryId", t.getCategory().getId());
                map.put("categoryName", t.getCategory().getName());
            }
            if (t.getWallet() != null) map.put("walletId", t.getWallet().getId());
            if (t.getToWallet() != null) map.put("toWalletId", t.getToWallet().getId());
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
