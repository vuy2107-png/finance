package com.codegym.finance.controller.user;
import com.codegym.finance.entity.icon.Icon;
import com.codegym.finance.entity.transaction.Transaction;
import com.codegym.finance.entity.user.User;

import com.codegym.finance.entity.category.Category;
import com.codegym.finance.entity.transaction.TransactionType;
import com.codegym.finance.entity.icon.UserIcon;
import com.codegym.finance.service.category.ICategoryService;
import com.codegym.finance.service.icon.IIconService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user/categories")
public class CategoryController {

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private IIconService iconService;

    @GetMapping
    public String list(Model model, Authentication auth) {
        model.addAttribute("categories", categoryService.findByUserName(auth.getName()));
        model.addAttribute("newCategory", new Category());
        model.addAttribute("types", TransactionType.values());
        
        // Lấy danh sách icon đã sở hữu
        List<UserIcon> userIcons = iconService.findUserIconsByUsername(auth.getName());
        List<String> ownedIcons = userIcons.stream()
                .map(ui -> ui.getIcon().getUrl())
                .collect(Collectors.toList());
        
        model.addAttribute("ownedIcons", ownedIcons);
        
        return "user/category/list";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Category category, Authentication auth, RedirectAttributes ra) {
        try {
            categoryService.save(category, auth.getName());
            ra.addFlashAttribute("message", "Đã lưu danh mục thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/user/categories";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            categoryService.delete(id, auth.getName());
            ra.addFlashAttribute("message", "Đã xóa danh mục!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/user/categories";
    }
}
