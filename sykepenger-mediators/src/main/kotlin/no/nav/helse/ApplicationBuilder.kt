package no.nav.helse

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.jackson.jackson
import io.ktor.request.path
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.HelseBuilder
import no.nav.helse.spleis.db.Meldingstype.*
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.io.File
import java.io.FileNotFoundException
import kotlin.math.floor

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
        val ds = dataSourceBuilder.getDataSource()

        val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        using(sessionOf(ds)) {
            val antall = it.run(queryOf("SELECT COUNT(1) FROM melding WHERE fnr=''").map { it.long(1) }.asSingle) ?: return@using
            var håndtert = 0
            var forrigeProsent = 0
            while (antall > håndtert) {
                val meldinger = it.run(queryOf("SELECT id,melding_type,data FROM melding ORDER BY id ASC LIMIT 1000 OFFSET ?", håndtert).map {
                    Triple(it.long(1), it.string(2), it.string(3))
                }.asList)

                meldinger.map { (id, type, jsonString) ->
                    val json = objectMapper.readTree(jsonString)
                    val fnr = when (valueOf(type)) {
                        NY_SØKNAD, SENDT_SØKNAD -> json["fnr"].asText()
                        INNTEKTSMELDING -> json["arbeidstakerFnr"].asText()
                        PÅMINNELSE, YTELSER, VILKÅRSGRUNNLAG, MANUELL_SAKSBEHANDLING, UTBETALING -> json["fødselsnummer"].asText()
                    }

                    it.run(queryOf("UPDATE melding SET fnr=? WHERE id=?", fnr, id).asUpdate)
                    håndtert += 1

                    val donePercent = floor(håndtert / antall.toDouble() * 100).toInt()
                    if (donePercent > forrigeProsent) {
                        forrigeProsent = donePercent
                        log.info("$donePercent % ferdig, $håndtert av $antall håndtert. ${antall - håndtert} gjenstående.")
                    }
                }
            }
            log.info("100 % ferdig, $håndtert av $antall håndtert. ${antall - håndtert} gjenstående.")
        }
    }
}

private fun String.readFile() =
    try {
        File(this).readText(Charsets.UTF_8)
    } catch (err: FileNotFoundException) {
        null
    }
