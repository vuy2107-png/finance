package com.codegym.finance.service.savings;
import com.codegym.finance.entity.wallet.Wallet;
import com.codegym.finance.entity.savings.GoalStatus;
import com.codegym.finance.entity.savings.SavingsGoal;
import com.codegym.finance.entity.transaction.Transaction;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.entity.transaction.TransactionType;


import com.codegym.finance.repository.savings.SavingsGoalRepository;
import com.codegym.finance.repository.user.UserRepository;
import com.codegym.finance.service.transaction.ITransactionService;
import com.codegym.finance.service.wallet.IWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class SavingsGoalService implements ISavingsGoalService {

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IWalletService walletService;

    @Autowired
    private ITransactionService transactionService;

    @Override
    public List<SavingsGoal> findByUserName(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return savingsGoalRepository.findByUser(user);
    }

    @Override
    public SavingsGoal findById(Long id, String username) {
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Savings Goal not found"));
        if (!goal.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Không có quyền truy cập");
        }
        return goal;
    }

    @Override
    @Transactional
    public void save(SavingsGoal goal, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        goal.setUser(user);
        
        // Auto update status if somehow current >= target upon creation
        if (goal.getCurrentAmount() != null && goal.getCurrentAmount() >= goal.getTargetAmount()) {
            goal.setStatus(GoalStatus.ACHIEVED);
        } else {
            goal.setStatus(GoalStatus.IN_PROGRESS);
        }
        
        savingsGoalRepository.save(goal);
    }

    @Override
    @Transactional
    public void update(SavingsGoal goal, String username) {
        SavingsGoal existing = findById(goal.getId(), username);
        
        existing.setName(goal.getName());
        existing.setTargetAmount(goal.getTargetAmount());
        existing.setTargetDate(goal.getTargetDate());
        existing.setColorCode(goal.getColorCode());
        existing.setIcon(goal.getIcon());
        
        if (existing.getCurrentAmount() >= existing.getTargetAmount()) {
            existing.setStatus(GoalStatus.ACHIEVED);
        } else {
            existing.setStatus(GoalStatus.IN_PROGRESS);
        }
        
        savingsGoalRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id, String username) {
        SavingsGoal goal = findById(id, username);
        savingsGoalRepository.delete(goal);
    }

    @Override
    @Transactional
    public void addFunds(Long goalId, Double amount, Long walletId, String username) {
        SavingsGoal goal = findById(goalId, username);
        Wallet wallet = walletService.findById(walletId, username);
        
        if (amount <= 0) {
            throw new RuntimeException("Số tiền nạp phải lớn hơn 0");
        }

        // 1. Tạo giao dịch trừ tiền trong ví (EXPENSE)
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setDate(LocalDate.now());
        transaction.setDescription("Nạp tiền vào quỹ: " + goal.getName());
        transaction.setWallet(wallet);
        // Lưu giao dịch (hàm save của TransactionService sẽ tự động trừ tiền trong ví)
        transactionService.save(transaction, username);

        // 2. Cộng tiền vào quỹ
        double newAmount = goal.getCurrentAmount() + amount;
        goal.setCurrentAmount(newAmount);
        
        // 3. Kiểm tra hoàn thành mục tiêu
        if (newAmount >= goal.getTargetAmount()) {
            goal.setStatus(GoalStatus.ACHIEVED);
        }
        
        savingsGoalRepository.save(goal);
    }
}

