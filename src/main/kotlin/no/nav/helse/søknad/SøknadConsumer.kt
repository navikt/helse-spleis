package no.nav.helse.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelse.NySykepengesøknad
import no.nav.helse.hendelse.SendtSykepengesøknad
import no.nav.helse.hendelse.Sykepengesøknad
import no.nav.helse.sakskompleks.SakskompleksService
import no.nav.helse.serde.JsonNodeSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed

internal class SøknadConsumer(
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
                    when (jsonNode["type"].textValue()) {
                        "NY" -> NySykepengesøknad(jsonNode)
                        "FREMTIDIG" -> NySykepengesøknad(jsonNode)
                        "SENDT" -> SendtSykepengesøknad(jsonNode)
                        else -> throw IllegalArgumentException("Kan ikke håndtere søknad med type ${jsonNode["type"].textValue()}.")
                    }
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

        return if (type in listOf("ARBEIDSTAKERE", "SELVSTENDIGE_OG_FRILANSERE") && (status == "SENDT" || status == "NY" || status == "FREMTIDIG")) {
            true
        } else {
            søknadProbe.søknadIgnorert(id, type, status)
            false
        }
    }

    private fun håndterSøknad(key: String, søknad: Sykepengesøknad) {
        when (søknad) {
            is NySykepengesøknad -> sakskompleksService.håndterNySøknad(søknad)
            is SendtSykepengesøknad -> sakskompleksService.håndterSendtSøknad(søknad)
        }
    }
}

