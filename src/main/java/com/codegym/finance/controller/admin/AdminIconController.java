package com.codegym.finance.controller.admin;

import com.codegym.finance.entity.icon.Icon;
import com.codegym.finance.service.icon.IIconService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/icons")
public class AdminIconController {

    @Autowired
    private IIconService iconService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("icons", iconService.findAll());
        model.addAttribute("newIcon", new Icon());
        return "admin/icon/list";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Icon icon, RedirectAttributes redirectAttributes) {
        iconService.save(icon);
        redirectAttributes.addFlashAttribute("message", "Đã lưu Icon thành công!");
        return "redirect:/admin/icons";
    }

    @GetMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id) {
        iconService.toggleStatus(id);
        return "redirect:/admin/icons";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        iconService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Đã xóa Icon thành công!");
        return "redirect:/admin/icons";
    }
}
