package com.codegym.finance.controller.user;

import com.codegym.finance.entity.budget.Budget;
import com.codegym.finance.entity.transaction.Transaction;
import com.codegym.finance.entity.transaction.TransactionType;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.entity.wallet.Wallet;
import com.codegym.finance.service.budget.BudgetAlertDTO;
import com.codegym.finance.service.budget.IBudgetService;
import com.codegym.finance.service.category.ICategoryService;
import com.codegym.finance.service.transaction.ITransactionService;
import com.codegym.finance.service.user.IUserService;
import com.codegym.finance.service.wallet.IWalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
@Slf4j
public class DashboardController {

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
                java.math.BigDecimal totalDaySpent = java.math.BigDecimal.ZERO;
                java.math.BigDecimal totalDayLimit = java.math.BigDecimal.ZERO;
                boolean isDailyOverspent = false;
                List<Wallet> userWallets = walletService.findByUsername(username);
                for (Wallet w : userWallets) {
                    java.math.BigDecimal spentInWallet = transactionService.getSpentInWalletOnDate(username, w.getId(), reportDate);
                    totalDaySpent = totalDaySpent.add(spentInWallet);
                    java.math.BigDecimal histLimit = budgetService.getDailyLimitForWallet(username, w.getId(), reportDate);
                    if (histLimit != null && histLimit.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        totalDayLimit = totalDayLimit.add(histLimit);
                        if (spentInWallet.compareTo(histLimit) > 0) {
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
                    
                    java.math.BigDecimal spentOnDate;
                    if (b.getWallet() != null) {
                        spentOnDate = transactionService.getSpentByCategoryAndWalletOnDate(username, b.getCategory().getId(), b.getWallet().getId(), reportDate);
                    } else {
                        spentOnDate = transactionService.getSpentByCategoryOnDate(username, b.getCategory().getId(), reportDate);
                    }
                    if (spentOnDate == null) spentOnDate = java.math.BigDecimal.ZERO;
                    
                    if (spentOnDate.compareTo(b.getAmount()) > 0) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("name", b.getCategory().getName());
                        item.put("spent", spentOnDate);
                        item.put("limit", b.getAmount());
                        item.put("wallet", b.getWallet() != null ? b.getWallet().getName() : "Chung");
                        overspentCategories.add(item);
                    } else if (spentOnDate.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("name", b.getCategory().getName());
                        item.put("spent", spentOnDate);
                        goodCategories.add(item);
                    }
                }
                
                yesterdayReport.put("overspent", overspentCategories);
                yesterdayReport.put("good", goodCategories);
                java.math.BigDecimal totalSpentOnDate = transactionService.getSpentOnDate(username, TransactionType.EXPENSE, reportDate);
                yesterdayReport.put("totalSpent", totalSpentOnDate != null ? totalSpentOnDate : java.math.BigDecimal.ZERO);
                
                model.addAttribute("morningReport", yesterdayReport);
                model.addAttribute("showMorningReport", true);
                
                if (!forceReport) {
                    currentUser.setLastReportDate(now);
                    userService.save(currentUser);
                }
            }
            
            // Core Metrics
            java.math.BigDecimal monthlyIncome = transactionService.getThisMonthIncome(username);
            java.math.BigDecimal monthlyExpense = transactionService.getThisMonthExpense(username);
            
            model.addAttribute("totalBalance", transactionService.getBalance(username)); // Tổng tài sản thực tế
            model.addAttribute("monthlyIncome", monthlyIncome);
            model.addAttribute("monthlyExpense", monthlyExpense);
            model.addAttribute("monthlyBalance", monthlyIncome.subtract(monthlyExpense)); // Số dư tháng này
            
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

            // Budgeting Data restructured by wallet
            List<Wallet> userWallets = walletService.findByUsername(username);
            List<Map<String, Object>> walletLimits = new ArrayList<>();
            
            for (Wallet w : userWallets) {
                Map<String, Object> walletData = new HashMap<>();
                walletData.put("walletName", w.getName());
                walletData.put("id", w.getId());
                
                // 1. Daily Limit for this wallet (Use historical/effective limit for the simulated date)
                java.math.BigDecimal effectiveLimit = budgetService.getDailyLimitForWallet(username, w.getId(), now);
                boolean isSuggested = false;
                
                // Cảnh báo chi tiêu vượt quá tiến trình thời gian của tháng
                int currentDay = now.getDayOfMonth();
                java.time.YearMonth yearMonthObject = java.time.YearMonth.of(now.getYear(), now.getMonthValue());
                int daysInMonth = yearMonthObject.lengthOfMonth();
                java.math.BigDecimal spentThisMonth = transactionService.getThisMonthExpenseForWallet(username, w.getId());
                
                java.math.BigDecimal initialBalance = w.getBalance() != null ? w.getBalance().add(spentThisMonth) : spentThisMonth; // xấp xỉ số tiền đầu tháng
                if (initialBalance.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    double spentPercent = spentThisMonth.multiply(java.math.BigDecimal.valueOf(100)).divide(initialBalance, 2, java.math.RoundingMode.HALF_UP).doubleValue();
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
                        java.math.BigDecimal suggested = w.getBalance().divide(java.math.BigDecimal.valueOf(remainingDays), 2, java.math.RoundingMode.HALF_UP);
                        effectiveLimit = suggested.compareTo(java.math.BigDecimal.ZERO) > 0 ? suggested : java.math.BigDecimal.ZERO;
                        isSuggested = true;
                        walletData.put("isSuggested", true);
                    }
                }

                if (effectiveLimit != null && effectiveLimit.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    java.math.BigDecimal todayWalletExp = transactionService.getTodayExpenseForWallet(username, w.getId());
                    walletData.put("spent", todayWalletExp);
                    walletData.put("limit", effectiveLimit);
                    double percent = todayWalletExp.multiply(java.math.BigDecimal.valueOf(100)).divide(effectiveLimit, 2, java.math.RoundingMode.HALF_UP).doubleValue();
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
            model.addAttribute("totalBalance", java.math.BigDecimal.ZERO);
            model.addAttribute("monthlyIncome", java.math.BigDecimal.ZERO);
            model.addAttribute("monthlyExpense", java.math.BigDecimal.ZERO);
            model.addAttribute("monthlyStats", new HashMap<>());
            model.addAttribute("categoryStats", new HashMap<>());
            model.addAttribute("recentTransactions", new ArrayList<>());
            model.addAttribute("budgetStatus", new ArrayList<>());
            log.error("Dashboard error for user: {}", username, e);
        }
        return "user/dashboard";
    }
}
