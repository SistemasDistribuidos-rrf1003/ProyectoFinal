/**
 * BANKSphere - GESTOR DE TEMA DUAL (DARK / LIGHT MODE)
 * -----------------------------------------------------------------------------
 * Administra la persistencia de la preferencia del tema visual en el LocalStorage
 * del navegador del usuario y controla los cambios de estado del interruptor UI.
 */

// 1. Lógica de Aplicación Inmediata de Tema
// Se ejecuta al cargar el script para evitar el molesto destello de color
(function initTheme() {
    const savedTheme = localStorage.getItem('theme');

    if (savedTheme === 'light') {
        document.documentElement.classList.add('light-mode');
    } else {
        document.documentElement.classList.remove('light-mode');
    }
})();

// 2. Lógica de interacción tras cargarse el DOM
document.addEventListener("DOMContentLoaded", () => {
    const themeToggleBtn = document.getElementById('themeToggle');
    const toggleIcon = document.getElementById('themeToggleIcon');
    const toggleText = document.getElementById('themeToggleText');

    if (!themeToggleBtn) return; // Si la página no contiene la sidebar, salimos de forma segura

    // Función auxiliar para actualizar los textos e iconos del botón de manera limpia
    const updateToggleButtonUI = (isLight) => {
        if (isLight) {
            // Si estamos en Modo Claro, el botón sugiere cambiar a Modo Oscuro
            toggleIcon.className = 'bi bi-moon-stars-fill me-2 text-warning';
            toggleText.innerText = 'Modo Oscuro';
            themeToggleBtn.style.color = '#0f172a'; // Ajusta texto en modo claro
        } else {
            // Si estamos en Modo Oscuro, el botón sugiere cambiar a Modo Claro
            toggleIcon.className = 'bi bi-sun-fill me-2 text-warning';
            toggleText.innerText = 'Modo Claro';
            themeToggleBtn.style.color = 'var(--text-muted)';
        }
    };

    // Inicializamos el botón de la UI según el estado actual de la clase en html
    const isCurrentlyLight = document.documentElement.classList.contains('light-mode');
    updateToggleButtonUI(isCurrentlyLight);

    // Evento Click: Alterna el tema y guarda en el localStorage del cliente
    themeToggleBtn.addEventListener('click', () => {
        const isLight = document.documentElement.classList.toggle('light-mode');

        // Persistencia en el navegador
        if (isLight) {
            localStorage.setItem('theme', 'light');
        } else {
            localStorage.setItem('theme', 'dark');
        }

        // Actualizamos los textos e iconos instantáneamente
        updateToggleButtonUI(isLight);

        // [Opcional]: Si el dashboard contiene gráficos dinámicos de Chart.js,
        // forzamos la recarga de página para que las leyendas de Chart.js se recalculen con el nuevo color de texto
        if (document.getElementById('evolutionChart') || document.getElementById('radialRiskChart') || document.getElementById('barComparisonChart')) {
            window.location.reload();
        }
    });
});