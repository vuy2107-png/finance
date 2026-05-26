package com.codegym.finance.controller.user;

import com.codegym.finance.entity.user.User;
import com.codegym.finance.service.transaction.ITransactionService;
import com.codegym.finance.service.user.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.codegym.finance.entity.transaction.Transaction;
import com.codegym.finance.repository.transaction.TransactionRepository;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user/diary")
public class SpendingDiaryController {

    @Autowired
    private ITransactionService transactionService;

    @Autowired
    private IUserService userService;

    @Autowired
    private TransactionRepository transactionRepository;

    @GetMapping
    public String list(@RequestParam(required = false) Integer month,
                       @RequestParam(required = false) Integer year,
                       Authentication auth, Model model) {
        String username = auth.getName();
        LocalDate now = userService.getEffectiveDate(username);
        
        if (month == null) month = now.getMonthValue();
        if (year == null) year = now.getYear();

        List<Map<String, Object>> dailyReport = transactionService.getDailySpendingReport(username, month, year);
        
        // Tính toán số ngày kỷ luật tốt
        long successDays = dailyReport.stream()
                .filter(day -> "SUCCESS".equals(day.get("status")))
                .count();

        // Tính toán số ngày thực tế có chi tiêu
        long totalSpentDays = dailyReport.stream()
                .filter(day -> day.get("spent") != null && ((Number) day.get("spent")).doubleValue() > 0)
                .count();

        // Xây dựng cấu trúc ô lịch (calendarCells) phẳng để Thymeleaf dễ dàng lặp qua
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int startPadding = firstDay.getDayOfWeek().getValue() - 1; // Thứ 2 = 1, nên số ngày đệm = getValue() - 1
        int totalDays = firstDay.lengthOfMonth();
        int endPadding = (7 - ((startPadding + totalDays) % 7)) % 7;

        List<Map<String, Object>> calendarCells = new java.util.ArrayList<>();
        // Ô đệm ở đầu tháng
        for (int i = 0; i < startPadding; i++) {
            Map<String, Object> cell = new java.util.HashMap<>();
            cell.put("isPadding", true);
            calendarCells.add(cell);
        }
        // Các ngày thực tế trong tháng
        for (Map<String, Object> day : dailyReport) {
            day.put("isPadding", false);
            calendarCells.add(day);
        }
        // Ô đệm ở cuối tháng
        for (int i = 0; i < endPadding; i++) {
            Map<String, Object> cell = new java.util.HashMap<>();
            cell.put("isPadding", true);
            calendarCells.add(cell);
        }

        model.addAttribute("calendarCells", calendarCells);
        model.addAttribute("successDays", successDays);
        model.addAttribute("totalSpentDays", totalSpentDays);
        model.addAttribute("currentMonth", month);
        model.addAttribute("currentYear", year);
        model.addAttribute("today", now);
        model.addAttribute("user", userService.findByUsername(username));
        
        return "user/diary/list";
    }

    @GetMapping("/api/transactions")
    @ResponseBody
    public List<Map<String, Object>> getTransactionsByDate(
            @RequestParam String date,
            Authentication auth) {
        String username = auth.getName();
        LocalDate localDate = LocalDate.parse(date);
        
        User user = userService.findByUsername(username);
        List<Transaction> list = transactionRepository.filterTransactions(
                user, localDate, localDate, null, null, null, PageRequest.of(0, 100)).getContent();
                
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Transaction t : list) {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", t.getId());
            map.put("amount", t.getAmount());
            map.put("type", t.getType().name());
            map.put("description", t.getDescription());
            map.put("categoryName", t.getCategory() != null ? t.getCategory().getName() : "Khác");
            map.put("categoryColor", t.getCategory() != null ? t.getCategory().getColorCode() : "#64748b");
            map.put("categoryIcon", t.getCategory() != null ? t.getCategory().getIcon() : "fas fa-tag");
            map.put("walletName", t.getWallet() != null ? t.getWallet().getName() : "");
            map.put("toWalletName", t.getToWallet() != null ? t.getToWallet().getName() : "");
            result.add(map);
        }
        return result;
    }
}
