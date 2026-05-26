package com.codegym.finance.controller.user;

import com.codegym.finance.entity.user.User;
import com.codegym.finance.service.transaction.ITransactionService;
import com.codegym.finance.service.user.IUserService;
import com.codegym.finance.service.wallet.IWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user/deposit")
public class UserDepositController {

    @Autowired
    private IUserService userService;

    @Autowired
    private ITransactionService transactionService;

    @Autowired
    private IWalletService walletService;

    @GetMapping
    public String showDepositPage(Authentication auth, Model model) {
        User user = userService.findByUsername(auth.getName());
        model.addAttribute("user", user);
        
        // Lấy lịch sử nạp tiền gần nhất (Lọc theo keyword SYSTEM_DEPOSIT_INFLOW)
        var deposits = transactionService.filterTransactions(auth.getName(), null, null, null, null, "SYSTEM_DEPOSIT_INFLOW", org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("date").descending()));
        model.addAttribute("deposits", deposits.getContent());
        
        return "user/deposit/index";
    }

    @PostMapping("/process")
    public String processDeposit(@RequestParam java.math.BigDecimal amount, Authentication auth, RedirectAttributes ra) {
        if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            ra.addFlashAttribute("error", "Số tiền nạp không hợp lệ!");
            return "redirect:/user/deposit";
        }

        User user = userService.findByUsername(auth.getName());
        java.math.BigDecimal currentBalance = (user.getBalance() != null) ? user.getBalance() : java.math.BigDecimal.ZERO;
        user.setBalance(currentBalance.add(amount));
        userService.save(user);

        // Tạo bản ghi giao dịch nạp tiền
        com.codegym.finance.entity.transaction.Transaction t = new com.codegym.finance.entity.transaction.Transaction();
        t.setUser(user);
        t.setAmount(amount);
        t.setType(com.codegym.finance.entity.transaction.TransactionType.INCOME);
        t.setDate(java.time.LocalDate.now());
        t.setDescription("SYSTEM_DEPOSIT_INFLOW: Nạp tiền vào tài khoản");
        
        transactionService.save(t, auth.getName());

        ra.addFlashAttribute("message", "Nạp tiền thành công! Đã cộng " + amount.setScale(0, java.math.RoundingMode.HALF_UP).toString() + "đ vào tài khoản.");
        return "redirect:/user/shop"; // Sau khi nạp xong dẫn ra shop để tiêu tiền luôn
    }
}
