package no.nav.helse

import com.zaxxer.hikari.HikariConfig
import io.ktor.application.Application
import io.ktor.application.ApplicationStarted
import io.ktor.application.ApplicationStopping
import io.ktor.application.log
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.Topics.behovTopic
import no.nav.helse.Topics.inntektsmeldingTopic
import no.nav.helse.Topics.opprettGosysOppgaveTopic
import no.nav.helse.Topics.søknadTopic
import no.nav.helse.behov.BehovConsumer
import no.nav.helse.behov.BehovProducer
import no.nav.helse.inntektsmelding.InntektsmeldingConsumer
import no.nav.helse.oppgave.GosysOppgaveProducer
import no.nav.helse.person.LagrePersonDao
import no.nav.helse.person.PersonMediator
import no.nav.helse.person.PersonPostgresRepository
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
import java.time.Duration
import java.util.Properties

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

    val builder = StreamsBuilder()

    SøknadConsumer(builder, søknadTopic, personMediator)
    InntektsmeldingConsumer(builder, inntektsmeldingTopic, personMediator)
    BehovConsumer(builder, behovTopic, personMediator)

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
            log.warn("No reason to keep living, closing stream")
            streams.close(Duration.ofSeconds(10))
        }
    }
    streams.setUncaughtExceptionHandler { _, ex ->
        log.error("Caught exception in stream, exiting", ex)
        streams.close(Duration.ofSeconds(10))
    }
}
