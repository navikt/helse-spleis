package no.nav.helse.hendelser

import no.nav.helse.hendelser.OverstyrArbeidsforhold.ArbeidsforholdOverstyrt.Companion.overstyringFor
import no.nav.helse.person.*
import no.nav.helse.person.Arbeidsgiver.Companion.finn
import no.nav.helse.person.Arbeidsgiver.Companion.harPeriodeSomBlokkererOverstyrArbeidsforhold
import java.time.LocalDate
import java.util.*

class OverstyrArbeidsforhold(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    private val skjæringstidspunkt: LocalDate,
    private val overstyrteArbeidsforhold: List<ArbeidsforholdOverstyrt>
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, Aktivitetslogg()) {

    internal fun erRelevant(skjæringstidspunkt: LocalDate) = this.skjæringstidspunkt == skjæringstidspunkt

    internal fun valider(arbeidsgivere: List<Arbeidsgiver>) {
        val relevanteArbeidsgivere = overstyrteArbeidsforhold
            .map { overstyring ->
                arbeidsgivere.finn(overstyring.orgnummer) ?: severe("Kan ikke overstyre arbeidsforhold for en arbeidsgiver vi ikke kjenner til")
            }
        if (relevanteArbeidsgivere.any { it.harSykdomFor(skjæringstidspunkt) }) {
            severe("Kan ikke overstyre arbeidsforhold for en arbeidsgiver som har sykdom")
        }
        if (arbeidsgivere.harPeriodeSomBlokkererOverstyrArbeidsforhold(skjæringstidspunkt)) {
            severe("Kan ikke overstyre arbeidsforhold for en pågående behandling der én eller flere perioder er behandlet ferdig")
        }
    }

    internal fun lagre(arbeidsgiver: Arbeidsgiver) {
        val overstyring = overstyrteArbeidsforhold.overstyringFor(arbeidsgiver.organisasjonsnummer())
        if (overstyring != null) {
            arbeidsgiver.lagreOverstyrArbeidsforhold(skjæringstidspunkt, overstyring)
        }
    }

    internal fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) {
        hendelseIder.add(Dokumentsporing.overstyrArbeidsforhold(meldingsreferanseId()))
    }

    class ArbeidsforholdOverstyrt(internal val orgnummer: String, private val deaktivert: Boolean) {

        companion object {
            internal fun Iterable<ArbeidsforholdOverstyrt>.overstyringFor(orgnummer: String) = firstOrNull { it.orgnummer == orgnummer }
        }

        internal fun lagre(skjæringstidspunkt: LocalDate, arbeidsforholdhistorikk: Arbeidsforholdhistorikk) {
            if (deaktivert) {
                arbeidsforholdhistorikk.deaktiverArbeidsforhold(skjæringstidspunkt)
            } else {
                arbeidsforholdhistorikk.aktiverArbeidsforhold(skjæringstidspunkt)
            }
        }
    }
}
