package com.codegym.finance.entity.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username không được để trống")
    @Size(min = 6, message = "Tên đăng nhập không được ít hơn 6 ký tự")
    @Column(unique = true, length = 50)
    private String username;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 6, message = "Mật khẩu không được ít hơn 6 ký tự")
    private String password;

    @Transient
    private String confirmPassword;

    @Column(name = "full_name")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Column(unique = true)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String address;

    private String avatar;

    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "is_premium")
    private Boolean premium;

    @Column(name = "is_active")
    private Boolean active;

    @Column(name = "is_online")
    private Boolean isOnline = false;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "has_seen_tour")
    private Boolean hasSeenTour;

    @Column(name = "balance")
    private Double balance;

    @Column(name = "premium_plan")
    private String premiumPlan;

    @Column(name = "last_report_date")
    private LocalDate lastReportDate;

    private LocalDate testDate; // Ngày giả lập hệ thống cho mục đích test

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (this.role == null) this.role = Role.USER;
        if (this.premium == null) this.premium = false;
        if (this.active == null) this.active = true;
        if (this.hasSeenTour == null) this.hasSeenTour = false;
        if (this.balance == null) this.balance = 0.0; // Không tặng tiền ảo nữa để báo cáo chính xác
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isPremiumActive() {
        if (premium == null || !premium) {
            return false;
        }
        if (expiryDate != null) {
            if (testDate != null) {
                return testDate.atStartOfDay().isBefore(expiryDate);
            }
            return LocalDateTime.now().isBefore(expiryDate);
        }
        return true;
    }
}

