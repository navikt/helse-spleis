package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.Sykmeldingsperioder
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg

class AvbruttSøknad(
    private val periode: Periode,
    meldingsreferanseId: UUID,
    orgnummer: String,
    fødselsnummer: String,
    aktørId: String,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : ArbeidstakerHendelse(
    meldingsreferanseId = meldingsreferanseId,
    fødselsnummer = fødselsnummer,
    aktørId = aktørId,
    organisasjonsnummer = orgnummer,
    aktivitetslogg = aktivitetslogg
) {
    internal fun avbryt(sykmeldingsperioder: Sykmeldingsperioder) {
        sykmeldingsperioder.fjern(periode)
    }
}