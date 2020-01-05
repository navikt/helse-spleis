package no.nav.helse

import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spleis.HelseBuilder
import no.nav.helse.spleis.HendelseStream
import no.nav.helse.spleis.nais.nais
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KafkaStreams
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit

// Understands how to build our application server
@KtorExperimentalAPI
class ApplicationBuilder(env: Map<String, String>) {

    private val applicationLog = LoggerFactory.getLogger(ApplicationBuilder::class.java)

    private val kafkaConfigBuilder = KafkaConfigBuilder(env)
    private val dataSourceBuilder = DataSourceBuilder(env)

    private val hendelseStream = HendelseStream(listOf(
        Topics.behovTopic,
        Topics.påminnelseTopic,
        Topics.inntektsmeldingTopic,
        Topics.søknadTopic
    ))

    private val helseBuilder = HelseBuilder(
        dataSource = dataSourceBuilder.getDataSource(),
        hendelseStream = hendelseStream,
        hendelseProducer = KafkaProducer<String, String>(kafkaConfigBuilder.producerConfig(), StringSerializer(), StringSerializer())
    )

    private val app = embeddedServer(Netty, applicationEngineEnvironment {
        log = applicationLog

        connector {
            port = env["HTTP_PORT"]?.toInt() ?: 8080
        }

        module {
            nais(::isApplicationAlive, ::isApplicationReady)

            val clientId = "/var/run/secrets/nais.io/azure/client_id".readFile() ?: env.getValue("AZURE_CLIENT_ID")
            // val clientSecret = "/var/run/secrets/nais.io/azure/client_secret".readFile() ?: env.getValue("AZURE_CLIENT_SECRET")

            restInterface(
                personMediator = helseBuilder.personMediator,
                configurationUrl = requireNotNull(env["AZURE_CONFIG_URL"]),
                clientId = clientId,
                requiredGroup = requireNotNull(env["AZURE_REQUIRED_GROUP"])
            )
        }
    })

    init {
        setUncaughtExceptionHandler(applicationLog)
        stopApplicationOnShutdown()

        hendelseStream.addStateListener(KafkaStreams.StateListener { newState, _ ->
            if (newState == KafkaStreams.State.ERROR || newState == KafkaStreams.State.NOT_RUNNING) {
                stop()
            }
        })
    }

    fun start() {
        app.start(wait = false)
        dataSourceBuilder.migrate()
        hendelseStream.start(kafkaConfigBuilder.streamsConfig())
    }

    fun stop() {
        hendelseStream.stop()
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
