package no.nav.helse

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.jackson.jackson
import io.ktor.request.path
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.HendelseMediator
import no.nav.helse.spleis.db.*
import no.nav.helse.spleis.rest.PersonRestInterface
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.io.File
import java.io.FileNotFoundException

// Understands how to build our application server
class ApplicationBuilder(env: Map<String, String>) : RapidsConnection.StatusListener {

    private val dataSourceBuilder = DataSourceBuilder(env)
    private val dataSource = dataSourceBuilder.getDataSource()

    private val hendelseRecorder = HendelseRecorder(dataSource)
    private val personRepository = PersonPostgresRepository(dataSource)
    private val lagrePersonDao = LagrePersonDao(dataSource)
    private val utbetalingsreferanseRepository = UtbetalingsreferansePostgresRepository(dataSource)
    private val lagreUtbetalingDao = LagreUtbetalingDao(dataSource)
    private val personRestInterface = PersonRestInterface(personRepository, utbetalingsreferanseRepository)

    private val rapidsConnection = RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
        .withKtorModule { spleisApi(env, personRestInterface, hendelseRecorder) }
        .build()

    init {
        rapidsConnection.register(this)
        HendelseMediator(
            rapidsConnection = rapidsConnection,
            personRepository = personRepository,
            lagrePersonDao = lagrePersonDao,
            lagreUtbetalingDao = lagreUtbetalingDao,
            hendelseRecorder = hendelseRecorder
        )
    }

    fun start() = rapidsConnection.start()
    fun stop() = rapidsConnection.stop()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        dataSourceBuilder.migrate()
    }
}

internal fun Application.spleisApi(env: Map<String, String>, restInterface: PersonRestInterface, hendelseRecorder: HendelseRecorder) {
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
        personRestInterface = restInterface,
        hendelseRecorder = hendelseRecorder,
        configurationUrl = requireNotNull(env["AZURE_CONFIG_URL"]),
        clientId = clientId,
        requiredGroup = requireNotNull(env["AZURE_REQUIRED_GROUP"])
    )
}

private fun String.readFile() =
    try {
        File(this).readText(Charsets.UTF_8)
    } catch (err: FileNotFoundException) {
        null
    }
