package com.codegym.finance.service.category;
import com.codegym.finance.entity.transaction.TransactionType;

import com.codegym.finance.entity.category.Category;
import java.util.List;

public interface ICategoryService {
    List<Category> findByUserName(String username);
    List<Category> findByUserNameAndType(String username, com.codegym.finance.entity.transaction.TransactionType type);
    Category findById(Long id, String username);
    Category save(Category category, String username);
    Category getOrCreateCategory(String name, com.codegym.finance.entity.transaction.TransactionType type, String username);
    void delete(Long id, String username);
}

