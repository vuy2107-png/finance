package com.codegym.finance.controller.user;
import com.codegym.finance.entity.category.Category;

import com.codegym.finance.entity.budget.Budget;
import com.codegym.finance.entity.transaction.Transaction;
import com.codegym.finance.entity.transaction.TransactionType;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.entity.wallet.Wallet;
import com.codegym.finance.service.budget.BudgetAlertDTO;
import com.codegym.finance.service.budget.IBudgetService;
import com.codegym.finance.service.category.ICategoryService;
import com.codegym.finance.service.user.IUserService;
import com.codegym.finance.service.transaction.ITransactionService;
import com.codegym.finance.service.wallet.IWalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.codegym.finance.exception.SpendingLimitException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
public class TransactionController {

    @Autowired
    private ITransactionService transactionService;

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private IBudgetService budgetService;

    @Autowired
    private IWalletService walletService;

    @Autowired
    private IUserService userService;

    // TRANSACTIONS LIST
    @GetMapping("/transactions")
    public String list(Model model, Authentication auth,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                       @RequestParam(required = false) Long walletId,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) String keyword,
                       @PageableDefault(size = 10, sort = {"date", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        String username = auth.getName();
        
        Page<Transaction> transactionPage = transactionService.filterTransactions(username, startDate, endDate, walletId, categoryId, keyword, pageable);
        
        // Group transactions by date for intuitive UI
        Map<LocalDate, List<Transaction>> groupedTransactions = transactionPage.getContent().stream()
                .collect(Collectors.groupingBy(
                    Transaction::getDate, 
                    LinkedHashMap::new,
                    Collectors.toList()
                ));
        
        model.addAttribute("groupedTransactions", groupedTransactions);
        model.addAttribute("page", transactionPage);
        model.addAttribute("categories", categoryService.findByUserName(username));
        model.addAttribute("wallets", walletService.findByUsername(username));
        
        // Preserve filter values in the UI
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("walletId", walletId);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("keyword", keyword);
        
        return "user/transaction/list";
    }

    @GetMapping("/transactions/create")
    public String showCreateForm(@RequestParam(required = false) Long walletId, Model model, Authentication auth) {
        String username = auth.getName();
        Transaction transaction = new Transaction();
        transaction.setDate(LocalDate.now());
        
        if (walletId != null) {
            try {
                transaction.setWallet(walletService.findById(walletId, username));
            } catch (Exception e) {
                // Ignore if wallet not found or doesn't belong to user
            }
        }
        
        model.addAttribute("transaction", transaction);
        model.addAttribute("categories", categoryService.findByUserName(username));
        model.addAttribute("wallets", walletService.findByUsername(username));
        return "user/transaction/create";
    }

    @GetMapping("/transactions/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, Authentication auth) {
        String username = auth.getName();
        model.addAttribute("transaction", transactionService.findById(id, username));
        model.addAttribute("categories", categoryService.findByUserName(username));
        model.addAttribute("wallets", walletService.findByUsername(username));
        return "user/transaction/edit";
    }

    // CREATE SAVE
    @PostMapping("/transactions/create")
    public Object create(@Valid @ModelAttribute Transaction transaction,
                         @RequestParam(required = false) String categoryName,
                         BindingResult result, Model model, Authentication auth,
                         RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {
        if (result.hasErrors()) {
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                return "redirect:/user/transactions/error-ajax";
            }
            model.addAttribute("categories", categoryService.findByUserName(auth.getName()));
            model.addAttribute("wallets", walletService.findByUsername(auth.getName()));
            return "user/transaction/create";
        }
        try {
            BudgetAlertDTO alert = transactionService.createTransaction(transaction, categoryName, auth.getName());
            if (alert != null) {
                if (alert.isAlert()) {
                    redirectAttributes.addFlashAttribute("budgetAlert", alert);
                }
                redirectAttributes.addFlashAttribute("message", alert.getMessage());
            }
        } catch (SpendingLimitException e) {
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.findByUserName(auth.getName()));
            model.addAttribute("wallets", walletService.findByUsername(auth.getName()));
            return "user/transaction/create";
        } catch (Exception e) {
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
            }
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.findByUserName(auth.getName()));
            model.addAttribute("wallets", walletService.findByUsername(auth.getName()));
            return "user/transaction/create";
        }
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/user/transactions");
    }

    @GetMapping("/transactions/error-ajax")
    @ResponseBody
    public org.springframework.http.ResponseEntity<String> errorAjax() {
        return org.springframework.http.ResponseEntity.badRequest().body("Validation failed");
    }

    // UPDATE
    @PostMapping("/transactions/edit")
    public String update(@Valid @ModelAttribute Transaction transaction,
                         @RequestParam(required = false) String categoryName,
                         BindingResult result, Model model, Authentication auth,
                         RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findByUserName(auth.getName()));
            model.addAttribute("wallets", walletService.findByUsername(auth.getName()));
            return "user/transaction/edit";
        }

        try {
            BudgetAlertDTO alert = transactionService.updateTransaction(transaction, categoryName, auth.getName());
            if (alert != null) {
                if (alert.isAlert()) {
                    redirectAttributes.addFlashAttribute("budgetAlert", alert);
                }
                redirectAttributes.addFlashAttribute("message", alert.getMessage());
            }
        } catch (SpendingLimitException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.findByUserName(auth.getName()));
            model.addAttribute("wallets", walletService.findByUsername(auth.getName()));
            return "user/transaction/edit";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.findByUserName(auth.getName()));
            model.addAttribute("wallets", walletService.findByUsername(auth.getName()));
            return "user/transaction/edit";
        }
        
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/user/transactions");
    }

    @PostMapping("/transactions/delete")
    public String delete(@RequestParam Long id, Authentication auth, RedirectAttributes ra) {
        try {
            transactionService.delete(id, auth.getName());
            ra.addFlashAttribute("message", "Đã xóa giao dịch thành công!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/user/transactions";
    }

    @PostMapping("/update-ajax")
    public String updateAjax(@ModelAttribute Transaction transaction, Authentication auth, 
                             RedirectAttributes ra, HttpServletRequest request, Model model) {
        try {
            transactionService.updateTransaction(transaction, null, auth.getName());
            return "redirect:/user/transactions";
        } catch (Exception e) {
            return "redirect:/user/transactions/error-ajax";
        }
    }

    @GetMapping("/api/transactions/{id}")
    @ResponseBody
    public ResponseEntity<?> getTransaction(@PathVariable Long id, Authentication auth) {
        try {
            Transaction t = transactionService.findById(id, auth.getName());
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("amount", t.getAmount());
            map.put("type", t.getType().name());
            map.put("date", t.getDate().toString());
            map.put("description", t.getDescription());
            if (t.getCategory() != null) {
                map.put("categoryId", t.getCategory().getId());
                map.put("categoryName", t.getCategory().getName());
            }
            if (t.getWallet() != null) map.put("walletId", t.getWallet().getId());
            if (t.getToWallet() != null) map.put("toWalletId", t.getToWallet().getId());
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
