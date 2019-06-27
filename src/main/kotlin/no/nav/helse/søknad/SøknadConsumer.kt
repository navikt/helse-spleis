package no.nav.helse.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.serde.JsonNodeSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory

class SøknadConsumer(streamsBuilder: StreamsBuilder) {

    init {
        build(streamsBuilder)
    }

    companion object {
        private val log = LoggerFactory.getLogger(SøknadConsumer::class.java)

        private val topics = listOf("soknader")

        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    fun build(builder: StreamsBuilder): StreamsBuilder {
        builder.stream<String, JsonNode>(topics, Consumed.with(Serdes.String(), JsonNodeSerde(objectMapper))
                .withOffsetResetPolicy(Topology.AutoOffsetReset.EARLIEST))
                .foreach(::håndterSøknad)

        return builder
    }

    private fun håndterSøknad(key: String, søknad: JsonNode) {
        log.info("mottok søknad med key=$key")
        // TODO: finn eksisterende sak fra søknad
    }
}
