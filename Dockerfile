FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew bootJar

# ─────────────────────────────────────────────
# 런타임 이미지
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre

WORKDIR /app

# Tesseract + 여러 언어 데이터 설치
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    tesseract-ocr-kor \
    tesseract-ocr-jpn \
    tesseract-ocr-chi-sim \
 && rm -rf /var/lib/apt/lists/*

# (선택) tessdata 위치 명시하고 싶으면 환경변수 추가
# ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/4.00/

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]