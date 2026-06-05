# 🏦 BankSphere - Plataforma Bancaria Distribuida (Core & Analytics)

¡Bienvenido al equipo de desarrollo de **BankSphere**! Esta guía técnica está diseñada para ayudarte a configurar tu entorno local y desplegar la arquitectura distribuida del proyecto desde cero.

BankSphere es una plataforma Fintech moderna, construida con arquitectura multi-módulo Maven y dividida en componentes desacoplados de alta disponibilidad: **Core Bancario** (Web UI y REST API), **Motor Antifraude / AML** y **Hub de Notificaciones en Tiempo Real**.

---

## 📋 1. Requisitos Previos

Antes de comenzar, asegúrate de tener instalado el siguiente software en tu máquina local.

| Herramienta | Versión Recomendada | Enlace Oficial de Descarga | Comando de Verificación |
| :--- | :--- | :--- | :--- |
| **Java JDK** | 17 LTS (Eclipse Temurin) | [Adoptium JDK 17](https://adoptium.net/temurin/releases/?version=17) | `java -version` |
| **Apache Maven** | 3.8.x o superior | [Maven Download](https://maven.apache.org/download.cgi) | `mvn -version` |
| **Git** | 2.40.x o superior | [Git SCM](https://git-scm.com/) | `git --version` |
| **Docker Desktop** | 4.20.x o superior (con WSL2 en Windows) | [Docker Desktop](https://www.docker.com/products/docker-desktop/) | `docker --version` |
| **Docker Compose** | 2.x (incluido con Docker Desktop) | Incluido en Docker Desktop | `docker compose version` |
| **PostgreSQL** (Local) | 15.x (opcional si usas Docker) | [PostgreSQL Downloads](https://www.postgresql.org/download/) | `psql --version` |
| **RabbitMQ** (Local) | 3.11.x o superior (opcional si usas Docker) | [RabbitMQ Release](https://www.rabbitmq.com/download.html) | `rabbitmqctl status` |
| **IntelliJ IDEA** | 2023.x o superior (Community o Ultimate) | [JetBrains IntelliJ](https://www.jetbrains.com/idea/download/) | Abrir la interfaz gráfica |
| **Postman** | 10.x o superior | [Postman App](https://www.postman.com/downloads/) | Abrir la interfaz gráfica |
| **SonarQube** (Local) | 9.9 LTS o superior (opcional) | [SonarQube Community](https://www.sonarsource.com/products/sonarqube/downloads/) | Acceder a `http://localhost:9000` |
| **pgAdmin 4** | 7.x o superior | [pgAdmin Download](https://www.pgadmin.org/download/) | Abrir la interfaz gráfica |

---

## ⚙️ 2. Configuración del Entorno

### Paso 1: Clonado del Repositorio
Clona el repositorio e ingresa al directorio raíz del proyecto:
```bash
git clone https://github.com/tu-usuario/banksphere-proyecto.git
cd banksphere-proyecto