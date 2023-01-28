package no.nav.helse.hendelser.utbetaling

import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.hendelser.ArbeidstakerHendelse
import java.time.LocalDate
import java.util.*

class Grunnbeløpsregulering(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    private val gyldighetsdato: LocalDate,
    private val fagsystemId: String,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, aktivitetslogg) {

    internal fun erRelevant(fagsystemId: String) = this.fagsystemId == fagsystemId
}
