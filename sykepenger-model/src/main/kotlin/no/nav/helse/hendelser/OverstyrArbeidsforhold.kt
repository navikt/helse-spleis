package no.nav.helse.hendelser

import no.nav.helse.Organisasjonsnummer
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.PersonHendelse
import java.time.LocalDate
import java.util.*

class OverstyrArbeidsforhold(
    meldingsreferanseId: UUID,
    private val fødselsnummer: String,
    private val aktørId: String,
    private val skjæringstidspunkt: LocalDate,
    internal val overstyrteArbeidsforhold: List<ArbeidsforholdOverstyrt> // TODO: Kan denne privatiseres?
) : PersonHendelse(meldingsreferanseId, Aktivitetslogg()) {
    override fun fødselsnummer() = fødselsnummer
    override fun aktørId() = aktørId

    internal fun erRelevant(skjæringstidspunkt: LocalDate) = this.skjæringstidspunkt == skjæringstidspunkt

    internal fun lagre(arbeidsgiver: Arbeidsgiver) {
        val overstyring = overstyrteArbeidsforhold.firstOrNull { it.orgnummer.toString() == arbeidsgiver.organisasjonsnummer() }
        if (overstyring != null) {
            arbeidsgiver.lagreOverstyrArbeidsforhold(skjæringstidspunkt, overstyring)
        }
    }

    class ArbeidsforholdOverstyrt(internal val orgnummer: Organisasjonsnummer, private val erAktivt: Boolean) {

        internal fun lagre(skjæringstidspunkt: LocalDate, arbeidsforholdhistorikk: Arbeidsforholdhistorikk) {
            if (erAktivt) {
                arbeidsforholdhistorikk.aktiverArbeidsforhold(skjæringstidspunkt)
            } else {
                arbeidsforholdhistorikk.deaktiverArbeidsforhold(skjæringstidspunkt)
            }
        }
    }
}
