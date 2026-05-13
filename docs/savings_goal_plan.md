# Kế hoạch Triển khai Tính năng Mục tiêu tiết kiệm (Savings Goal)

*Bạn có thể xem file này trực tiếp trên IDE để theo dõi tiến độ.*

## Luồng nghiệp vụ (Đã chốt: Phương án B)
- **Quản lý Mục tiêu:** Người dùng có thể tạo, xem, chỉnh sửa và xóa các mục tiêu tiết kiệm.
- **Nạp tiền (Add Funds):**
  - Khi nạp tiền vào mục tiêu, người dùng BẮT BUỘC phải chọn nguồn tiền từ một **Ví (Wallet)** cụ thể.
  - Số tiền sẽ được **trừ khỏi Ví** đó.
  - Hệ thống tự động sinh ra một giao dịch (`Transaction`) loại `EXPENSE` (Chi tiêu) để ghi lại lịch sử dòng tiền.
  - Số tiền `currentAmount` của mục tiêu sẽ tăng lên.
  - Nếu `currentAmount >= targetAmount`, trạng thái mục tiêu sẽ tự động chuyển sang `ACHIEVED` (Hoàn thành).

## Danh sách công việc (Task List)
- [x] **1. Tầng Service**
    - [x] Tạo `ISavingsGoalService`
    - [x] Tạo `SavingsGoalService` (Xử lý logic cộng tiền và tạo giao dịch tự động).
- [x] **2. Tầng Controller**
    - [x] Tạo `SavingsGoalController` để nhận request từ giao diện.
- [/] **3. Giao diện (UI)**
    - [ ] Bổ sung Navigation menu ở các trang.
    - [ ] Thiết kế trang `savings/list.html` với Progress Bar (Thanh tiến độ).
    - [ ] Tạo Modal Thêm mục tiêu.
    - [ ] Tạo Modal Nạp tiền (Có chọn Ví).
