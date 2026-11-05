# ---------- Etapa de construccion ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copiamos primero el wrapper y el pom para aprovechar la cache de dependencias
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Codigo fuente y empaquetado
COPY src ./src
RUN ./mvnw -B -q clean package -DskipTests

# ---------- Etapa de ejecucion ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Sin root (auditoría R-17).
RUN groupadd --system glossup && useradd --system --gid glossup glossup
USER glossup
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-jar", "app.jar"]
