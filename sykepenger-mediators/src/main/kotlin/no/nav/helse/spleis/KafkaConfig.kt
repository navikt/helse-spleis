package no.nav.helse.spleis

import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Properties

private val log = LoggerFactory.getLogger("KafkaConfig")

@KtorExperimentalAPI
internal fun ApplicationConfig.streamsConfig() = kafkaBaseConfig().apply {
    put(StreamsConfig.APPLICATION_ID_CONFIG, property("kafka.app-id").getString())
    put(
        StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
        LogAndFailExceptionHandler::class.java
    )
    put(
        StreamsConfig.COMMIT_INTERVAL_MS_CONFIG,
        property("kafka.commit-interval-ms-config").getString()
    )
}

@KtorExperimentalAPI
internal fun ApplicationConfig.producerConfig() = this.kafkaBaseConfig().apply {
    put(ProducerConfig.ACKS_CONFIG, "all")
    put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
}

@KtorExperimentalAPI
private fun ApplicationConfig.kafkaBaseConfig() = Properties().apply {
    put(
        CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
        property("kafka.bootstrap-servers").getString()
    )
    put(SaslConfigs.SASL_MECHANISM, "PLAIN")
    put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")

    propertyOrNull("kafka.username")?.getString()?.let { username ->
        propertyOrNull("kafka.password")?.getString()?.let { password ->
            put(
                SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";"
            )
        }
    }

    propertyOrNull("kafka.truststore-path")?.getString()?.let { truststorePath ->
        propertyOrNull("kafka.truststore-password")?.getString().let { truststorePassword ->
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
