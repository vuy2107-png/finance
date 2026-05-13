package com.codegym.finance.controller.admin;
import com.codegym.finance.entity.transaction.Transaction;
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
}
