package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.OverstyrArbeidsforhold.ArbeidsforholdOverstyrt.Companion.overstyringFor
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Arbeidsgiver.Companion.finn
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Opptjening
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.Sykepengegrunnlag
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
                    ?: logiskFeil("Kan ikke overstyre arbeidsforhold for en arbeidsgiver vi ikke kjenner til")
            }
        if (relevanteArbeidsgivere.any { it.harSykdomFor(skjæringstidspunkt) }) {
            logiskFeil("Kan ikke overstyre arbeidsforhold for en arbeidsgiver som har sykdom")
        }
    }

    internal fun lagre(arbeidsgiver: Arbeidsgiver) {
        val overstyring = overstyrteArbeidsforhold.overstyringFor(arbeidsgiver.organisasjonsnummer()) ?: return
        arbeidsgiver.lagreOverstyrArbeidsforhold(skjæringstidspunkt, overstyring)
    }

    internal fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) {
        hendelseIder.add(Dokumentsporing.overstyrArbeidsforhold(meldingsreferanseId()))
    }

    internal fun overstyr(sykepengegrunnlag: Sykepengegrunnlag, subsumsjonObserver: SubsumsjonObserver): Sykepengegrunnlag {
        return overstyrteArbeidsforhold.fold(sykepengegrunnlag, ) { acc, overstyring ->
            overstyring.overstyr(acc, subsumsjonObserver)
        }
    }

    internal fun overstyr(opptjening: Opptjening, subsumsjonObserver: SubsumsjonObserver): Opptjening {
        return overstyrteArbeidsforhold.fold(opptjening, ) { acc, overstyring ->
            overstyring.overstyr(acc, subsumsjonObserver)
        }
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

        internal fun lagre(skjæringstidspunkt: LocalDate, arbeidsforholdhistorikk: Arbeidsforholdhistorikk) {
            if (deaktivert) {
                arbeidsforholdhistorikk.deaktiverArbeidsforhold(skjæringstidspunkt)
            } else {
                arbeidsforholdhistorikk.aktiverArbeidsforhold(skjæringstidspunkt)
            }
        }

        internal fun overstyr(sykepengegrunnlag: Sykepengegrunnlag, subsumsjonObserver: SubsumsjonObserver) = when (deaktivert){
            true -> sykepengegrunnlag.deaktiver(orgnummer, forklaring, subsumsjonObserver)
            else -> sykepengegrunnlag.aktiver(orgnummer, forklaring, subsumsjonObserver)
        }

        internal fun overstyr(opptjening: Opptjening, subsumsjonObserver: SubsumsjonObserver) = when (deaktivert){
            true -> opptjening.deaktiver(orgnummer, subsumsjonObserver)
            else -> opptjening.aktiver(orgnummer, subsumsjonObserver)
        }
    }
}
