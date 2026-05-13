/**
 * Wallet Management Interactions
 */

// Các hàm openModal và closeModal đã được chuyển vào common.js dùng chung cho toàn bộ ứng dụng.
// Ở đây chỉ giữ lại logic đặc thù nếu có.

// Global click handler to close modals when clicking on overlay
document.addEventListener('DOMContentLoaded', function() {
    window.onclick = function(event) {
        if (event.target.classList.contains('modal-overlay')) {
            event.target.classList.remove('active');
        }
    };
});
