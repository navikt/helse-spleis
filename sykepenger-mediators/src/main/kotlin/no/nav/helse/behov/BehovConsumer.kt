package no.nav.helse.behov

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.spleis.SakMediator
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream

internal class BehovConsumer(
        streamsBuilder: StreamsBuilder,
        private val behovTopic: String,
        private val mediator: SakMediator
) {

    init {
        build(streamsBuilder)
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    fun build(builder: StreamsBuilder): StreamsBuilder {
        builder.stream<String, String>(
                listOf(behovTopic), Consumed.with(Serdes.String(), Serdes.String())
                .withOffsetResetPolicy(Topology.AutoOffsetReset.EARLIEST)
        )
                .mapValues { _, json ->
                    try {
                        Behov.fromJson(json)
                    } catch (err: Exception) {
                        null
                    }
                }.filterNotNull()
                .filter { _, behov ->
                    behov.erLøst()
                }
                .peek { _, behov -> BehovProbe.mottattBehov(behov) }
                .foreach { _, behov -> mediator.håndter(behov) }

        return builder
    }

    private inline fun <reified V> KStream<*, V?>.filterNotNull(): KStream<*, V> =
        this.filter { _, value -> value != null }
            .mapValues { _, value -> value as V }
}

