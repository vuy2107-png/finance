package com.codegym.finance.repository.budget;

import com.codegym.finance.entity.budget.Budget;
import com.codegym.finance.entity.category.Category;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.entity.wallet.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserAndMonthAndYear(User user, Integer month, Integer year);
    List<Budget> findByUserAndWalletAndMonthAndYear(User user, Wallet wallet, Integer month, Integer year);
    
    Optional<Budget> findByUserAndCategoryAndMonthAndYear(User user, Category category, Integer month, Integer year);
    Optional<Budget> findByUserAndWalletAndCategoryAndMonthAndYear(User user, Wallet wallet, Category category, Integer month, Integer year);

    void deleteByCategory(Category category);
    void deleteByUser(User user);
    void deleteByWallet(Wallet wallet);
}
