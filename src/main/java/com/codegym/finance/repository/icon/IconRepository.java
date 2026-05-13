package com.codegym.finance.repository.icon;

import com.codegym.finance.entity.icon.Icon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IconRepository extends JpaRepository<Icon, Long> {
    List<Icon> findByActiveTrue();
}
