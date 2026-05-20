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
    
    const selector = document.getElementById('walletSelectorContainer');
    const nameSection = document.getElementById('modalWalletName');
    
    if (walletId) {
        // Mở cho ví cụ thể
        selector.style.display = 'none';
        nameSection.style.display = 'block';
        document.getElementById('targetWalletName').textContent = walletName;
        
        // Luôn fetch giá trị mới nhất từ server để đảm bảo chính xác
        const today = new Date().toISOString().split('T')[0];
        fetch(`/user/wallets/${walletId}/effective-limit?date=${today}`)
            .then(res => res.json())
            .then(limit => {
                document.getElementById('dailySpendingLimit').value = limit || 0;
            });

        loadBudgetModalData(walletId);
    } else {
        // Mở tổng quát (Header)
        selector.style.display = 'block';
        nameSection.style.display = 'none';
        document.getElementById('dailySpendingLimit').value = 0;
        document.getElementById('categoryBudgetGrid').innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 2rem; color: var(--text-muted);">Vui lòng chọn ví để tiếp tục.</div>';
    }
    
    document.getElementById('modalMonthYear').textContent = `${window.currentMonth}/${window.currentYear}`;
    document.getElementById('budgetModal').classList.add('active');
    
    // Update onchange for daily limit
    document.getElementById('dailySpendingLimit').onchange = () => {
        if (activeWalletId) saveDailyLimit(activeWalletId);
        else showToast('Vui lòng chọn ví trước! ⚠️', 'WARNING');
    };
}

function changeModalWallet(walletId) {
    if (!walletId) {
        activeWalletId = null;
        document.getElementById('dailySpendingLimit').value = 0;
        document.getElementById('categoryBudgetGrid').innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 2rem; color: var(--text-muted);">Vui lòng chọn ví để tiếp tục.</div>';
        return;
    }
    
    activeWalletId = walletId;
    
    // Lấy hạn mức từ data-attribute của option (hoặc fetch từ server để chính xác nhất)
    // Để chính xác nhất (lịch sử), nên fetch từ server
    fetch(`/user/wallets/${walletId}/effective-limit?date=${window.currentYear}-${String(window.currentMonth).padStart(2,'0')}-01`)
        .then(res => res.json())
        .then(limit => {
            document.getElementById('dailySpendingLimit').value = limit || 0;
        });

    loadBudgetModalData(walletId);
}

function loadBudgetModalData(walletId) {
    const grid = document.getElementById('categoryBudgetGrid');
    grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 2rem;"><i class="fas fa-spinner fa-spin"></i> Đang tải dữ liệu...</div>';
    
    fetch(`/user/wallets/${walletId}/budgets?month=${window.currentMonth}&year=${window.currentYear}`)
        .then(response => response.json())
        .then(budgetMap => {
            renderCategoryGrid(budgetMap);
        });
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
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: `walletId=${walletId}&dailyLimit=${dailyLimit}`
    })
    .then(response => {
        if (response.ok) {
            showToast('Thiết lập hạn mức thành công! ✅', 'SUCCESS');
            
            // Cập nhật thuộc tính trên nút
            const btn = document.querySelector(`button[onclick*="openBudgetModal('${walletId}'"]`);
            if (btn) btn.setAttribute('data-limit', dailyLimit);
        } else {
            showToast('Lỗi khi thiết lập hạn mức. ❌', 'DANGER');
        }
    })
    .catch(err => {
        console.error('Save error:', err);
        showToast('Lỗi kết nối máy chủ. ❌', 'DANGER');
    });
}

function saveBudget(categoryId, walletId) {
    const amount = document.getElementById('budget-' + categoryId).value;
    
    fetch('/user/budgets/save-ajax', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: `categoryId=${categoryId}&amount=${amount}&month=${window.currentMonth}&year=${window.currentYear}&walletId=${walletId}`
    }).then(response => {
        if (response.ok) {
            showToast('Thiết lập ngân sách thành công! ✅', 'SUCCESS');
        } else {
            showToast('Lỗi khi thiết lập ngân sách. ❌', 'DANGER');
        }
    }).catch(err => {
        console.error('Save budget error:', err);
        showToast('Lỗi kết nối máy chủ. ❌', 'DANGER');
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
function openFundingModal() {
    document.getElementById('fundingModal').classList.add('active');
}

function closeFundingModal() {
    document.getElementById('fundingModal').classList.remove('active');
}

// Close when clicking outside
window.addEventListener('click', function(event) {
    const detailModal = document.getElementById('detailModal');
    const budgetModal = document.getElementById('budgetModal');
    const fundingModal = document.getElementById('fundingModal');
    if (event.target == detailModal) {
        closeDetailModal();
    }
    if (event.target == budgetModal) {
        closeBudgetModal();
    }
    if (event.target == fundingModal) {
        closeFundingModal();
    }
});
