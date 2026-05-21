package com.codegym.finance.repository.wallet;

import com.codegym.finance.entity.user.User;
import com.codegym.finance.entity.wallet.DailySpendingLimit;
import com.codegym.finance.entity.wallet.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailySpendingLimitRepository extends JpaRepository<DailySpendingLimit, Long> {
    Optional<DailySpendingLimit> findByUserAndWalletAndDate(User user, Wallet wallet, LocalDate date);
    
    @org.springframework.data.jpa.repository.Query("SELECT d.amount FROM DailySpendingLimit d " +
           "WHERE d.user = :user AND d.wallet = :wallet AND d.date <= :date " +
           "ORDER BY d.date DESC, d.id DESC")
    java.util.List<Double> findHistoricalLimits(@org.springframework.data.repository.query.Param("user") User user, 
                                               @org.springframework.data.repository.query.Param("wallet") Wallet wallet, 
                                               @org.springframework.data.repository.query.Param("date") LocalDate date,
                                               org.springframework.data.domain.Pageable pageable);
}
