package com.codegym.finance.service.wallet;
import com.codegym.finance.entity.transaction.Transaction;

import com.codegym.finance.entity.wallet.Wallet;
import com.codegym.finance.repository.wallet.WalletRepository;
import com.codegym.finance.service.wallet.IWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WalletService implements IWalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Override
    public List<Wallet> findByUsername(String username) {
        return walletRepository.findByUserUsername(username);
    }

    @Override
    public Wallet findById(Long id, String username) {
        return walletRepository.findByIdAndUserUsername(id, username).orElse(null);
    }

    @Override
    public void save(Wallet wallet) {
        walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public void delete(Long id, String username) {
        Wallet wallet = walletRepository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví hoặc bạn không có quyền xóa ví này"));
        walletRepository.delete(wallet);
    }

    @Override
    @Transactional
    public void updateBalance(Long walletId, java.math.BigDecimal amount, boolean isIncome) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví với ID: " + walletId));
        
        if (wallet.getBalance() == null) {
            wallet.setBalance(java.math.BigDecimal.ZERO);
        }

        if (isIncome) {
            wallet.setBalance(wallet.getBalance().add(amount));
        } else {
            // Cho phép số dư âm để theo dõi chi tiêu vượt mức
            wallet.setBalance(wallet.getBalance().subtract(amount));
        }
        walletRepository.save(wallet);
    }
    @Override
    public long countByUsername(String username) {
        return walletRepository.countByUserUsername(username);
    }

    @Override
    public org.springframework.data.domain.Page<com.codegym.finance.entity.wallet.Wallet> searchWallets(String username, String keyword, org.springframework.data.domain.Pageable pageable) {
        return walletRepository.searchWallets(username, keyword, pageable);
    }
}

