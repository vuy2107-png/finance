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
import com.codegym.finance.service.user.IUserService;
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
    private IUserService userService;

    /**
     * Lưu hoặc cập nhật hạn mức ngân sách (Budget) cho một danh mục chi tiêu trong tháng/năm xác định.
     */
    @Override
    public void save(Long categoryId, java.math.BigDecimal amount, Integer month, Integer year, String username, Long walletId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        Wallet wallet = walletId != null ? walletRepository.findById(walletId).orElse(null) : null;
        Optional<Budget> existing = findOptionalBudget(user, category, walletId, month, year);
        
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

    /**
     * Lấy danh sách tất cả ngân sách (Budget) trong tháng/năm cụ thể của người dùng, có thể lọc theo ví.
     */
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

    /**
     * Lấy bản đồ (Map) ánh xạ từ ID danh mục sang số tiền hạn mức ngân sách tương ứng trong tháng.
     */
    @Override
    public Map<Long, java.math.BigDecimal> getBudgetMapByMonth(String username, Integer month, Integer year, Long walletId) {
        List<Budget> budgets = getBudgetsByMonth(username, month, year, walletId);
        return budgets.stream().collect(Collectors.toMap(
                b -> b.getCategory().getId(),
                Budget::getAmount,
                (existing, replacement) -> existing
        ));
    }

    /**
     * Kiểm tra và đưa ra cảnh báo ngân sách tháng cho một danh mục chi tiêu cụ thể (Cảnh báo khi tiêu quá 80% hoặc 100%).
     */
    @Override
    public BudgetAlertDTO checkBudgetAlert(String username, Long categoryId, Integer month, Integer year, Long walletId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Optional<Budget> budgetOpt = findOptionalBudget(user, category, walletId, month, year);

        if (budgetOpt.isEmpty()) {
            return new BudgetAlertDTO(false, null, null, 0.0, java.math.BigDecimal.ZERO);
        }

        Budget budget = budgetOpt.get();
        java.math.BigDecimal limit = budget.getAmount();
        if (limit == null || limit.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return new BudgetAlertDTO(false, null, null, 0.0, java.math.BigDecimal.ZERO);
        }

        LocalDate start = java.time.LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        
        java.math.BigDecimal spent;
        Wallet wallet = walletId != null ? walletRepository.findById(walletId).orElse(null) : null;
        if (wallet != null) {
            spent = transactionRepository.sumByUserAndCategoryAndWalletAndDateBetween(user, category, wallet, start, end);
        } else {
            spent = transactionRepository.sumByUserAndCategoryAndDateBetween(user, category, start, end);
        }
        
        if (spent == null) spent = java.math.BigDecimal.ZERO;

        double percentage = spent.multiply(java.math.BigDecimal.valueOf(100))
                .divide(limit, 2, java.math.RoundingMode.HALF_UP)
                .doubleValue();

        return buildBudgetAlertResult(category, wallet, percentage, spent);
    }

    /**
     * Kiểm tra và đưa ra cảnh báo ngân sách ngày cho một danh mục chi tiêu dựa trên hạn mức ngân sách tháng chia đều.
     */
    @Override
    public BudgetAlertDTO checkDailyCategoryBudgetAlert(String username, Long categoryId, Integer month, Integer year, Long walletId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Optional<Budget> budgetOpt = findOptionalBudget(user, category, walletId, month, year);

        if (budgetOpt.isEmpty()) {
            return new BudgetAlertDTO(false, null, null, 0.0, java.math.BigDecimal.ZERO);
        }

        Budget budget = budgetOpt.get();
        java.math.BigDecimal limit = budget.getAmount();
        if (limit == null || limit.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return new BudgetAlertDTO(false, null, null, 0.0, java.math.BigDecimal.ZERO);
        }

        LocalDate today = userService.getEffectiveDate(username);
        java.math.BigDecimal spent;
        Wallet wallet = walletId != null ? walletRepository.findById(walletId).orElse(null) : null;
        if (wallet != null) {
            spent = transactionRepository.sumByUserAndCategoryAndWalletAndDate(user, category, wallet, today);
        } else {
            spent = transactionRepository.sumByUserAndCategoryAndDate(user, category, today);
        }
        
        if (spent == null) spent = java.math.BigDecimal.ZERO;

        double percentage = spent.multiply(java.math.BigDecimal.valueOf(100))
                .divide(limit, 2, java.math.RoundingMode.HALF_UP)
                .doubleValue();

        return buildDailyBudgetAlertResult(category, percentage, spent);
    }

    /**
     * Hàm helper nội bộ: Tìm kiếm đối tượng Budget theo các tiêu chí kết hợp của User, Category, Wallet, Tháng và Năm.
     */
    private Optional<Budget> findOptionalBudget(User user, Category category, Long walletId, Integer month, Integer year) {
        Wallet wallet = null;
        if (walletId != null) {
            wallet = walletRepository.findById(walletId).orElse(null);
        }
        if (wallet != null) {
            return budgetRepository.findByUserAndWalletAndCategoryAndMonthAndYear(user, wallet, category, month, year);
        } else {
            return budgetRepository.findByUserAndCategoryAndMonthAndYear(user, category, month, year);
        }
    }

    /**
     * Hàm helper nội bộ: Xây dựng đối tượng BudgetAlertDTO chứa thông điệp cảnh báo ngân sách tháng tương ứng với tỷ lệ phần trăm đã tiêu.
     */
    private BudgetAlertDTO buildBudgetAlertResult(Category category, Wallet wallet, double percentage, java.math.BigDecimal spent) {
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

    /**
     * Hàm helper nội bộ: Xây dựng đối tượng BudgetAlertDTO chứa thông điệp cảnh báo ngân sách ngày tương ứng với tỷ lệ phần trăm đã tiêu.
     */
    private BudgetAlertDTO buildDailyBudgetAlertResult(Category category, double percentage, java.math.BigDecimal spent) {
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

    /**
     * Kiểm tra cảnh báo vượt hạn mức chi tiêu ngày của ví theo ngày hiệu lực hiện tại của hệ thống.
     */
    @Override
    public BudgetAlertDTO checkDailyLimitAlert(String username, Long walletId) {
        return checkDailyLimitAlert(username, walletId, userService.getEffectiveDate(username));
    }

    /**
     * Kiểm tra cảnh báo vượt hạn mức chi tiêu ngày của ví theo một ngày cụ thể (Cảnh báo khi tiêu quá 100% hạn mức ngày).
     */
    @Override
    public BudgetAlertDTO checkDailyLimitAlert(String username, Long walletId, LocalDate date) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (walletId == null) return new BudgetAlertDTO(false, null, null, 0.0, java.math.BigDecimal.ZERO);
        
        Wallet wallet = walletRepository.findById(walletId).orElse(null);
        if (wallet == null) return new BudgetAlertDTO(false, null, null, 0.0, java.math.BigDecimal.ZERO);
        
        java.math.BigDecimal dailyLimit = getDailyLimitForWallet(username, walletId, date);
        if (dailyLimit == null || dailyLimit.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return new BudgetAlertDTO(false, null, null, 0.0, java.math.BigDecimal.ZERO);
        }
        
        java.math.BigDecimal todaySpent = transactionRepository.sumByUserAndWalletAndTypeAndDate(user, wallet, TransactionType.EXPENSE, date);
        if (todaySpent == null) todaySpent = java.math.BigDecimal.ZERO;
        
        double percentage = todaySpent.multiply(java.math.BigDecimal.valueOf(100))
                .divide(dailyLimit, 2, java.math.RoundingMode.HALF_UP)
                .doubleValue();

        if (percentage > 100) {
            return new BudgetAlertDTO(true, 
                "Cảnh báo: Bạn đã vượt hạn mức chi tiêu hôm nay cho ví " + wallet.getName() + " (" + String.format("%.0f", dailyLimit) + "đ)!", 
                "DANGER", percentage, todaySpent);
        }
        return new BudgetAlertDTO(false, null, "SUCCESS", percentage, todaySpent);
    }

    /**
     * Lấy hạn mức chi tiêu trong ngày của một ví tại một ngày cụ thể từ lịch sử thiết lập hạn mức (nếu không có thì lấy hạn mức mặc định của ví).
     */
    @Override
    public java.math.BigDecimal getDailyLimitForWallet(String username, Long walletId, LocalDate date) {
        User user = userRepository.findByUsername(username).orElse(null);
        Wallet wallet = walletRepository.findById(walletId).orElse(null);
        if (user == null || wallet == null) return java.math.BigDecimal.ZERO;

        // Tìm hạn mức có hiệu lực gần nhất tính đến ngày 'date' bằng JPQL tường minh
        List<java.math.BigDecimal> limits = dailySpendingLimitRepository.findHistoricalLimits(user, wallet, date, org.springframework.data.domain.PageRequest.of(0, 1));
        
        if (limits != null && !limits.isEmpty()) {
            return limits.get(0);
        }
        
        // Nếu hoàn toàn chưa bao giờ thiết lập trong bảng lịch sử, lấy từ trường mặc định của Wallet
        // Lưu ý: Trường này giờ đây chỉ đóng vai trò là "giá trị khởi tạo"
        return wallet.getDailySpendingLimit() != null ? wallet.getDailySpendingLimit() : java.math.BigDecimal.ZERO;
    }

    /**
     * Lưu thông tin thiết lập hạn mức chi tiêu ngày mới cho một ví cụ thể.
     */
    @Override
    public void saveDailyLimit(Long walletId, java.math.BigDecimal dailyLimit, String username) {
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
