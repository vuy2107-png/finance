package com.codegym.finance.service.admin;

import com.codegym.finance.repository.transaction.TransactionRepository;
import com.codegym.finance.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
public class AdminStatsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalUsers = userRepository.count();
        long premiumUsers = userRepository.countByPremium(true);
        long activeUsers = userRepository.countByActive(true);
        
        // Người dùng mới hôm nay
        long newUsersToday = userRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay());
        
        // Số giao dịch hôm nay (Chỉ tính giao dịch chi tiêu/thu nhập thông thường, không tính nạp tiền)
        long transactionsToday = transactionRepository.countAllByDate(LocalDate.now());
        
        // 1. DOANH THU THỰC (Từ việc bán Premium)
        Double totalRevenue = transactionRepository.sumTransactionsByDescriptionLike("Premium");
        if (totalRevenue == null) totalRevenue = 0.0;

        // 2. TỔNG NẠP THỰC TẾ (Từ các giao dịch nạp tiền)
        Double totalDeposits = transactionRepository.sumTransactionsByDescriptionLike("SYSTEM_DEPOSIT_INFLOW");
        if (totalDeposits == null) totalDeposits = 0.0;

        // 3. TIỀN KHÁCH ĐANG GIỮ (Tiền khách đã nạp - Tiền khách đã tiêu cho mình)
        double actualAssets = totalDeposits - totalRevenue;
        if (actualAssets < 0) actualAssets = 0.0; // Tránh số âm nếu dùng tiền khuyến mãi cũ

        stats.put("totalUsers", totalUsers);
        stats.put("premiumUsers", premiumUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("newUsersToday", newUsersToday);
        stats.put("transactionsToday", transactionsToday);
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalDeposits", totalDeposits);
        stats.put("actualAssets", actualAssets);
        
        // Thống kê theo gói
        stats.put("starterUsers", userRepository.countByPremiumPlan("weekly"));
        stats.put("proUsers", userRepository.countByPremiumPlan("monthly"));
        stats.put("eliteUsers", userRepository.countByPremiumPlan("yearly"));

        return stats;
    }

    public Map<String, Long> getUserGrowthData() {
        // Lấy dữ liệu tăng trưởng 7 ngày gần nhất
        Map<String, Long> growthData = new TreeMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            long count = userRepository.countByCreatedAtBetween(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
            growthData.put(date.toString(), count);
        }
        return growthData;
    }
}
