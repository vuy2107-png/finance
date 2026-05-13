package com.codegym.finance.entity.savings;
import com.codegym.finance.entity.icon.Icon;
import com.codegym.finance.entity.user.User;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "savings_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên mục tiêu không được để trống")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Số tiền mục tiêu không được để trống")
    @Column(name = "target_amount", nullable = false)
    private Double targetAmount;

    @Builder.Default
    @Column(name = "current_amount", nullable = false)
    private Double currentAmount = 0.0;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status = GoalStatus.IN_PROGRESS;

    @Column(name = "color_code")
    private String colorCode;

    private String icon;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Transient
    public Double getProgressPercentage() {
        if (targetAmount == null || targetAmount <= 0) return 0.0;
        double pct = (currentAmount * 100.0) / targetAmount;
        return pct > 100.0 ? 100.0 : pct;
    }
}
