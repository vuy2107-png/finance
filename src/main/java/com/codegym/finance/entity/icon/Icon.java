package com.codegym.finance.entity.icon;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "icons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Icon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url; // Đường dẫn ảnh icon

    @Column(nullable = false)
    private Double price; // Giá bán (Ví dụ: 10 xu hoặc 10.000đ)

    private String description;
    
    @Column(name = "is_active")
    private Boolean active;
}
