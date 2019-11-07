package no.nav.helse

import com.auth0.jwk.JwkProviderBuilder
import com.zaxxer.hikari.HikariConfig
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.Topics.behovTopic
import no.nav.helse.Topics.inntektsmeldingTopic
import no.nav.helse.Topics.søknadTopic
import no.nav.helse.behov.BehovProducer
import no.nav.helse.http.getJson
import no.nav.helse.inntektsmelding.InntektsmeldingConsumer
import no.nav.helse.oppgave.GosysOppgaveProducer
import no.nav.helse.person.LagrePersonDao
import no.nav.helse.person.PersonMediator
import no.nav.helse.person.PersonPostgresRepository
import no.nav.helse.person.person
import no.nav.helse.søknad.SøknadConsumer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import java.io.File
import java.net.URL
import java.time.Duration
import java.util.*

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
                username = environment.config.propertyOrNull("database.user")?.getString(),
                password = environment.config.propertyOrNull("database.password")?.getString()
        )

@KtorExperimentalAPI
fun Application.sakskompleksApplication(): KafkaStreams {

    migrate(createHikariConfigFromEnvironment())

    val dataSource = getDataSource(createHikariConfigFromEnvironment())

    val producer = KafkaProducer<String, String>(behovProducerConfig(), StringSerializer(), StringSerializer())
    val personMediator = PersonMediator(
            personRepository = PersonPostgresRepository(dataSource),
            lagrePersonDao = LagrePersonDao(dataSource),
            behovProducer = BehovProducer(behovTopic, producer),
            gosysOppgaveProducer = GosysOppgaveProducer(commonKafkaProperties()))

    restInterface(personMediator)

    val builder = StreamsBuilder()

    SøknadConsumer(builder, søknadTopic, personMediator)
    InntektsmeldingConsumer(builder, inntektsmeldingTopic, personMediator)
    //BehovConsumer(builder, behovTopic, personMediator)

    return KafkaStreams(builder.build(), streamsConfig()).apply {
        addShutdownHook(this)

        environment.monitor.subscribe(ApplicationStarted) {
            start()
        }

        environment.monitor.subscribe(ApplicationStopping) {
            close(Duration.ofSeconds(10))
        }
    }
}

@KtorExperimentalAPI
private fun Application.restInterface(personMediator: PersonMediator,
                                      configurationUrl: String = environment.config.property("azure.configuration_url").getString(),
                                      clientId: String = environment.config.property("azure.client_id").getString(),
                                      requiredGroup: String = environment.config.property("azure.required_group").getString()) {
    val idProvider = configurationUrl.getJson()
    val jwkProvider = JwkProviderBuilder(URL(idProvider["jwks_uri"].textValue())).build()

    install(Authentication) {
        jwt {
            verifier(jwkProvider, idProvider["issuer"].textValue())
            validate { credentials ->
                val groupsClaim = credentials.payload.getClaim("groups").asList(String::class.java)
                if (requiredGroup in groupsClaim && clientId in credentials.payload.audience) {
                    JWTPrincipal(credentials.payload)
                } else {
                    log.info("${credentials.payload.subject} with audience ${credentials.payload.audience} " +
                        "is not authorized to use this app, denying access")
                    null
                }
            }
        }
    }

    routing {
        authenticate {
            person(personMediator)
        }
    }

}


@KtorExperimentalAPI
private fun Application.streamsConfig() = commonKafkaProperties().apply {
    put(StreamsConfig.APPLICATION_ID_CONFIG, environment.config.property("kafka.app-id").getString())
    put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler::class.java)
    put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, environment.config.property("kafka.commit-interval-ms-config").getString())
}

@KtorExperimentalAPI
private fun Application.behovProducerConfig() = commonKafkaProperties().apply {
    put(ProducerConfig.ACKS_CONFIG, "all")
    put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
}

@KtorExperimentalAPI
private fun Application.commonKafkaProperties() = Properties().apply {
    put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, environment.config.property("kafka.bootstrap-servers").getString())
    put(SaslConfigs.SASL_MECHANISM, "PLAIN")
    put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")

    environment.config.propertyOrNull("kafka.username")?.getString()?.let { username ->
        environment.config.propertyOrNull("kafka.password")?.getString()?.let { password ->
            put(
                    SaslConfigs.SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";"
            )
        }
    }

    environment.config.propertyOrNull("kafka.truststore-path")?.getString()?.let { truststorePath ->
        environment.config.propertyOrNull("kafka.truststore-password")?.getString().let { truststorePassword ->
            try {
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(truststorePath).absolutePath)
                put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword)
                log.info("Configured '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location ")
            } catch (ex: Exception) {
                log.error("Failed to set '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location", ex)
            }
        }
    }
}

private fun Application.addShutdownHook(streams: KafkaStreams) {
    streams.setStateListener { newState, oldState ->
        log.info("From state={} to state={}", oldState, newState)

        if (newState == KafkaStreams.State.ERROR) {
            // if the stream has died there is no reason to keep spinning
            log.warn("closing stream because it went into error state")
            streams.close(Duration.ofSeconds(10))
        }
    }
    streams.setUncaughtExceptionHandler { _, err ->
        log.error("Caught exception in stream: ${err.message}", err)
        streams.close(Duration.ofSeconds(10))
    }
}
