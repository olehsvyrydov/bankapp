
function getToastContainer() {
    const containerId = 'toast-container';
    let container = document.getElementById(containerId);
    if (!container) {
        container = document.createElement('div');
        container.id = containerId;
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    return container;
}

function showNotification(message, type = 'info', duration = 4000) {
    if (!message) {
        return;
    }
    const container = getToastContainer();
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    container.appendChild(toast);

    requestAnimationFrame(() => toast.classList.add('show'));

    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, duration);
}



function showConfirmModal(message) {
    const existing = document.getElementById('confirm-modal');
    if (existing) {
        existing.remove();
    }
    const modal = document.createElement('div');
    modal.id = 'confirm-modal';
    modal.className = 'modal-overlay';
    modal.innerHTML = `
        <div class="modal">
            <p class="modal-message">${message || 'Are you sure?'}</p>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" id="modal-cancel">${document.body.dataset.modalCancel || 'Cancel'}</button>
                <button type="button" class="btn btn-primary" id="modal-confirm">${document.body.dataset.modalConfirm || 'Confirm'}</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);

    return new Promise(resolve => {
        modal.querySelector('#modal-cancel').addEventListener('click', () => {
            modal.remove();
            resolve(false);
        });
        modal.querySelector('#modal-confirm').addEventListener('click', () => {
            modal.remove();
            resolve(true);
        });
    });
}


// Validate registration form
document.addEventListener('DOMContentLoaded', function() {
    if (window.app && Array.isArray(window.app.flashErrors)) {
        window.app.flashErrors.forEach(msg => showNotification(msg, 'error', 6000));
    }
    if (window.app && Array.isArray(window.app.flashSuccess)) {
        window.app.flashSuccess.forEach(msg => showNotification(msg, 'success', 4000));
    }

    const registerForm = document.getElementById('registerForm');

    if (registerForm) {
        registerForm.addEventListener('submit', function(e) {
            const birthDate = document.getElementById('birthDate').value;
            const age = calculateAge(new Date(birthDate));
            const ageMessage = registerForm.dataset.ageMessage || 'You must be at least 18 years old to register.';

            if (age < 18) {
                e.preventDefault();
                showNotification(ageMessage, 'error');
                return false;
            }
        });
    }


    document.querySelectorAll('form[data-confirm]').forEach(form => {
        form.addEventListener('submit', function(e) {
            if (form.dataset.confirmPending === 'true') {
                form.dataset.confirmPending = '';
                return;
            }
            e.preventDefault();
            const message = form.getAttribute('data-confirm');
            showConfirmModal(message).then(confirmed => {
                if (confirmed) {
                    form.dataset.confirmPending = 'true';
                    form.submit();
                }
            });
        });
    });

    // Prevent selecting same account for transfers
    const fromAccount = document.getElementById('fromAccount');
    const toAccount = document.getElementById('toAccount');

    if (fromAccount && toAccount) {
        const sameAccountMessage = document.body.dataset.transferSameAccount || 'Cannot transfer to the same account';
        fromAccount.addEventListener('change', function() {
            if (fromAccount.value === toAccount.value) {
                showNotification(sameAccountMessage, 'error');
                toAccount.value = '';
            }
        });

        toAccount.addEventListener('change', function() {
            if (fromAccount.value === toAccount.value) {
                showNotification(sameAccountMessage, 'error');
                toAccount.value = '';
            }
        });
    }

    // Auto-refresh exchange rates every 5 seconds
    const exchangeRatesTable = document.querySelector('.exchange-rates table tbody');
    if (exchangeRatesTable) {
        setInterval(refreshExchangeRates, 5000);
    }

    // Toggle between email and account ID for transfers
    const recipientTypeEmail = document.getElementById('recipientTypeEmail');
    const recipientTypeAccountId = document.getElementById('recipientTypeAccountId');
    const emailGroup = document.getElementById('emailGroup');
    const accountIdGroup = document.getElementById('accountIdGroup');
    const recipientEmail = document.getElementById('recipientEmail');
    const recipientBankAccountId = document.getElementById('recipientBankAccountId');

    if (recipientTypeEmail && recipientTypeAccountId) {
        recipientTypeEmail.addEventListener('change', function() {
            if (this.checked) {
                emailGroup.style.display = 'block';
                accountIdGroup.style.display = 'none';
                recipientEmail.required = true;
                recipientBankAccountId.required = false;
                recipientBankAccountId.value = '';
            }
        });

        recipientTypeAccountId.addEventListener('change', function() {
            if (this.checked) {
                emailGroup.style.display = 'none';
                accountIdGroup.style.display = 'block';
                recipientEmail.required = false;
                recipientBankAccountId.required = true;
                recipientEmail.value = '';
            }
        });
    }
});

function calculateAge(birthDate) {
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();

    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
        age--;
    }

    return age;
}

function refreshExchangeRates() {
    fetch('/api/exchange/rates')
        .then(response => response.json())
        .then(data => {
            const tbody = document.querySelector('.exchange-rates table tbody');
            if (tbody && data) {
                tbody.innerHTML = '';
                data.forEach(rate => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${rate.currency}</td>
                        <td>${rate.buyRate.toFixed(2)}</td>
                        <td>${rate.sellRate.toFixed(2)}</td>
                    `;
                    tbody.appendChild(row);
                });
            }
        })
        .catch(error => console.error('Error refreshing exchange rates:', error));
}

// Form validation helpers
function validateAmount(input) {
    const value = parseFloat(input.value);
    const positiveMessage = document.body.dataset.amountPositive || 'Amount must be positive';
    if (value <= 0) {
        showNotification(positiveMessage, 'error');
        input.value = '';
        return false;
    }
    return true;
}

// Confirmation dialogs
function confirmDelete(message) {
    return showConfirmModal(message || document.body.dataset.modalDelete || 'Are you sure you want to delete this item?');
}
