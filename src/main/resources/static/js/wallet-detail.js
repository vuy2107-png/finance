function showTransactionDetail(btn) {
    const date = btn.getAttribute('data-date');
    const type = btn.getAttribute('data-type');
    const cat = btn.getAttribute('data-cat');
    const amount = btn.getAttribute('data-amount');
    const desc = btn.getAttribute('data-desc');
    const wallet = btn.getAttribute('data-wallet');
    const toWallet = btn.getAttribute('data-towallet');

    document.getElementById('detailDate').textContent = date;
    document.getElementById('detailCategory').textContent = cat;
    document.getElementById('detailWallet').textContent = wallet;
    document.getElementById('detailDescription').textContent = desc || '(Không có ghi chú)';
    
    const amountEl = document.getElementById('detailAmount');
    const badgeEl = document.getElementById('detailTypeBadge');
    
    if (desc && desc.includes('Nạp tiền vào quỹ')) {
        amountEl.textContent = '-' + amount + ' đ';
        amountEl.className = 'text-danger';
        badgeEl.textContent = 'TIẾT KIỆM';
        badgeEl.style.background = '#e0e7ff';
        badgeEl.style.color = '#4338ca';
        document.getElementById('detailToWalletRow').style.display = 'none';
    } else if (desc && desc.includes('Khởi tạo')) {
        amountEl.textContent = '+' + amount + ' đ';
        amountEl.className = 'text-success';
        badgeEl.textContent = 'VỐN QUẢN LÝ';
        badgeEl.style.background = '#d1fae5';
        badgeEl.style.color = '#059669';
        document.getElementById('detailToWalletRow').style.display = 'none';
    } else if (type === 'INCOME') {
        amountEl.textContent = '+' + amount + ' đ';
        amountEl.className = 'text-success';
        badgeEl.textContent = 'THU NHẬP';
        badgeEl.className = 'badge badge-income';
        document.getElementById('detailToWalletRow').style.display = 'none';
    } else if (type === 'EXPENSE') {
        amountEl.textContent = '-' + amount + ' đ';
        amountEl.className = 'text-danger';
        badgeEl.textContent = 'CHI TIÊU';
        badgeEl.className = 'badge badge-expense';
        document.getElementById('detailToWalletRow').style.display = 'none';
    } else {
        amountEl.textContent = amount + ' đ';
        amountEl.className = '';
        badgeEl.textContent = 'CHUYỂN KHOẢN';
        badgeEl.className = 'badge badge-blue';
        document.getElementById('detailToWalletRow').style.display = 'flex';
        document.getElementById('detailToWallet').textContent = toWallet;
    }

    document.getElementById('detailModal').classList.add('active');
}

function closeDetailModal() {
    document.getElementById('detailModal').classList.remove('active');
}

let activeWalletId = null;

function openBudgetModal(walletId, walletName, dailyLimit) {
    activeWalletId = walletId;
    document.getElementById('targetWalletName').textContent = walletName;
    document.getElementById('dailySpendingLimit').value = dailyLimit || 0;
    document.getElementById('modalMonthYear').textContent = `${window.currentMonth}/${window.currentYear}`;
    
    // Clear and populate category grid
    const grid = document.getElementById('categoryBudgetGrid');
    grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 2rem;"><i class="fas fa-spinner fa-spin"></i> Đang tải dữ liệu...</div>';
    
    document.getElementById('budgetModal').classList.add('active');

    // Fetch existing budgets
    fetch(`/user/wallets/${walletId}/budgets?month=${window.currentMonth}&year=${window.currentYear}`)
        .then(response => response.json())
        .then(budgetMap => {
            renderCategoryGrid(budgetMap);
        });
        
    // Update onchange for daily limit
    document.getElementById('dailySpendingLimit').onchange = () => saveDailyLimit(walletId);
}

function renderCategoryGrid(budgetMap) {
    const grid = document.getElementById('categoryBudgetGrid');
    grid.innerHTML = '';
    
    if (!window.allCategories || window.allCategories.length === 0) {
        grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 1rem; color: var(--text-muted);">Không có danh mục chi tiêu nào.</div>';
        return;
    }

    window.allCategories.forEach(cat => {
        if (cat.type !== 'EXPENSE') return;
        
        const amount = budgetMap[cat.id] || 0;
        const item = document.createElement('div');
        item.className = 'card';
        item.style.padding = '1rem';
        item.style.border = '1px solid var(--border-color)';
        item.style.background = '#f8fafc';
        item.style.marginBottom = '0';
        
        item.innerHTML = `
            <div style="display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.75rem;">
                <div style="width: 32px; height: 32px; border-radius: 0.5rem; background: white; color: var(--primary-color); display: flex; align-items: center; justify-content: center; border: 1px solid #e2e8f0;">
                    <i class="${cat.icon || 'fas fa-tag'}"></i>
                </div>
                <span style="font-weight: 600; color: var(--text-main); font-size: 0.875rem;">${cat.name}</span>
            </div>
            <div class="form-group" style="margin-bottom: 0;">
                <div style="position: relative;">
                    <input type="number" 
                           id="budget-${cat.id}"
                           class="form-control budget-input" 
                           placeholder="0"
                           value="${amount}"
                           onchange="saveBudget(${cat.id}, ${activeWalletId})"
                           style="padding-right: 2.5rem; font-weight: 600; text-align: right; font-size: 0.9375rem; border-radius: 0.5rem;">
                    <span style="position: absolute; right: 0.75rem; top: 50%; transform: translateY(-50%); color: var(--text-muted); font-size: 0.8125rem;">đ</span>
                </div>
            </div>
        `;
        grid.appendChild(item);
    });
}

function closeBudgetModal() {
    document.getElementById('budgetModal').classList.remove('active');
    activeWalletId = null;
}

function saveDailyLimit(walletId) {
    const dailyLimit = document.getElementById('dailySpendingLimit').value;
    
    fetch('/user/wallets/save-daily-limit', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content
        },
        body: `walletId=${walletId}&dailyLimit=${dailyLimit}`
    })
    .then(response => {
        if (response.ok) {
            showToast('Đã cập nhật hạn mức ngày! ✅', 'SUCCESS');
        } else {
            showToast('Lỗi khi cập nhật hạn mức. ❌', 'DANGER');
        }
    });
}

function saveBudget(categoryId, walletId) {
    const amount = document.getElementById('budget-' + categoryId).value;

    fetch('/user/budgets/save-ajax', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content
        },
        body: `categoryId=${categoryId}&amount=${amount}&month=${window.currentMonth}&year=${window.currentYear}&walletId=${walletId}`
    }).then(response => {
        if (response.ok) {
            showToast('Đã cập nhật ngân sách! ✅', 'SUCCESS');
        } else {
            showToast('Lỗi khi cập nhật ngân sách. ❌', 'DANGER');
        }
    });
}

function showToast(message, type) {
    if (window.showToast) {
        window.showToast(message, type);
    } else {
        alert(message);
    }
}

// Close when clicking outside
window.addEventListener('click', function(event) {
    const detailModal = document.getElementById('detailModal');
    const budgetModal = document.getElementById('budgetModal');
    if (event.target == detailModal) {
        closeDetailModal();
    }
    if (event.target == budgetModal) {
        closeBudgetModal();
    }
});
