# Kế hoạch Triển khai Tính năng Mục tiêu tiết kiệm (Savings Goal)

Mục tiêu: Xây dựng một trang quản lý hoàn chỉnh cho phép người dùng đặt ra các mục tiêu tài chính (ví dụ: Mua nhà, Du lịch), theo dõi tiến độ qua các thanh phần trăm (progress bar) và trải nghiệm cảm giác đạt được mục tiêu.

## Các hạng mục công việc

### 1. Xây dựng Tầng Logic (Backend Service)
Tạo `ISavingsGoalService` và `SavingsGoalService` trong package `service.user` (theo chuẩn cấu trúc mới) để xử lý các nghiệp vụ:
- Lấy danh sách mục tiêu của user hiện tại.
- Thêm mới một mục tiêu.
- Cập nhật số tiền hiện có (Nạp thêm tiền vào mục tiêu).
- Xóa mục tiêu.
- Tự động kiểm tra trạng thái: Nếu `currentAmount >= targetAmount`, chuyển trạng thái thành `ACHIEVED`.

### 2. Xây dựng Controller
Tạo `SavingsGoalController` trong package `controller.user`:
- Định tuyến các URL: `/user/savings` (trang chủ), `/user/savings/create`, `/user/savings/add-funds`, `/user/savings/delete`.

### 3. Thiết kế Giao diện (UI/UX)
- Cập nhật thanh điều hướng (Navbar) ở tất cả các trang: Thêm nút **"Tiết kiệm"**.
- Tạo trang mới: `templates/user/savings/list.html`.
- **UI/UX Focus:** Giao diện dạng thẻ (Cards) với:
    - Biểu tượng (Icon) tùy chỉnh cho từng mục tiêu.
    - Thanh tiến độ (Progress Bar) sinh động (chuyển màu xanh lá khi đạt 100%).
    - Nút "Nạp tiền" (Add Funds) nhanh qua Modal.

---

## > User Review Required

Đây là luồng xử lý nạp tiền (Add Funds) vào mục tiêu, tôi có 2 phương án để bạn lựa chọn:

*   **Phương án A (Đơn giản):** Nút "Nạp tiền" chỉ đơn thuần là cộng thêm số tiền vào `currentAmount` của mục tiêu, không ảnh hưởng đến số dư của các ví (Wallet) hay tạo ra Giao dịch (Transaction) mới. Phù hợp nếu người dùng chỉ muốn ghi chú nhanh tiến độ.
*   **Phương án B (Chặt chẽ - Khuyên dùng):** Khi bấm "Nạp tiền", số tiền đó sẽ được **trừ** vào một Ví (Wallet) mà người dùng chọn, đồng thời sinh ra một giao dịch (Transaction) loại EXPENSE (chi tiêu) ghi chú là "Chuyển tiền vào quỹ...". Cách này giúp quản lý dòng tiền chính xác 100%.

> [!IMPORTANT]
> **Vui lòng cho tôi biết bạn muốn triển khai theo Phương án A hay Phương án B?** Nếu bạn chọn B, tôi sẽ bổ sung logic liên kết giữa SavingsGoal và Wallet.
