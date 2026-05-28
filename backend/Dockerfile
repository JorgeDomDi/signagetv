# ---------- Build stage ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copia primero el pom para cachear las dependencias
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# Copia el código y empaqueta
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Carpeta donde se persisten los media (montar como volumen en compose)
RUN mkdir -p /var/signagetv/media && chmod -R 777 /var/signagetv

COPY --from=build /app/target/signagetv-backend.jar /app/app.jar

EXPOSE 8080

ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
