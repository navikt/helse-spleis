package no.nav.helse.spleis

import com.apurebase.kgraphql.GraphQL
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.helse.spleis.config.ApplicationConfiguration
import no.nav.helse.spleis.config.AzureAdAppConfig
import no.nav.helse.spleis.config.DataSourceConfiguration
import no.nav.helse.spleis.config.KtorConfig
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

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
    embeddedServer(Netty, applicationEngineEnvironment {
        ktorConfig.configure(this)
        module {
            install(CallId) {
                generate {
                    UUID.randomUUID().toString()
                }
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
            val dataSource = dataSourceConfiguration.getDataSource(DataSourceConfiguration.Role.ReadOnly)
            spesialistApi(dataSource, API_SERVICE)
            spannerApi(dataSource, API_SERVICE)
            installGraphQLApi(dataSource, API_SERVICE)
        }
    })

