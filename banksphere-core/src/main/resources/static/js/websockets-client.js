/**
 * BANKSphere - CLIENTE DE WEBSOCKETS Y PUSH NOTIFICATIONS (STOMP / SOCKJS)
 * -----------------------------------------------------------------------------
 * Establece la conexión con el microservicio de notificaciones (Puerto 8083)
 * y renderiza Toast Notifications dinámicas ante eventos de transferencias.
 */

document.addEventListener("DOMContentLoaded", () => {

    // 1. INYECCIÓN AUTOMÁTICA DEL CONTENEDOR TOAST EN EL DOM
    // Crea el contenedor flotante en la esquina inferior derecha si no existe previo en la página
    let toastContainer = document.getElementById('banksphere-toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'banksphere-toast-container';
        toastContainer.className = 'toast-container position-fixed bottom-0 end-0 p-4';
        toastContainer.style.zIndex = '9999';
        document.body.appendChild(toastContainer);
    }

    // 2. CONEXIÓN WEBSOCKET CON SOCKJS Y STOMP CLIENT
    // Apunta explícitamente al puerto 8083 del microservicio de notificaciones
    const socket = new SockJS('http://localhost:8083/ws');
    const stompClient = Stomp.over(socket);

    // Desactiva los logs detallados del broker STOMP en la consola para mantenerla limpia
    stompClient.debug = null;

    // Establecemos la conexión de red
    stompClient.connect({}, (frame) => {
        console.log(">>> [WebSocket-Client] Conexión establecida con éxito con el Broker STOMP.");

        // 3. SUSCRIPCIÓN AL CANAL DE NOTIFICACIONES
        stompClient.subscribe('/topic/notifications', (message) => {
            if (message.body) {
                const event = JSON.parse(message.body);
                showLiveNotification(event);
            }
        });
    }, (error) => {
        console.error(">>> [WebSocket-Client-Error] Fallo en la conexión con el servidor de notificaciones: " + error);
    });

    // 4. RENDERIZADOR DINÁMICO DE TOAST NOTIFICATION (Bootstrap 5 + Custom Glassmorphism)
    function showLiveNotification(event) {
        // Formateamos el importe neto para que sea sumamente legible en la alerta
        const amountFormatted = parseFloat(event.amount).toLocaleString('es-ES', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });

        // Generamos un ID único para controlar la instancia del Toast
        const toastId = 'toast-' + event.id + '-' + Date.now();

        // Creamos la estructura HTML de la alerta flotante con estética de Cristal Oscuro
        const toastHTML = `
            <div id="${toastId}" class="toast" role="alert" aria-live="assertive" aria-atomic="true" data-bs-delay="7000"
                 style="background: rgba(17, 24, 39, 0.95); backdrop-filter: blur(10px); border: 1px solid rgba(0, 242, 254, 0.2); border-radius: 16px; box-shadow: 0 10px 30px rgba(0, 242, 254, 0.15); color: #f3f4f6;">
                
                <!-- Cabecera del Toast -->
                <div class="toast-header border-bottom border-secondary border-opacity-10" style="background: transparent; color: #9ca3af; padding: 12px 15px;">
                    <i class="bi bi-wallet2 text-info me-2 fs-5"></i>
                    <strong class="me-auto text-white" style="font-family: 'Outfit', sans-serif; font-size: 0.85rem;">BankSphere Inmediatez</strong>
                    <small class="text-muted" style="font-size: 0.65rem;">Ahora</small>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
                
                <!-- Cuerpo del Toast -->
                <div class="toast-body" style="padding: 15px 18px; font-size: 0.85rem;">
                    <div class="d-flex align-items-center gap-3">
                        <div class="spinner-grow text-info border-0" role="status" style="width: 10px; height: 10px; flex-shrink: 0;"></div>
                        <div>
                            Se ha liquidado una transferencia instantánea por <strong class="text-info">${amountFormatted} €</strong>.
                            <div class="text-muted small mt-1" style="font-size: 0.72rem; line-height: 1.2;">
                                Beneficiario: <strong>${event.destinationUserEmail}</strong><br>
                                Concepto: <em>"${event.concept}"</em>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;

        // Insertamos el Toast en el contenedor e inicializamos el componente mediante Bootstrap JS API
        toastContainer.insertAdjacentHTML('beforeend', toastHTML);
        const toastElement = document.getElementById(toastId);
        const bsToast = new bootstrap.Toast(toastElement);

        // Disparamos la visualización del Toast
        bsToast.show();

        // Opcional: Sonido sutil de notificación tipo "Ping" para enriquecer la experiencia bancaria
        try {
            const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
            const osc = audioCtx.createOscillator();
            const gain = audioCtx.createGain();
            osc.connect(gain);
            gain.connect(audioCtx.destination);
            osc.frequency.setValueAtTime(880, audioCtx.currentTime); // Tonalidad musical alta
            gain.gain.setValueAtTime(0.05, audioCtx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.3);
            osc.start();
            osc.stop(audioCtx.currentTime + 0.3);
        } catch (e) {
            // Falla de forma silenciosa si el navegador bloquea el auto-audio sin interacción previa
        }

        // Eliminación del elemento del DOM una vez que la alerta se cierra de forma automática para evitar saturar la memoria
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }
});