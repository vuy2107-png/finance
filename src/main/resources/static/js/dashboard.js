function openBudgetModal() {
    document.getElementById('budgetModal').classList.add('active');
}
function closeBudgetModal() {
    document.getElementById('budgetModal').classList.remove('active');
}

function initDashboardCharts(monthlyStats, categoryStats, weeklyStats) {
    /* Chart 1: Revenue & Expenses (Line Chart with Gradients) */
    const mainCtx = document.getElementById('mainChart').getContext('2d');
    const monthlyLabels = monthlyStats ? (monthlyStats.labels || []) : [];
    const incomeData = monthlyStats ? (monthlyStats.income || []) : [];
    const expenseData = monthlyStats ? (monthlyStats.expense || []) : [];

    const incomeGradient = mainCtx.createLinearGradient(0, 0, 0, 400);
    incomeGradient.addColorStop(0, 'rgba(5, 150, 105, 0.4)');
    incomeGradient.addColorStop(1, 'rgba(5, 150, 105, 0.0)');

    const expenseGradient = mainCtx.createLinearGradient(0, 0, 0, 400);
    expenseGradient.addColorStop(0, 'rgba(225, 29, 72, 0.4)');
    expenseGradient.addColorStop(1, 'rgba(225, 29, 72, 0.0)');

    new Chart(mainCtx, {
        type: 'line',
        data: {
            labels: monthlyLabels,
            datasets: [
                {
                    label: 'Vốn quản lý',
                    data: incomeData || [],
                    borderColor: '#059669',
                    backgroundColor: incomeGradient,
                    fill: true,
                    tension: 0.4,
                    borderWidth: 3,
                    pointRadius: 4,
                    pointBackgroundColor: '#059669',
                    pointHoverRadius: 6
                },
                {
                    label: 'Chi tiêu',
                    data: expenseData,
                    borderColor: '#e11d48',
                    backgroundColor: expenseGradient,
                    fill: true,
                    tension: 0.4,
                    borderWidth: 3,
                    pointRadius: 4,
                    pointBackgroundColor: '#e11d48',
                    pointHoverRadius: 6
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                intersect: false,
                mode: 'index'
            },
            plugins: {
                legend: { 
                    position: 'top',
                    align: 'end',
                    labels: { boxWidth: 10, usePointStyle: true, padding: 20 }
                },
                tooltip: {
                    backgroundColor: '#1e293b',
                    padding: 12,
                    callbacks: {
                        label: function(context) {
                            let label = context.dataset.label || '';
                            if (label) label += ': ';
                            if (context.parsed.y !== null) {
                                label += new Intl.NumberFormat('vi-VN').format(context.parsed.y) + ' đ';
                            }
                            return label;
                        }
                    }
                }
            },
            scales: {
                x: { grid: { display: false } },
                y: { 
                    beginAtZero: true,
                    grid: { borderDash: [5, 5], color: '#e2e8f0' },
                    ticks: {
                        callback: function(value) {
                            if (value >= 1000000) return (value / 1000000) + 'M';
                            return value.toLocaleString('vi-VN');
                        }
                    }
                }
            }
        }
    });

    /* Chart 2: Category Distribution (Doughnut Chart) */
    const catCtx = document.getElementById('categoryChart').getContext('2d');
    const catLabels = Object.keys(categoryStats || {});
    const catValues = Object.values(categoryStats || {});
    const catTotal = catValues.reduce((a, b) => a + b, 0);
    
    // Pre-calculate labels with percentages for the legend
    const catLabelsWithPercent = catLabels.map((label, index) => {
        const val = catValues[index];
        const percent = catTotal > 0 ? ((val / catTotal) * 100).toFixed(1) : 0;
        return `${label} (${percent}%)`;
    });

    new Chart(catCtx, {
        type: 'doughnut',
        data: {
            labels: catLabelsWithPercent,
            datasets: [{
                data: catValues,
                backgroundColor: [
                    '#2563eb', '#059669', '#e11d48', '#f59e0b', '#7c3aed', '#db2777', '#0891b2'
                ],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { 
                    position: 'bottom',
                    labels: {
                        padding: 20,
                        usePointStyle: true
                    }
                },
                tooltip: {
                    backgroundColor: '#1e293b',
                    padding: 12,
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.parsed || 0;
                            const percent = catTotal > 0 ? ((value / catTotal) * 100).toFixed(1) : 0;
                            return ` ${label}: ${new Intl.NumberFormat('vi-VN').format(value)} đ`;
                        }
                    }
                }
            },
            cutout: '70%'
        }
    });

    /* Chart 3: Weekly Trend (Bar Chart) */
    const weeklyCtx = document.getElementById('weeklyChart').getContext('2d');
    const weeklyLabels = weeklyStats ? (weeklyStats.labels || []).map(l => {
        const d = new Date(l);
        return d.getDate() + '/' + (d.getMonth() + 1);
    }) : [];
    const weeklyIncome = weeklyStats ? (weeklyStats.income || []) : [];
    const weeklyExpense = weeklyStats ? (weeklyStats.expense || []) : [];

    new Chart(weeklyCtx, {
        type: 'bar',
        data: {
            labels: weeklyLabels,
            datasets: [
                {
                    label: 'Vốn quản lý',
                    data: weeklyIncome,
                    backgroundColor: '#059669',
                    borderRadius: 4
                },
                {
                    label: 'Chi tiêu',
                    data: weeklyExpense,
                    backgroundColor: '#e11d48',
                    borderRadius: 4
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'bottom' }
            },
            scales: {
                y: { beginAtZero: true }
            }
        }
    });
}
