package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IHendelseMediator

// Understands a JSON message representing a Ny Søknad
internal class NySøknadMessage(packet: JsonMessage) : SøknadMessage(packet) {

    private val søknadOpprettet = packet["opprettet"].asLocalDateTime()
    private val aktørId = packet["aktorId"].asText()
    private val orgnummer = packet["arbeidsgiver.orgnummer"].asText()
    private val sykeperioder = packet["soknadsperioder"].map {
        Sykmeldingsperiode(
            fom = it.path("fom").asLocalDate(),
            tom = it.path("tom").asLocalDate(),
            grad = it.path("sykmeldingsgrad").asInt()
        )
    }

    private val sykmelding get() = Sykmelding(
        meldingsreferanseId = this.id,
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykeperioder = sykeperioder,
        mottatt = søknadOpprettet
    )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, sykmelding)
    }
}
