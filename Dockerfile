# GlossUP — imagen única: Angular (frontend) servido por Spring Boot (backend).
# Se despliega en Azure Container Apps: expone el puerto $PORT y se conecta a la BD por variables de entorno.

# ---------- 1) Build del frontend ----------
FROM node:24-alpine AS frontend
WORKDIR /front
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build        # genera /front/dist

# ---------- 2) Build del backend (empaqueta el front dentro del jar) ----------
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY backend/ ./
COPY --from=frontend /front/dist/ ./src/main/resources/static/
RUN mvn -q -DskipTests clean package

# ---------- 3) Imagen de ejecución ----------
FROM eclipse-temurin:21-jre-alpine
# Paquetes del sistema base al día (auditoría R-17: brotli-libs crítica).
RUN apk upgrade --no-cache
WORKDIR /app
COPY --from=backend /app/target/*.jar app.jar
# Sin root: si alguien explota la app no tiene privilegios en el contenedor (auditoría R-17).
RUN addgroup -S glossup && adduser -S glossup -G glossup
USER glossup
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s \
  CMD wget -qO- "http://localhost:${PORT:-8080}/actuator/health/liveness" || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
