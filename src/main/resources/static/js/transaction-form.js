document.addEventListener('DOMContentLoaded', function() {
    const typeSelect = document.getElementById('type');
    const categoryInput = document.getElementById('categoryInput');
    const categoryOptions = document.getElementById('categoryOptions');
    const walletSelect = document.getElementById('wallet');
    const toWalletSelect = document.getElementById('toWallet');
    const toWalletGroup = document.getElementById('toWalletGroup');
    const categoryGroup = document.getElementById('categoryGroup');
    const walletLabel = document.querySelector('label[for="wallet"]');

    if (!typeSelect || !categoryOptions || !walletSelect) return;

    // Lưu trữ toàn bộ danh sách gốc từ datalist
    const masterOptions = Array.from(categoryOptions.options).map(opt => ({
        value: opt.value,
        type: (opt.getAttribute('data-type') || '').toUpperCase().trim()
    }));

    // Lưu trữ danh sách ví nhận gốc
    const masterToWalletOptions = toWalletSelect ? Array.from(toWalletSelect.options).map(opt => ({
        value: opt.value,
        text: opt.text
    })) : [];

    function filterCategories() {
        const selectedType = typeSelect.value.toUpperCase().trim();
        console.log("Filtering categories for type:", selectedType);
        
        if (selectedType === 'TRANSFER') {
            if (categoryGroup) categoryGroup.style.display = 'none';
            if (toWalletGroup) toWalletGroup.style.display = 'block';
            if (toWalletSelect) toWalletSelect.required = true;
            if (categoryInput) categoryInput.required = false;
            if (walletLabel) walletLabel.innerText = 'Từ ví';
            filterToWallets();
        } else {
            if (categoryGroup) categoryGroup.style.display = 'block';
            if (toWalletGroup) toWalletGroup.style.display = 'none';
            if (toWalletSelect) toWalletSelect.required = false;
            if (categoryInput) categoryInput.required = true;
            if (walletLabel) walletLabel.innerText = 'Ví thực hiện';
            
            // Xóa sạch datalist hiện tại
            categoryOptions.innerHTML = '';
            
            // Chỉ thêm lại các option phù hợp với Type
            masterOptions.forEach(optData => {
                if (!optData.type || optData.type === selectedType) {
                    const newOpt = document.createElement('option');
                    newOpt.value = optData.value;
                    newOpt.setAttribute('data-type', optData.type);
                    categoryOptions.appendChild(newOpt);
                }
            });
            
            // Xóa giá trị cũ nếu không phù hợp (tùy chọn)
            // categoryInput.value = ''; 
        }
    }

    function filterToWallets() {
        if (!toWalletSelect || typeSelect.value !== 'TRANSFER') return;
        
        const sourceWalletId = walletSelect.value;
        const currentToValue = toWalletSelect.value;
        
        toWalletSelect.innerHTML = '';
        masterToWalletOptions.forEach(optData => {
            if (optData.value === "" || optData.value !== sourceWalletId) {
                const newOpt = document.createElement('option');
                newOpt.value = optData.value;
                newOpt.text = optData.text;
                toWalletSelect.appendChild(newOpt);
            }
        });
        
        const exists = Array.from(toWalletSelect.options).some(o => o.value === currentToValue);
        toWalletSelect.value = exists ? currentToValue : '';
    }

    const amountInput = document.getElementById('amount');
    const balanceWarning = document.createElement('div');
    balanceWarning.style.color = 'var(--danger)';
    balanceWarning.style.fontSize = '0.75rem';
    balanceWarning.style.marginTop = '0.25rem';
    balanceWarning.style.display = 'none';
    balanceWarning.id = 'balanceWarning';
    if (amountInput) amountInput.parentNode.appendChild(balanceWarning);

    function checkBalance() {
        if (!amountInput || !walletSelect || !typeSelect) return;
        
        const type = typeSelect.value;
        const amount = parseFloat(amountInput.value) || 0;
        const selectedOption = walletSelect.options[walletSelect.selectedIndex];
        
        if (!selectedOption || !selectedOption.value) {
            balanceWarning.style.display = 'none';
            amountInput.style.borderColor = '';
            return;
        }

        const balance = parseFloat(selectedOption.getAttribute('data-balance')) || 0;

        if ((type === 'EXPENSE' || type === 'TRANSFER') && amount > balance) {
            balanceWarning.innerText = `💡 Lưu ý: Giao dịch này sẽ khiến số dư ví trở nên âm (Hiện có: ${new Intl.NumberFormat('vi-VN').format(balance)}đ)`;
            balanceWarning.style.color = '#f59e0b'; // Màu cam cảnh báo
            balanceWarning.style.display = 'block';
            amountInput.style.borderColor = '#f59e0b';
        } else {
            balanceWarning.style.display = 'none';
            balanceWarning.style.color = 'var(--danger)';
            amountInput.style.borderColor = '';
        }
    }

    typeSelect.addEventListener('change', filterCategories);
    typeSelect.addEventListener('change', checkBalance);
    if (walletSelect) walletSelect.addEventListener('change', () => {
        filterToWallets();
        checkBalance();
    });
    if (amountInput) amountInput.addEventListener('input', checkBalance);
    
    // Chặn ngày tương lai
    const dateInput = document.getElementById('date');
    if (dateInput && window.effectiveDate) {
        dateInput.max = window.effectiveDate;
        if (dateInput.value > window.effectiveDate) {
            dateInput.value = window.effectiveDate;
        }
    }

    // Khởi tạo lần đầu
    filterCategories();
    if (walletSelect) checkBalance();
});
