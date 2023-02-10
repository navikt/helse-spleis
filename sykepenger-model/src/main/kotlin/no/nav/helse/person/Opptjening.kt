package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.sammenhengende
import no.nav.helse.hendelser.til
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold.Companion.ansattVedSkjæringstidspunkt
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold.Companion.opptjeningsperiode
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold.Companion.toEtterlevelseMap
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.aktiver
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.arbeidsforholdForJurist
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.deaktiver
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.opptjeningsperiode
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.startdatoFor
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OV_1
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.inntekt.AnsattPeriode

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
            antallOpptjeningsdager = opptjeningsperiode.dagerMellom()
        )
    }

    internal fun ansattVedSkjæringstidspunkt(orgnummer: String) =
        arbeidsforhold.any { it.ansattVedSkjæringstidspunkt(orgnummer, skjæringstidspunkt) }

    internal fun opptjeningsdager() = opptjeningsperiode.dagerMellom()
    internal fun erOppfylt(): Boolean = opptjeningsperiode.dagerMellom() >= TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER

    internal fun valider(aktivitetslogg: IAktivitetslogg): Boolean {
        val erOppfylt = erOppfylt()
        if (!erOppfylt) aktivitetslogg.varsel(RV_OV_1)
        return erOppfylt
    }

    internal fun accept(visitor: OpptjeningVisitor) {
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

    internal class ArbeidsgiverOpptjeningsgrunnlag(private val orgnummer: String, private val ansattPerioder: List<Arbeidsforhold>) {
        internal fun accept(visitor: OpptjeningVisitor) {
            visitor.preVisitArbeidsgiverOpptjeningsgrunnlag(orgnummer, ansattPerioder)
            ansattPerioder.forEach { it.accept(visitor) }
            visitor.postVisitArbeidsgiverOpptjeningsgrunnlag(orgnummer, ansattPerioder)
        }

        internal fun ansattVedSkjæringstidspunkt(orgnummer: String, skjæringstidspunkt: LocalDate) =
            this.orgnummer == orgnummer && ansattPerioder.ansattVedSkjæringstidspunkt(skjæringstidspunkt)

        private fun aktiver(orgnummer: String): ArbeidsgiverOpptjeningsgrunnlag {
            if (orgnummer != this.orgnummer) return this
            return ArbeidsgiverOpptjeningsgrunnlag(orgnummer, ansattPerioder.map { it.aktiver() })
        }

        private fun deaktiver(orgnummer: String): ArbeidsgiverOpptjeningsgrunnlag {
            if (orgnummer != this.orgnummer) return this
            return ArbeidsgiverOpptjeningsgrunnlag(orgnummer, ansattPerioder.map { it.deaktiver() })
        }

        internal class Arbeidsforhold(
            private val ansattFom: LocalDate,
            private val ansattTom: LocalDate?,
            private val deaktivert: Boolean
        ) {
            internal fun gjelder(skjæringstidspunkt: LocalDate) = ansattFom <= skjæringstidspunkt && (ansattTom == null || ansattTom >= skjæringstidspunkt)

            override fun equals(other: Any?) = other is Arbeidsforhold
                    && ansattFom == other.ansattFom
                    && ansattTom == other.ansattTom
                    && deaktivert == other.deaktivert

            override fun hashCode(): Int {
                var result = ansattFom.hashCode()
                result = 31 * result + (ansattTom?.hashCode() ?: 0)
                result = 31 * result + deaktivert.hashCode()
                return result
            }

            internal fun accept(visitor: OpptjeningVisitor) {
                visitor.visitArbeidsforhold(ansattFom = ansattFom, ansattTom = ansattTom, deaktivert = deaktivert)
            }

            internal fun deaktiver() = Arbeidsforhold(ansattFom = ansattFom, ansattTom = ansattTom, deaktivert = true)

            internal fun aktiver() = Arbeidsforhold(ansattFom = ansattFom, ansattTom = ansattTom, deaktivert = false)

            private fun somAnsattPeriode() = AnsattPeriode(ansattFom = ansattFom, ansattTom = ansattTom)

            companion object {

                internal fun Collection<Arbeidsforhold>.opptjeningsperiode(skjæringstidspunkt: LocalDate) = this
                    .filter { !it.deaktivert }
                    .map { it.ansattFom til (it.ansattTom ?: skjæringstidspunkt) }
                    .sammenhengende(skjæringstidspunkt)

                internal fun Collection<Arbeidsforhold>.somAnsattPerioder() = this.map { it.somAnsattPeriode() }

                internal fun Collection<Arbeidsforhold>.ansattVedSkjæringstidspunkt(skjæringstidspunkt: LocalDate) = any { it.gjelder(skjæringstidspunkt) }

                internal fun Iterable<Arbeidsforhold>.toEtterlevelseMap(orgnummer: String) = map {
                    mapOf(
                        "orgnummer" to orgnummer,
                        "fom" to it.ansattFom,
                        "tom" to it.ansattTom
                    )
                }
            }

        }

        companion object {
            internal fun Map<String, List<Arbeidsforhold>>.opptjening(skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver): Opptjening {
                val arbeidsforhold = this
                    .filterValues { it.isNotEmpty() }
                    .map { (orgnr, arbeidsforhold) -> Opptjening.ArbeidsgiverOpptjeningsgrunnlag(orgnr, arbeidsforhold) }
                return Opptjening(arbeidsforhold, skjæringstidspunkt, subsumsjonObserver)
            }

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
    }

    companion object {
        private fun Periode.dagerMellom() = count() - 1 // 😭
        private const val TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER = 28

        internal fun gjenopprett(skjæringstidspunkt: LocalDate, arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlag>, opptjeningsperiode: Periode) =
            Opptjening(skjæringstidspunkt, arbeidsforhold, opptjeningsperiode)
    }
}
