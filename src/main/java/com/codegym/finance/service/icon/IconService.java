package com.codegym.finance.service.icon;

import com.codegym.finance.entity.icon.Icon;
import com.codegym.finance.repository.icon.IconRepository;
import com.codegym.finance.entity.icon.UserIcon;
import com.codegym.finance.repository.icon.UserIconRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class IconService implements IIconService {

    @Autowired
    private IconRepository iconRepository;

    @Autowired
    private UserIconRepository userIconRepository;

    @Override
    public List<Icon> findAll() {
        return iconRepository.findAll();
    }

    @Override
    public List<Icon> findAllActive() {
        return iconRepository.findByActiveTrue();
    }

    @Override
    public Icon findById(Long id) {
        return iconRepository.findById(id).orElse(null);
    }

    @Override
    public void save(Icon icon) {
        if (icon.getActive() == null) icon.setActive(true);
        iconRepository.save(icon);
    }

    @Override
    public void delete(Long id) {
        iconRepository.deleteById(id);
    }

    @Override
    public void toggleStatus(Long id) {
        Icon icon = findById(id);
        if (icon != null) {
            icon.setActive(!icon.getActive());
            iconRepository.save(icon);
        }
    }

    @Override
    public List<UserIcon> findUserIconsByUsername(String username) {
        return userIconRepository.findByUserUsername(username);
    }

    @Override
    public boolean existsByUserUsernameAndIconId(String username, Long iconId) {
        return userIconRepository.existsByUserUsernameAndIconId(username, iconId);
    }

    @Override
    public void saveUserIcon(UserIcon userIcon) {
        userIconRepository.save(userIcon);
    }
}
