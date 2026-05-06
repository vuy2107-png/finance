package com.codegym.finance.repository;

import com.codegym.finance.entity.Transaction;
import com.codegym.finance.entity.TransactionType;
import com.codegym.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // ================= BASIC =================

    // Lấy danh sách theo user
    List<Transaction> findByUser(User user);

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

    // Tổng tiền theo loại và khoảng thời gian (theo tháng)
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.type = :type AND t.date >= :startDate AND t.date <= :endDate")
    Double sumByUserAndTypeAndDateBetween(@Param("user") User user,
                                          @Param("type") TransactionType type,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);


    // ================= FILTER =================

    // Lọc theo ngày
    List<Transaction> findByUserAndDate(User user, LocalDate date);

    // Lọc theo khoảng ngày
    List<Transaction> findByUserAndDateBetween(User user,
                                               LocalDate startDate,
                                               LocalDate endDate);

    // Lọc theo type
    List<Transaction> findByUserAndType(User user, TransactionType type);


    // ================= ADVANCED =================

    // Lấy mới nhất
    List<Transaction> findTop5ByUserOrderByDateDesc(User user);

    // Sắp xếp theo ngày giảm dần
    List<Transaction> findByUserOrderByDateDesc(User user);
}