package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

// Understands a JSON message representing a Ny Søknad
internal class NySøknadMessage(packet: JsonMessage) : SøknadMessage(packet) {

    private val aktørId = packet["aktorId"].asText()
    private val orgnummer = packet["arbeidsgiver.orgnummer"].asText()
    private val sykeperioder = packet["soknadsperioder"].map {
        Sykmeldingsperiode(
            fom = it.path("fom").asLocalDate(),
            tom = it.path("tom").asLocalDate(),
            grad = it.path("sykmeldingsgrad").asInt().prosent
        )
    }

    private val sykmelding get() = Sykmelding(
        meldingsreferanseId = this.id,
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykeperioder = sykeperioder,
        sykmeldingSkrevet = sykmeldingSkrevet,
        mottatt = opprettet
    )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, sykmelding)
    }
}
