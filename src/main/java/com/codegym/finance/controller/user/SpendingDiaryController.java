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

    @GetMapping
    public String list(@RequestParam(required = false) Integer month,
                       @RequestParam(required = false) Integer year,
                       Authentication auth, Model model) {
        String username = auth.getName();
        LocalDate now = userService.getEffectiveDate(username);
        
        if (month == null) month = now.getMonthValue();
        if (year == null) year = now.getYear();

        List<Map<String, Object>> dailyReport = transactionService.getDailySpendingReport(username, month, year);
        
        model.addAttribute("dailyReport", dailyReport);
        model.addAttribute("currentMonth", month);
        model.addAttribute("currentYear", year);
        model.addAttribute("user", userService.findByUsername(username));
        
        return "user/diary/list";
    }
}
