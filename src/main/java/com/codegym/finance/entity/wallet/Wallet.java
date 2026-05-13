package com.codegym.finance.entity.wallet;
import com.codegym.finance.entity.icon.Icon;
import com.codegym.finance.entity.user.User;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên ví không được để trống")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Số dư không được để trống")
    @Column(nullable = false)
    @Builder.Default
    private Double balance = 0.0;

    private String icon;

    @Column(name = "color_code")
    private String colorCode;

    @Column(name = "daily_spending_limit")
    private Double dailySpendingLimit;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (balance == null) balance = 0.0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
