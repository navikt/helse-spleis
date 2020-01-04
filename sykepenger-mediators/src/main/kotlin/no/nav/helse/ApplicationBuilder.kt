package no.nav.helse

import io.ktor.config.MapApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spleis.nais.nais
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit

// Understands how to build our application server
@KtorExperimentalAPI
class ApplicationBuilder(env: Map<String, String>) {

    private val applicationLog = LoggerFactory.getLogger(ApplicationBuilder::class.java)

    private val ktorConfig = createConfigFromEnvironment(env)
    private val app = embeddedServer(Netty, applicationEngineEnvironment {
        config = ktorConfig

        log = applicationLog

        connector {
            port = ktorConfig.getInt("server.port")
        }

        module {
            nais(::isApplicationAlive, ::isApplicationReady)
            helseStream()
        }
    })

    init {
        setUncaughtExceptionHandler(applicationLog)
        stopApplicationOnShutdown()
    }

    fun start() {
        app.start(wait = false)
    }

    fun stop() {
        app.stop(1, 5, TimeUnit.SECONDS)
    }

    private fun isApplicationAlive() = true
    private fun isApplicationReady() = true

    private fun stopApplicationOnShutdown() {
        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
        })
    }

    companion object {
        fun createConfigFromEnvironment(env: Map<String, String>) =
            MapApplicationConfig().apply {
                put("server.port", env.getOrDefault("HTTP_PORT", "8080"))

                put("kafka.app-id", env.getOrDefault("KAFKA_APP_ID", "spleis-v3"))

                env["KAFKA_BOOTSTRAP_SERVERS"]?.let { put("kafka.bootstrap-servers", it) }
                env["KAFKA_USERNAME"]?.let { put("kafka.username", it) }
                env["KAFKA_PASSWORD"]?.let { put("kafka.password", it) }

                put("kafka.commit-interval-ms-config", env.getOrDefault("KAFKA_COMMIT_INTERVAL_MS_CONFIG", "1000"))

                env["NAV_TRUSTSTORE_PATH"]?.let { put("kafka.truststore-path", it) }
                env["NAV_TRUSTSTORE_PASSWORD"]?.let { put("kafka.truststore-password", it) }

                env["DATABASE_HOST"]?.let { put("database.host", it) }
                env["DATABASE_PORT"]?.let { put("database.port", it) }
                env["DATABASE_NAME"]?.let { put("database.name", it) }
                env["DATABASE_USERNAME"]?.let { put("database.username", it) }
                env["DATABASE_PASSWORD"]?.let { put("database.password", it) }

                put("database.jdbc-url", env["DATABASE_JDBC_URL"]
                    ?: String.format(
                        "jdbc:postgresql://%s:%s/%s%s",
                        property("database.host").getString(),
                        property("database.port").getString(),
                        property("database.name").getString(),
                        propertyOrNull("database.username")?.getString()?.let {
                            "?user=$it"
                        } ?: ""))

                env["VAULT_MOUNTPATH"]?.let { put("database.vault.mountpath", it) }

                put("azure.client_id", "/var/run/secrets/nais.io/azure/client_id".readFile() ?: env.getValue("AZURE_CLIENT_ID"))
                put("azure.client_secret", "/var/run/secrets/nais.io/azure/client_secret".readFile() ?: env.getValue("AZURE_CLIENT_SECRET"))
                env["AZURE_CONFIG_URL"]?.let { put("azure.configuration_url", it) }
                env["AZURE_REQUIRED_GROUP"]?.let { put("azure.required_group", it) }
            }

        private fun setUncaughtExceptionHandler(logger: Logger) {
            Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
                logger.error("uncaught exception in thread ${thread.name}: ${err.message}", err)
            }
        }
    }
}

@KtorExperimentalAPI
private fun MapApplicationConfig.getString(property: String) =
    this.property(property).getString()

@KtorExperimentalAPI
private fun MapApplicationConfig.getInt(property: String) =
    this.getString(property).toInt()

private fun String.readFile() =
    try {
        File(this).readText(Charsets.UTF_8)
    } catch (err: FileNotFoundException) {
        null
    }
