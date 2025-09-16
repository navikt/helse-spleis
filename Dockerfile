FROM busybox AS builder

# GCP profiling agent
RUN mkdir -p /opt/cprof &&  \
    wget -q -O- https://storage.googleapis.com/cloud-profiler/java/latest/profiler_java_agent.tar.gz  \
    | tar xzv -C /opt/cprof
RUN ls -l /opt/cprof

FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21

COPY --from=builder /opt/cprof/profiler_java_agent.so /opt/cprof/
COPY build/libs/*.jar /app/

ENV TZ="Europe/Oslo"
ENV JDK_JAVA_OPTIONS='-XX:MaxRAMPercentage=90'

WORKDIR /app

CMD ["-jar", "app.jar"]