FROM navikt/java:18

ENV JAVA_OPTS='-XX:MaxRAMPercentage=90'

COPY sykepenger-mediators/build/libs/*.jar ./
