FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -u 1000 -S appuser -G appgroup \
    && mkdir -p /app/data && chown -R appuser:appgroup /app

ARG JAR_FILE=build/libs/app.jar
COPY --chown=appuser:appgroup ${JAR_FILE} app.jar

USER appuser

ENTRYPOINT ["java","-jar","/app/app.jar"]
