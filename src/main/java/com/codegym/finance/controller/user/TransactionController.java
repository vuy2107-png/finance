package com.codegym.finance.controller.user;

import com.codegym.finance.entity.Transaction;
import com.codegym.finance.service.ITransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/user/transactions")
public class TransactionController {

    @Autowired
    private ITransactionService transactionService;

    // LIST
    @GetMapping
    public String list(Model model, Authentication auth) {
        model.addAttribute("transactions",
                transactionService.findByUserName(auth.getName()));
        return "user/transaction/list";
    }

    // CREATE FORM
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("transaction", new Transaction());
        return "user/transaction/create";
    }

    // CREATE SAVE
    @PostMapping("/create")
    public String create(@Valid @ModelAttribute Transaction transaction,
                         BindingResult result,
                         Authentication auth) {

        if (result.hasErrors()) {
            return "user/transaction/create";
        }

        transactionService.save(transaction, auth.getName());
        return "redirect:/user/transactions";
    }

    // EDIT FORM
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id,
                           Model model,
                           Authentication auth) {

        Transaction transaction =
                transactionService.findById(id, auth.getName());

        model.addAttribute("transaction", transaction);
        return "user/transaction/edit";
    }

    // UPDATE
    @PostMapping("/edit")
    public String update(@Valid @ModelAttribute Transaction transaction,
                         BindingResult result,
                         Authentication auth) {

        if (result.hasErrors()) {
            return "user/transaction/edit";
        }

        transactionService.update(transaction, auth.getName());
        return "redirect:/user/transactions";
    }

    // DELETE
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id,
                         Authentication auth) {

        transactionService.delete(id, auth.getName());
        return "redirect:/user/transactions";
    }
}