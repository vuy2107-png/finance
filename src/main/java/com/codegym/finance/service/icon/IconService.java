package com.codegym.finance.service.icon;

import com.codegym.finance.entity.icon.Icon;
import com.codegym.finance.repository.icon.IconRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class IconService implements IIconService {

    @Autowired
    private IconRepository iconRepository;

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
}
