package no.nav.helse.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.sakskompleks.SakskompleksService
import no.nav.helse.serde.JsonNodeSerde
import no.nav.helse.søknad.domain.Sykepengesøknad
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed

class SøknadConsumer(
    streamsBuilder: StreamsBuilder,
    private val sakskompleksService: SakskompleksService,
    private val probe: SøknadProbe
) {

    init {
        build(streamsBuilder)
    }

    companion object {
        private val topics = listOf("syfo-soknad-v2")

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    fun build(builder: StreamsBuilder): StreamsBuilder {
        builder.stream<String, JsonNode>(
            topics, Consumed.with(Serdes.String(), JsonNodeSerde(objectMapper))
                .withOffsetResetPolicy(Topology.AutoOffsetReset.EARLIEST)
        )
            .mapValues { jsonNode ->
                Sykepengesøknad(jsonNode)
            }
            .foreach(::håndterSøknad)

        return builder
    }

    private fun håndterSøknad(key: String, søknad: Sykepengesøknad) {
        probe.mottattSøknad(søknad)

        sakskompleksService
            .finnSak(søknad)
            ?.let { sak ->
                sakskompleksService.leggSøknadPåSak(sak, søknad)
                probe.søknadKobletTilSakskompleks(søknad, sak)
            }
            ?: probe.søknadManglerSakskompleks(søknad)
    }
}
