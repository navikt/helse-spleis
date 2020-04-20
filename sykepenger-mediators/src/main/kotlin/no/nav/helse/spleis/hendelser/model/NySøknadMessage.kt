package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing a Ny Søknad
internal class NySøknadMessage(packet: JsonMessage) : SøknadMessage(packet) {

    private val aktørId = packet["aktorId"].asText()
    private val orgnummer = packet["arbeidsgiver.orgnummer"].asText()
    private val sykeperioder = packet["soknadsperioder"].map {
        Triple(
            first = it.path("fom").asLocalDate(),
            second = it.path("tom").asLocalDate(),
            third = it.path("sykmeldingsgrad").asInt()
        )
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asSykmelding() = Sykmelding(
        meldingsreferanseId = this.id,
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykeperioder = sykeperioder
    )
}
