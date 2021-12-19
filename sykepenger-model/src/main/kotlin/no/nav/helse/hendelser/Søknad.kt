package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import java.time.LocalDateTime
import java.util.*

class Søknad(
    meldingsreferanseId: UUID,
    fnr: String,
    aktørId: String,
    orgnummer: String,
    private val perioder: List<Søknadsperiode>,
    andreInntektskilder: List<Inntektskilde>,
    sendtTilNAV: LocalDateTime,
    permittert: Boolean,
    merknaderFraSykmelding: List<Merknad>,
    sykmeldingSkrevet: LocalDateTime
) : SendtSøknad(meldingsreferanseId, fnr, aktørId, orgnummer, perioder, andreInntektskilder, sendtTilNAV, permittert, merknaderFraSykmelding, sykmeldingSkrevet) {

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    override fun melding(klassName: String) = "Søknad"

    internal fun harArbeidsdager() = perioder.filterIsInstance<Søknadsperiode.Arbeid>().isNotEmpty()
}
