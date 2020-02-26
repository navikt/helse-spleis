package no.nav.helse

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.jackson.jackson
import io.ktor.request.path
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.CollectorRegistry
import no.nav.helse.rapids_rivers.KafkaConfig
import no.nav.helse.rapids_rivers.KafkaRapid
import no.nav.helse.rapids_rivers.KtorBuilder
import no.nav.helse.spleis.HelseBuilder
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

// Understands how to build our application server
@KtorExperimentalAPI
class ApplicationBuilder(env: Map<String, String>) {

    init {
        Thread.currentThread().setUncaughtExceptionHandler(::uncaughtExceptionHandler)
        Runtime.getRuntime().addShutdownHook(Thread(::shutdownHook))
    }

    private companion object {
        private val log = LoggerFactory.getLogger(ApplicationBuilder::class.java)
    }

    private val kafkaConfig = KafkaConfig(
        bootstrapServers = env.getValue("KAFKA_BOOTSTRAP_SERVERS"),
        consumerGroupId = env.getOrDefault("KAFKA_APP_ID", "spleis-v1"),
        username = "/var/run/secrets/nais.io/service_user/username".readFile() ?: env.getValue("KAFKA_USERNAME"),
        password = "/var/run/secrets/nais.io/service_user/password".readFile() ?: env.getValue("KAFKA_PASSWORD"),
        truststore = env["NAV_TRUSTSTORE_PATH"],
        truststorePassword = env["NAV_TRUSTSTORE_PASSWORD"]
    )

    private val rapidsConnection = KafkaRapid.create(kafkaConfig, Topics.rapidTopic, listOf(Topics.søknadTopic))

    private val ktor = KtorBuilder()
        .log(log)
        .port(env["HTTP_PORT"]?.toInt() ?: 8080)
        .liveness(::isAlive)
        .readiness(rapidsConnection::isRunning)
        .metrics(CollectorRegistry.defaultRegistry)
        .module {spleisApi(env) }
        .build()

    private val dataSourceBuilder = DataSourceBuilder(env)

    private val helseBuilder = HelseBuilder(
        dataSource = dataSourceBuilder.getDataSource(),
        rapidsConnection = rapidsConnection
    )

    private val migrationsStarted = AtomicBoolean(false)
    private val migrationsFinished = AtomicBoolean(false)

    private fun Application.spleisApi(env: Map<String, String>) {
        install(io.ktor.features.CallLogging) {
            logger = LoggerFactory.getLogger("sikkerLogg")
            level = Level.INFO
            filter { call -> call.request.path().startsWith("/api/") }
        }
        install(io.ktor.features.ContentNegotiation) {
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

    fun start() {
        ktor.start(wait = false)
        try {
            migrationsStarted.set(true)
            dataSourceBuilder.migrate()
        } finally {
            migrationsFinished.set(true)
        }
        rapidsConnection.start()
    }

    fun stop() {
        rapidsConnection.stop()
        ktor.stop(1, 1, TimeUnit.SECONDS)
    }

    private fun isAlive(): Boolean {
        if (migrationsFinished.get()) return rapidsConnection.isRunning()
        return migrationsStarted.get()
    }

    private fun shutdownHook() {
        log.info("received shutdown signal, stopping app")
        stop()
    }

    private fun uncaughtExceptionHandler(thread: Thread, err: Throwable) {
        log.error("Uncaught exception in thread ${thread.name}: ${err.message}", err)
    }
}

private fun String.readFile() =
    try {
        File(this).readText(Charsets.UTF_8)
    } catch (err: FileNotFoundException) {
        null
    }
