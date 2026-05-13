package com.codegym.finance.service.budget;
import com.codegym.finance.entity.transaction.TransactionType;

import com.codegym.finance.entity.budget.Budget;
import com.codegym.finance.entity.category.Category;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.entity.transaction.Transaction;
import com.codegym.finance.entity.wallet.Wallet;
import com.codegym.finance.repository.budget.BudgetRepository;
import com.codegym.finance.repository.category.CategoryRepository;
import com.codegym.finance.repository.transaction.TransactionRepository;
import com.codegym.finance.repository.user.UserRepository;
import com.codegym.finance.repository.wallet.WalletRepository;
import com.codegym.finance.service.budget.IBudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BudgetService implements IBudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private com.codegym.finance.service.user.IUserService userService;

    @Override
    public void save(Long categoryId, Double amount, Integer month, Integer year, String username, Long walletId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        Wallet wallet = null;
        if (walletId != null) {
            wallet = walletRepository.findById(walletId).orElse(null);
        }

        Optional<Budget> existing;
        if (wallet != null) {
            existing = budgetRepository.findByUserAndWalletAndCategoryAndMonthAndYear(user, wallet, category, month, year);
        } else {
            existing = budgetRepository.findByUserAndCategoryAndMonthAndYear(user, category, month, year);
        }
        
        Budget budget;
        if (existing.isPresent()) {
            budget = existing.get();
            budget.setAmount(amount);
        } else {
            budget = Budget.builder()
                    .amount(amount)
                    .month(month)
                    .year(year)
                    .category(category)
                    .user(user)
                    .wallet(wallet)
                    .build();
        }
        budgetRepository.save(budget);
    }

    @Override
    public List<Budget> getBudgetsByMonth(String username, Integer month, Integer year, Long walletId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (walletId != null) {
            Wallet wallet = walletRepository.findById(walletId).orElse(null);
            if (wallet != null) {
                return budgetRepository.findByUserAndWalletAndMonthAndYear(user, wallet, month, year);
            }
        }
        return budgetRepository.findByUserAndMonthAndYear(user, month, year);
    }

    @Override
    public Map<Long, Double> getBudgetMapByMonth(String username, Integer month, Integer year, Long walletId) {
        List<Budget> budgets = getBudgetsByMonth(username, month, year, walletId);
        return budgets.stream().collect(Collectors.toMap(
                b -> b.getCategory().getId(),
                Budget::getAmount,
                (existing, replacement) -> existing
        ));
    }

    @Override
    public IBudgetService.BudgetAlertDTO checkBudgetAlert(String username, Long categoryId, Integer month, Integer year, Long walletId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Wallet wallet = null;
        if (walletId != null) {
            wallet = walletRepository.findById(walletId).orElse(null);
        }

        Optional<Budget> budgetOpt;
        if (wallet != null) {
            budgetOpt = budgetRepository.findByUserAndWalletAndCategoryAndMonthAndYear(user, wallet, category, month, year);
        } else {
            budgetOpt = budgetRepository.findByUserAndCategoryAndMonthAndYear(user, category, month, year);
        }

        if (budgetOpt.isEmpty()) {
            return new IBudgetService.BudgetAlertDTO(false, null, null, 0.0, 0.0);
        }

        Budget budget = budgetOpt.get();
        double limit = budget.getAmount();
        if (limit <= 0) return new IBudgetService.BudgetAlertDTO(false, null, null, 0.0, 0.0);

        LocalDate start = java.time.LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        
        Double spent;
        if (wallet != null) {
            spent = transactionRepository.sumByUserAndCategoryAndWalletAndDateBetween(user, category, wallet, start, end);
        } else {
            spent = transactionRepository.sumByUserAndCategoryAndDateBetween(user, category, start, end);
        }
        
        if (spent == null) spent = 0.0;

        double percentage = (spent / limit) * 100;

        if (percentage >= 100) {
            return new IBudgetService.BudgetAlertDTO(true, 
                "Cảnh báo nguy hiểm! Bạn đã tiêu " + String.format("%.1f", percentage) + "% hạn mức cho mục " + category.getName() + (wallet != null ? " trong ví " + wallet.getName() : ""), 
                "DANGER", percentage, spent);
        } else if (percentage >= 80) {
            return new IBudgetService.BudgetAlertDTO(true, 
                "Lưu ý: Bạn đã tiêu " + String.format("%.1f", percentage) + "% hạn mức cho mục " + category.getName() + (wallet != null ? " trong ví " + wallet.getName() : ""), 
                "WARNING", percentage, spent);
        }
        return new IBudgetService.BudgetAlertDTO(false, null, null, percentage, spent);
    }

    @Override
    public IBudgetService.BudgetAlertDTO checkDailyCategoryBudgetAlert(String username, Long categoryId, Integer month, Integer year, Long walletId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Wallet wallet = null;
        if (walletId != null) {
            wallet = walletRepository.findById(walletId).orElse(null);
        }

        Optional<Budget> budgetOpt;
        if (wallet != null) {
            budgetOpt = budgetRepository.findByUserAndWalletAndCategoryAndMonthAndYear(user, wallet, category, month, year);
        } else {
            budgetOpt = budgetRepository.findByUserAndCategoryAndMonthAndYear(user, category, month, year);
        }

        if (budgetOpt.isEmpty()) {
            return new IBudgetService.BudgetAlertDTO(false, null, null, 0.0, 0.0);
        }

        Budget budget = budgetOpt.get();
        double limit = budget.getAmount();
        if (limit <= 0) return new IBudgetService.BudgetAlertDTO(false, null, null, 0.0, 0.0);

        LocalDate today = userService.getEffectiveDate(username);
        Double spent;
        if (wallet != null) {
            spent = transactionRepository.sumByUserAndCategoryAndWalletAndDate(user, category, wallet, today);
        } else {
            spent = transactionRepository.sumByUserAndCategoryAndDate(user, category, today);
        }
        
        if (spent == null) spent = 0.0;
        double percentage = (spent / limit) * 100;

        if (percentage >= 100) {
            return new IBudgetService.BudgetAlertDTO(true, 
                "Cảnh báo: Bạn đã vượt hạn mức ngày cho mục " + category.getName() + "!", 
                "DANGER", percentage, spent);
        } else if (percentage >= 80) {
            return new IBudgetService.BudgetAlertDTO(true, 
                "Sắp chạm hạn mức ngày cho mục " + category.getName(), 
                "WARNING", percentage, spent);
        }

        return new IBudgetService.BudgetAlertDTO(false, null, null, percentage, spent);
    }

    @Override
    public IBudgetService.BudgetAlertDTO checkDailyLimitAlert(String username, Long walletId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (walletId == null) return new IBudgetService.BudgetAlertDTO(false, null, null, 0.0, 0.0);
        
        Wallet wallet = walletRepository.findById(walletId).orElse(null);
        if (wallet == null) return new IBudgetService.BudgetAlertDTO(false, null, null, 0.0, 0.0);
        
        Double dailyLimit = wallet.getDailySpendingLimit();
        if (dailyLimit == null || dailyLimit <= 0) {
            return new IBudgetService.BudgetAlertDTO(false, null, null, 0.0, 0.0);
        }
        
        Double todaySpent = transactionRepository.sumByUserAndWalletAndTypeAndDate(user, wallet, com.codegym.finance.entity.transaction.TransactionType.EXPENSE, userService.getEffectiveDate(username));
        if (todaySpent == null) todaySpent = 0.0;
        
        double percentage = (todaySpent / dailyLimit) * 100;
        if (percentage > 100) {
            return new IBudgetService.BudgetAlertDTO(true, 
                "Cảnh báo: Bạn đã vượt hạn mức chi tiêu hôm nay cho ví " + wallet.getName() + " (" + String.format("%.0f", dailyLimit) + "đ)!", 
                "DANGER", percentage, todaySpent);
        }
        return new IBudgetService.BudgetAlertDTO(false, null, "SUCCESS", percentage, todaySpent);
    }

    @Override
    public void saveDailyLimit(Long walletId, Double dailyLimit, String username) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        if (!wallet.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized");
        }
        wallet.setDailySpendingLimit(dailyLimit);
        walletRepository.save(wallet);
    }
}

