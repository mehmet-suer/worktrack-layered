FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system appgroup \
 && useradd  --system \
            --uid 1000 \
            --gid appgroup \
            --home-dir /app \
            --create-home \
            --shell /usr/sbin/nologin \
            appuser \
 && mkdir -p /app/data \
 && chown -R appuser:appgroup /app

ARG JAR_FILE=build/libs/app.jar
COPY --chown=appuser:appgroup ${JAR_FILE} /app/app.jar

USER appuser

ENTRYPOINT ["java","-jar","/app/app.jar"]