package com.codegym.finance.repository.icon;
import com.codegym.finance.entity.icon.Icon;

import com.codegym.finance.entity.icon.UserIcon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserIconRepository extends JpaRepository<UserIcon, Long> {
    List<UserIcon> findByUserUsername(String username);
    boolean existsByUserUsernameAndIconId(String username, Long iconId);
}
