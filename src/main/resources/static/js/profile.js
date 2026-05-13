function toggleEditProfile() {
    const form = document.getElementById('profileForm');
    const inputs = form.querySelectorAll('input, select, textarea');
    const editBtn = document.getElementById('btnEditProfile');
    const actionButtons = document.getElementById('actionButtons');

    // Toggle readonly/disabled state
    const isReadOnly = inputs[0].hasAttribute('readonly') || inputs[0].hasAttribute('disabled');

    inputs.forEach(input => {
        if (input.tagName === 'SELECT') {
            input.disabled = !isReadOnly;
        } else {
            if (isReadOnly) {
                input.removeAttribute('readonly');
                input.classList.remove('readonly-input');
            } else {
                input.setAttribute('readonly', true);
                input.classList.add('readonly-input');
            }
        }
    });

    if (isReadOnly) {
        // Switching to EDIT mode
        editBtn.style.display = 'none';
        actionButtons.style.display = 'flex';
    } else {
        // Switching to VIEW mode
        editBtn.style.display = 'block';
        actionButtons.style.display = 'none';
    }
}

function cancelEditProfile() {
    window.location.reload(); // Quick way to revert all changes
}
