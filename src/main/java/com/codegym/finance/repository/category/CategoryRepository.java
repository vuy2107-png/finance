package com.codegym.finance.repository.category;
import com.codegym.finance.entity.transaction.Transaction;

import com.codegym.finance.entity.category.Category;
import com.codegym.finance.entity.transaction.TransactionType;
import com.codegym.finance.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUser(User user);
    List<Category> findByUserAndType(User user, TransactionType type);
    java.util.Optional<Category> findByUserAndName(User user, String name);
    java.util.Optional<Category> findByUserAndNameAndType(User user, String name, TransactionType type);
    void deleteByUser(User user);
}
