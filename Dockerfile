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
    fontconfig \
 && rm -rf /var/lib/apt/lists/*

# ─────────────────────────────────────────────
# 커스텀 폰트 추가 (프로젝트 resources/fonts → 시스템 폰트 디렉토리)
# ─────────────────────────────────────────────
COPY src/main/resources/fonts /usr/share/fonts/truetype/custom/

# 폰트 캐시 재생성
RUN fc-cache -f -v

# ─────────────────────────────────────────────
# 앱 Jar 복사
# ─────────────────────────────────────────────
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]