FROM navikt/java:12

COPY build/libs/*.jar ./

RUN echo 'java -XX:+PrintFlagsFinal -version | grep -Ei "maxheapsize|maxram"' > /init-scripts/0-dump-memory-config.sh
