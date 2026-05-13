package com.codegym.finance.controller;
import com.codegym.finance.entity.wallet.Wallet;
import com.codegym.finance.entity.category.Category;
import com.codegym.finance.entity.transaction.Transaction;

import com.codegym.finance.service.category.ICategoryService;
import com.codegym.finance.service.user.IUserService;
import com.codegym.finance.service.wallet.IWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
import com.codegym.finance.entity.user.User;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private IWalletService walletService;

    @Autowired
    private IUserService userService;

    @Autowired
    private com.codegym.finance.service.transaction.ITransactionService transactionService;

    @ModelAttribute
    public void addGlobalAttributes(Model model, Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            try {
                model.addAttribute("categories", categoryService.findByUserName(username));
                model.addAttribute("wallets", walletService.findByUsername(username));
                
                User user = userService.findByUsername(username);
                model.addAttribute("user", user);
                model.addAttribute("showWelcomeTour", user != null && (user.getHasSeenTour() == null || !user.getHasSeenTour()));

                // Logic cảnh báo cuối tháng
                LocalDate today = LocalDate.now();
                model.addAttribute("currentDate", today);
                model.addAttribute("currentMonth", today.getMonthValue());
                model.addAttribute("currentYear", today.getYear());
                
                LocalDate firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());
                
                model.addAttribute("minDate", firstDayOfMonth);
                model.addAttribute("maxDate", lastDayOfMonth);
                
                // Kiểm tra xem đã cấp vốn tháng này chưa
                boolean hasFunded = transactionService.hasCompletedMonthlyFunding(username);
                
                // Banner sẽ hiện nếu: (Đầu tháng HOẶC người dùng mới) VÀ chưa cấp vốn
                boolean isNewUser = user != null && user.getCreatedAt() != null && 
                                   user.getCreatedAt().isAfter(LocalDateTime.now().minusDays(3));
                boolean isStartOfMonth = (today.getDayOfMonth() <= 7 || isNewUser) && !hasFunded;
                
                model.addAttribute("isStartOfMonth", isStartOfMonth);
                model.addAttribute("monthName", today.getMonthValue() + "/" + today.getYear());
                model.addAttribute("newWallet", new com.codegym.finance.entity.wallet.Wallet());
                
                boolean isNearEndOfMonth = today.isAfter(lastDayOfMonth.minusDays(5)) || today.isEqual(lastDayOfMonth);
                model.addAttribute("isNearEndOfMonth", isNearEndOfMonth);
                if (isNearEndOfMonth) {
                    model.addAttribute("endOfMonthMessage", "Sắp đến cuối tháng! Vui lòng kiểm tra kỹ các giao dịch. Sau ngày " + 
                        lastDayOfMonth.format(DateTimeFormatter.ofPattern("dd/MM")) + " bạn sẽ không thể sửa đổi dữ liệu tháng này.");
                }
            } catch (Exception e) {
                // Ignore if services fail
            }
        }
    }
}

