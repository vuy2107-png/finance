package com.codegym.finance.service.transaction;
import com.codegym.finance.entity.wallet.Wallet;
import com.codegym.finance.entity.category.Category;
import com.codegym.finance.entity.transaction.Transaction;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.entity.transaction.TransactionType;
import com.codegym.finance.entity.budget.Budget;


import com.codegym.finance.repository.transaction.TransactionRepository;
import com.codegym.finance.repository.user.UserRepository;
import com.codegym.finance.service.transaction.ITransactionService;
import com.codegym.finance.service.wallet.IWalletService;
import com.codegym.finance.repository.budget.BudgetRepository;
import com.codegym.finance.exception.SpendingLimitException;
import com.codegym.finance.service.budget.IBudgetService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private IWalletService walletService;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private com.codegym.finance.repository.wallet.WalletRepository walletRepository;

    @Autowired
    private com.codegym.finance.service.user.IUserService userService;

    @Autowired
    private IBudgetService budgetService;

    @Override
    public List<Transaction> findByUserName(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return transactionRepository.findByUserOrderByDateDescIdDesc(user);
    }
    
    @Override
    public Page<Transaction> findByUserName(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return transactionRepository.findByUserOrderByDateDescIdDesc(user, pageable);
    }

    @Override
    @Transactional
    public void save(Transaction transaction, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Prevent future dates
        LocalDate effectiveDate = userService.getEffectiveDate(username);
        if (transaction.getDate().isAfter(effectiveDate)) {
            throw new RuntimeException("Không thể tạo giao dịch cho ngày trong tương lai! (Hôm nay là " + 
                effectiveDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")");
        }
        
        transaction.setUser(user);
        
        // Update wallet balance FIRST to catch balance exception
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

        transactionRepository.save(transaction);
    }

    @Override
    public Transaction findById(Long id, String username) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        if (!transaction.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Không có quyền truy cập");
        }
        return transaction;
    }

    @Override
    @Transactional
    public void update(Transaction transaction, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Prevent future dates
        LocalDate effectiveDate = userService.getEffectiveDate(username);
        if (transaction.getDate().isAfter(effectiveDate)) {
            throw new RuntimeException("Không thể sửa giao dịch thành ngày trong tương lai! (Hôm nay là " + 
                effectiveDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")");
        }

        Transaction old = findById(transaction.getId(), username);
        
        // 1. Reverse old balance
        if (old.getType() == TransactionType.TRANSFER) {
            if (old.getWallet() != null && old.getToWallet() != null) {
                walletService.updateBalance(old.getWallet().getId(), old.getAmount(), true); // Add back to source
                walletService.updateBalance(old.getToWallet().getId(), old.getAmount(), false); // Deduct from destination
            }
        } else if (old.getWallet() != null) {
            boolean oldWasIncome = old.getType() == TransactionType.INCOME;
            walletService.updateBalance(old.getWallet().getId(), old.getAmount(), !oldWasIncome);
        }

        // 2. Update transaction data
        old.setAmount(transaction.getAmount());
        old.setDescription(transaction.getDescription());
        old.setDate(transaction.getDate());
        old.setType(transaction.getType());
        old.setCategory(transaction.getCategory());
        old.setWallet(transaction.getWallet());
        old.setToWallet(transaction.getToWallet());
        old.setLocation(transaction.getLocation());
        
        transactionRepository.save(old);

        // Check Daily Limit - We only WARN
        // Check Category Budget - We only WARN
        
        // 3. Apply new balance
        if (old.getType() == TransactionType.TRANSFER) {
            if (old.getWallet() != null && old.getToWallet() != null) {
                if (old.getWallet().getId().equals(old.getToWallet().getId())) {
                    throw new RuntimeException("Ví gửi và ví nhận không được trùng nhau");
                }
                walletService.updateBalance(old.getWallet().getId(), old.getAmount(), false); // Deduct from source
                walletService.updateBalance(old.getToWallet().getId(), old.getAmount(), true);  // Add to destination
            }
        } else if (old.getWallet() != null) {
            boolean isIncome = old.getType() == TransactionType.INCOME;
            walletService.updateBalance(old.getWallet().getId(), old.getAmount(), isIncome);
        }
    }

    @Override
    @Transactional
    public void delete(Long id, String username) {
        Transaction transaction = findById(id, username);
        
        // Reverse balance before deleting
        if (transaction.getType() == TransactionType.TRANSFER) {
            if (transaction.getWallet() != null && transaction.getToWallet() != null) {
                walletService.updateBalance(transaction.getWallet().getId(), transaction.getAmount(), true);
                walletService.updateBalance(transaction.getToWallet().getId(), transaction.getAmount(), false);
            }
        } else if (transaction.getWallet() != null) {
            boolean wasIncome = transaction.getType() == TransactionType.INCOME;
            walletService.updateBalance(transaction.getWallet().getId(), transaction.getAmount(), !wasIncome);
        }
        
        transactionRepository.delete(transaction);
    }

    @Override
    public double getTotalIncome(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Double result = transactionRepository.sumByUserAndType(user, TransactionType.INCOME);
        return result != null ? result : 0;
    }

    @Override
    public double getTotalExpense(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Double result = transactionRepository.sumByUserAndType(user, TransactionType.EXPENSE);
        return result != null ? result : 0;
    }

    @Override
    public double getTodayIncome(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Double result = transactionRepository.sumByUserAndTypeAndDate(user, TransactionType.INCOME, userService.getEffectiveDate(username));
        return result != null ? result : 0;
    }

    @Override
    public double getTodayExpense(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Double result = transactionRepository.sumByUserAndTypeAndDate(user, TransactionType.EXPENSE, userService.getEffectiveDate(username));
        return result != null ? result : 0;
    }

    @Override
    public double getTodayExpenseForWallet(String username, Long walletId) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Wallet wallet = walletService.findById(walletId, username);
        Double result = transactionRepository.sumByUserAndWalletAndTypeAndDate(user, wallet, TransactionType.EXPENSE, userService.getEffectiveDate(username));
        return result != null ? result : 0;
    }

    @Override
    public double getThisMonthIncome(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        LocalDate now = userService.getEffectiveDate(username);
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        Double result = transactionRepository.sumByUserAndTypeAndDateBetween(user, TransactionType.INCOME, startOfMonth, endOfMonth);
        return result != null ? result : 0;
    }

    @Override
    public double getThisMonthExpense(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        LocalDate now = userService.getEffectiveDate(username);
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        Double result = transactionRepository.sumByUserAndTypeAndDateBetween(user, TransactionType.EXPENSE, startOfMonth, endOfMonth);
        return result != null ? result : 0;
    }

    @Override
    public double getThisMonthExpenseForWallet(String username, Long walletId) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Wallet wallet = walletService.findById(walletId, username);
        LocalDate now = userService.getEffectiveDate(username);
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        Double result = transactionRepository.sumByUserAndWalletAndTypeAndDateBetween(user, wallet, TransactionType.EXPENSE, startOfMonth, endOfMonth);
        return result != null ? result : 0.0;
    }

    @Override
    public double getBalance(String username) {
        return walletService.findByUsername(username).stream()
                .mapToDouble(Wallet::getBalance)
                .sum();
    }

    @Override
    public int getTotalTransactions(String username) {
        return findByUserName(username).size();
    }

    @Override
    public Map<String, Double> getCategoryExpenses(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        LocalDate now = userService.getEffectiveDate(username);
        LocalDate start = now.withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(now.lengthOfMonth());

        List<Object[]> results = transactionRepository.sumAmountByCategory(user, start, end);
        Map<String, Double> chartData = new HashMap<>();
        for (Object[] res : results) {
            chartData.put(res[0] != null ? res[0].toString() : "Khác", (Double) res[1]);
        }
        return chartData;
    }

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
        
        Map<String, Double> incomeMap = new HashMap<>();
        Map<String, Double> expenseMap = new HashMap<>();

        for (Object[] res : results) {
            String label = res[0] + "/" + res[1]; // month/year
            TransactionType type = (TransactionType) res[2];
            double amount = (Double) res[3];
            if (type == TransactionType.INCOME) incomeMap.put(label, amount);
            else if (type == TransactionType.EXPENSE) expenseMap.put(label, amount);
        }

        List<Double> incomes = new ArrayList<>();
        List<Double> expenses = new ArrayList<>();

        for (String label : labels) {
            incomes.add(incomeMap.getOrDefault(label, 0.0));
            expenses.add(expenseMap.getOrDefault(label, 0.0));
        }

        Map<String, Object> finalData = new HashMap<>();
        finalData.put("labels", labels);
        finalData.put("income", incomes);
        finalData.put("expense", expenses);
        return finalData;
    }

    @Override
    public Map<String, Object> getLast7DaysTrend(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(6);

        List<Object[]> results = transactionRepository.getDailyStats(user, sevenDaysAgo);
        
        List<String> labels = new ArrayList<>();
        List<Double> incomes = new ArrayList<>();
        List<Double> expenses = new ArrayList<>();

        Map<String, Double> incomeMap = new HashMap<>();
        Map<String, Double> expenseMap = new HashMap<>();

        // Fill data
        for (Object[] res : results) {
            String label = res[0].toString(); // YYYY-MM-DD
            if (!labels.contains(label)) labels.add(label);

            TransactionType type = (TransactionType) res[1];
            double amount = (Double) res[2];
            if (type == TransactionType.INCOME) incomeMap.put(label, amount);
            else expenseMap.put(label, amount);
        }

        // Ensure all 7 days are present in labels (optional but good for consistency)
        // For simplicity, we'll just use the dates that have transactions or pad them
        for (String label : labels) {
            incomes.add(incomeMap.getOrDefault(label, 0.0));
            expenses.add(expenseMap.getOrDefault(label, 0.0));
        }

        Map<String, Object> finalData = new HashMap<>();
        finalData.put("labels", labels);
        finalData.put("income", incomes);
        finalData.put("expense", expenses);
        return finalData;
    }

    @Override
    public Map<String, Double> getCategorySummary(String username) {
        return getCategoryExpenses(username);
    }

    @Override
    public List<Transaction> getWalletHistory(Long walletId, String username) {
        Wallet wallet = walletService.findById(walletId, username);
        User user = userRepository.findByUsername(username).orElseThrow();
        return transactionRepository.findByWalletHistory(user, wallet);
    }

    @Override
    public Page<Transaction> filterTransactions(String username, LocalDate start, LocalDate end, Long walletId, Long categoryId, String keyword, Pageable pageable) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return transactionRepository.filterTransactions(user, start, end, walletId, categoryId, keyword, pageable);
    }

    @Override
    public boolean hasCompletedMonthlyFunding(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        LocalDate now = LocalDate.now();
        LocalDate start = now.with(java.time.temporal.TemporalAdjusters.firstDayOfMonth());
        LocalDate end = now.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
        return transactionRepository.countMonthlyFunding(user, start, end) > 0;
    }

    @Override
    public List<Map<String, Object>> getDailySpendingReport(String username, int month, int year) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return new java.util.ArrayList<>();
        
        List<Wallet> wallets = walletRepository.findByUserUsername(username);
        // Hạn mức giờ đây sẽ được tính cho từng ngày trong vòng lặp bên dưới

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate lastDayOfMonth = start.withDayOfMonth(start.lengthOfMonth());
        LocalDate today = userService.getEffectiveDate(username);
        if (today == null) today = LocalDate.now();
        
        LocalDate end = lastDayOfMonth;

        // Lấy tất cả chi tiêu trong tháng bằng 1 câu query duy nhất
        List<Object[]> monthlyExpenses = transactionRepository.getDailySpendingSum(user, com.codegym.finance.entity.transaction.TransactionType.EXPENSE, start, end);
        java.util.Map<LocalDate, Double> expenseMap = new java.util.HashMap<>();
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
                    Double val = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                    expenseMap.put(d, val);
                }
            }
        }

        // Tính toán thông tin cho gợi ý hạn mức tự động
        boolean isAutoSuggest = Boolean.TRUE.equals(user.getAutoSuggestDailyLimit());
        double currentTotalBalance = 0;
        if (wallets != null) {
            for (Wallet w : wallets) {
                currentTotalBalance += w.getBalance();
            }
        }
        
        LocalDate queryEnd = today.isBefore(lastDayOfMonth) ? today : lastDayOfMonth;
        double spentInPeriod = 0;
        if (!start.isAfter(queryEnd)) {
            Double sum = transactionRepository.sumByUserAndTypeAndDateBetween(user, com.codegym.finance.entity.transaction.TransactionType.EXPENSE, start, queryEnd);
            spentInPeriod = sum != null ? sum : 0.0;
        }
        double startingBalance = currentTotalBalance + spentInPeriod;
        double accumulatedSpent = 0;

        List<Map<String, Object>> report = new java.util.ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Double spent = expenseMap.getOrDefault(date, 0.0);

            // Tính hạn mức chi tiêu hàng ngày (Tự động gợi ý hoặc Tự đặt)
            double totalDailyLimitForDate = 0;
            if (isAutoSuggest) {
                double remainingDays = lastDayOfMonth.getDayOfMonth() - date.getDayOfMonth() + 1;
                double remainingBalance = startingBalance - accumulatedSpent;
                totalDailyLimitForDate = remainingDays > 0 ? (remainingBalance / remainingDays) : 0.0;
                if (totalDailyLimitForDate < 0) totalDailyLimitForDate = 0.0;
            } else {
                if (wallets != null) {
                    for (Wallet w : wallets) {
                        totalDailyLimitForDate += budgetService.getDailyLimitForWallet(username, w.getId(), date);
                    }
                }
            }

            Map<String, Object> day = new java.util.HashMap<>();
            day.put("date", date);
            day.put("spent", spent);
            day.put("limit", totalDailyLimitForDate);
            
            if (spent == 0) {
                day.put("percent", 0.0);
                day.put("status", "NONE");
            } else {
                if (totalDailyLimitForDate > 0) {
                    double percent = (spent / totalDailyLimitForDate) * 100;
                    day.put("percent", percent);
                    day.put("status", percent > 100 ? "DANGER" : "SUCCESS");
                } else {
                    day.put("percent", 100.0);
                    day.put("status", "DANGER");
                }
            }
            
            accumulatedSpent += spent;
            report.add(day);
        }
        
        return report;
    }
}

