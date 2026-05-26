package com.codegym.finance.entity.transaction;
import com.codegym.finance.entity.wallet.Wallet;
import com.codegym.finance.entity.category.Category;
import com.codegym.finance.entity.user.User;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "recurring_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên giao dịch định kỳ không được để trống")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Số tiền không được để trống")
    @Column(nullable = false)
    private java.math.BigDecimal amount;

    @NotNull(message = "Loại giao dịch không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @NotNull(message = "Tần suất không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurringFrequency frequency;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "next_execution_date")
    private LocalDate nextExecutionDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurringStatus status = RecurringStatus.ACTIVE;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    private String description;
}
