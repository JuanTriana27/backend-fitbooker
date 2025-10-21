FROM openjdk:17-jdk-slim

WORKDIR /app

# Instalar Maven y copiar archivos de proyecto
COPY pom.xml .
COPY src ./src

# Instalar Maven y construir la aplicación
RUN apt-get update && \
    apt-get install -y maven && \
    mvn clean package -DskipTests

# Exponer puerto
EXPOSE 8080

# Comando para ejecutar
ENTRYPOINT ["java", "-jar", "target/gimnasio-0.0.1-SNAPSHOT.jar"]