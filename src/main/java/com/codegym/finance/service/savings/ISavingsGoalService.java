package com.codegym.finance.service.savings;

import com.codegym.finance.entity.savings.SavingsGoal;

import java.util.List;

public interface ISavingsGoalService {
    List<SavingsGoal> findByUserName(String username);
    SavingsGoal findById(Long id, String username);
    void save(SavingsGoal goal, String username);
    void update(SavingsGoal goal, String username);
    void delete(Long id, String username);
    void addFunds(Long goalId, Double amount, Long walletId, String username);
}
