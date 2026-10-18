# GlossUP — imagen única: React (frontend) servido por Spring Boot (backend).
# Pensada para Cloud Run: expone el puerto $PORT y se conecta a la BD por variables de entorno.

# ---------- 1) Build del frontend ----------
FROM node:22-alpine AS frontend
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
WORKDIR /app
COPY --from=backend /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
