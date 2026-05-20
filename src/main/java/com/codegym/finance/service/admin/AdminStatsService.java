package com.codegym.finance.service.admin;

import com.codegym.finance.repository.transaction.TransactionRepository;
import com.codegym.finance.repository.user.UserRepository;
import com.codegym.finance.repository.wallet.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.session.SessionRegistry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.entity.transaction.Transaction;
import jakarta.annotation.PostConstruct;

@Service
public class AdminStatsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private SessionRegistry sessionRegistry;

    @PostConstruct
    @Transactional
    public void init() {
        // Khởi tạo nếu cần
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalUsers = userRepository.count();
        long premiumUsers = userRepository.countByPremium(true);
        long freeUsers = totalUsers - premiumUsers;
        
        // Trạng thái người dùng REAL-TIME (Dựa trên Session thực tế)
        long onlineUsers = sessionRegistry.getAllPrincipals().stream()
                .filter(p -> !sessionRegistry.getAllSessions(p, false).isEmpty())
                .count();
        
        long blockedUsers = userRepository.countByActive(false);
        long offlineUsers = totalUsers - (onlineUsers + blockedUsers);
        if (offlineUsers < 0) offlineUsers = 0;
        
        // Chỉ số tài chính (TIỀN THỰC)
        Double totalUserBalances = userRepository.sumAllBalances();
        if (totalUserBalances == null) totalUserBalances = 0.0;
        
        // 1. DOANH THU THỰC (Từ việc bán Premium)
        Double totalRevenue = transactionRepository.sumTransactionsByDescriptionLike("Premium");
        if (totalRevenue == null) totalRevenue = 0.0;

        // 2. TỔNG NẠP THỰC TẾ (Từ các giao dịch nạp tiền)
        Double totalDeposits = transactionRepository.sumTransactionsByDescriptionLike("SYSTEM_DEPOSIT_INFLOW");
        if (totalDeposits == null) totalDeposits = 0.0;

        stats.put("totalUsers", totalUsers);
        stats.put("premiumUsers", premiumUsers);
        stats.put("freeUsers", freeUsers);
        stats.put("onlineUsers", onlineUsers);
        stats.put("offlineUsers", offlineUsers);
        stats.put("blockedUsers", blockedUsers);
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalDeposits", totalDeposits);
        stats.put("totalUserBalances", totalUserBalances);
        
        return stats;
    }

    public Map<String, Long> getUserGrowthData() {
        Map<String, Long> growthData = new TreeMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            long count = userRepository.countByCreatedAtBetween(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
            growthData.put(date.toString(), count);
        }
        return growthData;
    }

    public Map<String, Long> getUserSegmentationData() {
        Map<String, Long> segmentation = new LinkedHashMap<>();
        long total = userRepository.count();
        long starter = userRepository.countByPremiumPlan("weekly");
        long pro = userRepository.countByPremiumPlan("monthly");
        long elite = userRepository.countByPremiumPlan("yearly");
        long free = total - (starter + pro + elite);

        segmentation.put("Free", free);
        segmentation.put("Starter", starter);
        segmentation.put("Pro", pro);
        segmentation.put("Elite", elite);
        return segmentation;
    }

    public Map<String, Double> getTopSpendingCategories() {
        return new HashMap<>(); 
    }

    public List<Map<String, Object>> getCashflowComparisonData() {
        LocalDate sixMonthsAgo = LocalDate.now().withDayOfMonth(1).minusMonths(5);
        List<Object[]> results = transactionRepository.getSystemMonthlyCashflow(sixMonthsAgo);
        
        List<Map<String, Object>> cashflowData = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate monthDate = LocalDate.now().minusMonths(i);
            int year = monthDate.getYear();
            int month = monthDate.getMonthValue();
            
            Object[] foundRow = results.stream()
                .filter(r -> ((Number)r[0]).intValue() == year && ((Number)r[1]).intValue() == month)
                .findFirst()
                .orElse(new Object[]{year, month, 0.0, 0.0});
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("label", month + "/" + year);
            monthData.put("deposits", foundRow[2]);
            monthData.put("revenue", foundRow[3]);
            cashflowData.add(monthData);
        }
        return cashflowData;
    }

    public List<User> getAllUsersForExport() {
        return userRepository.findAll();
    }

    public List<Transaction> getAllTransactionsForExport() {
        return transactionRepository.findAll();
    }
}
