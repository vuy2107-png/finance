// Handle Form Submission
document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('quickTransactionForm');
    if (form) {
        form.addEventListener('submit', function() {
            const btn = form.querySelector('button[type="submit"]');
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang lưu...';
            btn.disabled = true;
        });
    }
});

function openTransactionModal() {
    const modal = document.getElementById('transactionModal');
    if (modal) {
        modal.style.display = 'flex';
        // Reset form but keep the current type
        const form = document.getElementById('quickTransactionForm');
        if (form) {
            const type = document.getElementById('modalType').value;
            form.reset();
            document.getElementById('modalType').value = type;
            setTransactionType(type); // Re-apply UI state
        }
    }
}

function closeTransactionModal() {
    const modal = document.getElementById('transactionModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

function setTransactionType(type) {
    // Update hidden input
    const typeInput = document.getElementById('modalType');
    if (typeInput) {
        typeInput.value = type;
    }
    
    // Update tabs UI
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-type') === type);
    });

    // Toggle groups
    const categoryGroup = document.getElementById('categoryGroup');
    const toWalletGroup = document.getElementById('toWalletGroup');
    const walletLabel = document.getElementById('walletLabel');

    if (type === 'TRANSFER') {
        if (categoryGroup) categoryGroup.style.display = 'none';
        if (toWalletGroup) toWalletGroup.style.display = 'block';
        if (walletLabel) walletLabel.innerText = 'Từ ví';
    } else {
        if (categoryGroup) categoryGroup.style.display = 'block';
        if (toWalletGroup) toWalletGroup.style.display = 'none';
        if (walletLabel) walletLabel.innerText = 'Ví sử dụng';
    }
}

// Close modal when clicking outside
window.addEventListener('click', function(event) {
    const modal = document.getElementById('transactionModal');
    if (event.target === modal) {
        closeTransactionModal();
    }
});
