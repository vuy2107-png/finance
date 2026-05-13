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

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // ================= BASIC =================

    // Lấy danh sách theo user
    List<Transaction> findByUser(User user);

    List<Transaction> findByCategory(Category category);

    List<Transaction> findByUserAndCategory(User user, Category category);

    // Lấy danh sách theo user phân trang
    Page<Transaction> findByUserOrderByDateDescIdDesc(User user, Pageable pageable);

    // Đếm số lượng transaction của user
    int countByUser(User user);


    // ================= DASHBOARD =================

    // Tổng tiền theo loại (INCOME / EXPENSE)
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.type = :type")
    Double sumByUserAndType(@Param("user") User user,
                            @Param("type") TransactionType type);

    // Tổng tiền theo loại và ngày cụ thể
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.type = :type AND t.date = :date")
    Double sumByUserAndTypeAndDate(@Param("user") User user,
                                   @Param("type") TransactionType type,
                                   @Param("date") LocalDate date);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.wallet = :wallet AND t.type = :type AND t.date = :date")
    Double sumByUserAndWalletAndTypeAndDate(@Param("user") User user,
                                           @Param("wallet") Wallet wallet,
                                           @Param("type") TransactionType type,
                                           @Param("date") LocalDate date);

    // Tổng tiền theo loại và khoảng thời gian (theo tháng)
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.type = :type AND t.date >= :startDate AND t.date <= :endDate")
    Double sumByUserAndTypeAndDateBetween(@Param("user") User user,
                                          @Param("type") TransactionType type,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.category = :category AND t.type = 'EXPENSE' AND t.date BETWEEN :startDate AND :endDate")
    Double sumByUserAndCategoryAndDateBetween(@Param("user") User user,
                                             @Param("category") Category category,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.category = :category AND (t.wallet = :wallet) AND t.type = 'EXPENSE' AND t.date BETWEEN :startDate AND :endDate")
    Double sumByUserAndCategoryAndWalletAndDateBetween(@Param("user") User user,
                                                      @Param("category") Category category,
                                                      @Param("wallet") Wallet wallet,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.category = :category AND t.type = 'EXPENSE' AND t.date = :date")
    Double sumByUserAndCategoryAndDate(@Param("user") User user,
                                      @Param("category") Category category,
                                      @Param("date") LocalDate date);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.category = :category AND t.wallet = :wallet AND t.type = 'EXPENSE' AND t.date = :date")
    Double sumByUserAndCategoryAndWalletAndDate(@Param("user") User user,
                                               @Param("category") Category category,
                                               @Param("wallet") Wallet wallet,
                                               @Param("date") LocalDate date);

    // ================= FILTER =================

    // Lọc theo ngày
    List<Transaction> findByUserAndDate(User user, LocalDate date);

    // Lọc theo khoảng ngày
    List<Transaction> findByUserAndDateBetween(User user,
                                               LocalDate startDate,
                                               LocalDate endDate);

    // Lọc theo type
    List<Transaction> findByUserAndType(User user, TransactionType type);

    // Lọc theo ví (Bao gồm cả ví gửi và ví nhận trong chuyển tiền)
    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND (t.wallet = :wallet OR t.toWallet = :wallet) ORDER BY t.date DESC")
    List<Transaction> findByWalletHistory(@Param("user") User user, @Param("wallet") Wallet wallet);

    // ================= ADVANCED =================

    // Lấy mới nhất
    List<Transaction> findTop5ByUserOrderByDateDesc(User user);

    // Sắp xếp theo ngày giảm dần
    List<Transaction> findByUserOrderByDateDescIdDesc(User user);

    // ================= CHART DATA =================

    // Thống kê chi tiêu theo danh mục trong khoảng thời gian
    @Query("SELECT t.category.name, SUM(t.amount) FROM Transaction t " +
           "WHERE t.user = :user AND t.type = 'EXPENSE' " +
           "AND t.date BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category.name")
    List<Object[]> sumAmountByCategory(@Param("user") User user,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    @Query("SELECT FUNCTION('MONTH', t.date) as m, FUNCTION('YEAR', t.date) as y, t.type, SUM(t.amount) " +
           "FROM Transaction t WHERE t.user = :user AND t.date >= :startDate " +
           "GROUP BY y, m, t.type " +
           "ORDER BY y ASC, m ASC")
    List<Object[]> getMonthlyStats(@Param("user") User user,
                                   @Param("startDate") LocalDate startDate);

    // Thống kê thu chi theo ngày (cho 7 ngày gần nhất)
    @Query("SELECT t.date, t.type, SUM(t.amount) " +
           "FROM Transaction t WHERE t.user = :user AND t.date >= :startDate " +
           "GROUP BY t.date, t.type " +
           "ORDER BY t.date ASC")
    List<Object[]> getDailyStats(@Param("user") User user,
                                 @Param("startDate") LocalDate startDate);

    @Modifying
    @Query("UPDATE Transaction t SET t.category = null WHERE t.user.id = :userId")
    void clearCategoryByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user " +
           "AND (:startDate IS NULL OR t.date >= :startDate) " +
           "AND (:endDate IS NULL OR t.date <= :endDate) " +
           "AND (:walletId IS NULL OR t.wallet.id = :walletId OR t.toWallet.id = :walletId) " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
           "AND (:keyword IS NULL OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
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
    Double sumAllTransactions();

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
    Double sumTransactionsByDescriptionLike(@Param("keyword") String keyword);
}

