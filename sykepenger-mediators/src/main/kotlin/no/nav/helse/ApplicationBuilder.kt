package no.nav.helse

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.jackson.jackson
import io.ktor.request.path
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.HelseBuilder
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.io.File
import java.io.FileNotFoundException

// Understands how to build our application server
class ApplicationBuilder(env: Map<String, String>) : RapidsConnection.StatusListener {

    private companion object {
        private val log = LoggerFactory.getLogger(ApplicationBuilder::class.java)
    }

    private val rapidsConnection = RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
        .withKtorModule { spleisApi(env) }
        .build()
        .apply { register(this@ApplicationBuilder) }

    private val dataSourceBuilder = DataSourceBuilder(env)

    private val helseBuilder = HelseBuilder(
        dataSource = dataSourceBuilder.getDataSource(),
        rapidsConnection = rapidsConnection
    )

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

    fun start() = rapidsConnection.start()
    fun stop() = rapidsConnection.stop()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        dataSourceBuilder.migrate()
    }
}

private fun String.readFile() =
    try {
        File(this).readText(Charsets.UTF_8)
    } catch (err: FileNotFoundException) {
        null
    }
