package com.codegym.finance.exception;
import com.codegym.finance.entity.wallet.Wallet;
import com.codegym.finance.entity.category.Category;

public class SpendingLimitException extends RuntimeException {
    private String limitType; // WALLET, DAILY, CATEGORY

    public SpendingLimitException(String message, String limitType) {
        super(message);
        this.limitType = limitType;
    }

    public String getLimitType() {
        return limitType;
    }
}
