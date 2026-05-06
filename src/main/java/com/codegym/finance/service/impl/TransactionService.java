package com.codegym.finance.service.impl;

import com.codegym.finance.entity.Transaction;
import com.codegym.finance.entity.TransactionType;
import com.codegym.finance.entity.User;
import com.codegym.finance.repository.TransactionRepository;
import com.codegym.finance.repository.UserRepository;
import com.codegym.finance.service.ITransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService implements ITransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    // 🔥 LẤY DANH SÁCH THEO USER
    @Override
    public List<Transaction> findByUserName(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return transactionRepository.findByUser(user);
    }

    // 🔥 CREATE
    @Override
    public void save(Transaction transaction, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        transaction.setUser(user); // 🔥 GÁN USER
        transactionRepository.save(transaction);
    }

    // 🔥 FIND 1 + CHECK QUYỀN
    @Override
    public Transaction findById(Long id, String username) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // 🔥 CHECK: đúng user mới được truy cập
        if (!transaction.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Không có quyền truy cập");
        }

        return transaction;
    }

    // 🔥 UPDATE
    @Override
    public void update(Transaction transaction, String username) {
        Transaction old = findById(transaction.getId(), username);

        old.setAmount(transaction.getAmount());
        old.setDescription(transaction.getDescription());
        old.setDate(transaction.getDate());
        old.setType(transaction.getType()); // 🔥 CẬP NHẬT THÊM TYPE

        transactionRepository.save(old);
    }

    // 🔥 DELETE
    @Override
    public void delete(Long id, String username) {
        Transaction transaction = findById(id, username);
        transactionRepository.delete(transaction);
    }

    // Tổng thu nhập: chỉ tính giao dịch có amount > 0
    @Override
    public double getTotalIncome(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Double result = transactionRepository.sumByUserAndType(user, TransactionType.INCOME);
        return result != null ? result : 0;
    }

    // Tổng chi tiêu: chỉ tính giao dịch có amount < 0
    @Override
    public double getTotalExpense(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Double result = transactionRepository.sumByUserAndType(user, TransactionType.EXPENSE);
        return result != null ? result : 0;
    }

    // Tổng thu nhập hôm nay
    @Override
    public double getTodayIncome(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Double result = transactionRepository.sumByUserAndTypeAndDate(user, TransactionType.INCOME, java.time.LocalDate.now());
        return result != null ? result : 0;
    }

    // Tổng chi tiêu hôm nay
    @Override
    public double getTodayExpense(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Double result = transactionRepository.sumByUserAndTypeAndDate(user, TransactionType.EXPENSE, java.time.LocalDate.now());
        return result != null ? result : 0;
    }

    // Tổng thu nhập tháng này
    @Override
    public double getThisMonthIncome(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        java.time.LocalDate startOfMonth = java.time.LocalDate.now().withDayOfMonth(1);
        java.time.LocalDate endOfMonth = java.time.LocalDate.now().withDayOfMonth(java.time.LocalDate.now().lengthOfMonth());
        Double result = transactionRepository.sumByUserAndTypeAndDateBetween(user, TransactionType.INCOME, startOfMonth, endOfMonth);
        return result != null ? result : 0;
    }

    // Tổng chi tiêu tháng này
    @Override
    public double getThisMonthExpense(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        java.time.LocalDate startOfMonth = java.time.LocalDate.now().withDayOfMonth(1);
        java.time.LocalDate endOfMonth = java.time.LocalDate.now().withDayOfMonth(java.time.LocalDate.now().lengthOfMonth());
        Double result = transactionRepository.sumByUserAndTypeAndDateBetween(user, TransactionType.EXPENSE, startOfMonth, endOfMonth);
        return result != null ? result : 0;
    }

    // Số dư: tổng thu nhập - tổng chi tiêu
    @Override
    public double getBalance(String username) {
        return getTotalIncome(username) - getTotalExpense(username);
    }

    // Tổng số giao dịch
    @Override
    public int getTotalTransactions(String username) {
        return findByUserName(username).size();
    }
}