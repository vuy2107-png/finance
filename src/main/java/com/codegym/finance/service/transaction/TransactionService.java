package com.codegym.finance.service.transaction;
import com.codegym.finance.entity.wallet.Wallet;
import com.codegym.finance.entity.category.Category;
import com.codegym.finance.entity.transaction.Transaction;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.entity.transaction.TransactionType;
import com.codegym.finance.entity.budget.Budget;

import com.codegym.finance.repository.transaction.TransactionRepository;
import com.codegym.finance.repository.user.UserRepository;
import com.codegym.finance.repository.wallet.WalletRepository;
import com.codegym.finance.repository.category.CategoryRepository;
import com.codegym.finance.service.transaction.ITransactionService;
import com.codegym.finance.service.wallet.IWalletService;
import com.codegym.finance.service.user.IUserService;
import com.codegym.finance.repository.budget.BudgetRepository;
import com.codegym.finance.exception.SpendingLimitException;
import com.codegym.finance.service.budget.IBudgetService;
import com.codegym.finance.service.budget.BudgetAlertDTO;
import com.codegym.finance.service.category.ICategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class TransactionService implements ITransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Lazy
    private IWalletService walletService;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    @Lazy
    private IUserService userService;

    @Autowired
    private IBudgetService budgetService;

    @Autowired
    private ICategoryService categoryService;

    /**
     * Tìm danh sách tất cả các giao dịch của người dùng theo tên đăng nhập (Sắp xếp theo ngày giảm dần và ID giảm dần).
     */
    @Override
    public List<Transaction> findByUserName(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return transactionRepository.findByUserOrderByDateDescIdDesc(user);
    }
    
    /**
     * Tìm danh sách tất cả các giao dịch của người dùng có hỗ trợ phân trang (Pageable).
     */
    @Override
    public Page<Transaction> findByUserName(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return transactionRepository.findByUserOrderByDateDescIdDesc(user, pageable);
    }

    /**
     * Tạo mới và lưu trữ một giao dịch của người dùng, đồng thời cập nhật số dư ví tương ứng.
     */
    @Override
    @Transactional
    public void save(Transaction transaction, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        validateTransactionDate(transaction, username);
        
        transaction.setUser(user);
        updateWalletBalances(transaction);
        transactionRepository.save(transaction);
    }

    /**
     * Tìm kiếm một giao dịch theo ID và xác nhận quyền sở hữu của người dùng.
     */
    @Override
    public Transaction findById(Long id, String username) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        if (!transaction.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Không có quyền truy cập");
        }
        return transaction;
    }

    /**
     * Cập nhật thông tin giao dịch, hoàn tác số dư cũ trên ví cũ và áp dụng số dư mới lên ví mới.
     */
    @Override
    @Transactional
    public void update(Transaction transaction, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        validateTransactionDate(transaction, username);

        Transaction old = findById(transaction.getId(), username);
        
        reverseWalletBalances(old);
        
        old.setAmount(transaction.getAmount());
        old.setDescription(transaction.getDescription());
        old.setDate(transaction.getDate());
        old.setType(transaction.getType());
        old.setCategory(transaction.getCategory());
        old.setWallet(transaction.getWallet());
        old.setToWallet(transaction.getToWallet());
        old.setLocation(transaction.getLocation());
        
        transactionRepository.save(old);
        updateWalletBalances(old);
    }

    @Override
    @Transactional
    public void delete(Long id, String username) {
        Transaction transaction = findById(id, username);
        if (transaction == null) {
            throw new RuntimeException("Giao dịch không tồn tại.");
        }

        // Kiểm tra khóa tháng
        java.time.LocalDate today = java.time.LocalDate.now();
        if (transaction.getDate().getMonthValue() != today.getMonthValue() || 
            transaction.getDate().getYear() != today.getYear()) {
            throw new RuntimeException("Không thể xóa giao dịch từ các tháng trước.");
        }

        reverseWalletBalances(transaction);
        transactionRepository.delete(transaction);
    }

    /**
     * Hàm helper nội bộ: Kiểm tra xem ngày giao dịch có nằm trong tương lai so với ngày hiệu lực hệ thống hay không.
     */
    private void validateTransactionDate(Transaction transaction, String username) {
        LocalDate effectiveDate = userService.getEffectiveDate(username);
        if (transaction.getDate().isAfter(effectiveDate)) {
            throw new RuntimeException("Không thể tạo hoặc sửa giao dịch cho ngày trong tương lai! (Hôm nay là " + 
                effectiveDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")");
        }
    }

    /**
     * Hàm helper nội bộ: Cập nhật tăng/giảm số dư của các ví liên quan dựa trên loại giao dịch (THU NHẬP, CHI TIÊU, CHUYỂN KHOẢN).
     */
    private void updateWalletBalances(Transaction transaction) {
        if (transaction.getType() == TransactionType.TRANSFER) {
            if (transaction.getWallet() != null && transaction.getToWallet() != null) {
                if (transaction.getWallet().getId().equals(transaction.getToWallet().getId())) {
                    throw new RuntimeException("Ví gửi và ví nhận không được trùng nhau");
                }
                walletService.updateBalance(transaction.getWallet().getId(), transaction.getAmount(), false);
                walletService.updateBalance(transaction.getToWallet().getId(), transaction.getAmount(), true);
            }
        } else if (transaction.getWallet() != null) {
            boolean isIncome = transaction.getType() == TransactionType.INCOME;
            walletService.updateBalance(transaction.getWallet().getId(), transaction.getAmount(), isIncome);
        }
    }

    /**
     * Hàm helper nội bộ: Hoàn trả lại số dư ví về trạng thái trước khi thực hiện giao dịch (được dùng khi sửa hoặc xóa giao dịch).
     */
    private void reverseWalletBalances(Transaction transaction) {
        if (transaction.getType() == TransactionType.TRANSFER) {
            if (transaction.getWallet() != null && transaction.getToWallet() != null) {
                walletService.updateBalance(transaction.getWallet().getId(), transaction.getAmount(), true);
                walletService.updateBalance(transaction.getToWallet().getId(), transaction.getAmount(), false);
            }
        } else if (transaction.getWallet() != null) {
            boolean wasIncome = transaction.getType() == TransactionType.INCOME;
            walletService.updateBalance(transaction.getWallet().getId(), transaction.getAmount(), !wasIncome);
        }
    }

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Tính tổng số tiền thu nhập của người dùng.
     */
    @Override
    public java.math.BigDecimal getTotalIncome(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        java.math.BigDecimal result = transactionRepository.sumByUserAndType(user, TransactionType.INCOME);
        return result != null ? result : java.math.BigDecimal.ZERO;
    }

    /**
     * Tính tổng số tiền chi tiêu của người dùng.
     */
    @Override
    public java.math.BigDecimal getTotalExpense(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        java.math.BigDecimal result = transactionRepository.sumByUserAndType(user, TransactionType.EXPENSE);
        return result != null ? result : java.math.BigDecimal.ZERO;
    }

    /**
     * Tính tổng số tiền thu nhập trong ngày hôm nay của người dùng.
     */
    @Override
    public java.math.BigDecimal getTodayIncome(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        java.math.BigDecimal result = transactionRepository.sumByUserAndTypeAndDate(user, TransactionType.INCOME, userService.getEffectiveDate(username));
        return result != null ? result : java.math.BigDecimal.ZERO;
    }

    /**
     * Tính tổng số tiền chi tiêu trong ngày hôm nay của người dùng.
     */
    @Override
    public java.math.BigDecimal getTodayExpense(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        java.math.BigDecimal result = transactionRepository.sumByUserAndTypeAndDate(user, TransactionType.EXPENSE, userService.getEffectiveDate(username));
        return result != null ? result : java.math.BigDecimal.ZERO;
    }

    /**
     * Tính tổng số tiền chi tiêu trong ngày hôm nay đối với một ví cụ thể.
     */
    @Override
    public java.math.BigDecimal getTodayExpenseForWallet(String username, Long walletId) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Wallet wallet = walletService.findById(walletId, username);
        java.math.BigDecimal result = transactionRepository.sumByUserAndWalletAndTypeAndDate(user, wallet, TransactionType.EXPENSE, userService.getEffectiveDate(username));
        return result != null ? result : java.math.BigDecimal.ZERO;
    }

    /**
     * Tính tổng số tiền thu nhập trong tháng này của người dùng.
     */
    @Override
    public java.math.BigDecimal getThisMonthIncome(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        LocalDate now = userService.getEffectiveDate(username);
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        java.math.BigDecimal result = transactionRepository.sumByUserAndTypeAndDateBetween(user, TransactionType.INCOME, startOfMonth, endOfMonth);
        return result != null ? result : java.math.BigDecimal.ZERO;
    }

    /**
     * Tính tổng số tiền chi tiêu trong tháng này của người dùng.
     */
    @Override
    public java.math.BigDecimal getThisMonthExpense(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        LocalDate now = userService.getEffectiveDate(username);
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        java.math.BigDecimal result = transactionRepository.sumByUserAndTypeAndDateBetween(user, TransactionType.EXPENSE, startOfMonth, endOfMonth);
        return result != null ? result : java.math.BigDecimal.ZERO;
    }

    /**
     * Tính tổng số tiền chi tiêu trong tháng này đối với một ví cụ thể.
     */
    @Override
    public java.math.BigDecimal getThisMonthExpenseForWallet(String username, Long walletId) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Wallet wallet = walletService.findById(walletId, username);
        LocalDate now = userService.getEffectiveDate(username);
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        java.math.BigDecimal result = transactionRepository.sumByUserAndWalletAndTypeAndDateBetween(user, wallet, TransactionType.EXPENSE, startOfMonth, endOfMonth);
        return result != null ? result : java.math.BigDecimal.ZERO;
    }

    /**
     * Tính tổng số dư của tất cả các ví thuộc sở hữu của người dùng.
     */
    @Override
    public java.math.BigDecimal getBalance(String username) {
        return walletService.findByUsername(username).stream()
                .map(Wallet::getBalance)
                .filter(Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    /**
     * Lấy tổng số lượng giao dịch của người dùng.
     */
    @Override
    public int getTotalTransactions(String username) {
        return findByUserName(username).size();
    }

    /**
     * Thống kê tổng số tiền chi tiêu của từng danh mục trong tháng hiện tại.
     */
    @Override
    public Map<String, java.math.BigDecimal> getCategoryExpenses(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        LocalDate now = userService.getEffectiveDate(username);
        LocalDate start = now.withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(now.lengthOfMonth());

        List<Object[]> results = transactionRepository.sumAmountByCategory(user, start, end);
        Map<String, java.math.BigDecimal> chartData = new HashMap<>();
        for (Object[] res : results) {
            chartData.put(res[0] != null ? res[0].toString() : "Khác", res[1] != null ? (java.math.BigDecimal) res[1] : java.math.BigDecimal.ZERO);
        }
        return chartData;
    }

    /**
     * Lấy xu hướng thu nhập và chi tiêu theo tháng trong vòng 6 tháng gần nhất.
     */
    @Override
    public Map<String, Object> getMonthlyTrend(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        
        // Prepare labels for the last 6 months
        List<String> labels = new ArrayList<>();
        List<LocalDate> monthStarts = new ArrayList<>();
        LocalDate now = userService.getEffectiveDate(username);
        for (int i = 5; i >= 0; i--) {
            LocalDate d = now.minusMonths(i);
            String label = d.getMonthValue() + "/" + d.getYear();
            labels.add(label);
            monthStarts.add(d.withDayOfMonth(1));
        }

        LocalDate startDate = monthStarts.get(0);
        List<Object[]> results = transactionRepository.getMonthlyStats(user, startDate);
        
        Map<String, java.math.BigDecimal> incomeMap = new HashMap<>();
        Map<String, java.math.BigDecimal> expenseMap = new HashMap<>();

        for (Object[] res : results) {
            String label = res[0] + "/" + res[1]; // month/year
            TransactionType type = (TransactionType) res[2];
            java.math.BigDecimal amount = res[3] != null ? (java.math.BigDecimal) res[3] : java.math.BigDecimal.ZERO;
            if (type == TransactionType.INCOME) incomeMap.put(label, amount);
            else if (type == TransactionType.EXPENSE) expenseMap.put(label, amount);
        }

        List<java.math.BigDecimal> incomes = new ArrayList<>();
        List<java.math.BigDecimal> expenses = new ArrayList<>();

        for (String label : labels) {
            incomes.add(incomeMap.getOrDefault(label, java.math.BigDecimal.ZERO));
            expenses.add(expenseMap.getOrDefault(label, java.math.BigDecimal.ZERO));
        }

        Map<String, Object> finalData = new HashMap<>();
        finalData.put("labels", labels);
        finalData.put("income", incomes);
        finalData.put("expense", expenses);
        return finalData;
    }

    /**
     * Lấy xu hướng thu nhập và chi tiêu hàng ngày trong vòng 7 ngày gần nhất.
     */
    @Override
    public Map<String, Object> getLast7DaysTrend(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(6);

        List<Object[]> results = transactionRepository.getDailyStats(user, sevenDaysAgo);
        
        List<String> labels = new ArrayList<>();
        List<java.math.BigDecimal> incomes = new ArrayList<>();
        List<java.math.BigDecimal> expenses = new ArrayList<>();

        Map<String, java.math.BigDecimal> incomeMap = new HashMap<>();
        Map<String, java.math.BigDecimal> expenseMap = new HashMap<>();

        // Fill data
        for (Object[] res : results) {
            String label = res[0].toString(); // YYYY-MM-DD
            if (!labels.contains(label)) labels.add(label);

            TransactionType type = (TransactionType) res[1];
            java.math.BigDecimal amount = res[2] != null ? (java.math.BigDecimal) res[2] : java.math.BigDecimal.ZERO;
            if (type == TransactionType.INCOME) incomeMap.put(label, amount);
            else expenseMap.put(label, amount);
        }

        // Ensure all 7 days are present in labels (optional but good for consistency)
        for (String label : labels) {
            incomes.add(incomeMap.getOrDefault(label, java.math.BigDecimal.ZERO));
            expenses.add(expenseMap.getOrDefault(label, java.math.BigDecimal.ZERO));
        }

        Map<String, Object> finalData = new HashMap<>();
        finalData.put("labels", labels);
        finalData.put("income", incomes);
        finalData.put("expense", expenses);
        return finalData;
    }

    /**
     * Lấy tóm tắt chi tiêu theo danh mục (tương tự như getCategoryExpenses).
     */
    @Override
    public Map<String, java.math.BigDecimal> getCategorySummary(String username) {
        return getCategoryExpenses(username);
    }

    /**
     * Lấy tổng chi tiêu của một ví vào một ngày cụ thể.
     */
    @Override
    public java.math.BigDecimal getSpentInWalletOnDate(String username, Long walletId, LocalDate date) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Wallet wallet = walletService.findById(walletId, username);
        java.math.BigDecimal result = transactionRepository.sumByUserAndWalletAndTypeAndDate(user, wallet, TransactionType.EXPENSE, date);
        return result != null ? result : java.math.BigDecimal.ZERO;
    }

    /**
     * Lấy tổng chi tiêu của một danh mục thuộc một ví cụ thể vào một ngày xác định.
     */
    @Override
    public java.math.BigDecimal getSpentByCategoryAndWalletOnDate(String username, Long categoryId, Long walletId, LocalDate date) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Wallet wallet = walletService.findById(walletId, username);
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        java.math.BigDecimal result = transactionRepository.sumByUserAndCategoryAndWalletAndDate(user, category, wallet, date);
        return result != null ? result : java.math.BigDecimal.ZERO;
    }

    /**
     * Lấy tổng chi tiêu của một danh mục vào một ngày xác định (tất cả các ví).
     */
    @Override
    public java.math.BigDecimal getSpentByCategoryOnDate(String username, Long categoryId, LocalDate date) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        java.math.BigDecimal result = transactionRepository.sumByUserAndCategoryAndDate(user, category, date);
        return result != null ? result : java.math.BigDecimal.ZERO;
    }

    /**
     * Lấy tổng số tiền giao dịch theo loại giao dịch và ngày xác định.
     */
    @Override
    public java.math.BigDecimal getSpentOnDate(String username, TransactionType type, LocalDate date) {
        User user = userRepository.findByUsername(username).orElseThrow();
        java.math.BigDecimal result = transactionRepository.sumByUserAndTypeAndDate(user, type, date);
        return result != null ? result : java.math.BigDecimal.ZERO;
    }

    /**
     * Lấy lịch sử giao dịch liên quan đến một ví cụ thể của người dùng.
     */
    @Override
    public List<Transaction> getWalletHistory(Long walletId, String username) {
        Wallet wallet = walletService.findById(walletId, username);
        User user = userRepository.findByUsername(username).orElseThrow();
        return transactionRepository.findByWalletHistory(user, wallet);
    }

    /**
     * Bộ lọc tìm kiếm và phân trang giao dịch theo ngày, ví, danh mục và từ khóa.
     */
    @Override
    public Page<Transaction> filterTransactions(String username, LocalDate start, LocalDate end, Long walletId, Long categoryId, String keyword, Pageable pageable) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return transactionRepository.filterTransactions(user, start, end, walletId, categoryId, keyword, pageable);
    }

    /**
     * Kiểm tra xem người dùng đã thực hiện giao dịch nạp tiền định kỳ trong tháng này chưa.
     */
    @Override
    public boolean hasCompletedMonthlyFunding(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        LocalDate now = LocalDate.now();
        LocalDate start = now.with(java.time.temporal.TemporalAdjusters.firstDayOfMonth());
        LocalDate end = now.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
        return transactionRepository.countMonthlyFunding(user, start, end) > 0;
    }

    /**
     * Tạo báo cáo chi tiết về tình hình chi tiêu và hạn mức chi tiêu cho từng ngày trong tháng được chọn.
     */
    @Override
    public List<Map<String, Object>> getDailySpendingReport(String username, int month, int year) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return new java.util.ArrayList<>();
        
        List<Wallet> wallets = walletRepository.findByUserUsername(username);
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate lastDayOfMonth = start.withDayOfMonth(start.lengthOfMonth());
        LocalDate today = userService.getEffectiveDate(username);
        if (today == null) today = LocalDate.now();
        LocalDate end = lastDayOfMonth;

        List<Object[]> monthlyExpenses = transactionRepository.getDailySpendingSum(user, TransactionType.EXPENSE, start, end);
        Map<LocalDate, java.math.BigDecimal> expenseMap = parseExpensesList(monthlyExpenses);

        boolean isAutoSuggest = Boolean.TRUE.equals(user.getAutoSuggestDailyLimit());
        java.math.BigDecimal startingBalance = computeStartingBalance(user, wallets, start, today, lastDayOfMonth);
        java.math.BigDecimal accumulatedSpent = java.math.BigDecimal.ZERO;

        List<Map<String, Object>> report = new java.util.ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            java.math.BigDecimal spent = expenseMap.getOrDefault(date, java.math.BigDecimal.ZERO);
            java.math.BigDecimal totalDailyLimitForDate = determineDailyLimit(date, isAutoSuggest, startingBalance, accumulatedSpent, wallets, username, lastDayOfMonth);

            Map<String, Object> day = createDailyReportEntry(date, spent, totalDailyLimitForDate);
            accumulatedSpent = accumulatedSpent.add(spent);
            report.add(day);
        }
        
        return report;
    }

    /**
     * Hàm helper nội bộ: Chuyển đổi dữ liệu thô từ truy vấn cơ sở dữ liệu thành bản đồ chi tiêu theo ngày.
     */
    private Map<LocalDate, java.math.BigDecimal> parseExpensesList(List<Object[]> monthlyExpenses) {
        Map<LocalDate, java.math.BigDecimal> expenseMap = new java.util.HashMap<>();
        for (Object[] row : monthlyExpenses) {
            if (row != null && row.length >= 2 && row[0] != null) {
                Object dateObj = row[0];
                LocalDate d = null;
                if (dateObj instanceof java.time.LocalDate) {
                    d = (java.time.LocalDate) dateObj;
                } else if (dateObj instanceof java.sql.Date) {
                    d = ((java.sql.Date) dateObj).toLocalDate();
                } else if (dateObj instanceof java.util.Date) {
                    d = new java.sql.Date(((java.util.Date) dateObj).getTime()).toLocalDate();
                }
                
                if (d != null) {
                    java.math.BigDecimal val = row[1] != null ? (java.math.BigDecimal) row[1] : java.math.BigDecimal.ZERO;
                    expenseMap.put(d, val);
                }
            }
        }
        return expenseMap;
    }

    /**
     * Hàm helper nội bộ: Tính toán số dư ban đầu tại thời điểm bắt đầu tháng để phục vụ việc tính toán hạn mức tự động đề xuất.
     */
    private java.math.BigDecimal computeStartingBalance(User user, List<Wallet> wallets, LocalDate start, LocalDate today, LocalDate lastDayOfMonth) {
        java.math.BigDecimal currentTotalBalance = java.math.BigDecimal.ZERO;
        if (wallets != null) {
            for (Wallet w : wallets) {
                if (w.getBalance() != null) {
                    currentTotalBalance = currentTotalBalance.add(w.getBalance());
                }
            }
        }
        
        LocalDate queryEnd = today.isBefore(lastDayOfMonth) ? today : lastDayOfMonth;
        java.math.BigDecimal spentInPeriod = java.math.BigDecimal.ZERO;
        if (!start.isAfter(queryEnd)) {
            java.math.BigDecimal sum = transactionRepository.sumByUserAndTypeAndDateBetween(user, TransactionType.EXPENSE, start, queryEnd);
            if (sum != null) {
                spentInPeriod = sum;
            }
        }
        return currentTotalBalance.add(spentInPeriod);
    }

    /**
     * Hàm helper nội bộ: Xác định hạn mức chi tiêu cho một ngày cụ thể (tự động đề xuất hoặc lấy từ thiết lập hạn mức thủ công).
     */
    private java.math.BigDecimal determineDailyLimit(LocalDate date, boolean isAutoSuggest, java.math.BigDecimal startingBalance, java.math.BigDecimal accumulatedSpent, List<Wallet> wallets, String username, LocalDate lastDayOfMonth) {
        java.math.BigDecimal totalDailyLimitForDate = java.math.BigDecimal.ZERO;
        if (isAutoSuggest) {
            double remainingDays = lastDayOfMonth.getDayOfMonth() - date.getDayOfMonth() + 1;
            java.math.BigDecimal remainingBalance = startingBalance.subtract(accumulatedSpent);
            if (remainingDays > 0) {
                totalDailyLimitForDate = remainingBalance.divide(java.math.BigDecimal.valueOf(remainingDays), 2, java.math.RoundingMode.HALF_UP);
            }
            if (totalDailyLimitForDate.compareTo(java.math.BigDecimal.ZERO) < 0) {
                totalDailyLimitForDate = java.math.BigDecimal.ZERO;
            }
        } else {
            if (wallets != null) {
                for (Wallet w : wallets) {
                    java.math.BigDecimal limitVal = budgetService.getDailyLimitForWallet(username, w.getId(), date);
                    if (limitVal != null) {
                        totalDailyLimitForDate = totalDailyLimitForDate.add(limitVal);
                    }
                }
            }
        }
        return totalDailyLimitForDate;
    }

    /**
     * Hàm helper nội bộ: Đóng gói thông tin chi tiêu, hạn mức và trạng thái vượt hạn mức của ngày thành đối tượng Map.
     */
    private Map<String, Object> createDailyReportEntry(LocalDate date, java.math.BigDecimal spent, java.math.BigDecimal limit) {
        Map<String, Object> day = new java.util.HashMap<>();
        day.put("date", date);
        day.put("spent", spent);
        day.put("limit", limit);
        
        if (spent.compareTo(java.math.BigDecimal.ZERO) == 0) {
            day.put("percent", 0.0);
            day.put("status", "NONE");
        } else {
            if (limit.compareTo(java.math.BigDecimal.ZERO) > 0) {
                double percent = spent.multiply(java.math.BigDecimal.valueOf(100))
                        .divide(limit, 2, java.math.RoundingMode.HALF_UP)
                        .doubleValue();
                day.put("percent", percent);
                day.put("status", percent > 100 ? "DANGER" : "SUCCESS");
            } else {
                day.put("percent", 100.0);
                day.put("status", "DANGER");
            }
        }
        return day;
    }

    @Override
    @Transactional
    public BudgetAlertDTO createTransaction(Transaction transaction, String categoryName, String username) {
        LocalDate today = userService.getEffectiveDate(username);
        if (transaction.getDate().isAfter(today)) {
            throw new RuntimeException("Không thể thêm giao dịch cho ngày trong tương lai! (Hôm nay là " + 
                today.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")");
        }

        if (categoryName != null && !categoryName.trim().isEmpty()) {
            transaction.setCategory(categoryService.getOrCreateCategory(categoryName, transaction.getType(), username));
        }
        
        save(transaction, username);

        if (transaction.getCategory() != null) {
            Long walletId = (transaction.getWallet() != null) ? transaction.getWallet().getId() : null;
            BudgetAlertDTO alert = budgetService.checkBudgetAlert(
                    username, 
                    transaction.getCategory().getId(), 
                    transaction.getDate().getMonthValue(), 
                    transaction.getDate().getYear(),
                    walletId
            );
            
            BudgetAlertDTO dailyAlert = budgetService.checkDailyLimitAlert(username, walletId);
            
            if (dailyAlert.isAlert()) {
                return dailyAlert;
            } else if (alert.isAlert()) {
                return alert;
            } else {
                return new BudgetAlertDTO(false, "Tuyệt vời! Bạn vẫn đang quản lý ngân sách rất tốt. ✅", null, null, null);
            }
        } else {
            return new BudgetAlertDTO(false, "Đã thêm giao dịch thành công! ✅", null, null, null);
        }
    }

    @Override
    @Transactional
    public BudgetAlertDTO updateTransaction(Transaction transaction, String categoryName, String username) {
        Transaction existing = findById(transaction.getId(), username);
        if (existing == null) {
            throw new RuntimeException("Giao dịch không tồn tại.");
        }

        LocalDate today = userService.getEffectiveDate(username);
        if (transaction.getDate().isAfter(today)) {
            throw new RuntimeException("Không thể sửa giao dịch thành ngày trong tương lai!");
        }

        if (categoryName != null && !categoryName.trim().isEmpty()) {
            transaction.setCategory(categoryService.getOrCreateCategory(categoryName, transaction.getType(), username));
        }
        
        update(transaction, username);

        if (transaction.getCategory() != null) {
            Long walletId = (transaction.getWallet() != null) ? transaction.getWallet().getId() : null;
            BudgetAlertDTO alert = budgetService.checkBudgetAlert(
                    username, 
                    transaction.getCategory().getId(), 
                    transaction.getDate().getMonthValue(), 
                    transaction.getDate().getYear(),
                    walletId
            );
            
            BudgetAlertDTO dailyAlert = budgetService.checkDailyLimitAlert(username, walletId);
            
            if (dailyAlert.isAlert()) {
                dailyAlert.setMessage("Cập nhật thành công. " + dailyAlert.getMessage());
                return dailyAlert;
            } else if (alert.isAlert()) {
                alert.setMessage("Cập nhật thành công. " + alert.getMessage());
                return alert;
            } else {
                return new BudgetAlertDTO(false, "Đã cập nhật giao dịch thành công! Bạn vẫn đang quản lý ngân sách rất tốt. ✅", null, null, null);
            }
        } else {
            return new BudgetAlertDTO(false, "Đã cập nhật giao dịch thành công! ✅", null, null, null);
        }
    }

    @Override
    @Transactional
    public int batchFund(Map<Long, java.math.BigDecimal> amounts, Map<Long, String> descriptions, String username) {
        int count = 0;
        for (Map.Entry<Long, java.math.BigDecimal> entry : amounts.entrySet()) {
            Long walletId = entry.getKey();
            java.math.BigDecimal amount = entry.getValue();
            if (amount != null && amount.compareTo(java.math.BigDecimal.ZERO) > 0) {
                Wallet wallet = walletService.findById(walletId, username);
                if (wallet != null) {
                    Transaction t = new Transaction();
                    t.setAmount(amount);
                    t.setType(TransactionType.INCOME);
                    t.setDate(LocalDate.now());
                    t.setWallet(wallet);
                    t.setDescription(descriptions.get(walletId));
                    
                    save(t, username);
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    @Transactional
    public void fund(Long walletId, java.math.BigDecimal amount, String description, String username) {
        Wallet wallet = walletService.findById(walletId, username);
        if (wallet == null) {
            throw new RuntimeException("Không tìm thấy ví hoặc ví không thuộc về bạn");
        }
        if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền cấp vốn không hợp lệ!");
        }
        Transaction t = new Transaction();
        t.setAmount(amount);
        t.setType(TransactionType.INCOME);
        t.setDate(LocalDate.now());
        t.setWallet(wallet);
        t.setDescription(description != null && !description.isEmpty() ? description : "Cấp vốn bổ sung");
        
        save(t, username);
    }
}

