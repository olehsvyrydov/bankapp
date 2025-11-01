
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


// Validate registration form
document.addEventListener('DOMContentLoaded', function() {
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
    fetch('/api/exchange-rates')
        .then(response => response.json())
        .then(data => {
            const tbody = document.querySelector('.exchange-rates table tbody');
            if (tbody && data) {
                tbody.innerHTML = '';
                data.forEach(rate => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${rate.currency}</td>
                        <td>${rate.buyRate.toFixed(4)}</td>
                        <td>${rate.sellRate.toFixed(4)}</td>
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
    return confirm(message || 'Are you sure you want to delete this item?');
}
