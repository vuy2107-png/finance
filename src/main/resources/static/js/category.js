const icons = [
    'fas fa-tag', 'fas fa-utensils', 'fas fa-shopping-cart', 'fas fa-car', 'fas fa-home',
    'fas fa-film', 'fas fa-heartbeat', 'fas fa-file-invoice-dollar', 'fas fa-gift', 'fas fa-briefcase',
    'fas fa-chart-line', 'fas fa-money-bill-wave', 'fas fa-graduation-cap', 'fas fa-plane', 'fas fa-coffee',
    'fas fa-gamepad', 'fas fa-dumbbell', 'fas fa-pills', 'fas fa-tshirt', 'fas fa-tools'
];

function initIconPicker() {
    const grid = document.getElementById('iconPicker');
    if (!grid) return;
    grid.innerHTML = '';
    icons.forEach(iconClass => {
        const div = document.createElement('div');
        div.className = 'icon-option';
        div.innerHTML = `<i class="${iconClass}"></i>`;
        div.onclick = () => selectIcon(iconClass, div);
        grid.appendChild(div);
    });
}

function selectIcon(iconClass, element) {
    document.getElementById('catIcon').value = iconClass;
    document.querySelectorAll('.icon-option').forEach(el => el.classList.remove('selected'));
    element.classList.add('selected');
}

function openAddModal() {
    document.getElementById('modalTitle').innerText = 'Thêm danh mục mới';
    document.getElementById('categoryId').value = '';
    document.getElementById('catName').value = '';
    document.getElementById('catDesc').value = '';
    document.getElementById('catColor').value = '#3b82f6';
    document.getElementById('catIcon').value = 'fas fa-tag';
    document.getElementById('catType').value = 'EXPENSE';
    document.querySelectorAll('.icon-option').forEach(el => el.classList.remove('selected'));

    // Hiển thị modal
    const modal = document.getElementById('categoryModal');
    modal.style.display = 'flex'; // hoặc 'block', tùy CSS
    modal.classList.add('active');
}

function openEditModal(id, name, type, icon, color, desc) {
    document.getElementById('modalTitle').innerText = 'Chỉnh sửa danh mục';
    document.getElementById('categoryId').value = id;
    document.getElementById('catName').value = name;
    document.getElementById('catType').value = type;
    document.getElementById('catIcon').value = icon || 'fas fa-tag';
    document.getElementById('catColor').value = color || '#3b82f6';
    document.getElementById('catDesc').value = desc || '';

    document.querySelectorAll('.icon-option').forEach(el => {
        el.classList.remove('selected');
        if (el.querySelector('i').className === (icon || 'fas fa-tag')) {
            el.classList.add('selected');
        }
    });

    // Hiển thị modal
    const modal = document.getElementById('categoryModal');
    modal.style.display = 'flex';
    modal.classList.add('active');
}

function closeModal() {
    const modal = document.getElementById('categoryModal');
    modal.style.display = 'none';
    modal.classList.remove('active');
}

document.addEventListener('DOMContentLoaded', () => {
    // Ẩn modal mặc định (đảm bảo không che khuất giao diện)
    const modal = document.getElementById('categoryModal');
    if (modal) {
        modal.style.display = 'none';
    }

    initIconPicker();

    // Handle Edit Button Clicks
    document.addEventListener('click', (e) => {
        const editBtn = e.target.closest('.btn-edit');
        if (editBtn) {
            const data = editBtn.dataset;
            openEditModal(
                data.id,
                data.name,
                data.type,
                data.icon,
                data.color,
                data.desc
            );
        }
    });
});