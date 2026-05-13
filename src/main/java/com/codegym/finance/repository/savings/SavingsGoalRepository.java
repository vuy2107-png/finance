package com.codegym.finance.repository.savings;

import com.codegym.finance.entity.savings.SavingsGoal;
import com.codegym.finance.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByUser(User user);
}
