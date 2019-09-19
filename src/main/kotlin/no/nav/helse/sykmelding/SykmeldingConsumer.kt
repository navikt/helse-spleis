package no.nav.helse.sykmelding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.sakskompleks.SakskompleksService
import no.nav.helse.serde.JsonNodeSerde
import no.nav.helse.sykmelding.domain.SykmeldingMessage
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed

class SykmeldingConsumer(streamsBuilder: StreamsBuilder,
                         private val sykmeldingKafkaTopic: String,
                         private val sakskompleksService: SakskompleksService,
                         private val probe: SykmeldingProbe = SykmeldingProbe()
) {

    init {
        build(streamsBuilder)
    }

    companion object {
        val sykmeldingObjectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    fun build(builder: StreamsBuilder): StreamsBuilder {
        builder.stream<String, JsonNode>(listOf(sykmeldingKafkaTopic), Consumed.with(Serdes.String(), JsonNodeSerde(sykmeldingObjectMapper))
                .withOffsetResetPolicy(Topology.AutoOffsetReset.EARLIEST))
                .mapValues { jsonNode ->
                    SykmeldingMessage(jsonNode)
                }
                .peek { _, sykmeldingMessage ->
                    probe.mottattSykmelding(sykmeldingMessage.sykmelding)
                }
                .foreach(::håndterSykmelding)

        return builder
    }

    private fun håndterSykmelding(key: String, sykmeldingMessage: SykmeldingMessage) {
        sakskompleksService.finnEllerOpprettSak(sykmeldingMessage.sykmelding)
    }
}
