package no.nav.helse.rapids_rivers

import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
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

    private fun shutdownHook() {
        log.info("received shutdown signal, stopping app")
        stop()
    }

    companion object {
        private val log = LoggerFactory.getLogger(RapidApplication::class.java)

        class Builder(private val env: Map<String, String>) {
            init {
                Thread.currentThread().setUncaughtExceptionHandler(::uncaughtExceptionHandler)
            }

            private val kafkaConfig = KafkaConfigBuilder(
                bootstrapServers = env.getValue("KAFKA_BOOTSTRAP_SERVERS"),
                consumerGroupId = env.getValue("KAFKA_CONSUMER_GROUP_ID"),
                username = "/var/run/secrets/nais.io/service_user/username".readFile(),
                password = "/var/run/secrets/nais.io/service_user/password".readFile(),
                truststore = env.getValue("NAV_TRUSTSTORE_PATH"),
                truststorePassword = env.getValue("NAV_TRUSTSTORE_PASSWORD")
            )

            private val rapid = KafkaRapid(
                consumerConfig = kafkaConfig.consumerConfig(),
                producerConfig = kafkaConfig.producerConfig(),
                topic = env.getValue("KAFKA_RAPID_TOPIC")
            )

            private val ktor = createKtorApp(rapid::isRunning, rapid::isRunning)

            fun build(): RapidsConnection = RapidApplication(ktor, rapid)

            private fun uncaughtExceptionHandler(thread: Thread, err: Throwable) {
                log.error("Uncaught exception in thread ${thread.name}: ${err.message}", err)
            }

            private fun createKtorApp(
                isAliveCheck: () -> Boolean = { true },
                isReadyCheck: () -> Boolean = { true }
            ): ApplicationEngine {
                return embeddedServer(Netty, applicationEngineEnvironment {
                    log = RapidApplication.log

                    connector {
                        port = env["HTTP_PORT"]?.toInt() ?: 8080
                    }

                    module {
                        nais(isAliveCheck, isReadyCheck)
                    }
                })
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
