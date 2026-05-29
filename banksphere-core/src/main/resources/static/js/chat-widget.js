/**
 * BANKSphere - CONTROLADOR CLIENTE CHAT DE SOPORTE EN VIVO (STOMP WEBSOCKET)
 * -----------------------------------------------------------------------------
 * Gestiona la interactividad de la UI flotante y la comunicación bidireccional
 * con el controlador de soporte técnico en el puerto 8083.
 */

document.addEventListener("DOMContentLoaded", () => {
    const chatBubble = document.getElementById('chat-bubble');
    const chatWindow = document.getElementById('chat-window');
    const closeChatBtn = document.getElementById('close-chat');
    const chatForm = document.getElementById('chat-form');
    const chatInput = document.getElementById('chat-input');
    const chatMessagesArea = document.getElementById('chat-messages');

    if (!chatBubble || !chatWindow) return; // Salir de forma segura si no está en la vista

    let stompClient = null;
    let username = null; // Se recuperará del email del usuario logueado en la sesión
    let isConnected = false;

    // 1. EVENTOS DE UI: Abrir y Cerrar Ventana de Soporte
    chatBubble.addEventListener('click', () => {
        chatBubble.classList.add('d-none');
        chatWindow.classList.remove('d-none');

        // Iniciamos la conexión WebSocket al abrir por primera vez
        if (!isConnected) {
            connectToChat();
        }
    });

    closeChatBtn.addEventListener('click', () => {
        chatWindow.classList.add('d-none');
        chatBubble.classList.remove('d-none');
    });

    // 2. CONEXIÓN WEBSOCKET Y SUSCRIPCIÓN AL CANAL DE SOPORTE
    function connectToChat() {
        // Simulamos la obtención del usuario logueado o inyectamos una sigla
        username = document.querySelector('[sec\\:authentication="name"]')
            ? document.querySelector('[sec\\:authentication="name"]').innerText.trim()
            : "Cliente_Sphere";

        const socket = new SockJS('http://localhost:8083/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // Apaga logs de consola

        stompClient.connect({}, () => {
            isConnected = true;

            // Nos suscribimos al canal de mensajería pública de Soporte Técnico
            stompClient.subscribe('/topic/support', (message) => {
                if (message.body) {
                    const msg = JSON.parse(message.body);
                    appendMessageUI(msg);
                }
            });

            // Enviamos un evento de JOIN indicando que nos unimos a la sala de soporte
            stompClient.send("/app/chat.addUser", {}, JSON.stringify({
                sender: username,
                type: 'JOIN'
            }));

        }, (error) => {
            console.error(">>> [Chat-Error] Fallo en la conexión del chat: " + error);
        });
    }

    // 3. ENVÍO DE MENSAJES HACIA EL SERVIDOR STOMP
    chatForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const content = chatInput.value.trim();

        if (content && stompClient && isConnected) {
            const chatMessage = {
                sender: username,
                content: content,
                type: 'CHAT'
            };

            stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
            chatInput.value = ''; // Limpiamos el campo de texto
        }
    });

    // 4. RENDERIZACIÓN DINÁMICA DE BURBUJAS DE TEXTO
    function appendMessageUI(message) {
        const messageElement = document.createElement('div');

        if (message.type === 'JOIN') {
            // Notificación de unión al soporte
            messageElement.className = 'text-center text-muted my-1';
            messageElement.style.fontSize = '0.7rem';
            messageElement.innerText = `${message.sender} se ha unido a la conversación de soporte.`;
        } else if (message.type === 'CHAT') {
            // Mensaje de texto normal
            const isMe = (message.sender === username);

            messageElement.className = isMe
                ? 'd-flex justify-content-end mb-2'
                : 'd-flex justify-content-start mb-2';

            const bubbleStyle = isMe
                ? 'background: var(--primary-grad); color: #08090d; border-radius: 12px 12px 0px 12px; box-shadow: 0 4px 10px rgba(0, 242, 254, 0.15);'
                : 'background: rgba(255,255,255,0.04); border: 1px solid var(--border-color); color: white; border-radius: 12px 12px 12px 0px;';

            messageElement.innerHTML = `
                <div class="p-2 px-3 text-wrap" style="max-width: 80%; font-size: 0.8rem; font-weight: 500; ${bubbleStyle}">
                    <div class="small fw-bold opacity-75" style="font-size: 0.65rem; margin-bottom: 2px;">${message.sender}</div>
                    <div>${message.content}</div>
                </div>
            `;
        }

        chatMessagesArea.appendChild(messageElement);
        // Autodesplazamiento automático al fondo para visualizar el mensaje más nuevo
        chatMessagesArea.scrollTop = chatMessagesArea.scrollHeight;
    }
});