package com.codegym.finance.service.wallet;

import com.codegym.finance.entity.wallet.Wallet;
import java.util.List;

public interface IWalletService {
    List<Wallet> findByUsername(String username);
    Wallet findById(Long id, String username);
    void save(Wallet wallet);
    void delete(Long id, String username);
    void updateBalance(Long walletId, java.math.BigDecimal amount, boolean isIncome);
    long countByUsername(String username);
    org.springframework.data.domain.Page<Wallet> searchWallets(String username, String keyword, org.springframework.data.domain.Pageable pageable);
    Wallet createWallet(Wallet wallet, String username);
}
