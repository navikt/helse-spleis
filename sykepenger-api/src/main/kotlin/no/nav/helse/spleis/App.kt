package no.nav.helse.spleis

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.naisful.naisApp
import com.github.navikt.tbd_libs.spurtedu.SpurteDuClient
import io.ktor.server.application.Application
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import javax.sql.DataSource
import no.nav.helse.spleis.config.ApplicationConfiguration
import no.nav.helse.spleis.config.AzureAdAppConfig
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.graphql.Api.installGraphQLApi
import org.slf4j.LoggerFactory

internal val nyObjectmapper get() = jacksonObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .registerModule(JavaTimeModule())
    .setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
        indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
        indentObjectsWith(DefaultIndenter("  ", "\n"))
    })

internal val objectMapper = nyObjectmapper
internal val logg = LoggerFactory.getLogger("no.nav.helse.spleis.api.Application")

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { thread, err ->
        logg.error("Uncaught exception in thread ${thread.name}: {}", err.message, err)
    }

    val config = ApplicationConfiguration()
    val dataSource by lazy {
        // viktig å cache resultatet fra getDataSource() fordi den gir en -ny- tilkobling hver gang.
        // gjentatte kall til getDataSource() vil til slutt tømme databasen for tilkoblinger
        config.dataSourceConfiguration.getDataSource()
    }
    val app = createApp(config.azureConfig, config.spekematClient, config.spurteDuClient, { dataSource })
    app.start(wait = true)
}

internal fun createApp(
    azureConfig: AzureAdAppConfig,
    spekematClient: SpekematClient,
    spurteDuClient: SpurteDuClient?,
    dataSourceProvider: () -> DataSource,
    meterRegistry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
    port: Int = 8080
) = naisApp(
    meterRegistry = meterRegistry,
    objectMapper = objectMapper,
    applicationLogger = logg,
    callLogger = LoggerFactory.getLogger("no.nav.helse.spleis.api.CallLogging"),
    port = port,
    applicationModule = {
        azureAdAppAuthentication(azureConfig)
        lagApplikasjonsmodul(spekematClient, spurteDuClient, dataSourceProvider, meterRegistry)
    }
)

internal fun Application.lagApplikasjonsmodul(
    spekematClient: SpekematClient,
    spurteDuClient: SpurteDuClient?,
    dataSourceProvider: () -> DataSource,
    meterRegistry: PrometheusMeterRegistry
) {
    requestResponseTracing(LoggerFactory.getLogger("no.nav.helse.spleis.api.Tracing"), meterRegistry)

    val hendelseDao = HendelseDao(dataSourceProvider, meterRegistry)
    val personDao = PersonDao(dataSourceProvider, meterRegistry)

    spannerApi(hendelseDao, personDao, spurteDuClient)
    sporingApi(hendelseDao, personDao)
    installGraphQLApi(spekematClient, hendelseDao, personDao, meterRegistry)
}