package no.nav.helse.behov

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.PersonMediator
import no.nav.helse.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.sykepengehistorikk.Sykepengehistorikk
import no.nav.helse.sykepengehistorikk.SykepengehistorikkHendelse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed

internal class BehovConsumer(
        streamsBuilder: StreamsBuilder,
        private val behovTopic: String,
        private val personMediator: PersonMediator,
        private val probe: BehovProbe = BehovProbe
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
                    Behov.fromJson(json)
                }
                .filter { _, behov ->
                    behov.harLøsning()
                }
                .peek { _, behov -> probe.mottattBehov(behov) }
                .foreach { _, behov -> håndterLøsning(behov) }

        return builder
    }

    private fun håndterLøsning(løsning: Behov) {
        when (løsning.behovType()) {
            BehovsTyper.Sykepengehistorikk.name -> Sykepengehistorikk(objectMapper.readTree(løsning.toJson())).let {
                personMediator.håndterSykepengehistorikk(SykepengehistorikkHendelse(it))
            }
            BehovsTyper.GodkjenningFraSaksbehandler.name -> ManuellSaksbehandlingHendelse(løsning).let {
                personMediator.håndterManuellSaksbehandling(it)
            }
        }
    }
}

