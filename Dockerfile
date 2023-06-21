FROM ghcr.io/navikt/baseimages/temurin:17

ENV JAVA_OPTS='-XX:MaxRAMPercentage=90'

COPY build/libs/*.jar ./
