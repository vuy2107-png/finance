package com.codegym.finance.service.budget;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BudgetAlertDTO {
    private boolean alert;
    private String message;
    private String type; // WARNING (80%), DANGER (100%)
    private Double percentage;
    private Double spentAmount;
}
