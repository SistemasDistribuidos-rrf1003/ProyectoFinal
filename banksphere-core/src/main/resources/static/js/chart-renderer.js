/**
 * BANKSphere - MOTOR DE RENDERIZADO DE GRÁFICOS (CHART.JS INTEGRATION)
 * -----------------------------------------------------------------------------
 * Inicializa y renderiza los gráficos de analíticas financieras del Dashboard.
 * Recupera de forma dinámica las variables CSS (HSL) para asegurar una consistencia
 * visual perfecta y soporte automático para cambios de modo o temas.
 */

document.addEventListener("DOMContentLoaded", () => {

    // =========================================================================
    // 0. HELPER: Recuperador Dinámico de Variables CSS (Design Tokens)
    // =========================================================================
    const getThemeColor = (variableName) => {
        return getComputedStyle(document.documentElement)
            .getPropertyValue(variableName)
            .trim();
    };

    // Obtenemos los colores primarios y de estado definidos en variables.css
    const primaryNeon  = getThemeColor('--primary-neon')  || '#00f2fe';
    const primaryBlue  = getThemeColor('--primary-blue')  || '#4facfe';
    const accentPurple = getThemeColor('--accent-purple') || '#b15cff';
    const accentIndigo = getThemeColor('--accent-indigo') || '#5c53ff';
    const incomeColor  = getThemeColor('--income-color')  || '#34d399';
    const expenseColor = getThemeColor('--expense-color') || '#f87171';
    const warningColor = getThemeColor('--warning-color') || '#fbbf24';
    const textMuted    = getThemeColor('--text-muted')    || '#9ca3af';

    // Opciones globales comunes para los Tooltips del Dashboard
    const commonTooltipOptions = {
        backgroundColor: 'rgba(17, 24, 39, 0.95)',
        titleFont: { family: 'Inter', size: 13, weight: 'bold' },
        bodyFont: { family: 'Inter', size: 12 },
        borderColor: 'rgba(255, 255, 255, 0.08)',
        borderWidth: 1,
        padding: 12,
        cornerRadius: 10,
        displayColors: true
    };

    // =========================================================================
    // 1. GRÁFICO DE LÍNEA: Evolución Histórica de Saldo
    // =========================================================================
    const evolutionCanvas = document.getElementById('evolutionChart');
    if (evolutionCanvas) {
        const ctx = evolutionCanvas.getContext('2d');

        // Creamos un gradiente vertical translúcido para el relleno de la curva
        const gradientFill = ctx.createLinearGradient(0, 0, 0, 300);
        gradientFill.addColorStop(0, 'rgba(0, 242, 254, 0.15)');
        gradientFill.addColorStop(1, 'rgba(0, 242, 254, 0.0)');

        new Chart(ctx, {
            type: 'line',
            data: {
                labels: ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun'],
                datasets: [{
                    label: 'Saldo Disponible (€)',
                    data: [18500, 21000, 19500, 24800, 22200, 29850],
                    borderColor: primaryNeon,
                    backgroundColor: gradientFill,
                    borderWidth: 3,
                    fill: true,
                    tension: 0.4, // Curva suave (Spline)
                    pointBackgroundColor: primaryBlue,
                    pointBorderColor: '#ffffff',
                    pointRadius: 4,
                    pointHoverRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: commonTooltipOptions
                },
                scales: {
                    y: {
                        grid: { color: 'rgba(255, 255, 255, 0.04)' },
                        ticks: { color: textMuted, font: { family: 'Inter', size: 11 } }
                    },
                    x: {
                        grid: { display: false },
                        ticks: { color: textMuted, font: { family: 'Inter', size: 11 } }
                    }
                }
            }
        });
    }

    // =========================================================================
    // 2. GRÁFICO DOUGHNUT (TORTA): Distribución de Gastos por Categorías
    // =========================================================================
    const doughnutCanvas = document.getElementById('expensesDoughnutChart');
    if (doughnutCanvas) {
        const ctx = doughnutCanvas.getContext('2d');

        new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['Alquiler', 'Ocio/Restauración', 'Suscripciones', 'Compras', 'Viajes'],
                datasets: [{
                    data: [850, 340, 120, 480, 290], // Importes de simulación
                    backgroundColor: [
                        accentPurple,
                        primaryBlue,
                        warningColor,
                        incomeColor,
                        expenseColor
                    ],
                    borderColor: 'rgba(20, 24, 39, 0.8)', // Combina con el fondo de la tarjeta
                    borderWidth: 3,
                    hoverOffset: 8
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '72%', // Grosor de la rosca del Doughnut
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            color: textMuted,
                            padding: 15,
                            font: { family: 'Inter', size: 11 }
                        }
                    },
                    tooltip: commonTooltipOptions
                }
            }
        });
    }

    // =========================================================================
    // 3. GRÁFICO DE BARRAS: Comparativa Mensual de Ingresos vs Egresos
    // =========================================================================
    const barCanvas = document.getElementById('barComparisonChart');
    if (barCanvas) {
        const ctx = barCanvas.getContext('2d');

        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['Mar', 'Abr', 'May', 'Jun'],
                datasets: [
                    {
                        label: 'Ingresos (€)',
                        data: [4200, 5800, 3900, 6200],
                        backgroundColor: incomeColor,
                        borderRadius: 6, // Esquinas de las barras redondeadas
                        borderWidth: 0
                    },
                    {
                        label: 'Gastos (€)',
                        data: [3100, 4500, 2800, 4900],
                        backgroundColor: expenseColor,
                        borderRadius: 6,
                        borderWidth: 0
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top',
                        labels: {
                            color: textMuted,
                            font: { family: 'Inter', size: 11 }
                        }
                    },
                    tooltip: commonTooltipOptions
                },
                scales: {
                    y: {
                        grid: { color: 'rgba(255, 255, 255, 0.04)' },
                        ticks: { color: textMuted, font: { family: 'Inter', size: 11 } }
                    },
                    x: {
                        grid: { display: false },
                        ticks: { color: textMuted, font: { family: 'Inter', size: 11 } }
                    }
                }
            }
        });
    }
});