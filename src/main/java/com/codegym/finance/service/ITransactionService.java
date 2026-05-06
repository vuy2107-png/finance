package com.codegym.finance.service;

import com.codegym.finance.entity.Transaction;

import java.util.List;

public interface ITransactionService {

    // 🔥 LẤY DANH SÁCH THEO USER
    List<Transaction> findByUserName(String username);

    // 🔥 CREATE
    void save(Transaction transaction, String username);

    // 🔥 FIND 1
    Transaction findById(Long id, String username);

    // 🔥 UPDATE
    void update(Transaction transaction, String username);

    // 🔥 DELETE
    void delete(Long id, String username);

    double getTotalIncome(String username);

    double getTotalExpense(String username);

    double getTodayIncome(String username);

    double getTodayExpense(String username);

    double getThisMonthIncome(String username);

    double getThisMonthExpense(String username);

    double getBalance(String username);

    int getTotalTransactions(String username);
}