package no.nav.helse.spleis

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.naisful.naisApp
import com.github.navikt.tbd_libs.speed.SpeedClient
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.header
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.spleis.config.ApplicationConfiguration
import no.nav.helse.spleis.config.AzureAdAppConfig
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.graphql.Api.installGraphQLApi
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.text.get

internal val nyObjectmapper get() =
    jacksonObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .registerModule(JavaTimeModule())
        .setDefaultPrettyPrinter(
            DefaultPrettyPrinter().apply {
                indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                indentObjectsWith(DefaultIndenter("  ", "\n"))
            }
        )

internal val objectMapper = nyObjectmapper
internal val logg = LoggerFactory.getLogger("no.nav.helse.spleis.api.Application")

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { thread, err ->
        logg.error("Uncaught exception in thread ${thread.name}: {}", err.message, err)
    }

    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val config = ApplicationConfiguration(meterRegistry)
    val app = createApp(config.azureConfig, config.speedClient, config.spekematClient, { config.dataSource }, meterRegistry)
    app.start(wait = true)
}

internal fun createApp(
    azureConfig: AzureAdAppConfig,
    speedClient: SpeedClient,
    spekematClient: SpekematClient,
    dataSourceProvider: () -> DataSource,
    meterRegistry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
    port: Int = 8080
) = naisApp(
    meterRegistry = meterRegistry,
    objectMapper = objectMapper,
    applicationLogger = logg,
    callLogger = LoggerFactory.getLogger("no.nav.helse.spleis.api.CallLogging"),
    timersConfig = { call, _ ->
        this
            .tag("azp_name", call.principal<JWTPrincipal>()?.get("azp_name") ?: "n/a")
            // https://github.com/linkerd/polixy/blob/main/DESIGN.md#l5d-client-id-client-id
            // eksempel: <APP>.<NAMESPACE>.serviceaccount.identity.linkerd.cluster.local
            .tag("konsument", call.request.header("L5d-Client-Id") ?: "n/a")
    },
    mdcEntries =
        mapOf(
            "azp_name" to { call: ApplicationCall -> call.principal<JWTPrincipal>()?.get("azp_name") },
            "konsument" to { call: ApplicationCall -> call.request.header("L5d-Client-Id") }
        ),
    port = port,
    applicationModule = {
        azureAdAppAuthentication(azureConfig)
        lagApplikasjonsmodul(speedClient, spekematClient, dataSourceProvider, meterRegistry)
    }
)

internal fun Application.lagApplikasjonsmodul(
    speedClient: SpeedClient,
    spekematClient: SpekematClient,
    dataSourceProvider: () -> DataSource,
    meterRegistry: PrometheusMeterRegistry
) {
    requestResponseTracing(LoggerFactory.getLogger("no.nav.helse.spleis.api.Tracing"), meterRegistry)

    val hendelseDao = HendelseDao(dataSourceProvider, meterRegistry)
    val personDao = PersonDao(dataSourceProvider, meterRegistry)

    spannerApi(hendelseDao, personDao)
    sporingApi(hendelseDao, personDao)
    installGraphQLApi(speedClient, spekematClient, hendelseDao, personDao, meterRegistry)
}
