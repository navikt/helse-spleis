package no.nav.helse.dsl

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.OverstyrArbeidsforhold

internal class PersonHendelsefabrikk(
    private val aktørId: String,
    private val fødselsnummer: Fødselsnummer
) {
    internal fun lagOverstyrArbeidsforhold(skjæringstidspunkt: LocalDate, vararg overstyrteArbeidsforhold: OverstyrArbeidsforhold.ArbeidsforholdOverstyrt) =
        OverstyrArbeidsforhold(
            meldingsreferanseId = UUID.randomUUID(),
            fødselsnummer = fødselsnummer.toString(),
            aktørId = aktørId,
            skjæringstidspunkt = skjæringstidspunkt,
            overstyrteArbeidsforhold = overstyrteArbeidsforhold.toList()
        )
}