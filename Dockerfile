FROM gcr.io/distroless/java21-debian12:nonroot

ENV JAVA_OPTS='-XX:MaxRAMPercentage=90'

COPY build/libs/*.jar ./
