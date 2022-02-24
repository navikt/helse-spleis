package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.opptjeningsperiode
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.toEtterlevelseMap
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.arbeidsforholdForJurist
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.opptjeningsperiode
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import java.time.LocalDate

internal class Opptjening (
    private val arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlag>,
    private val opptjeningsperiode: Periode
) {
    internal fun opptjeningsdager() = opptjeningsperiode.dagerMellom().toInt()
    internal fun erOppfylt(): Boolean = opptjeningsperiode.dagerMellom() >= TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER

    internal fun valider(aktivitetslogg: IAktivitetslogg): Boolean {
        val erOppfylt = erOppfylt()
        if (!erOppfylt) aktivitetslogg.warn("Perioden er avslått på grunn av manglende opptjening")
        return erOppfylt
    }

    internal fun accept(visitor: VilkårsgrunnlagHistorikkVisitor) {
        visitor.preVisitOpptjening(this, arbeidsforhold, opptjeningsperiode)
        arbeidsforhold.forEach { it.accept(visitor) }
        visitor.postVisitOpptjening(this, arbeidsforhold, opptjeningsperiode)
    }

    internal fun opptjeningFom() = opptjeningsperiode.start

    internal class ArbeidsgiverOpptjeningsgrunnlag(private val orgnummer: String, private val arbeidsforhold: List<Arbeidsforholdhistorikk.Arbeidsforhold>) {
        companion object {
            internal fun List<ArbeidsgiverOpptjeningsgrunnlag>.opptjeningsperiode(skjæringstidspunkt: LocalDate) =
                flatMap { it.arbeidsforhold }.opptjeningsperiode(skjæringstidspunkt)

            internal fun List<ArbeidsgiverOpptjeningsgrunnlag>.arbeidsforholdForJurist() =
                flatMap { it.arbeidsforhold.toEtterlevelseMap(it.orgnummer) }
        }

        internal fun accept(visitor: VilkårsgrunnlagHistorikkVisitor) {
            visitor.preVisitArbeidsgiverOpptjeningsgrunnlag(orgnummer, arbeidsforhold)
            arbeidsforhold.forEach { it.accept(visitor) }
            visitor.postVisitArbeidsgiverOpptjeningsgrunnlag(orgnummer, arbeidsforhold)
        }
    }

    companion object {
        private const val TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER = 28

        fun opptjening(
            arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlag>,
            skjæringstidspunkt: LocalDate,
            subsumsjonObserver: SubsumsjonObserver
        ): Opptjening {
            val opptjeningsperiode = arbeidsforhold.opptjeningsperiode(skjæringstidspunkt)
            val opptjening = Opptjening(arbeidsforhold, opptjeningsperiode)
            val arbeidsforholdForJurist = arbeidsforhold.arbeidsforholdForJurist()

            subsumsjonObserver.`§ 8-2 ledd 1`(
                oppfylt = opptjening.erOppfylt(),
                skjæringstidspunkt = skjæringstidspunkt,
                tilstrekkeligAntallOpptjeningsdager = TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER,
                arbeidsforhold = arbeidsforholdForJurist,
                antallOpptjeningsdager = opptjening.opptjeningsperiode.dagerMellom().toInt()
            )
            return opptjening
        }
    }
}
