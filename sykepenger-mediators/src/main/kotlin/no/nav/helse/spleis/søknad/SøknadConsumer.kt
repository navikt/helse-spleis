package no.nav.helse.spleis.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.hendelser.søknad.Sykepengesøknad
import no.nav.helse.spleis.SakMediator
import no.nav.helse.spleis.serde.JsonNodeSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed

internal class SøknadConsumer(
    streamsBuilder: StreamsBuilder,
    private val søknadKafkaTopic: String,
    private val sakMediatorMediator: SakMediator,
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
                .filter { _, søknad -> skalTaInnSøknad(søknad = søknad) }
                .mapValues { søknad -> Sykepengesøknad(søknad) }
                .peek { _, søknad -> SøknadProbe.mottattSøknad(søknad) }
                .foreach { _, søknad -> håndter(søknad) }

        return builder
    }

    private fun håndter(søknad: Sykepengesøknad) {
        when (søknad.status) {
            "NY" -> sakMediatorMediator.håndter(NySøknadHendelse(søknad))
            "FREMTIDIG" -> sakMediatorMediator.håndter(NySøknadHendelse(søknad))
            "SENDT" -> sakMediatorMediator.håndter(SendtSøknadHendelse(søknad))
        }
    }
}

