/* Transaction List Interactions */

document.addEventListener('DOMContentLoaded', function() {
    const modal = document.getElementById('transactionModal');
    const form = document.getElementById('transactionForm');
    const modalTitle = document.getElementById('modalTitle');
    const submitBtn = document.getElementById('submitBtn');

    // Define globally so HTML onclick can reach them
    window.openAddModal = function() {
        if (!modalTitle || !submitBtn || !form) return;
        
        modalTitle.innerText = "Thêm giao dịch mới";
        submitBtn.innerText = "Lưu giao dịch";
        form.action = "/user/transactions/create";
        form.reset();
        
        const idField = document.getElementById('transactionId');
        if (idField) idField.value = "";
        
        // Default date to today
        const dateField = document.getElementById('date');
        if (dateField) dateField.value = new Date().toISOString().split('T')[0];
        
        // Trigger category filtering
        if (typeof window.filterCategories === 'function') {
            window.filterCategories();
        }
        
        if (modal) modal.classList.add('active');
    };

    window.openEditModal = function(id) {
        if (!modalTitle || !submitBtn || !form) return;

        modalTitle.innerText = "Cập nhật giao dịch";
        submitBtn.innerText = "Lưu thay đổi";
        form.action = "/user/transactions/edit";
        
        // Fetch data from API
        fetch('/api/dashboard/transaction/' + id)
            .then(response => response.json())
            .then(data => {
                const idField = document.getElementById('transactionId');
                const typeField = document.getElementById('type');
                const amountField = document.getElementById('amount');
                const descField = document.getElementById('description');
                const locField = document.getElementById('location');
                const catField = document.getElementById('category');
                const walletField = document.getElementById('wallet');
                const toWalletField = document.getElementById('toWallet');

                if (idField) idField.value = data.id;
                if (typeField) {
                    typeField.value = data.type;
                    // Trigger change to show/hide fields
                    typeField.dispatchEvent(new Event('change'));
                }
                if (amountField) amountField.value = data.amount;
                if (dateField) dateField.value = data.date;
                if (descField) descField.value = data.description;
                if (locField) locField.value = data.location || "";
                if (walletField && data.wallet) walletField.value = data.wallet.id;
                if (toWalletField && data.toWallet) toWalletField.value = data.toWallet.id;
                
                // Delay to ensure filterCategories runs after type is set
                setTimeout(() => {
                    if (typeof window.filterCategories === 'function') {
                        window.filterCategories();
                    }
                    if (catField) catField.value = data.category ? data.category.id : "";
                }, 50);

                if (modal) modal.classList.add('active');
            })
            .catch(err => console.error("Error fetching transaction:", err));
    };

    window.closeModal = function() {
        if (modal) modal.classList.remove('active');
    };

    window.onclick = function(event) {
        if (event.target == modal || event.target.classList.contains('modal-overlay')) {
            window.closeModal();
        }
    };
});
