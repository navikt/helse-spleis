package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import java.time.LocalDateTime
import java.util.*

class SøknadArbeidsgiver(
    meldingsreferanseId: UUID,
    fnr: String,
    aktørId: String,
    orgnummer: String,
    perioder: List<Søknadsperiode>,
    andreInntektskilder: List<Inntektskilde>,
    sendtTilArbeidsgiver: LocalDateTime,
    permittert: Boolean,
    merknaderFraSykmelding: List<Merknad>,
    sykmeldingSkrevet: LocalDateTime
) : SendtSøknad(meldingsreferanseId, fnr, aktørId, orgnummer, perioder, andreInntektskilder, sendtTilArbeidsgiver, permittert, merknaderFraSykmelding, sykmeldingSkrevet) {

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    override fun melding(klassName: String) = "SøknadArbeidsgiver"
}
