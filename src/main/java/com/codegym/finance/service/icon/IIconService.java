package com.codegym.finance.service.icon;

import com.codegym.finance.entity.icon.Icon;
import com.codegym.finance.entity.icon.UserIcon;
import java.util.List;

public interface IIconService {
    List<Icon> findAll();
    List<Icon> findAllActive();
    Icon findById(Long id);
    void save(Icon icon);
    void delete(Long id);
    void toggleStatus(Long id);
    List<UserIcon> findUserIconsByUsername(String username);
    boolean existsByUserUsernameAndIconId(String username, Long iconId);
    void saveUserIcon(UserIcon userIcon);
}
