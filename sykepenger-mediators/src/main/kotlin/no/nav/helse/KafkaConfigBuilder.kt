package no.nav.helse

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

// Understands how to configure kafka from environment variables
internal class KafkaConfigBuilder(private val env: Map<String, String>) {
    private val log = LoggerFactory.getLogger(KafkaConfigBuilder::class.java)

    fun streamsConfig() = kafkaBaseConfig().apply {
        put(StreamsConfig.APPLICATION_ID_CONFIG, env["KAFKA_APP_ID"] ?: "spleis-v3")
        put(
            StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
            LogAndFailExceptionHandler::class.java
        )
        put(
            StreamsConfig.COMMIT_INTERVAL_MS_CONFIG,
            env["KAFKA_COMMIT_INTERVAL_MS_CONFIG"] ?: "1000"
        )
    }

    fun producerConfig() = kafkaBaseConfig().apply {
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
    }

    private fun kafkaBaseConfig() = Properties().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, env["KAFKA_BOOTSTRAP_SERVERS"])
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")

        env["KAFKA_USERNAME"]?.let { username ->
            put(
                SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"${env["KAFKA_PASSWORD"]}\";"
            )
        }

        env["NAV_TRUSTSTORE_PATH"]?.let { truststorePath ->
            env["NAV_TRUSTSTORE_PASSWORD"].let { truststorePassword ->
                try {
                    put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                    put(
                        SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(
                            truststorePath
                        ).absolutePath)
                    put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword)
                    log.info("Configured '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location ")
                } catch (ex: Exception) {
                    log.error("Failed to set '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location", ex)
                }
            }
        }
    }
}
