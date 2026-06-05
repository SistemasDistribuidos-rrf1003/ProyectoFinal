# 🏦 BankSphere - Plataforma Bancaria Distribuida

¡Bienvenido a **BankSphere**! Una plataforma Fintech moderna con arquitectura distribuida, construida con Spring Boot, Thymeleaf, PostgreSQL, RabbitMQ y Docker.

---

## 🏗️ Arquitectura del Sistema

BankSphere está dividido en varios microservicios y contenedores de infraestructura para garantizar su escalabilidad:

1. **BankSphere Core (`:8080`)**: El núcleo del banco. Contiene el portal Web (UI), el registro de usuarios, gestión de cuentas y lógica de transferencias.
2. **Motor Antifraude (`:8082`)**: Microservicio asíncrono que lee eventos desde RabbitMQ para analizar y bloquear transferencias sospechosas en tiempo real.
3. **Hub Notificaciones (`:8083`)**: Microservicio que gestiona el envío de Emails y las conexiones WebSocket (alertas en tiempo real y chat de soporte en vivo).
4. **Infraestructura**:
    - **PostgreSQL (`:5432`)**: Base de datos relacional.
    - **pgAdmin (`:5050`)**: Panel web para consultar la base de datos visualmente.
    - **RabbitMQ (`:15672`)**: Broker de mensajería para comunicación interna entre los microservicios.
    - **Mailpit (`:8025`)**: Servidor de pruebas SMTP (Sandbox) para atrapar y leer los correos enviados por el sistema.

---

## 🐳 Guía de Despliegue con Docker

La forma más sencilla de ejecutar toda la arquitectura simultáneamente es utilizando **Docker Compose**. Esto levantará las bases de datos, el broker de mensajes, los servidores auxiliares y compilará los 3 microservicios en Java automáticamente.

### Requisitos previos
- Tener instalado y en ejecución [Docker Desktop](https://www.docker.com/products/docker-desktop/).

### Pasos para el Despliegue

1. Abre una terminal (PowerShell, CMD o Bash) en la carpeta raíz del proyecto (donde se encuentra el archivo `docker-compose.yml`).
2. Ejecuta el siguiente comando para construir las imágenes y levantar el sistema en segundo plano:
   ```bash
   docker compose up -d --build
3. La primera vez tardará varios minutos mientras descarga las imágenes pesadas (PostgreSQL, RabbitMQ) y compila el código Java usando Maven dentro de los contenedores Docker.
4. Para verificar que todo está funcionando, comprueba los contenedores activos con:
    ```bash
   docker compose ps
5. Para detener todo el ecosistema y liberar memoria de tu ordenador, ejecuta:
    ```bash
   docker compose down
   
### URLs de Acceso Rápido

Una vez que el despliegue termine y los contenedores estén en verde, tendrás acceso a los siguientes paneles:

- Plataforma Bancaria (Web Principal): http://localhost:8080
- Bandeja de Correos Electrónicos (Mailpit): http://localhost:8025
- Consola RabbitMQ: http://localhost:15672 (Usuario: guest / Contraseña: guest)
- Gestor de Base de Datos (pgAdmin): http://localhost:5050 (Usuario: admin@banksphere.com / Contraseña: admin)

### Guía de Uso de la Aplicación

El sistema está diseñado de forma realista, separando la lógica y permisos de los Administradores (trabajadores del banco) y los Clientes.

#### 1. Iniciar como Administrador
   
Por defecto, al crear la base de datos, el sistema inyecta un Administrador (Data Seeder):

- Email: admin@banksphere.com
- Contraseña: admin123

Como administrador de BankSphere, tus tareas principales son:

1. Evaluar el KYC (Anti-Lavado de Dinero): En el apartado Motor AML/KYC, puedes revisar los DNI de los clientes nuevos que se registran y Aprobar/Rechazar su identidad.
2. Gestión de Cuentas: Los usuarios no pueden abrirse cuentas bancarias libremente. Debes ir al panel de Cuentas, hacer clic en "Abrir Nueva Cuenta" y asignársela a un cliente concreto (Ahorros o Corriente).
3. Bloqueo/Desbloqueo: Puedes congelar las cuentas de clientes sospechosos desde el mismo panel de Cuentas.

#### 2. Ciclo de Vida de un Cliente

Para probar la aplicación y el flujo completo como un usuario final:

1. Entra a http://localhost:8080 y haz clic en Registrarse.
2. Completa el formulario de registro con tus datos y DNI.
3. Inicia sesión con el correo y contraseña que acabas de crear.
4. Alerta de Seguridad KYC: Al entrar por primera vez, verás que no tienes acceso a ninguna operación bancaria. El sistema te marcará como estado PENDING_KYC.
5. (Inicia sesión como Administrador en otro navegador o ventana de incógnito para Aprobar tu usuario en el sistema y crearle una Cuenta Bancaria inicial).
6. Si recargas la página como cliente, verás que tu Dashboard se ha desbloqueado y aparecerán los datos de tu nueva cuenta.
7. En el apartado Transferencias, podrás enviar dinero a otro usuario utilizando su número de cuenta IBAN. El sistema calculará automáticamente las comisiones de la red bancaria (SEPA estándar vs Transferencia Inmediata) y validará matemáticamente el IBAN que introduzcas usando el algoritmo oficial de Módulo 97.

#### 3. Funciones Especiales a Probar

- Chat de Soporte en Vivo: Verás un botón flotante abajo a la izquierda en la pantalla. Al abrirlo y escribir un mensaje, viajará a través del protocolo WebSocket en tiempo real hacia el microservicio de notificaciones, y nuestro Bot Asistente Virtual programado te devolverá una respuesta dinámica automática.
- Recuperación de Contraseña: Si cierras sesión y vas a "¿Olvidaste tu contraseña?", el servidor generará un token de recuperación. Podrás simular que abres tu bandeja de entrada y lees este correo accediendo al panel web del sandbox Mailpit (http://localhost:8025).
