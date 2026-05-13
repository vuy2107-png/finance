package com.codegym.finance.entity.category;
import com.codegym.finance.entity.icon.Icon;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.entity.transaction.TransactionType;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    private String icon;

    @Column(name = "color_code")
    private String colorCode;

    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
