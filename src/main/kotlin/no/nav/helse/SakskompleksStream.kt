package no.nav.helse

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.zaxxer.hikari.HikariConfig
import io.ktor.application.*
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.Topics.behovTopic
import no.nav.helse.Topics.inntektsmeldingTopic
import no.nav.helse.Topics.sykmeldingTopic
import no.nav.helse.Topics.søknadTopic
import no.nav.helse.behov.BehovProducer
import no.nav.helse.inntektsmelding.InntektsmeldingConsumer
import no.nav.helse.sakskompleks.SakskompleksDao
import no.nav.helse.sakskompleks.SakskompleksService
import no.nav.helse.sakskompleks.db.getDataSource
import no.nav.helse.sakskompleks.db.migrate
import no.nav.helse.søknad.SøknadConsumer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import java.io.File
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

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    migrate(createHikariConfigFromEnvironment())

    val sakskompleksService = SakskompleksService(
            behovProducer = BehovProducer(behovTopic, KafkaProducer(behovProducerConfig(), StringSerializer(), StringSerializer())),
            sakskompleksDao = SakskompleksDao(getDataSource(createHikariConfigFromEnvironment())))

    val builder = StreamsBuilder()

    SøknadConsumer(builder, søknadTopic, sakskompleksService)
    InntektsmeldingConsumer(builder, inntektsmeldingTopic, sakskompleksService)

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
private fun Application.streamsConfig() = commonKafkaProperties().apply {
    put(StreamsConfig.APPLICATION_ID_CONFIG, environment.config.property("kafka.app-id").getString())

    put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler::class.java)
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
            log.warn("No reason to keep living, closing stream")
            streams.close(Duration.ofSeconds(10))
        }
    }
    streams.setUncaughtExceptionHandler { _, ex ->
        log.error("Caught exception in stream, exiting", ex)
        streams.close(Duration.ofSeconds(10))
    }
}
