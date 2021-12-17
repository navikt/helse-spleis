package no.nav.helse.spleis.meldinger.model

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime

// Understands a JSON message representing a Søknad
internal abstract class SøknadMessage(packet: JsonMessage, builder: SøknadBuilder) :
    HendelseMessage(packet) {

    protected val sykmeldingSkrevet = packet["sykmeldingSkrevet"].asLocalDateTime()
    final override val fødselsnummer = packet["fnr"].asText()

    init {
        builder.meldingsreferanseId(this.id)
            .fnr(fødselsnummer)
            .opprettet(opprettet)
            .aktørId(packet["aktorId"].asText())
            .sykmeldingSkrevet(sykmeldingSkrevet)
            .organisasjonsnummer(packet["arbeidsgiver.orgnummer"].asText())

        packet["soknadsperioder"].forEach {
            builder.periode(
                fom = it.path("fom").asLocalDate(),
                tom = it.path("tom").asLocalDate(),
                grad = it.path("sykmeldingsgrad").asInt()
            )
        }
    }
}
