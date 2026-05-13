package com.codegym.finance.entity.icon;
import com.codegym.finance.entity.user.User;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_icons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserIcon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "icon_id")
    private Icon icon;

    private LocalDateTime purchaseDate;

    @PrePersist
    protected void onCreate() {
        purchaseDate = LocalDateTime.now();
    }
}
