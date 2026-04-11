# Single stage build - expects the JAR to be built in GitHub Actions
FROM eclipse-temurin:21-jre-jammy

LABEL maintainer="Baykul Team" \
      version="1.0.0" \
      description="Baykul Backend Application"

RUN groupadd --system --gid 1000 spring && \
    useradd --system --uid 1000 --gid spring spring

RUN mkdir -p /app/logs && \
    mkdir -p /app/uploads && \
    mkdir -p /app/uploads/tmp && \
    mkdir -p /app/config && \
    chown -R spring:spring /app

COPY --chown=spring:spring build/libs/*.jar /app/application.jar
RUN chown -R spring:spring /app
USER spring:spring
WORKDIR /app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms512m -Xmx1024m" \
    TZ=Europe/Moscow
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-jar", \
    "/app/application.jar"]
CMD ${JAVA_OPTS}