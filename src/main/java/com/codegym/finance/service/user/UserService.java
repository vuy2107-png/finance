package com.codegym.finance.service.user;
import com.codegym.finance.entity.transaction.Transaction;
import com.codegym.finance.entity.transaction.TransactionType;
import com.codegym.finance.entity.user.User;
import com.codegym.finance.entity.user.Role;
import com.codegym.finance.repository.user.UserRepository;
import com.codegym.finance.service.transaction.ITransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService implements IUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Lazy
    private ITransactionService transactionService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void save(User user) {
        userRepository.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User findById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @Override
    @Transactional
    public void toggleStatus(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            // Handle null safely
            boolean currentStatus = user.getActive() != null && user.getActive();
            user.setActive(!currentStatus);
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public void togglePremium(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            // Handle null safely
            boolean currentPremium = user.getPremium() != null && user.getPremium();
            user.setPremium(!currentPremium);
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public void activateTrial(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            user.setPremium(true);
            user.setPremiumPlan("trial");
            user.setExpiryDate(java.time.LocalDateTime.now().plusDays(7));
            user.setHasSeenTour(true);
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public void markTourAsSeen(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            user.setHasSeenTour(true);
            userRepository.save(user);
        }
    }

    @Override
    public java.time.LocalDate getEffectiveDate(String username) {
        User user = findByUsername(username);
        if (user != null && user.getTestDate() != null) {
            return user.getTestDate();
        }
        return java.time.LocalDate.now();
    }

    @Override
    @Transactional
    public void updateTestDate(String username, java.time.LocalDate testDate) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            user.setTestDate(testDate);
            userRepository.save(user);
        }
    }

    @Override
    public List<User> findRecentUsers() {
        return userRepository.findTop5ByOrderByCreatedAtDesc();
    }

    /**
     * Nạp tiền vào tài khoản hệ thống của người dùng và tạo bản ghi giao dịch nạp tiền.
     */
    @Override
    @Transactional
    public void deposit(String username, java.math.BigDecimal amount) {
        if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền nạp không hợp lệ!");
        }

        User user = findByUsername(username);
        if (user == null) {
            throw new RuntimeException("Không tìm thấy người dùng");
        }
        java.math.BigDecimal currentBalance = (user.getBalance() != null) ? user.getBalance() : java.math.BigDecimal.ZERO;
        user.setBalance(currentBalance.add(amount));
        save(user);

        // Tạo bản ghi giao dịch nạp tiền
        Transaction t = new Transaction();
        t.setUser(user);
        t.setAmount(amount);
        t.setType(TransactionType.INCOME);
        t.setDate(java.time.LocalDate.now());
        t.setDescription("SYSTEM_DEPOSIT_INFLOW: Nạp tiền vào tài khoản");
        
        transactionService.save(t, username);
    }

    /**
     * Mua gói Premium cho người dùng: trừ số dư tương ứng, gia hạn thời gian Premium và ghi nhận giao dịch doanh thu.
     */
    @Override
    @Transactional
    public void buyPremium(String username, String plan) {
        User user = findByUsername(username);
        if (user == null) {
            throw new RuntimeException("Không tìm thấy người dùng");
        }
        java.math.BigDecimal currentBalance = (user.getBalance() != null) ? user.getBalance() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal price = java.math.BigDecimal.ZERO;
        String planName = "";
        
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime expiryDate = (user.getExpiryDate() != null && user.getExpiryDate().isAfter(now))
                ? user.getExpiryDate() : now;

        switch (plan) {
            case "weekly":
                price = java.math.BigDecimal.valueOf(29000);
                planName = "Gói Starter (Tuần)";
                expiryDate = expiryDate.plusDays(7);
                break;
            case "monthly":
                price = java.math.BigDecimal.valueOf(99000);
                planName = "Gói Pro (Tháng)";
                expiryDate = expiryDate.plusDays(30);
                break;
            case "yearly":
                price = java.math.BigDecimal.valueOf(799000);
                planName = "Gói Elite (Năm)";
                expiryDate = expiryDate.plusYears(1);
                break;
            default:
                throw new RuntimeException("Gói nâng cấp không hợp lệ!");
        }

        if (currentBalance.compareTo(price) < 0) {
            throw new RuntimeException("Số dư không đủ để mua " + planName + ". Vui lòng nạp thêm tiền!");
        }

        // Trừ tiền và kích hoạt Premium
        user.setBalance(currentBalance.subtract(price));
        user.setPremium(true);
        user.setPremiumPlan(plan);
        user.setExpiryDate(expiryDate);
        save(user);

        // Tạo giao dịch ghi lại doanh thu cho hệ thống
        Transaction t = new Transaction();
        t.setUser(user);
        t.setAmount(price);
        t.setType(TransactionType.EXPENSE);
        t.setDate(java.time.LocalDate.now());
        t.setDescription("Nâng cấp Premium: " + planName);
        
        transactionService.save(t, username);
    }

    /**
     * Cập nhật các trường thông tin cá nhân của người dùng.
     */
    @Override
    @Transactional
    public void updateProfile(String username, User userUpdates) {
        User currentUser = findByUsername(username);
        if (currentUser == null) {
            throw new RuntimeException("Không tìm thấy người dùng");
        }
        
        currentUser.setFullName(userUpdates.getFullName());
        currentUser.setEmail(userUpdates.getEmail());
        currentUser.setPhoneNumber(userUpdates.getPhoneNumber());
        currentUser.setAddress(userUpdates.getAddress());
        currentUser.setGender(userUpdates.getGender());
        currentUser.setDateOfBirth(userUpdates.getDateOfBirth());
        currentUser.setTestDate(userUpdates.getTestDate());
        
        save(currentUser);
    }

    /**
     * Đăng ký tài khoản người dùng mới: mã hóa mật khẩu và thiết lập trạng thái mặc định.
     */
    @Override
    @Transactional
    public void register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.USER);
        user.setActive(true);
        user.setPremium(false);
        user.setExpiryDate(null);
        user.setHasSeenTour(false);
        save(user);
    }
}
