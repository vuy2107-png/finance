function openAddGoalModal() {
    document.getElementById('addGoalModal').classList.add('active');
}
function closeAddGoalModal() {
    document.getElementById('addGoalModal').classList.remove('active');
}

function openAddFundsModal(goalId, goalName) {
    document.getElementById('fundsGoalId').value = goalId;
    document.getElementById('fundsGoalName').textContent = goalName;
    document.getElementById('addFundsModal').classList.add('active');
}
function closeAddFundsModal() {
    document.getElementById('addFundsModal').classList.remove('active');
}

// Balance Validation for Adding Funds
document.addEventListener('DOMContentLoaded', function() {
    const fundsAmount = document.getElementById('fundsAmount');
    const fundsWallet = document.getElementById('fundsWallet');
    const warning = document.getElementById('fundsBalanceWarning');
    const btnSubmit = document.getElementById('btnSubmitFunds');

    if (fundsAmount && fundsWallet) {
        function checkSavingsBalance() {
            const amount = parseFloat(fundsAmount.value) || 0;
            const selectedOpt = fundsWallet.options[fundsWallet.selectedIndex];
            
            if (!selectedOpt || !selectedOpt.value) {
                warning.style.display = 'none';
                btnSubmit.disabled = false;
                return;
            }

            const balance = parseFloat(selectedOpt.getAttribute('data-balance')) || 0;

            if (amount > balance) {
                warning.textContent = `⚠️ Số dư không đủ (Hiện có: ${new Intl.NumberFormat('vi-VN').format(balance)}đ)`;
                warning.style.display = 'block';
                fundsAmount.style.borderColor = 'var(--danger)';
                btnSubmit.disabled = true;
                btnSubmit.style.opacity = '0.5';
                btnSubmit.style.cursor = 'not-allowed';
            } else {
                warning.style.display = 'none';
                fundsAmount.style.borderColor = '';
                btnSubmit.disabled = false;
                btnSubmit.style.opacity = '1';
                btnSubmit.style.cursor = 'pointer';
            }
        }

        fundsAmount.addEventListener('input', checkSavingsBalance);
        fundsWallet.addEventListener('change', checkSavingsBalance);
    }
});

// Close modals when clicking outside
document.addEventListener('mousedown', function(event) {
    const addGoalModal = document.getElementById('addGoalModal');
    const addFundsModal = document.getElementById('addFundsModal');
    
    if (event.target === addGoalModal) {
        closeAddGoalModal();
    }
    if (event.target === addFundsModal) {
        closeAddFundsModal();
    }
});
