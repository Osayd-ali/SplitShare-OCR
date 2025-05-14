# commands
# build: docker build -t splitshare:latest .
# run on localhost: docker run -p 8080:8080 splitshare

# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre AS runtime
USER root

# Install Tesseract and wget using apk
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    wget \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*  && \
    wget https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata -P /usr/share/tessdata && \
    wget https://github.com/tesseract-ocr/tessdata/raw/main/osd.traineddata -P /usr/share/tessdata


ENV TESSDATA_PREFIX=/usr/share/

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
