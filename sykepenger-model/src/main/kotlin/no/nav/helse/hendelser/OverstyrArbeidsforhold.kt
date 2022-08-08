package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.OverstyrArbeidsforhold.ArbeidsforholdOverstyrt.Companion.overstyringFor
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Arbeidsgiver.Companion.finn
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.etterlevelse.SubsumsjonObserver

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
                arbeidsgivere.finn(overstyring.orgnummer)
                    ?: severe("Kan ikke overstyre arbeidsforhold for en arbeidsgiver vi ikke kjenner til")
            }
        if (relevanteArbeidsgivere.any { it.harSykdomFor(skjæringstidspunkt) }) {
            severe("Kan ikke overstyre arbeidsforhold for en arbeidsgiver som har sykdom")
        }
    }

    internal fun lagre(arbeidsgiver: Arbeidsgiver, subsumsjonObserver: SubsumsjonObserver) {
        val overstyring = overstyrteArbeidsforhold.overstyringFor(arbeidsgiver.organisasjonsnummer())
        if (overstyring != null) {
            arbeidsgiver.lagreOverstyrArbeidsforhold(skjæringstidspunkt, overstyring, subsumsjonObserver)
        }
    }

    internal fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) {
        hendelseIder.add(Dokumentsporing.overstyrArbeidsforhold(meldingsreferanseId()))
    }

    class ArbeidsforholdOverstyrt(
        internal val orgnummer: String,
        private val deaktivert: Boolean,
        private val forklaring: String
    ) {

        companion object {
            internal fun Iterable<ArbeidsforholdOverstyrt>.overstyringFor(orgnummer: String) =
                firstOrNull { it.orgnummer == orgnummer }
        }

        internal fun lagre(
            skjæringstidspunkt: LocalDate,
            arbeidsforholdhistorikk: Arbeidsforholdhistorikk,
            subsumsjonObserver: SubsumsjonObserver,
            inntekterSisteTreMåneder: Inntektshistorikk.Inntektsopplysning?
        ) {
            requireNotNull(inntekterSisteTreMåneder) {"En ghost skal ha inntekt fra A-ordningen dersom arbeidsforholdet er eldre enn 3 måneder"}
            if (deaktivert) {
                arbeidsforholdhistorikk.deaktiverArbeidsforhold(skjæringstidspunkt)
                inntekterSisteTreMåneder.subsumerArbeidsforhold(subsumsjonObserver, skjæringstidspunkt, orgnummer, forklaring, true)
            } else {
                arbeidsforholdhistorikk.aktiverArbeidsforhold(skjæringstidspunkt)
                inntekterSisteTreMåneder.subsumerArbeidsforhold(subsumsjonObserver, skjæringstidspunkt, orgnummer, forklaring, false)
            }
        }
    }
}
