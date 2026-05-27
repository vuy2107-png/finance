package com.codegym.finance.service.icon;

import com.codegym.finance.entity.icon.Icon;
import com.codegym.finance.repository.icon.IconRepository;
import com.codegym.finance.entity.icon.UserIcon;
import com.codegym.finance.repository.icon.UserIconRepository;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.service.user.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class IconService implements IIconService {

    @Autowired
    private IconRepository iconRepository;

    @Autowired
    private UserIconRepository userIconRepository;

    @Autowired
    private IUserService userService;

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

    /**
     * Thực hiện mua một Icon cho người dùng: kiểm tra tính khả dụng, số dư và lưu thông tin sở hữu.
     */
    @Override
    @Transactional
    public void buyIcon(Long iconId, String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("Không tìm thấy người dùng");
        }
        Icon icon = findById(iconId);

        if (icon == null || !icon.getActive()) {
            throw new RuntimeException("Icon này hiện không còn bán.");
        }

        // Kiểm tra xem đã sở hữu chưa
        if (existsByUserUsernameAndIconId(username, iconId)) {
            throw new RuntimeException("Bạn đã sở hữu icon này rồi!");
        }

        java.math.BigDecimal currentBalance = (user.getBalance() != null) ? user.getBalance() : java.math.BigDecimal.ZERO;
        if (currentBalance.compareTo(icon.getPrice()) < 0) {
            throw new RuntimeException("Số dư của bạn không đủ. Vui lòng nạp thêm!");
        }

        // Thực hiện trừ tiền
        user.setBalance(currentBalance.subtract(icon.getPrice()));
        userService.save(user);

        // Lưu vào kho đồ (UserIcon)
        UserIcon userIcon = new UserIcon();
        userIcon.setUser(user);
        userIcon.setIcon(icon);
        saveUserIcon(userIcon);
    }
}
