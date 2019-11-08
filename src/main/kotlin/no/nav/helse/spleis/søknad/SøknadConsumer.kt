package no.nav.helse.spleis.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.hendelser.søknad.NySøknadHendelse
import no.nav.helse.person.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.person.hendelser.søknad.Sykepengesøknad
import no.nav.helse.spleis.PersonMediator
import no.nav.helse.spleis.serde.JsonNodeSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed

internal class SøknadConsumer(
        streamsBuilder: StreamsBuilder,
        private val søknadKafkaTopic: String,
        private val personMediator: PersonMediator,
        private val probe: SøknadProbe = SøknadProbe
) {

    init {
        build(streamsBuilder)
    }

    companion object {
        internal val søknadObjectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    fun build(builder: StreamsBuilder): StreamsBuilder {
        val sendtSøknad = builder.stream<String, JsonNode>(
                listOf(søknadKafkaTopic), Consumed.with(Serdes.String(), JsonNodeSerde(søknadObjectMapper))
                .withOffsetResetPolicy(Topology.AutoOffsetReset.EARLIEST)
        )
        sendtSøknad
                .filter { _, søknad -> skalTaInnSøknad(søknad = søknad, søknadProbe = probe) }
                .mapValues { søknad -> Sykepengesøknad(søknad) }
                .peek { _, søknad -> SøknadProbe.mottattSøknad(søknad) }
                .foreach { _, søknad -> håndterSøknad(søknad) }

        return builder
    }

    private fun håndterSøknad(søknad: Sykepengesøknad) {
        when (søknad.status) {
            "NY" -> personMediator.håndterNySøknad(NySøknadHendelse(søknad))
            "FREMTIDIG" -> personMediator.håndterNySøknad(NySøknadHendelse(søknad))
            "SENDT" -> personMediator.håndterSendtSøknad(SendtSøknadHendelse(søknad))
        }
    }
}

