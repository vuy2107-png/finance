package com.codegym.finance.service.category;
import com.codegym.finance.entity.icon.Icon;
import com.codegym.finance.entity.budget.Budget;

import com.codegym.finance.entity.category.Category;
import com.codegym.finance.entity.transaction.Transaction;
import com.codegym.finance.entity.transaction.TransactionType;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.repository.budget.BudgetRepository;
import com.codegym.finance.repository.category.CategoryRepository;
import com.codegym.finance.repository.transaction.TransactionRepository;
import com.codegym.finance.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService implements ICategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    private static final Map<String, String> ICON_MAP = new HashMap<>();
    static {
        ICON_MAP.put("Lương", "fas fa-money-bill-wave");
        ICON_MAP.put("Thưởng", "fas fa-gift");
        ICON_MAP.put("Kinh doanh", "fas fa-briefcase");
        ICON_MAP.put("Đầu tư", "fas fa-chart-line");
        ICON_MAP.put("Ăn uống", "fas fa-utensils");
        ICON_MAP.put("Di chuyển", "fas fa-car");
        ICON_MAP.put("Mua sắm", "fas fa-shopping-cart");
        ICON_MAP.put("Tiền nhà", "fas fa-home");
        ICON_MAP.put("Giải trí", "fas fa-film");
        ICON_MAP.put("Sức khỏe", "fas fa-heartbeat");
        ICON_MAP.put("Hóa đơn", "fas fa-file-invoice-dollar");
        ICON_MAP.put("Khác", "fas fa-tag");
    }

    @Override
    public List<Category> findByUserName(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Category> allCategories = categoryRepository.findByUser(user);
        
        // Tạo lại bộ danh mục chuẩn nếu thiếu
        ensureDefaultCategories(user, Arrays.asList("Lương", "Thưởng", "Kinh doanh", "Đầu tư", "Khác"), TransactionType.INCOME);
        ensureDefaultCategories(user, Arrays.asList("Ăn uống", "Di chuyển", "Mua sắm", "Tiền nhà", "Giải trí", "Sức khỏe", "Hóa đơn", "Khác"), TransactionType.EXPENSE);
        
        // Cập nhật Type/Icon cho dữ liệu nếu còn thiếu
        allCategories = categoryRepository.findByUser(user);
        for (Category cat : allCategories) {
            boolean updated = false;
            if (cat.getType() == null) {
                cat.setType(TransactionType.EXPENSE);
                updated = true;
            }
            if (cat.getIcon() == null || cat.getIcon().isEmpty()) {
                cat.setIcon(ICON_MAP.getOrDefault(cat.getName(), "fas fa-tag"));
                updated = true;
            }
            if (updated) {
                categoryRepository.save(cat);
            }
        }
        
        return categoryRepository.findByUser(user);
    }

    @Override
    public List<Category> findByUserNameAndType(String username, TransactionType type) {
        findByUserName(username); // Ensure defaults
        User user = userRepository.findByUsername(username).orElseThrow();
        return categoryRepository.findByUserAndType(user, type);
    }

    private boolean isMangled(String name) {
        if (name == null) return false;
        // Các dấu hiệu nhận biết chuỗi bị lỗi encoding (như Æ, Ã, á,...)
        return name.contains("Æ") || name.contains("Ã") || name.contains("á»") || name.contains("áº");
    }

    @Override
    public Category findById(Long id, String username) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        if (!category.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Access denied");
        }
        return category;
    }

    @Override
    public Category save(Category category, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        category.setUser(user);
        if (category.getIcon() == null || category.getIcon().isEmpty()) {
            category.setIcon("fas fa-tag");
        }
        return categoryRepository.save(category);
    }

    @Override
    public Category getOrCreateCategory(String name, TransactionType type, String username) {
        if (name == null || name.trim().isEmpty()) return null;
        
        User user = userRepository.findByUsername(username).orElseThrow();
        return categoryRepository.findByUserAndNameAndType(user, name.trim(), type)
                .orElseGet(() -> {
                    Category newCat = new Category();
                    newCat.setName(name.trim());
                    newCat.setType(type);
                    newCat.setUser(user);
                    newCat.setIcon("fas fa-tags"); // Mặc định cho danh mục mới
                    newCat.setColorCode("#64748b"); // Màu xám mặc định
                    return categoryRepository.save(newCat);
                });
    }

    @Override
    public void delete(Long id, String username) {
        Category category = findById(id, username);
        if ("Khác".equals(category.getName())) {
            throw new RuntimeException("Cannot delete default 'Khác' category");
        }

        User user = category.getUser();
        Category otherCategory = categoryRepository.findByUserAndType(user, category.getType()).stream()
                .filter(c -> "Khác".equals(c.getName()))
                .findFirst()
                .orElseGet(() -> {
                    return categoryRepository.save(Category.builder()
                            .name("Khác")
                            .user(user)
                            .type(category.getType())
                            .icon("fas fa-tag")
                            .colorCode("#64748b")
                            .build());
                });

        List<Transaction> transactions = transactionRepository.findByUserAndCategory(user, category);
        for (Transaction t : transactions) {
            t.setCategory(otherCategory);
            transactionRepository.save(t);
        }
        budgetRepository.deleteByCategory(category);
        categoryRepository.delete(category);
    }

    private void ensureDefaultCategories(User user, List<String> defaultNames, TransactionType type) {
        List<Category> existingCats = categoryRepository.findByUserAndType(user, type);
        Set<String> existingNames = existingCats.stream()
                .map(Category::getName)
                .collect(Collectors.toSet());

        for (String name : defaultNames) {
            if (!existingNames.contains(name)) {
                categoryRepository.save(Category.builder()
                        .name(name)
                        .user(user)
                        .type(type)
                        .icon(ICON_MAP.getOrDefault(name, "fas fa-tag"))
                        .build());
            }
        }
    }
}
