package com.codegym.finance.controller.admin;
import com.codegym.finance.entity.transaction.Transaction;
import java.util.List;
import java.util.Map;
import com.codegym.finance.entity.user.User;

import com.codegym.finance.repository.transaction.TransactionRepository;
import com.codegym.finance.repository.user.UserRepository;
import com.codegym.finance.service.user.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private IUserService userService;

    @Autowired
    private com.codegym.finance.service.admin.AdminStatsService adminStatsService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Object> stats = adminStatsService.getDashboardStats();
        model.addAllAttributes(stats);
        
        model.addAttribute("growthData", adminStatsService.getUserGrowthData());
        model.addAttribute("segmentationData", adminStatsService.getUserSegmentationData());
        model.addAttribute("recentUsers", userRepository.findTop5ByOrderByCreatedAtDesc());
        
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/user-list";
    }

    @PostMapping("/users/toggle-status")
    public String toggleUserStatus(@RequestParam Long id, RedirectAttributes ra) {
        userService.toggleStatus(id);
        ra.addFlashAttribute("message", "Đã cập nhật trạng thái tài khoản!");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/toggle-premium")
    public String toggleUserPremium(@RequestParam Long id, RedirectAttributes ra) {
        userService.togglePremium(id);
        ra.addFlashAttribute("message", "Đã cập nhật gói dịch vụ!");
        return "redirect:/admin/users";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("segmentationData", adminStatsService.getUserSegmentationData());
        model.addAttribute("topCategories", adminStatsService.getTopSpendingCategories());
        model.addAttribute("cashflowData", adminStatsService.getCashflowComparisonData());
        return "admin/reports";
    }

    @GetMapping("/export/users")
    public void exportUsers(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=users_export.csv");
        
        PrintWriter writer = response.getWriter();
        // BOM for Excel Vietnamese support
        writer.write('\ufeff');
        writer.println("ID,Username,Full Name,Email,Premium,Status,Created At");
        
        List<User> users = adminStatsService.getAllUsersForExport();
        for (User u : users) {
            writer.println(String.format("%d,%s,%s,%s,%s,%s,%s",
                u.getId(), u.getUsername(), u.getFullName(), u.getEmail(),
                u.getPremium() ? "PREMIUM" : "FREE",
                u.getActive() ? "ACTIVE" : "BLOCKED",
                u.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        }
    }

    @GetMapping("/export/transactions")
    public void exportTransactions(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=transactions_export.csv");
        
        PrintWriter writer = response.getWriter();
        // BOM for Excel Vietnamese support
        writer.write('\ufeff');
        writer.println("ID,Date,Amount,Type,Category,User,Description");
        
        List<Transaction> transactions = adminStatsService.getAllTransactionsForExport();
        for (Transaction t : transactions) {
            writer.println(String.format("%d,%s,%.2f,%s,%s,%s,%s",
                t.getId(), t.getDate(), t.getAmount(), t.getType(),
                t.getCategory() != null ? t.getCategory().getName() : "N/A",
                t.getUser().getUsername(),
                t.getDescription() != null ? t.getDescription().replace(",", ";") : ""));
        }
    }
}
