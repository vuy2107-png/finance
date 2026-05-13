package com.codegym.finance.repository.wallet;
import com.codegym.finance.entity.user.User;

import com.codegym.finance.entity.wallet.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    List<Wallet> findByUserId(Long userId);
    List<Wallet> findByUserUsername(String username);
    Optional<Wallet> findByIdAndUserUsername(Long id, String username);
    long countByUserUsername(String username);

    @org.springframework.data.jpa.repository.Query("SELECT w FROM Wallet w WHERE w.user.username = :username " +
           "AND (:keyword IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY w.id DESC")
    org.springframework.data.domain.Page<Wallet> searchWallets(@org.springframework.data.repository.query.Param("username") String username, 
                                                               @org.springframework.data.repository.query.Param("keyword") String keyword, 
                                                               org.springframework.data.domain.Pageable pageable);
}
