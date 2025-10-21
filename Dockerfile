FROM openjdk:17-jdk-slim

# Configurar locale y encoding UTF-8
RUN apt-get update && \
    apt-get install -y maven locales && \
    locale-gen en_US.UTF-8 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Variables de entorno para UTF-8
ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8

WORKDIR /app
COPY . .

# Forzar encoding UTF-8 en Maven
RUN mvn clean package -DskipTests -Dfile.encoding=UTF-8

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/gimnasio-0.0.1-SNAPSHOT.jar"]