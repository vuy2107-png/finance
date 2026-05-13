document.addEventListener('DOMContentLoaded', function() {
    // --- Donut Chart (Categories) ---
    fetch('/api/dashboard/category-stats')
        .then(response => response.json())
        .then(data => {
            const labels = Object.keys(data);
            const series = Object.values(data);
            
            if (labels.length === 0) {
                document.getElementById('categoryDonutChart').innerHTML = '<div style="padding: 20px; color: var(--text-muted); text-align: center;">Chưa có dữ liệu chi tiêu tháng này</div>';
                return;
            }

            const options = {
                series: series,
                chart: {
                    type: 'donut',
                    height: 350,
                    fontFamily: 'Inter, sans-serif'
                },
                labels: labels,
                colors: ['#2563eb', '#059669', '#e11d48', '#f59e0b', '#7c3aed', '#db2777', '#0891b2', '#4b5563'],
                responsive: [{
                    breakpoint: 480,
                    options: {
                        chart: { width: 200 },
                        legend: { position: 'bottom' }
                    }
                }],
                legend: { position: 'bottom' },
                stroke: { show: false }
            };

            const chart = new ApexCharts(document.querySelector("#categoryDonutChart"), options);
            chart.render();
        });

    // --- Area Chart (Monthly Trend) ---
    fetch('/api/dashboard/monthly-trend')
        .then(response => response.json())
        .then(data => {
            if (!data.labels || data.labels.length === 0) {
                document.getElementById('monthlyTrendChart').innerHTML = '<div style="padding: 20px; color: var(--text-muted); text-align: center;">Chưa có đủ dữ liệu thống kê tháng</div>';
                return;
            }

            const options = {
                series: [{
                    name: 'Thu nhập',
                    data: data.income
                }, {
                    name: 'Chi tiêu',
                    data: data.expense
                }],
                chart: {
                    type: 'area',
                    height: 350,
                    toolbar: { show: false },
                    fontFamily: 'Inter, sans-serif'
                },
                dataLabels: { enabled: false },
                stroke: { curve: 'smooth', width: 2 },
                xaxis: {
                    categories: data.labels,
                },
                colors: ['#059669', '#e11d48'],
                fill: {
                    type: 'gradient',
                    gradient: {
                        shadeIntensity: 1,
                        opacityFrom: 0.45,
                        opacityTo: 0.05,
                        stops: [20, 100, 100, 100]
                    }
                },
                tooltip: {
                    y: {
                        formatter: function (val) {
                            return val.toLocaleString() + " ₫"
                        }
                    }
                },
                grid: {
                    borderColor: '#f1f5f9'
                }
            };

            const chart = new ApexCharts(document.querySelector("#monthlyTrendChart"), options);
            chart.render();
        });
});
