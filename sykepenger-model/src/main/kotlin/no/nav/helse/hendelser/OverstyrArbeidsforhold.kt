package no.nav.helse.hendelser

import no.nav.helse.Organisasjonsnummer
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.PersonHendelse
import java.time.LocalDate
import java.util.*

class OverstyrArbeidsforhold(
    meldingsreferanseId: UUID,
    private val fødselsnummer: String,
    private val aktørId: String,
    private val skjæringstidspunkt: LocalDate,
    private val overstyrteArbeidsforhold: List<ArbeidsforholdOverstyrt>
) : PersonHendelse(meldingsreferanseId, Aktivitetslogg()) {
    override fun fødselsnummer() = fødselsnummer
    override fun aktørId() = aktørId

    class ArbeidsforholdOverstyrt(private val orgnummer: Organisasjonsnummer, private val erAktivt: Boolean)
}
