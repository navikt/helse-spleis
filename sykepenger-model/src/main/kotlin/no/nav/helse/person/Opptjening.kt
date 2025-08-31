package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.dto.ArbeidsforholdDto
import no.nav.helse.dto.ArbeidsgiverOpptjeningsgrunnlagDto
import no.nav.helse.dto.deserialisering.OpptjeningInnDto
import no.nav.helse.dto.serialisering.OpptjeningUtDto
import no.nav.helse.etterlevelse.Subsumsjon
import no.nav.helse.etterlevelse.`§ 8-2 ledd 1`
import no.nav.helse.etterlevelse.`§ 8-2 ledd 1 - selvstendig næringsdrivende`
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.person.ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag
import no.nav.helse.person.ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold.Companion.ansattVedSkjæringstidspunkt
import no.nav.helse.person.ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold.Companion.opptjeningsperiode
import no.nav.helse.person.ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold.Companion.toEtterlevelseMap
import no.nav.helse.person.ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.aktiver
import no.nav.helse.person.ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.arbeidsforholdForJurist
import no.nav.helse.person.ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.deaktiver
import no.nav.helse.person.ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.inngårIOpptjening
import no.nav.helse.person.ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.opptjeningsperiode
import no.nav.helse.person.ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag.Companion.startdatoFor
import no.nav.helse.person.Opptjening.Companion.TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OV_1

internal sealed interface Opptjening {
    val subsumsjon: Subsumsjon
    fun view(): OpptjeningView
    fun overstyrArbeidsforhold(hendelse: OverstyrArbeidsforhold): Opptjening {
        return hendelse.overstyr(this)
    }

    fun deaktiver(orgnummer: String): Opptjening
    fun aktiver(orgnummer: String): Opptjening
    fun dto(): OpptjeningUtDto?

    companion object {
        const val TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER = 28
        fun gjenopprett(skjæringstidspunkt: LocalDate, opptjening: OpptjeningInnDto?): Opptjening = when (opptjening) {
            is OpptjeningInnDto -> ArbeidstakerOpptjening.gjenopprett(skjæringstidspunkt, opptjening)
            null -> SelvstendigNæringsdrivendeOpptjening(skjæringstidspunkt)
        }
    }
}

internal class SelvstendigNæringsdrivendeOpptjening(private val skjæringstidspunkt: LocalDate) : Opptjening {
    override val subsumsjon = `§ 8-2 ledd 1 - selvstendig næringsdrivende`(
        skjæringstidspunkt = skjæringstidspunkt,
    )

    override fun view() = SelvstendigOpptjeningView
    override fun deaktiver(orgnummer: String): Opptjening {
        TODO("Not yet implemented")
    }

    override fun aktiver(orgnummer: String): Opptjening {
        TODO("Not yet implemented")
    }

    override fun dto(): OpptjeningUtDto? = null
}

internal class ArbeidstakerOpptjening private constructor(
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlag>,
    private val opptjeningsperiode: Periode
) : Opptjening {
    private val opptjeningsdager by lazy { opptjeningsperiode.count() }
    override val subsumsjon = `§ 8-2 ledd 1`(
        oppfylt = harTilstrekkeligAntallOpptjeningsdager(),
        skjæringstidspunkt = skjæringstidspunkt,
        tilstrekkeligAntallOpptjeningsdager = TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER,
        arbeidsforhold = arbeidsforhold.arbeidsforholdForJurist(),
        antallOpptjeningsdager = opptjeningsdager
    )

    override fun view() = ArbeidstakerOpptjeningView(arbeidsforhold = arbeidsforhold, opptjeningsdager = opptjeningsdager, erOppfylt = erOppfylt())

    internal fun ansattVedSkjæringstidspunkt(orgnummer: String) =
        arbeidsforhold.any { it.ansattVedSkjæringstidspunkt(orgnummer, skjæringstidspunkt) }

    internal fun opptjeningsdager() = opptjeningsdager
    internal fun harTilstrekkeligAntallOpptjeningsdager(): Boolean = opptjeningsdager >= TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER

    internal fun erOppfylt(): Boolean = harTilstrekkeligAntallOpptjeningsdager()

    internal fun validerOpptjeningsdager(aktivitetslogg: IAktivitetslogg): Boolean {
        val harTilstrekkeligAntallOpptjeningsdager = harTilstrekkeligAntallOpptjeningsdager()
        if (!harTilstrekkeligAntallOpptjeningsdager) aktivitetslogg.varsel(RV_OV_1)
        return harTilstrekkeligAntallOpptjeningsdager()
    }

    internal fun opptjeningFom() = opptjeningsperiode.start
    internal fun startdatoFor(orgnummer: String) = arbeidsforhold.startdatoFor(orgnummer, skjæringstidspunkt)

    override fun deaktiver(orgnummer: String): Opptjening {
        return nyOpptjening(arbeidsforhold.deaktiver(orgnummer), skjæringstidspunkt)
    }

    override fun aktiver(orgnummer: String): Opptjening {
        return nyOpptjening(arbeidsforhold.aktiver(orgnummer), skjæringstidspunkt)
    }

    internal data class ArbeidsgiverOpptjeningsgrunnlag(val orgnummer: String, val ansattPerioder: List<Arbeidsforhold>) {
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

        internal data class Arbeidsforhold(
            val ansattFom: LocalDate,
            val ansattTom: LocalDate?,
            val deaktivert: Boolean
        ) {
            val ansettelseperiode = ansattFom til (ansattTom ?: LocalDate.MAX)

            internal fun gjelder(skjæringstidspunkt: LocalDate) = ansattFom <= skjæringstidspunkt && (ansattTom == null || ansattTom >= skjæringstidspunkt)

            private fun periode(skjæringstidspunkt: LocalDate): Periode? {
                if (deaktivert) return null
                val opptjeningsperiode = LocalDate.MIN til skjæringstidspunkt.forrigeDag
                if (ansettelseperiode.starterEtter(opptjeningsperiode)) return null
                return ansettelseperiode.subset(opptjeningsperiode)
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

        internal fun gjenopprett(skjæringstidspunkt: LocalDate, dto: OpptjeningInnDto): ArbeidstakerOpptjening {
            return ArbeidstakerOpptjening(
                skjæringstidspunkt = skjæringstidspunkt,
                dto.arbeidsforhold.map { ArbeidsgiverOpptjeningsgrunnlag.gjenopprett(it) },
                opptjeningsperiode = Periode.gjenopprett(dto.opptjeningsperiode)
            )
        }

        internal fun nyOpptjening(grunnlag: List<ArbeidsgiverOpptjeningsgrunnlag>, skjæringstidspunkt: LocalDate): ArbeidstakerOpptjening {
            val opptjeningsperiode = grunnlag.opptjeningsperiode(skjæringstidspunkt)
            val arbeidsforhold = grunnlag.inngårIOpptjening(opptjeningsperiode)
            val opptjening = ArbeidstakerOpptjening(skjæringstidspunkt, arbeidsforhold, opptjeningsperiode)
            return opptjening
        }
    }

    override fun dto(): OpptjeningUtDto = OpptjeningUtDto(
        arbeidsforhold = this.arbeidsforhold.map { it.dto() },
        opptjeningsperiode = this.opptjeningsperiode.dto(),
        opptjeningsdager = opptjeningsdager,
        erOppfylt = erOppfylt()
    )
}

internal sealed interface OpptjeningView
internal data class ArbeidstakerOpptjeningView(val arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlag>, val opptjeningsdager: Int, val erOppfylt: Boolean) : OpptjeningView
internal object SelvstendigOpptjeningView : OpptjeningView
