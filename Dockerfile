# ==============================================================================
#                 STAGE 1: COMPILADOR MULTIMÓDULO (MAVEN BUILDER)
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# 1. Copiamos los archivos pom.xml del proyecto padre y de los submódulos
# para descargar las dependencias por separado
COPY pom.xml .
COPY banksphere-common/pom.xml banksphere-common/
COPY banksphere-core/pom.xml banksphere-core/
COPY banksphere-antifraud/pom.xml banksphere-antifraud/
COPY banksphere-notifications/pom.xml banksphere-notifications/

# 2. Descargamos las dependencias Maven para almacenarlas en la caché de Docker.
# Esto evita tener que descargar las librerías de internet en cada compilación.
RUN mvn dependency:go-offline -B

# 3. Copiamos el código fuente de los submódulos
COPY banksphere-common/src banksphere-common/src
COPY banksphere-core/src banksphere-core/src
COPY banksphere-antifraud/src banksphere-antifraud/src
COPY banksphere-notifications/src banksphere-notifications/src

# 4. Compilamos el proyecto completo omitiendo los tests (los ejecutamos en el CI/CD)
RUN mvn clean package -DskipTests

# ==============================================================================
#                 STAGE 2: SERVICIO CORE BANCARIO (RUNNER)
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine AS core
WORKDIR /app
# Copiamos el Fat JAR del constructor en la capa ligera de producción
COPY --from=builder /app/banksphere-core/target/banksphere-core-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

# ==============================================================================
#                 STAGE 3: SERVICIO ANTIFRAUDE & AML (RUNNER)
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine AS antifraud
WORKDIR /app
COPY --from=builder /app/banksphere-antifraud/target/banksphere-antifraud-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]

# ==============================================================================
#                 STAGE 4: SERVICIO NOTIFICACIONES & WEBSOCKETS (RUNNER)
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine AS notifications
WORKDIR /app
COPY --from=builder /app/banksphere-notifications/target/banksphere-notifications-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]