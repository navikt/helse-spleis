package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.dto.ArbeidsforholdDto
import no.nav.helse.dto.ArbeidsgiverOpptjeningsgrunnlagDto
import no.nav.helse.dto.deserialisering.OpptjeningInnDto
import no.nav.helse.dto.serialisering.OpptjeningUtDto
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold.Companion.ansattVedSkjæringstidspunkt
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold.Companion.opptjeningsperiode
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold.Companion.toEtterlevelseMap
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.aktiver
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.arbeidsforholdForJurist
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.deaktiver
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.inngårIOpptjening
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.opptjeningsperiode
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.startdatoFor
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OV_1

internal class Opptjening private constructor(
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlag>,
    private val opptjeningsperiode: Periode
) {
    private val opptjeningsdager by lazy { opptjeningsperiode.count() }

    internal fun ansattVedSkjæringstidspunkt(orgnummer: String) =
        arbeidsforhold.any { it.ansattVedSkjæringstidspunkt(orgnummer, skjæringstidspunkt) }

    internal fun opptjeningsdager() = opptjeningsdager
    internal fun erOppfylt(): Boolean = opptjeningsdager >= TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER

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
        return Opptjening.nyOpptjening(arbeidsforhold.deaktiver(orgnummer), skjæringstidspunkt, subsumsjonObserver)
    }

    internal fun aktiver(orgnummer: String, subsumsjonObserver: SubsumsjonObserver): Opptjening {
        return Opptjening.nyOpptjening(arbeidsforhold.aktiver(orgnummer), skjæringstidspunkt, subsumsjonObserver)
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

        private fun inngårIOpptjening(opptjeningsperiode: Periode): ArbeidsgiverOpptjeningsgrunnlag? {
            val perioder = ansattPerioder.filter { it.inngårIOpptjening(opptjeningsperiode) }.takeUnless { it.isEmpty() } ?: return null
            return ArbeidsgiverOpptjeningsgrunnlag(this.orgnummer, perioder)
        }

        internal class Arbeidsforhold(
            private val ansattFom: LocalDate,
            private val ansattTom: LocalDate?,
            private val deaktivert: Boolean
        ) {
            private val ansettelseperiode = ansattFom til (ansattTom ?: LocalDate.MAX)

            internal fun gjelder(skjæringstidspunkt: LocalDate) = ansattFom <= skjæringstidspunkt && (ansattTom == null || ansattTom >= skjæringstidspunkt)

            private fun periode(skjæringstidspunkt: LocalDate): Periode? {
                if (deaktivert) return null
                val opptjeningsperiode = LocalDate.MIN til skjæringstidspunkt.forrigeDag
                if (ansettelseperiode.starterEtter(opptjeningsperiode)) return null
                return ansettelseperiode.subset(opptjeningsperiode)
            }

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

            internal fun inngårIOpptjening(opptjeningsperiode: Periode): Boolean {
                return deaktivert || this.ansettelseperiode.overlapperMed(opptjeningsperiode)
            }

            companion object {

                internal fun Collection<Arbeidsforhold>.opptjeningsperiode(skjæringstidspunkt: LocalDate): Periode {
                    val grunnlag = this
                        .mapNotNull { it.periode(skjæringstidspunkt) }
                        .sortedByDescending { it.endInclusive }
                    val dagenFør = skjæringstidspunkt.forrigeDag.somPeriode()
                    if (grunnlag.firstOrNull()?.erRettFør(skjæringstidspunkt) != true) return dagenFør
                    return grunnlag.fold(dagenFør) { resultat, periode ->
                        if (!resultat.overlapperMed(periode) && !periode.erRettFør(resultat)) resultat
                        else resultat + periode
                    }
                }

                internal fun Collection<Arbeidsforhold>.ansattVedSkjæringstidspunkt(skjæringstidspunkt: LocalDate) = any { it.gjelder(skjæringstidspunkt) }

                internal fun Iterable<Arbeidsforhold>.toEtterlevelseMap(orgnummer: String) = map {
                    mapOf(
                        "orgnummer" to orgnummer,
                        "fom" to it.ansattFom,
                        "tom" to it.ansattTom
                    )
                }

                internal fun gjenopprett(dto: ArbeidsforholdDto): Arbeidsforhold {
                    return Arbeidsforhold(
                        ansattFom = dto.ansattFom,
                        ansattTom = dto.ansattTom,
                        deaktivert = dto.deaktivert
                    )
                }
            }

            internal fun dto() = ArbeidsforholdDto(
                ansattFom = this.ansattFom,
                ansattTom = this.ansattTom,
                deaktivert = this.deaktivert
            )
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
            internal fun List<ArbeidsgiverOpptjeningsgrunnlag>.inngårIOpptjening(opptjeningsperiode: Periode) =
                mapNotNull { it.inngårIOpptjening(opptjeningsperiode) }
            internal fun List<ArbeidsgiverOpptjeningsgrunnlag>.arbeidsforholdForJurist() =
                flatMap { it.ansattPerioder.toEtterlevelseMap(it.orgnummer) }

            internal fun gjenopprett(dto: ArbeidsgiverOpptjeningsgrunnlagDto): ArbeidsgiverOpptjeningsgrunnlag {
                return ArbeidsgiverOpptjeningsgrunnlag(
                    orgnummer = dto.orgnummer,
                    ansattPerioder = dto.ansattPerioder.map { Arbeidsforhold.gjenopprett(it) }
                )
            }
        }

        internal fun dto() = ArbeidsgiverOpptjeningsgrunnlagDto(
            orgnummer = this.orgnummer,
            ansattPerioder = this.ansattPerioder.map { it.dto() }
        )
    }

    companion object {
        private const val TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER = 28

        internal fun gjenopprett(skjæringstidspunkt: LocalDate, arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlag>, opptjeningsperiode: Periode) =
            Opptjening(skjæringstidspunkt, arbeidsforhold, opptjeningsperiode)

        internal fun gjenopprett(skjæringstidspunkt: LocalDate, dto: OpptjeningInnDto): Opptjening {
            return Opptjening(
                skjæringstidspunkt = skjæringstidspunkt,
                dto.arbeidsforhold.map { ArbeidsgiverOpptjeningsgrunnlag.gjenopprett(it) },
                opptjeningsperiode = Periode.gjenopprett(dto.opptjeningsperiode)
            )
        }


        internal fun nyOpptjening(grunnlag: List<ArbeidsgiverOpptjeningsgrunnlag>, skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver): Opptjening {
            val opptjeningsperiode = grunnlag.opptjeningsperiode(skjæringstidspunkt)
            val arbeidsforhold = grunnlag.inngårIOpptjening(opptjeningsperiode)

            val opptjening = Opptjening(skjæringstidspunkt, arbeidsforhold, opptjeningsperiode)
            val arbeidsforholdForJurist = arbeidsforhold.arbeidsforholdForJurist()
            subsumsjonObserver.`§ 8-2 ledd 1`(
                oppfylt = opptjening.erOppfylt(),
                skjæringstidspunkt = skjæringstidspunkt,
                tilstrekkeligAntallOpptjeningsdager = TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER,
                arbeidsforhold = arbeidsforholdForJurist,
                antallOpptjeningsdager = opptjening.opptjeningsdager
            )
            return opptjening
        }
    }

    internal fun dto() = OpptjeningUtDto(
        arbeidsforhold = this.arbeidsforhold.map { it.dto() },
        opptjeningsperiode = this.opptjeningsperiode.dto(),
        opptjeningsdager = opptjeningsdager,
        erOppfylt = erOppfylt()
    )
}
