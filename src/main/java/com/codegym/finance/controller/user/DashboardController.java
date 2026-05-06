package com.codegym.finance.controller.user;

import com.codegym.finance.service.ITransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @Autowired
    private ITransactionService transactionService;

     // DASHBOARD
     @GetMapping("/user/dashboard")
     public String dashboard(Model model, Authentication auth) {
         String username = auth.getName();

         model.addAttribute("income",
                 transactionService.getTotalIncome(username));

         model.addAttribute("expense",
                 transactionService.getTotalExpense(username));

         model.addAttribute("todayIncome",
                 transactionService.getTodayIncome(username));

         model.addAttribute("todayExpense",
                 transactionService.getTodayExpense(username));

         model.addAttribute("thisMonthIncome",
                 transactionService.getThisMonthIncome(username));

         model.addAttribute("thisMonthExpense",
                 transactionService.getThisMonthExpense(username));

         model.addAttribute("balance",
                 transactionService.getBalance(username));

         model.addAttribute("total",
                 transactionService.getTotalTransactions(username));

         return "user/dashboard";
     }
}
