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
import com.codegym.finance.repository.wallet.DailySpendingLimitRepository;
import com.codegym.finance.entity.wallet.DailySpendingLimit;
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
    private DailySpendingLimitRepository dailySpendingLimitRepository;

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
    public BudgetAlertDTO checkBudgetAlert(String username, Long categoryId, Integer month, Integer year, Long walletId) {
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
            return new BudgetAlertDTO(false, null, null, 0.0, 0.0);
        }

        Budget budget = budgetOpt.get();
        double limit = budget.getAmount();
        if (limit <= 0) return new BudgetAlertDTO(false, null, null, 0.0, 0.0);

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
            return new BudgetAlertDTO(true, 
                "Cảnh báo nguy hiểm! Bạn đã tiêu " + String.format("%.1f", percentage) + "% hạn mức cho mục " + category.getName() + (wallet != null ? " trong ví " + wallet.getName() : ""), 
                "DANGER", percentage, spent);
        } else if (percentage >= 80) {
            return new BudgetAlertDTO(true, 
                "Lưu ý: Bạn đã tiêu " + String.format("%.1f", percentage) + "% hạn mức cho mục " + category.getName() + (wallet != null ? " trong ví " + wallet.getName() : ""), 
                "WARNING", percentage, spent);
        }
        return new BudgetAlertDTO(false, null, null, percentage, spent);
    }

    @Override
    public BudgetAlertDTO checkDailyCategoryBudgetAlert(String username, Long categoryId, Integer month, Integer year, Long walletId) {
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
            return new BudgetAlertDTO(false, null, null, 0.0, 0.0);
        }

        Budget budget = budgetOpt.get();
        double limit = budget.getAmount();
        if (limit <= 0) return new BudgetAlertDTO(false, null, null, 0.0, 0.0);

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
            return new BudgetAlertDTO(true, 
                "Cảnh báo: Bạn đã vượt hạn mức ngày cho mục " + category.getName() + "!", 
                "DANGER", percentage, spent);
        } else if (percentage >= 80) {
            return new BudgetAlertDTO(true, 
                "Sắp chạm hạn mức ngày cho mục " + category.getName(), 
                "WARNING", percentage, spent);
        }

        return new BudgetAlertDTO(false, null, null, percentage, spent);
    }

    @Override
    public BudgetAlertDTO checkDailyLimitAlert(String username, Long walletId) {
        return checkDailyLimitAlert(username, walletId, userService.getEffectiveDate(username));
    }

    @Override
    public BudgetAlertDTO checkDailyLimitAlert(String username, Long walletId, LocalDate date) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (walletId == null) return new BudgetAlertDTO(false, null, null, 0.0, 0.0);
        
        Wallet wallet = walletRepository.findById(walletId).orElse(null);
        if (wallet == null) return new BudgetAlertDTO(false, null, null, 0.0, 0.0);
        
        Double dailyLimit = getDailyLimitForWallet(username, walletId, date);
        if (dailyLimit == null || dailyLimit <= 0) {
            return new BudgetAlertDTO(false, null, null, 0.0, 0.0);
        }
        
        Double todaySpent = transactionRepository.sumByUserAndWalletAndTypeAndDate(user, wallet, com.codegym.finance.entity.transaction.TransactionType.EXPENSE, date);
        if (todaySpent == null) todaySpent = 0.0;
        
        double percentage = (todaySpent / dailyLimit) * 100;
        if (percentage > 100) {
            return new BudgetAlertDTO(true, 
                "Cảnh báo: Bạn đã vượt hạn mức chi tiêu hôm nay cho ví " + wallet.getName() + " (" + String.format("%.0f", dailyLimit) + "đ)!", 
                "DANGER", percentage, todaySpent);
        }
        return new BudgetAlertDTO(false, null, "SUCCESS", percentage, todaySpent);
    }

    @Override
    public Double getDailyLimitForWallet(String username, Long walletId, LocalDate date) {
        User user = userRepository.findByUsername(username).orElse(null);
        Wallet wallet = walletRepository.findById(walletId).orElse(null);
        if (user == null || wallet == null) return 0.0;

        // Tìm hạn mức có hiệu lực gần nhất tính đến ngày 'date' bằng JPQL tường minh
        List<Double> limits = dailySpendingLimitRepository.findHistoricalLimits(user, wallet, date, org.springframework.data.domain.PageRequest.of(0, 1));
        
        if (limits != null && !limits.isEmpty()) {
            return limits.get(0);
        }
        
        // Nếu hoàn toàn chưa bao giờ thiết lập trong bảng lịch sử, lấy từ trường mặc định của Wallet
        // Lưu ý: Trường này giờ đây chỉ đóng vai trò là "giá trị khởi tạo"
        return wallet.getDailySpendingLimit() != null ? wallet.getDailySpendingLimit() : 0.0;
    }

    @Override
    public void saveDailyLimit(Long walletId, Double dailyLimit, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        if (!wallet.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized");
        }

        LocalDate today = userService.getEffectiveDate(username);
        
        // Save to history table
        Optional<DailySpendingLimit> existing = dailySpendingLimitRepository.findByUserAndWalletAndDate(user, wallet, today);
        DailySpendingLimit limit;
        if (existing.isPresent()) {
            limit = existing.get();
            limit.setAmount(dailyLimit);
        } else {
            limit = DailySpendingLimit.builder()
                    .user(user)
                    .wallet(wallet)
                    .date(today)
                    .amount(dailyLimit)
                    .build();
        }
        dailySpendingLimitRepository.save(limit);
    }
}
