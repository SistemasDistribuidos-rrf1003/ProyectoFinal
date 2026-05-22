-- =============================================================================
--               BANKSphere - PLATAFORMA BANCARIA DIGITAL INTELIGENTE
--                     SCRIPT DE INICIALIZACIÓN DE BASE DE DATOS
-- =============================================================================
-- Este script crea el esquema de base de datos relacional en PostgreSQL e
-- inyecta los datos semilla necesarios para arrancar y probar el sistema.
--
-- CONTRASEÑAS SEMILLA (Encriptadas con BCrypt - Fuerza: 10)
-- Todos los usuarios iniciales usan la contraseña exacta: password123
-- Hash BCrypt inyectado: $2a$10$Y50UaMFOxteibQEYDfARXOBDnYtB1CqG0tS9J8xYm2C8G.t4bY8tG
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. LIMPIEZA DE TABLAS EXISTENTES (Para evitar duplicados y facilitar reinicios)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS kyc_details CASCADE;
DROP TABLE IF EXISTS transfers CASCADE;
DROP TABLE IF EXISTS accounts CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- -----------------------------------------------------------------------------
-- 2. CREACIÓN DE TABLAS DE ESQUEMA
-- -----------------------------------------------------------------------------

-- TABLA: users (Mantenimiento de Usuarios / Gestión de Clientes)
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       first_name VARCHAR(50) NOT NULL,
                       last_name VARCHAR(50) NOT NULL,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       phone VARCHAR(20),
                       national_id VARCHAR(30) UNIQUE NOT NULL, -- DNI / NIE / Pasaporte
                       password VARCHAR(100) NOT NULL,          -- Contraseña encriptada con BCrypt
                       role VARCHAR(20) NOT NULL,               -- Roles: ADMIN, USER, ANALYST
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       status VARCHAR(20) NOT NULL              -- Estados: ACTIVE, SUSPENDED, PENDING_KYC
);

