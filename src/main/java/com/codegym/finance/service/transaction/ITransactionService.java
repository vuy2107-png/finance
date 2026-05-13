package com.codegym.finance.service.transaction;
import com.codegym.finance.entity.user.User;

import com.codegym.finance.entity.transaction.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;

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

    double getTotalIncome(String username);

    double getTotalExpense(String username);

    double getTodayIncome(String username);

    double getTodayExpense(String username);
    double getTodayExpenseForWallet(String username, Long walletId);

    double getThisMonthIncome(String username);

    double getThisMonthExpense(String username);

    double getBalance(String username);

    int getTotalTransactions(String username);

    Map<String, Double> getCategoryExpenses(String username);

    Map<String, Object> getMonthlyTrend(String username);

    Map<String, Object> getLast7DaysTrend(String username);

    Map<String, Double> getCategorySummary(String username);

    List<Transaction> getWalletHistory(Long walletId, String username);

    org.springframework.data.domain.Page<Transaction> filterTransactions(String username, java.time.LocalDate start, java.time.LocalDate end, Long walletId, Long categoryId, String keyword, org.springframework.data.domain.Pageable pageable);
    boolean hasCompletedMonthlyFunding(String username);
    List<Map<String, Object>> getDailySpendingReport(String username, int month, int year);
}
