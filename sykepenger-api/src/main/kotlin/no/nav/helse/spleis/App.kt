package no.nav.helse.spleis

import tools.jackson.core.util.DefaultIndenter
import tools.jackson.core.util.DefaultPrettyPrinter
import tools.jackson.module.kotlin.jacksonMapperBuilder
import com.github.navikt.tbd_libs.naisful.naisApp
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.header
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import javax.sql.DataSource
import no.nav.helse.spleis.config.ApplicationConfiguration
import no.nav.helse.spleis.config.AzureAdAppConfig
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.dao.SendtDao
import no.nav.helse.spleis.opptjening.opptjeningApi
import no.nav.helse.spleis.rest.personApi
import org.slf4j.LoggerFactory

internal val nyObjectmapper
    get() =
        jacksonMapperBuilder()
            .defaultPrettyPrinter(
                DefaultPrettyPrinter()
                    .withArrayIndenter(DefaultPrettyPrinter.FixedSpaceIndenter.instance())
                    .withObjectIndenter(DefaultIndenter("  ", "\n"))
            )
            .build()

internal val objectMapper = nyObjectmapper
internal val logg = LoggerFactory.getLogger("no.nav.helse.spleis.api.Application")

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { thread, err ->
        logg.error("Uncaught exception in thread ${thread.name}: {}", err.message, err)
    }

    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val config = ApplicationConfiguration(meterRegistry)
    val app =
        createApp(
            azureConfig = config.azureConfig,
            spekematClient = config.spekematClient,
            dataSourceProvider = { config.dataSource },
            meterRegistry = meterRegistry
        )
    app.start(wait = true)
}

internal fun createApp(
    azureConfig: AzureAdAppConfig,
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
        lagApplikasjonsmodul(
            spekematClient = spekematClient,
            dataSourceProvider = dataSourceProvider,
            meterRegistry = meterRegistry
        )
    }
)

internal fun Application.lagApplikasjonsmodul(
    spekematClient: SpekematClient,
    dataSourceProvider: () -> DataSource,
    meterRegistry: PrometheusMeterRegistry
) {
    requestResponseTracing(LoggerFactory.getLogger("no.nav.helse.spleis.api.Tracing"), meterRegistry)

    val hendelseDao = HendelseDao(dataSourceProvider, meterRegistry)
    val personDao = PersonDao(dataSourceProvider, meterRegistry)
    val sendtDao = SendtDao(dataSourceProvider)

    spannerApi(hendelseDao, personDao, sendtDao)
    sporingApi(personDao)
    personApi(
        spekematClient = spekematClient,
        hendelseDao = hendelseDao,
        personDao = personDao,
        meterRegistry = meterRegistry
    )
    opptjeningApi(personDao)
}
