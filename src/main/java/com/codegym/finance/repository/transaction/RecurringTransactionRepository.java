package com.codegym.finance.repository.transaction;
import com.codegym.finance.entity.transaction.Transaction;

import com.codegym.finance.entity.transaction.RecurringTransaction;
import com.codegym.finance.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {
    List<RecurringTransaction> findByUser(User user);
}
