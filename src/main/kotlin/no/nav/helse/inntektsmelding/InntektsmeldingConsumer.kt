package no.nav.helse.inntektsmelding

import no.nav.helse.inntektsmelding.serde.InntektsmeldingSerde
import no.nav.helse.sakskompleks.SakskompleksService
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed

class InntektsmeldingConsumer(
    streamsBuilder: StreamsBuilder,
    private val inntektsmeldingKafkaTopic: String,
    private val sakskompleksService: SakskompleksService,
    private val probe: InntektsmeldingProbe = InntektsmeldingProbe()
) {

    init {
        build(streamsBuilder)
    }

    fun build(builder: StreamsBuilder) =
        builder.stream<String, Inntektsmelding>(
            listOf(inntektsmeldingKafkaTopic), Consumed.with(Serdes.String(), InntektsmeldingSerde())
            .withOffsetResetPolicy(Topology.AutoOffsetReset.EARLIEST)
        )
            .peek{_, inntektsmelding -> probe.mottattInntektsmelding(inntektsmelding)}
            .foreach{_, inntektsmelding -> håndterInntektsmelding(inntektsmelding)}

    private fun håndterInntektsmelding(inntektsmelding: Inntektsmelding) {
        sakskompleksService
            .finnSak(inntektsmelding)
            ?.let { sak ->
                sakskompleksService.leggInntektsmeldingPåSak(sak, inntektsmelding)
                probe.inntektsmeldingKobletTilSakskompleks(inntektsmelding, sak)
            }
            ?: probe.inntektmeldingManglerSakskompleks(inntektsmelding)
    }

}