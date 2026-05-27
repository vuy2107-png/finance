package com.codegym.finance.service.wallet;
import com.codegym.finance.entity.transaction.Transaction;
import com.codegym.finance.entity.transaction.TransactionType;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.entity.wallet.Wallet;
import com.codegym.finance.repository.wallet.WalletRepository;
import com.codegym.finance.service.wallet.IWalletService;
import com.codegym.finance.service.user.IUserService;
import com.codegym.finance.service.transaction.ITransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WalletService implements IWalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private IUserService userService;

    @Autowired
    @Lazy
    private ITransactionService transactionService;

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
    public org.springframework.data.domain.Page<Wallet> searchWallets(String username, String keyword, org.springframework.data.domain.Pageable pageable) {
        return walletRepository.searchWallets(username, keyword, pageable);
    }

    /**
     * Tạo một ví mới cho người dùng. Thực hiện kiểm tra giới hạn ví đối với người dùng tài khoản thường (tối đa 2 ví).
     * Đồng thời, khởi tạo số dư ban đầu cho ví bằng cách tạo một giao dịch INCOME.
     */
    @Override
    @Transactional
    public Wallet createWallet(Wallet wallet, String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("Không tìm thấy người dùng");
        }

        // Kiểm tra giới hạn ví cho người dùng Free
        if (user.getPremium() == null || !user.getPremium()) {
            long walletCount = countByUsername(username);
            if (walletCount >= 2) {
                throw new RuntimeException("Bạn đã đạt giới hạn 2 ví cho tài khoản Miễn phí. Hãy nâng cấp Premium để tạo không giới hạn!");
            }
        }

        java.math.BigDecimal initialBalance = wallet.getBalance() != null ? wallet.getBalance() : java.math.BigDecimal.ZERO;
        wallet.setBalance(java.math.BigDecimal.ZERO); // Reset để Transaction service cập nhật lại
        wallet.setUser(user);
        save(wallet);

        if (initialBalance.compareTo(java.math.BigDecimal.ZERO) > 0) {
            Transaction t = new Transaction();
            t.setAmount(initialBalance);
            t.setType(TransactionType.INCOME);
            t.setDate(java.time.LocalDate.now());
            t.setWallet(wallet);
            t.setDescription("Khởi tạo số dư ban đầu");
            transactionService.save(t, username);
        }
        
        return wallet;
    }
}

