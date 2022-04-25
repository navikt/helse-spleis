package no.nav.helse.spleis

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.install
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import no.nav.helse.spleis.config.ApplicationConfiguration
import no.nav.helse.spleis.config.AzureAdAppConfig
import no.nav.helse.spleis.config.DataSourceConfiguration
import no.nav.helse.spleis.config.KtorConfig
import no.nav.helse.spleis.graphql.installGraphQLApi
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

internal val objectMapper = jacksonObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .registerModule(JavaTimeModule())
    .setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
        indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
        indentObjectsWith(DefaultIndenter("  ", "\n"))
    })

private val httpTraceLog = LoggerFactory.getLogger("tjenestekall")

fun main() {
    val config = ApplicationConfiguration()
    val teller = AtomicInteger(0)
    val app = createApp(config.ktorConfig, config.azureConfig, config.dataSourceConfiguration, teller)
    app.start(wait = true)
}

internal fun createApp(ktorConfig: KtorConfig, azureConfig: AzureAdAppConfig, dataSourceConfiguration: DataSourceConfiguration, teller: AtomicInteger) =
    embeddedServer(
        factory = Netty,
        environment = applicationEngineEnvironment {
            ktorConfig.configure(this)
            module {
                install(CallId) {
                    header("callId")
                    verify { it.isNotEmpty() }
                    generate { UUID.randomUUID().toString() }
                }
                install(CallLogging) {
                    logger = httpTraceLog
                    level = Level.INFO
                    callIdMdc("callId")
                    filter { call -> call.request.path().startsWith("/api/") }
                }
                preStopHook(teller)
                install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
                requestResponseTracing(httpTraceLog)
                nais(teller)
                azureAdAppAuthentication(azureConfig)
                val dataSource = dataSourceConfiguration.getDataSource()
                spesialistApi(dataSource, API_SERVICE)
                spannerApi(dataSource, API_SERVICE)
                sporingApi(dataSource, API_SERVICE)
                installGraphQLApi(dataSource, API_SERVICE)
            }
        },
        configure = {
            this.responseWriteTimeoutSeconds = 30
        }
    )

