package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.opptjeningsperiode
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.toEtterlevelseMap
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.aktiver
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.arbeidsforholdForJurist
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.deaktiver
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.opptjeningsperiode
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.startdatoFor
import no.nav.helse.person.Varselkode.RV_OV_1
import no.nav.helse.person.etterlevelse.SubsumsjonObserver

internal class Opptjening private constructor(
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlag>,
    private val opptjeningsperiode: Periode
) {
    internal constructor(arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlag>, skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver) : this(skjæringstidspunkt, arbeidsforhold, arbeidsforhold.opptjeningsperiode(skjæringstidspunkt)) {
        val arbeidsforholdForJurist = arbeidsforhold.arbeidsforholdForJurist()
        subsumsjonObserver.`§ 8-2 ledd 1`(
            oppfylt = erOppfylt(),
            skjæringstidspunkt = skjæringstidspunkt,
            tilstrekkeligAntallOpptjeningsdager = TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER,
            arbeidsforhold = arbeidsforholdForJurist,
            antallOpptjeningsdager = opptjeningsperiode.dagerMellom().toInt()
        )
    }

    internal fun opptjeningsdager() = opptjeningsperiode.dagerMellom().toInt()
    internal fun erOppfylt(): Boolean = opptjeningsperiode.dagerMellom() >= TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER

    internal fun valider(aktivitetslogg: IAktivitetslogg): Boolean {
        val erOppfylt = erOppfylt()
        if (!erOppfylt) aktivitetslogg.varsel(RV_OV_1)
        return erOppfylt
    }

    internal fun accept(visitor: VilkårsgrunnlagHistorikkVisitor) {
        visitor.preVisitOpptjening(this, arbeidsforhold, opptjeningsperiode)
        arbeidsforhold.forEach { it.accept(visitor) }
        visitor.postVisitOpptjening(this, arbeidsforhold, opptjeningsperiode)
    }

    internal fun opptjeningFom() = opptjeningsperiode.start
    internal fun startdatoFor(orgnummer: String) = arbeidsforhold.startdatoFor(orgnummer, skjæringstidspunkt)
    internal fun overstyrArbeidsforhold(hendelse: OverstyrArbeidsforhold, subsumsjonObserver: SubsumsjonObserver): Opptjening {
        return hendelse.overstyr(this, subsumsjonObserver)
    }

    internal fun deaktiver(orgnummer: String, subsumsjonObserver: SubsumsjonObserver): Opptjening {
        return Opptjening(arbeidsforhold.deaktiver(orgnummer), skjæringstidspunkt, subsumsjonObserver)
    }

    internal fun aktiver(orgnummer: String, subsumsjonObserver: SubsumsjonObserver): Opptjening {
        return Opptjening(arbeidsforhold.aktiver(orgnummer), skjæringstidspunkt, subsumsjonObserver)
    }

    internal class ArbeidsgiverOpptjeningsgrunnlag(private val orgnummer: String, private val ansattPerioder: List<Arbeidsforholdhistorikk.Arbeidsforhold>) {
        private fun aktiver(orgnummer: String): ArbeidsgiverOpptjeningsgrunnlag {
            if (orgnummer != this.orgnummer) return this
            return ArbeidsgiverOpptjeningsgrunnlag(orgnummer, ansattPerioder.map { it.aktiver() })
        }

        private fun deaktiver(orgnummer: String): ArbeidsgiverOpptjeningsgrunnlag {
            if (orgnummer != this.orgnummer) return this
            return ArbeidsgiverOpptjeningsgrunnlag(orgnummer, ansattPerioder.map { it.deaktiver() })
        }

        companion object {
            internal fun List<ArbeidsgiverOpptjeningsgrunnlag>.aktiver(orgnummer: String) = map { it.aktiver(orgnummer) }
            internal fun List<ArbeidsgiverOpptjeningsgrunnlag>.deaktiver(orgnummer: String) = map { it.deaktiver(orgnummer) }
            internal fun List<ArbeidsgiverOpptjeningsgrunnlag>.startdatoFor(orgnummer: String, skjæringstidspunkt: LocalDate) = this
                .singleOrNull { it.orgnummer == orgnummer }
                ?.ansattPerioder
                ?.opptjeningsperiode(skjæringstidspunkt)
                ?.start

            internal fun List<ArbeidsgiverOpptjeningsgrunnlag>.opptjeningsperiode(skjæringstidspunkt: LocalDate) =
                flatMap { it.ansattPerioder }.opptjeningsperiode(skjæringstidspunkt)

            internal fun List<ArbeidsgiverOpptjeningsgrunnlag>.arbeidsforholdForJurist() =
                flatMap { it.ansattPerioder.toEtterlevelseMap(it.orgnummer) }
        }

        internal fun accept(visitor: VilkårsgrunnlagHistorikkVisitor) {
            visitor.preVisitArbeidsgiverOpptjeningsgrunnlag(orgnummer, ansattPerioder)
            ansattPerioder.forEach { it.accept(visitor) }
            visitor.postVisitArbeidsgiverOpptjeningsgrunnlag(orgnummer, ansattPerioder)
        }
    }

    companion object {
        private const val TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER = 28

        internal fun gjenopprett(skjæringstidspunkt: LocalDate, arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlag>, opptjeningsperiode: Periode) =
            Opptjening(skjæringstidspunkt, arbeidsforhold, opptjeningsperiode)
    }
}
