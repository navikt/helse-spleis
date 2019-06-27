package no.nav.helse

import io.ktor.config.ApplicationConfig
import io.ktor.config.MapApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.nais.nais
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
fun createConfigFromEnvironment(env: Map<String, String>) =
        MapApplicationConfig().apply {
            put("server.port", env.getOrDefault("HTTP_PORT", "8080"))

            put("kafka.app-id", "sykepenger-sakskompleks-v1")
            put("kafka.bootstrap-servers", env.getValue("KAFKA_BOOTSTRAP_SERVERS"))

            env["KAFKA_USERNAME"]?.let { put("kafka.username", it) }
            env["KAFKA_PASSWORD"]?.let { put("kafka.password", it) }

            env["NAV_TRUSTSTORE_PATH"]?.let { put("kafka.truststore-path", it) }
            env["NAV_TRUSTSTORE_PASSWORD"]?.let { put("kafka.truststore-password", it) }
        }

@KtorExperimentalAPI
fun main() {
    val config = createConfigFromEnvironment(System.getenv())

    embeddedServer(Netty, createApplicationEnvironment(config)).let { app ->
        app.start(wait = false)

        Runtime.getRuntime().addShutdownHook(Thread {
            app.stop(1, 1, TimeUnit.SECONDS)
        })
    }
}

@KtorExperimentalAPI
fun createApplicationEnvironment(appConfig: ApplicationConfig) = applicationEngineEnvironment {
    config = appConfig

    connector {
        port = appConfig.property("server.port").getString().toInt()
    }

    module {
        nais()
        sakskompleksApplication()
    }
}
