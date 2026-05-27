package com.codegym.finance.service.transaction;
import com.codegym.finance.entity.user.User;

import com.codegym.finance.entity.transaction.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;

import com.codegym.finance.service.budget.BudgetAlertDTO;

public interface ITransactionService {

    // 🔥 LẤY DANH SÁCH THEO USER
    List<Transaction> findByUserName(String username);
    Page<Transaction> findByUserName(String username, Pageable pageable);

    // 🔥 CREATE
    void save(Transaction transaction, String username);

    // 🔥 FIND 1
    Transaction findById(Long id, String username);

    // 🔥 UPDATE
    void update(Transaction transaction, String username);

    // 🔥 DELETE
    void delete(Long id, String username);

    java.math.BigDecimal getTotalIncome(String username);

    java.math.BigDecimal getTotalExpense(String username);

    java.math.BigDecimal getTodayIncome(String username);

    java.math.BigDecimal getTodayExpense(String username);
    java.math.BigDecimal getTodayExpenseForWallet(String username, Long walletId);
    java.math.BigDecimal getThisMonthExpenseForWallet(String username, Long walletId);

    java.math.BigDecimal getThisMonthIncome(String username);

    java.math.BigDecimal getThisMonthExpense(String username);

    java.math.BigDecimal getBalance(String username);

    int getTotalTransactions(String username);

    Map<String, java.math.BigDecimal> getCategoryExpenses(String username);

    Map<String, Object> getMonthlyTrend(String username);

    Map<String, Object> getLast7DaysTrend(String username);

    Map<String, java.math.BigDecimal> getCategorySummary(String username);

    java.math.BigDecimal getSpentInWalletOnDate(String username, Long walletId, java.time.LocalDate date);
    java.math.BigDecimal getSpentByCategoryAndWalletOnDate(String username, Long categoryId, Long walletId, java.time.LocalDate date);
    java.math.BigDecimal getSpentByCategoryOnDate(String username, Long categoryId, java.time.LocalDate date);
    java.math.BigDecimal getSpentOnDate(String username, com.codegym.finance.entity.transaction.TransactionType type, java.time.LocalDate date);

    List<Transaction> getWalletHistory(Long walletId, String username);

    org.springframework.data.domain.Page<Transaction> filterTransactions(String username, java.time.LocalDate start, java.time.LocalDate end, Long walletId, Long categoryId, String keyword, org.springframework.data.domain.Pageable pageable);
    boolean hasCompletedMonthlyFunding(String username);
    List<Map<String, Object>> getDailySpendingReport(String username, int month, int year);

    BudgetAlertDTO createTransaction(Transaction transaction, String categoryName, String username);
    BudgetAlertDTO updateTransaction(Transaction transaction, String categoryName, String username);
    int batchFund(Map<Long, java.math.BigDecimal> amounts, Map<Long, String> descriptions, String username);
    void fund(Long walletId, java.math.BigDecimal amount, String description, String username);
}
