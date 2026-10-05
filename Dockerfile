# syntax=docker/dockerfile:1

# ---------- Estágio 1: build ----------
# Compila, roda todos os testes e valida a cobertura mínima (mvn verify).
# Se algum teste falhar, a imagem não é gerada.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Dependências primeiro: esta camada só é refeita quando o pom.xml muda.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B verify && cp target/coupon-api-*.jar app.jar

# ---------- Estágio 2: runtime ----------
# Imagem final enxuta: só o JRE e o jar, sem Maven, código-fonte ou testes.
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /workspace/app.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
