package com.codegym.finance.service.budget;

import com.codegym.finance.entity.budget.Budget;
import java.util.List;
import java.util.Map;

public interface IBudgetService {
    void save(Long categoryId, java.math.BigDecimal amount, Integer month, Integer year, String username, Long walletId);
    List<Budget> getBudgetsByMonth(String username, Integer month, Integer year, Long walletId);
    Map<Long, java.math.BigDecimal> getBudgetMapByMonth(String username, Integer month, Integer year, Long walletId);

    BudgetAlertDTO checkBudgetAlert(String username, Long categoryId, Integer month, Integer year, Long walletId);
    BudgetAlertDTO checkDailyCategoryBudgetAlert(String username, Long categoryId, Integer month, Integer year, Long walletId);
    BudgetAlertDTO checkDailyLimitAlert(String username, Long walletId);
    
    // Thêm phương thức lưu hạn mức ngày cho ví
    void saveDailyLimit(Long walletId, java.math.BigDecimal dailyLimit, String username);
    
    BudgetAlertDTO checkDailyLimitAlert(String username, Long walletId, java.time.LocalDate date);
    java.math.BigDecimal getDailyLimitForWallet(String username, Long walletId, java.time.LocalDate date);
}
