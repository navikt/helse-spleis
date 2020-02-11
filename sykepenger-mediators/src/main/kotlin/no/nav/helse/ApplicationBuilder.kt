package no.nav.helse

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.request.path
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.helse.spleis.HelseBuilder
import no.nav.helse.spleis.KafkaRapid
import no.nav.helse.spleis.nais.nais
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KafkaStreams
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit

// Understands how to build our application server
class ApplicationBuilder(env: Map<String, String>) {

    private val applicationLog = LoggerFactory.getLogger(ApplicationBuilder::class.java)

    private val kafkaConfigBuilder = KafkaConfigBuilder(
        applicationId = env.getOrDefault("KAFKA_APP_ID", "spleis-v3"),
        bootstrapServers = env.getValue("KAFKA_BOOTSTRAP_SERVERS"),
        username = "/var/run/secrets/nais.io/service_user/username".readFile() ?: env.getValue("KAFKA_USERNAME"),
        password = "/var/run/secrets/nais.io/service_user/password".readFile() ?: env.getValue("KAFKA_PASSWORD"),
        truststorePath = env["NAV_TRUSTSTORE_PATH"],
        truststorePassword = env["NAV_TRUSTSTORE_PASSWORD"]
    )
    private val dataSourceBuilder = DataSourceBuilder(env)

    private val rapid = KafkaRapid(Topics.hendelseKildeTopics)

    private val helseBuilder = HelseBuilder(
        dataSource = dataSourceBuilder.getDataSource(),
        kafkaRapid = rapid,
        hendelseProducer = KafkaProducer<String, String>(
            kafkaConfigBuilder.producerConfig(),
            StringSerializer(),
            StringSerializer()
        )
    )

    private val app = embeddedServer(Netty, applicationEngineEnvironment {
        log = applicationLog

        connector {
            port = env["HTTP_PORT"]?.toInt() ?: 8080

            println("HTTP_PORT = $port")

        }

        module {
            nais(::isApplicationAlive, ::isApplicationReady)
            install(CallLogging) {
                logger = LoggerFactory.getLogger("sikkerLogg")
                level = Level.INFO
                filter { call -> call.request.path().startsWith("/api/") }
            }
            install(ContentNegotiation) {
                jackson {
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                }
            }

            val clientId = "/var/run/secrets/nais.io/azure/client_id".readFile() ?: env.getValue("AZURE_CLIENT_ID")

            restInterface(
                personRestInterface = helseBuilder.personRestInterface,
                hendelseRecorder = helseBuilder.hendelseRecorder,
                configurationUrl = requireNotNull(env["AZURE_CONFIG_URL"]),
                clientId = clientId,
                requiredGroup = requireNotNull(env["AZURE_REQUIRED_GROUP"])
            )
        }
    })

    init {
        setUncaughtExceptionHandler(applicationLog)
        stopApplicationOnShutdown()

        rapid.addStateListener(KafkaStreams.StateListener { newState, _ ->
            if (newState == KafkaStreams.State.ERROR || newState == KafkaStreams.State.NOT_RUNNING) {
                stop()
            }
        })
    }

    fun start() {
        app.start(wait = false)
        dataSourceBuilder.migrate()
        rapid.start(kafkaConfigBuilder.streamsConfig())
    }

    fun stop() {
        rapid.stop()
        app.stop(1, 5, TimeUnit.SECONDS)
    }

    private fun isApplicationAlive() = true
    private fun isApplicationReady() = true

    private fun stopApplicationOnShutdown() {
        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
        })
    }

    private companion object {
        private fun setUncaughtExceptionHandler(logger: Logger) {
            Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
                logger.error("uncaught exception in thread ${thread.name}: ${err.message}", err)
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