-- TABLA: accounts (CRUD #1 - Cuentas Bancarias)
CREATE TABLE accounts (
                          id BIGSERIAL PRIMARY KEY,
                          iban VARCHAR(34) UNIQUE NOT NULL,       -- Código Internacional de Cuenta
                          account_type VARCHAR(20) NOT NULL,      -- Tipos: SAVINGS, CHECKING, BUSINESS
                          balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
                          currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          status VARCHAR(20) NOT NULL,             -- Estados: ACTIVE, SUSPENDED
                          user_id BIGINT NOT NULL,
                          CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- TABLA: transfers (CRUD #2 - Historial y Operaciones de Transferencia)
CREATE TABLE transfers (
                           id BIGSERIAL PRIMARY KEY,
                           source_account_id BIGINT NOT NULL,
                           destination_account_id BIGINT NOT NULL,
                           amount NUMERIC(15, 2) NOT NULL,
                           fee NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
                           concept VARCHAR(150),
                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                           status VARCHAR(20) NOT NULL,             -- Estados: COMPLETED, FAILED, PENDING, SUSPECTED_FRAUD
                           transfer_type VARCHAR(20) NOT NULL,      -- Tipos: SEPA, SWIFT, INSTANT
                           CONSTRAINT fk_transfer_source FOREIGN KEY (source_account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
                           CONSTRAINT fk_transfer_destination FOREIGN KEY (destination_account_id) REFERENCES accounts(id) ON DELETE RESTRICT
);

-- TABLA: kyc_details (Módulo AML / KYC)
CREATE TABLE kyc_details (
                             id BIGSERIAL PRIMARY KEY,
                             user_id BIGINT UNIQUE NOT NULL,
                             verification_status VARCHAR(30) NOT NULL, -- Estados: VERIFIED, REJECTED, PENDING, IN_PROGRESS
                             risk_level VARCHAR(20) NOT NULL,          -- Niveles: LOW, MEDIUM, HIGH
                             document_type VARCHAR(30),                -- DNI, NIE, PASSPORT
                             document_url VARCHAR(255),                -- Ruta simulada de almacenamiento del documento
                             updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT fk_kyc_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- 3. INYECCIÓN DE DATOS SEMILLA (Seed Data)
-- -----------------------------------------------------------------------------

-- 3.1 Usuarios de Prueba (Password para todos: password123)
-- Usuario 1: Administrador Global de la Plataforma (ADMIN)
INSERT INTO users (first_name, last_name, email, phone, national_id, password, role, status)
VALUES ('Sofía', 'Martínez', 'admin@banksphere.com', '+34 600 112 233', '12345678A',
        '$2a$10$Y50UaMFOxteibQEYDfARXOBDnYtB1CqG0tS9J8xYm2C8G.t4bY8tG', 'ADMIN', 'ACTIVE');

-- Usuario 2: Cliente Normal con saldo activo y cuentas (USER)
INSERT INTO users (first_name, last_name, email, phone, national_id, password, role, status)
VALUES ('Javier', 'Gómez Ruiz', 'client@banksphere.com', '+34 611 223 344', '87654321B',
        '$2a$10$Y50UaMFOxteibQEYDfARXOBDnYtB1CqG0tS9J8xYm2C8G.t4bY8tG', 'USER', 'ACTIVE');

-- Usuario 3: Analista de Fraudes y Cumplimiento Regulatorio (ANALYST)
INSERT INTO users (first_name, last_name, email, phone, national_id, password, role, status)
VALUES ('Alejandro', 'López Vega', 'analyst@banksphere.com', '+34 622 334 455', '56781234C',
        '$2a$10$Y50UaMFOxteibQEYDfARXOBDnYtB1CqG0tS9J8xYm2C8G.t4bY8tG', 'ANALYST', 'ACTIVE');

-- Usuario 4: Cliente nuevo con KYC pendiente (USER con PENDING_KYC)
INSERT INTO users (first_name, last_name, email, phone, national_id, password, role, status)
VALUES ('Laura', 'Sanz Torres', 'laura.sanz@gmail.com', '+34 633 445 566', '43218765D',
        '$2a$10$Y50UaMFOxteibQEYDfARXOBDnYtB1CqG0tS9J8xYm2C8G.t4bY8tG', 'USER', 'PENDING_KYC');


-- 3.2 Registros del Módulo KYC asociados
-- KYC Javier Gómez (Verificado - Riesgo Bajo)
INSERT INTO kyc_details (user_id, verification_status, risk_level, document_type, document_url)
VALUES (2, 'VERIFIED', 'LOW', 'DNI', '/uploads/docs/dni_javier.pdf');

-- KYC Laura Sanz (En proceso de verificación - Riesgo Medio)
INSERT INTO kyc_details (user_id, verification_status, risk_level, document_type, document_url)
VALUES (4, 'PENDING', 'MEDIUM', 'PASSPORT', '/uploads/docs/passport_laura.pdf');


-- 3.3 Cuentas Bancarias de Prueba (Monedas EUR y USD, saldos semilla)
-- Cuentas de Javier Gómez (Usuario ID: 2)
-- Cuenta Ahorros (SAVINGS)
INSERT INTO accounts (iban, account_type, balance, currency, status, user_id)
VALUES ('ES2114650100991234567890', 'SAVINGS', 45250.75, 'EUR', 'ACTIVE', 2);

-- Cuenta Corriente (CHECKING)
INSERT INTO accounts (iban, account_type, balance, currency, status, user_id)
VALUES ('ES9814650100990987654321', 'CHECKING', 3890.10, 'EUR', 'ACTIVE', 2);

-- Cuenta Business en Dólares (BUSINESS / USD)
INSERT INTO accounts (iban, account_type, balance, currency, status, user_id)
VALUES ('US1214650100994567890123', 'BUSINESS', 12500.00, 'USD', 'ACTIVE', 2);

-- Cuenta de Laura Sanz (Usuario ID: 4) - Saldo bajo inicial
INSERT INTO accounts (iban, account_type, balance, currency, status, user_id)
VALUES ('ES4514650100998888888888', 'CHECKING', 150.00, 'EUR', 'ACTIVE', 4);

-- Cuenta fantasma para simulaciones de pagos externos controlada por la administración
INSERT INTO accounts (iban, account_type, balance, currency, status, user_id)
VALUES ('ES0000000000000000000000', 'BUSINESS', 1000000.00, 'EUR', 'ACTIVE', 1);


-- 3.3 Transferencias de Prueba (Historial de transacciones de Javier Gómez)
-- Transacción 1: Recibe nómina desde la cuenta ADMIN
INSERT INTO transfers (source_account_id, destination_account_id, amount, fee, concept, status, transfer_type)
VALUES (5, 2, 2450.00, 0.00, 'NOMINA MAYO 2026 - BANKSphere Corp', 'COMPLETED', 'SEPA');

-- Transacción 2: Javier envía un pago inmediato a Laura Sanz
INSERT INTO transfers (source_account_id, destination_account_id, amount, fee, concept, status, transfer_type)
VALUES (3, 4, 45.50, 0.00, 'Pago cena de cumpleaños', 'COMPLETED', 'INSTANT');

-- Transacción 3: Javier envía una transferencia internacional con comisión a Laura Sanz
INSERT INTO transfers (source_account_id, destination_account_id, amount, fee, concept, status, transfer_type)
VALUES (2, 4, 500.00, 7.50, 'Fondo para viaje familiar', 'COMPLETED', 'SWIFT');