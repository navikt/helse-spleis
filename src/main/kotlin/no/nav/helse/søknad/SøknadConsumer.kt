package no.nav.helse.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.oppgave.GosysOppgaveProducer
import no.nav.helse.person.PersonMediator
import no.nav.helse.serde.JsonNodeSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class SøknadConsumer(
        streamsBuilder: StreamsBuilder,
        private val søknadKafkaTopic: String,
        private val opprettGosysOppgaveTopic: String,
        private val personMediator: PersonMediator,
        private val probe: SøknadProbe = SøknadProbe()
) {

    init {
        build(streamsBuilder)
    }

    companion object {
        val søknadObjectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val log = LoggerFactory.getLogger(SøknadProbe::class.java)

    }

    fun build(builder: StreamsBuilder): StreamsBuilder {
        val sendtSøknad = builder.stream<String, JsonNode>(
                listOf(søknadKafkaTopic), Consumed.with(Serdes.String(), JsonNodeSerde(søknadObjectMapper))
                .withOffsetResetPolicy(Topology.AutoOffsetReset.EARLIEST)
        )
        sendtSøknad
                .filter { _, jsonNode ->
                    skalTaInnSøknad(søknad = jsonNode, søknadProbe = probe)
                }
                .mapValues { jsonNode -> Sykepengesøknad(jsonNode) }
                .peek { _, søknad -> probe.mottattSøknad(søknad) }
                .foreach(::håndterSøknad)

        sendtSøknad
                .peek { key, value -> log.info("key $key value $value") }
                .filter { _, søknad -> søknad["status"].textValue() == "SENDT" }
                .map { key, søknad -> KeyValue(søknad["aktorId"].textValue(), søknadObjectMapper.writeValueAsString(GosysOppgaveProducer.OpprettGosysOppgaveDto(aktorId = søknad["aktorId"].textValue()))) }
                .peek { key, value -> log.info("key $key value $value") }
                .to(opprettGosysOppgaveTopic, Produced.with(Serdes.String(), Serdes.String()))


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
        when (søknad.status) {
            "NY" -> personMediator.håndterNySøknad(NySøknadHendelse(søknad))
            "FREMTIDIG" -> personMediator.håndterNySøknad(NySøknadHendelse(søknad))
            "SENDT" -> personMediator.håndterSendtSøknad(SendtSøknadHendelse(søknad))
        }
    }
}

