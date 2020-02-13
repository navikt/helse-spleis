package no.nav.helse.rapids_rivers

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineEnvironmentBuilder
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaConsumerMetrics
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit

class RapidApplication private constructor(
    private val ktor: ApplicationEngine,
    private val rapid: RapidsConnection
) : RapidsConnection(), RapidsConnection.MessageListener {

    init {
        Runtime.getRuntime().addShutdownHook(Thread(::shutdownHook))
        rapid.register(this)
    }

    override fun onMessage(message: String, context: MessageContext) {
        listeners.forEach { it.onMessage(message, context) }
    }

    override fun start() {
        ktor.start(wait = false)
        rapid.start()
    }

    override fun stop() {
        rapid.stop()
        ktor.stop(1, 1, TimeUnit.SECONDS)
    }

    override fun publish(message: String) {
        rapid.publish(message)
    }

    override fun publish(key: String, message: String) {
        rapid.publish(key, message)
    }

    private fun shutdownHook() {
        log.info("received shutdown signal, stopping app")
        stop()
    }

    companion object {
        private val log = LoggerFactory.getLogger(RapidApplication::class.java)

        fun create(env: Map<String, String>) = Builder(env).build()
    }

    class Builder(env: Map<String, String>) {
        init {
            Thread.currentThread().setUncaughtExceptionHandler(::uncaughtExceptionHandler)
        }

        private val kafkaConfig = KafkaConfigBuilder(
            bootstrapServers = env.getValue("KAFKA_BOOTSTRAP_SERVERS"),
            consumerGroupId = env.getValue("KAFKA_CONSUMER_GROUP_ID"),
            username = "/var/run/secrets/nais.io/service_user/username".readFile(),
            password = "/var/run/secrets/nais.io/service_user/password".readFile(),
            truststore = env["NAV_TRUSTSTORE_PATH"],
            truststorePassword = env["NAV_TRUSTSTORE_PASSWORD"]
        )

        private val rapid = KafkaRapid(
            consumerConfig = kafkaConfig.consumerConfig(),
            producerConfig = kafkaConfig.producerConfig(),
            topic = env.getValue("KAFKA_RAPID_TOPIC")
        )

        private val ktor = ApplicationEngineEnvironmentBuilder().apply {
            this.log = RapidApplication.log
            connector { port = env["HTTP_PORT"]?.toInt() ?: 8080 }
            liveness(rapid::isRunning)
            readiness(rapid::isRunning)
            metrics(CollectorRegistry.defaultRegistry)
        }

        fun withKtorModule(module: Application.() -> Unit) = apply {
            ktor.module(module)
        }

        fun build(): RapidsConnection {
            return RapidApplication(ktor.build(), rapid)
        }

        private fun uncaughtExceptionHandler(thread: Thread, err: Throwable) {
            log.error("Uncaught exception in thread ${thread.name}: ${err.message}", err)
        }

        private fun ApplicationEngineEnvironmentBuilder.build() = embeddedServer(Netty, this.build {  })

        private fun ApplicationEngineEnvironmentBuilder.liveness(isAliveCheck: () -> Boolean) = apply {
            module {
                routing {
                    get("/isalive") {
                        if (!isAliveCheck()) return@get call.respondText("NOT ALIVE", ContentType.Text.Plain, HttpStatusCode.ServiceUnavailable)
                        call.respondText("ALIVE", ContentType.Text.Plain)
                    }
                }
            }
        }

        private fun ApplicationEngineEnvironmentBuilder.readiness(isReadyCheck: () -> Boolean) = apply {
            module {
                routing {
                    get("/isready") {
                        if (!isReadyCheck()) return@get call.respondText("NOT READY", ContentType.Text.Plain, HttpStatusCode.ServiceUnavailable)
                        call.respondText("READY", ContentType.Text.Plain)
                    }
                }
            }
        }

        private fun ApplicationEngineEnvironmentBuilder.metrics(collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry) = apply {
            module {
                install(MicrometerMetrics) {
                    registry = PrometheusMeterRegistry(
                        PrometheusConfig.DEFAULT,
                        collectorRegistry,
                        Clock.SYSTEM
                    )
                    meterBinders = listOf(
                        ClassLoaderMetrics(),
                        JvmMemoryMetrics(),
                        JvmGcMetrics(),
                        ProcessorMetrics(),
                        JvmThreadMetrics(),
                        LogbackMetrics(),
                        KafkaConsumerMetrics()
                    )
                }

                routing {
                    get("/metrics") {
                        val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: emptySet()
                        call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                            TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
                        }
                    }
                }
            }
        }
    }
}

private fun String.readFile() =
    try {
        File(this).readText(Charsets.UTF_8)
    } catch (err: FileNotFoundException) {
        null
    }
