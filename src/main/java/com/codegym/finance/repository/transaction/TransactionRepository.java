package com.codegym.finance.repository.transaction;

import com.codegym.finance.entity.category.Category;
import com.codegym.finance.entity.transaction.Transaction;
import com.codegym.finance.entity.transaction.TransactionType;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.entity.wallet.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // ================= BASIC =================
    // ================= BASIC =================
    @Query("SELECT t FROM Transaction t WHERE t.user = :user " +
           "AND t.description NOT LIKE '%SYSTEM_DEPOSIT_INFLOW%' AND t.description NOT LIKE '%Premium%'")
    List<Transaction> findByUser(@Param("user") User user);

    List<Transaction> findByCategory(Category category);
    List<Transaction> findByUserAndCategory(User user, Category category);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user " +
           "AND t.description NOT LIKE '%SYSTEM_DEPOSIT_INFLOW%' AND t.description NOT LIKE '%Premium%' " +
           "ORDER BY t.date DESC, t.id DESC")
    Page<Transaction> findByUserOrderByDateDescIdDesc(@Param("user") User user, Pageable pageable);

    int countByUser(User user);

    // ================= DASHBOARD =================
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.type = :type " +
           "AND t.description NOT LIKE '%SYSTEM_DEPOSIT_INFLOW%' AND t.description NOT LIKE '%Premium%'")
    java.math.BigDecimal sumByUserAndType(@Param("user") User user, @Param("type") TransactionType type);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.type = :type AND t.date = :date " +
           "AND t.description NOT LIKE '%SYSTEM_DEPOSIT_INFLOW%' AND t.description NOT LIKE '%Premium%'")
    java.math.BigDecimal sumByUserAndTypeAndDate(@Param("user") User user, @Param("type") TransactionType type, @Param("date") LocalDate date);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.wallet = :wallet AND t.type = :type AND t.date = :date")
    java.math.BigDecimal sumByUserAndWalletAndTypeAndDate(@Param("user") User user, @Param("wallet") Wallet wallet, @Param("type") TransactionType type, @Param("date") LocalDate date);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.type = :type AND t.date >= :startDate AND t.date <= :endDate " +
           "AND t.description NOT LIKE '%SYSTEM_DEPOSIT_INFLOW%' AND t.description NOT LIKE '%Premium%'")
    java.math.BigDecimal sumByUserAndTypeAndDateBetween(@Param("user") User user, @Param("type") TransactionType type, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.wallet = :wallet AND t.type = :type AND t.date >= :startDate AND t.date <= :endDate " +
           "AND t.description NOT LIKE '%SYSTEM_DEPOSIT_INFLOW%' AND t.description NOT LIKE '%Premium%'")
    java.math.BigDecimal sumByUserAndWalletAndTypeAndDateBetween(@Param("user") User user, @Param("wallet") Wallet wallet, @Param("type") TransactionType type, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.category = :category AND t.type = 'EXPENSE' AND t.date BETWEEN :startDate AND :endDate " +
           "AND t.description NOT LIKE '%SYSTEM_DEPOSIT_INFLOW%' AND t.description NOT LIKE '%Premium%'")
    java.math.BigDecimal sumByUserAndCategoryAndDateBetween(@Param("user") User user, @Param("category") Category category, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.category = :category AND (t.wallet = :wallet) AND t.type = 'EXPENSE' AND t.date BETWEEN :startDate AND :endDate")
    java.math.BigDecimal sumByUserAndCategoryAndWalletAndDateBetween(@Param("user") User user, @Param("category") Category category, @Param("wallet") Wallet wallet, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.category = :category AND t.type = 'EXPENSE' AND t.date = :date " +
           "AND t.description NOT LIKE '%SYSTEM_DEPOSIT_INFLOW%' AND t.description NOT LIKE '%Premium%'")
    java.math.BigDecimal sumByUserAndCategoryAndDate(@Param("user") User user, @Param("category") Category category, @Param("date") LocalDate date);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.category = :category AND t.wallet = :wallet AND t.type = 'EXPENSE' AND t.date = :date")
    java.math.BigDecimal sumByUserAndCategoryAndWalletAndDate(@Param("user") User user, @Param("category") Category category, @Param("wallet") Wallet wallet, @Param("date") LocalDate date);

    // ================= FILTER =================
    List<Transaction> findByUserAndDate(User user, LocalDate date);
    List<Transaction> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
    List<Transaction> findByUserAndType(User user, TransactionType type);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND (t.wallet = :wallet OR t.toWallet = :wallet) ORDER BY t.date DESC")
    List<Transaction> findByWalletHistory(@Param("user") User user, @Param("wallet") Wallet wallet);

    // ================= ADVANCED =================
    @Query("SELECT t FROM Transaction t WHERE t.user = :user " +
           "AND t.description NOT LIKE '%SYSTEM_DEPOSIT_INFLOW%' AND t.description NOT LIKE '%Premium%' " +
           "ORDER BY t.date DESC")
    List<Transaction> findTop5ByUserOrderByDateDesc(@Param("user") User user);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user " +
           "AND t.description NOT LIKE '%SYSTEM_DEPOSIT_INFLOW%' AND t.description NOT LIKE '%Premium%' " +
           "ORDER BY t.date DESC, t.id DESC")
    List<Transaction> findByUserOrderByDateDescIdDesc(@Param("user") User user);

    // ================= CHART DATA =================
    @Query("SELECT t.category.name, SUM(t.amount) FROM Transaction t " +
           "WHERE t.user = :user AND t.type = 'EXPENSE' " +
           "AND t.date BETWEEN :startDate AND :endDate " +
           "AND t.description NOT LIKE '%SYSTEM_DEPOSIT_INFLOW%' AND t.description NOT LIKE '%Premium%' " +
           "GROUP BY t.category.name")
    List<Object[]> sumAmountByCategory(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT FUNCTION('MONTH', t.date) as m, FUNCTION('YEAR', t.date) as y, t.type, SUM(t.amount) " +
           "FROM Transaction t WHERE t.user = :user AND t.date >= :startDate " +
           "AND t.description NOT LIKE '%SYSTEM_DEPOSIT_INFLOW%' AND t.description NOT LIKE '%Premium%' " +
           "GROUP BY y, m, t.type " +
           "ORDER BY y ASC, m ASC")
    List<Object[]> getMonthlyStats(@Param("user") User user, @Param("startDate") LocalDate startDate);

    @Query("SELECT t.date, t.type, SUM(t.amount) " +
           "FROM Transaction t WHERE t.user = :user AND t.date >= :startDate " +
           "AND t.description NOT LIKE '%SYSTEM_DEPOSIT_INFLOW%' AND t.description NOT LIKE '%Premium%' " +
           "GROUP BY t.date, t.type " +
           "ORDER BY t.date ASC")
    List<Object[]> getDailyStats(@Param("user") User user, @Param("startDate") LocalDate startDate);

    @Modifying
    @Query("UPDATE Transaction t SET t.category = null WHERE t.user.id = :userId")
    void clearCategoryByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user " +
           "AND (:startDate IS NULL OR t.date >= :startDate) " +
           "AND (:endDate IS NULL OR t.date <= :endDate) " +
           "AND (:walletId IS NULL OR t.wallet.id = :walletId OR t.toWallet.id = :walletId) " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
           "AND (:keyword IS NULL OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:keyword IS NOT NULL OR (t.description NOT LIKE '%SYSTEM_DEPOSIT_INFLOW%' AND t.description NOT LIKE '%Premium%')) " +
           "ORDER BY t.date DESC")
    Page<Transaction> filterTransactions(@Param("user") User user,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("walletId") Long walletId,
                                         @Param("categoryId") Long categoryId,
                                         @Param("keyword") String keyword,
                                         Pageable pageable);

    // ================= ADMIN STATS =================
    @Query("SELECT SUM(t.amount) FROM Transaction t")
    java.math.BigDecimal sumAllTransactions();

    @Query("SELECT COUNT(t) FROM Transaction t")
    long countAllTransactions();

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user = :user " +
           "AND t.type = com.codegym.finance.entity.transaction.TransactionType.INCOME " +
           "AND t.date BETWEEN :startDate AND :endDate " +
           "AND (t.description LIKE '%Cấp vốn%' OR t.description LIKE '%Khởi tạo%')")
    long countMonthlyFunding(@Param("user") User user, 
                             @Param("startDate") LocalDate startDate, 
                             @Param("endDate") LocalDate endDate);

    long countAllByDate(LocalDate date);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE UPPER(t.description) LIKE UPPER(CONCAT('%', :keyword, '%'))")
    java.math.BigDecimal sumTransactionsByDescriptionLike(@Param("keyword") String keyword);

    @Query("SELECT t.date, SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.type = :type AND t.date BETWEEN :start AND :end " +
           "AND t.description NOT LIKE '%SYSTEM_DEPOSIT_INFLOW%' AND t.description NOT LIKE '%Premium%' " +
           "GROUP BY t.date")
    List<Object[]> getDailySpendingSum(@Param("user") User user, @Param("type") TransactionType type, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT t.category.name, SUM(t.amount) FROM Transaction t " +
           "WHERE t.type = com.codegym.finance.entity.transaction.TransactionType.EXPENSE " +
           "GROUP BY t.category.name ORDER BY SUM(t.amount) DESC")
    List<Object[]> getSystemTopSpendingCategories(Pageable pageable);

    @Query("SELECT FUNCTION('YEAR', t.date) as y, FUNCTION('MONTH', t.date) as m, " +
           "SUM(CASE WHEN t.description LIKE '%SYSTEM_DEPOSIT_INFLOW%' THEN t.amount ELSE 0 END) as deposits, " +
           "SUM(CASE WHEN t.description LIKE '%Premium%' THEN t.amount ELSE 0 END) as revenue " +
           "FROM Transaction t " +
           "WHERE t.date >= :startDate " +
           "GROUP BY y, m " +
           "ORDER BY y ASC, m ASC")
    List<Object[]> getSystemMonthlyCashflow(@Param("startDate") LocalDate startDate);
}
