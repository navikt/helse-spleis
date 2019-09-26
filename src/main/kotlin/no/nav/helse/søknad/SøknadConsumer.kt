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
    private val søknadKafkaTopic: String,
    private val sakskompleksService: SakskompleksService,
    private val probe: SøknadProbe = SøknadProbe()
) {

    init {
        build(streamsBuilder)
    }

    companion object {
        val søknadObjectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    fun build(builder: StreamsBuilder): StreamsBuilder {
        builder.stream<String, JsonNode>(
            listOf(søknadKafkaTopic), Consumed.with(Serdes.String(), JsonNodeSerde(søknadObjectMapper))
                .withOffsetResetPolicy(Topology.AutoOffsetReset.EARLIEST)
        )
            .filter { _, jsonNode ->
                skalTaInnSøknad(søknad = jsonNode, søknadProbe = probe)
            }
            .mapValues { jsonNode ->
                Sykepengesøknad(jsonNode)
            }
            .peek { _, søknad -> probe.mottattSøknad(søknad) }
            .foreach(::håndterSøknad)

        return builder
    }

    private fun skalTaInnSøknad(søknad: JsonNode, søknadProbe: SøknadProbe): Boolean {
        val id = søknad["id"].textValue()
        val type = søknad["soknadstype"]?.textValue()
            ?: søknad["type"]?.textValue()
            ?: throw RuntimeException("Fant ikke type på søknad")
        val status = søknad["status"].textValue()

        return if (type in listOf("ARBEIDSTAKERE", "SELVSTENDIGE_OG_FRILANSERE") && status == "SENDT") {
            true
        } else {
            søknadProbe.søknadIgnorert(id, type, status)
            false
        }
    }

    private fun håndterSøknad(key: String, søknad: Sykepengesøknad) {
        sakskompleksService.knyttSøknadTilSak(søknad)
    }
}

