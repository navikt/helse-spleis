package no.nav.helse

import com.auth0.jwk.JwkProviderBuilder
import com.zaxxer.hikari.HikariConfig
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.request.httpMethod
import io.ktor.request.uri
import io.ktor.response.ApplicationSendPipeline
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import no.nav.helse.spleis.*
import no.nav.helse.spleis.http.getJson
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KafkaStreams
import org.slf4j.LoggerFactory
import java.net.URL
import kotlin.system.exitProcess

fun createHikariConfig(jdbcUrl: String, username: String? = null, password: String? = null) =
    HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
        maximumPoolSize = 3
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
        username?.let { this.username = it }
        password?.let { this.password = it }
    }

@KtorExperimentalAPI
fun Application.createHikariConfigFromEnvironment() =
    createHikariConfig(
        jdbcUrl = environment.config.property("database.jdbc-url").getString(),
        username = environment.config.propertyOrNull("database.username")?.getString(),
        password = environment.config.propertyOrNull("database.password")?.getString()
    )

@KtorExperimentalAPI
fun Application.vedtaksperiodeApplication() {
    val helseBuilder = HelseBuilder(
        dataSource = getDataSource(createHikariConfigFromEnvironment()),
        hendelseProducer = KafkaProducer<String, String>(environment.config.producerConfig(), StringSerializer(), StringSerializer())
    )

    helseBuilder.addStateListener(KafkaStreams.StateListener { newState, oldState ->
        log.info("From state={} to state={}", oldState, newState)

        if (newState == KafkaStreams.State.ERROR) {
            log.error("exiting JVM process because the kafka stream has died")
            exitProcess(1)
        }
    })

    environment.monitor.subscribe(ApplicationStarted) {
        migrate(createHikariConfigFromEnvironment())
        helseBuilder.start(environment.config.streamsConfig())
    }

    environment.monitor.subscribe(ApplicationStopping) {
        helseBuilder.stop()
    }

    restInterface(helseBuilder.sakMediator)
}

private val httpTraceLog = LoggerFactory.getLogger("HttpTraceLog")

private val httpRequestCounter = Counter.build("http_requests_total", "Counts the http requests")
    .labelNames("method", "code")
    .register()

private val httpRequestDuration =
    Histogram.build("http_request_duration_seconds", "Distribution of http request duration")
        .register()

@KtorExperimentalAPI
private fun Application.restInterface(
    sakMediator: SakMediator,
    configurationUrl: String = environment.config.property("azure.configuration_url").getString(),
    clientId: String = environment.config.property("azure.client_id").getString(),
    requiredGroup: String = environment.config.property("azure.required_group").getString()
) {
    val idProvider = configurationUrl.getJson()
    val jwkProvider = JwkProviderBuilder(URL(idProvider["jwks_uri"].textValue())).build()

    intercept(ApplicationCallPipeline.Monitoring) {
        val timer = httpRequestDuration.startTimer()

        httpTraceLog.info("incoming ${call.request.httpMethod.value} ${call.request.uri}")

        try {
            proceed()
        } catch (err: Throwable) {
            httpTraceLog.info("exception thrown during processing: ${err.message}", err)
            throw err
        } finally {
            timer.observeDuration()
        }
    }

    sendPipeline.intercept(ApplicationSendPipeline.After) { message ->
        val status = call.response.status() ?: (when (message) {
            is OutgoingContent -> message.status
            is HttpStatusCode -> message
            else -> null
        } ?: HttpStatusCode.OK).also { status ->
            call.response.status(status)
        }

        httpTraceLog.info("responding with ${status.value}")
        httpRequestCounter.labels(call.request.httpMethod.value, "${status.value}").inc()
    }

    install(Authentication) {
        jwt {
            verifier(jwkProvider, idProvider["issuer"].textValue())
            validate { credentials ->
                val groupsClaim = credentials.payload.getClaim("groups").asList(String::class.java)
                if (requiredGroup in groupsClaim && clientId in credentials.payload.audience) {
                    JWTPrincipal(credentials.payload)
                } else {
                    log.info(
                        "${credentials.payload.subject} with audience ${credentials.payload.audience} " +
                            "is not authorized to use this app, denying access"
                    )
                    null
                }
            }
        }
    }

    routing {
        authenticate {
            utbetaling(sakMediator)
            sak(sakMediator)
        }
    }

}
