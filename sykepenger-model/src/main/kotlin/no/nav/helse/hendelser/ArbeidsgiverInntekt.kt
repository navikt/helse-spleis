package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt.Companion.harInntektFor
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt.Companion.nylig
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt.Companion.utenOffentligeYtelser
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.YTELSE_FRA_OFFENTLIGE
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Person
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Skatt
import no.nav.helse.person.inntekt.SkattComposite
import no.nav.helse.økonomi.Inntekt

typealias InntektCreator = (
    yearMonth: YearMonth,
    inntekt: Inntekt,
    type: ArbeidsgiverInntekt.MånedligInntekt.Inntekttype,
    fordel: String,
    beskrivelse: String
) -> ArbeidsgiverInntekt.MånedligInntekt

class ArbeidsgiverInntekt(
    private val arbeidsgiver: String,
    private val inntekter: List<MånedligInntekt>
) {
    internal fun ansattVedSkjæringstidspunkt(opptjening: Opptjening) =
        opptjening.ansattVedSkjæringstidspunkt(arbeidsgiver)

    internal fun tilSammenligningsgrunnlag(skjæringstidspunkt: LocalDate, meldingsreferanseId: UUID) =
        ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(
            orgnummer = arbeidsgiver,
            inntektsopplysninger = inntekter.map { it.somInntekt(skjæringstidspunkt, meldingsreferanseId) }.filterIsInstance<Skatt.RapportertInntekt>()
        )

    private fun harInntekter() = inntekter.isNotEmpty()

    internal companion object {
        internal fun List<ArbeidsgiverInntekt>.lagreInntekter(orgnummer: String, inntektshistorikk: Inntektshistorikk, skjæringstidspunkt: LocalDate, meldingsreferanseId: UUID) {
            inntektshistorikk.append {
                this@lagreInntekter.tilSkatt(skjæringstidspunkt, meldingsreferanseId).forEach { (orgnr, inntekter) ->
                    if (orgnr == orgnummer) inntekter.forEach { add(it) }
                }
            }
        }
        internal fun List<ArbeidsgiverInntekt>.lagreInntekter(hendelse: IAktivitetslogg, person: Person, skjæringstidspunkt: LocalDate, meldingsreferanseId: UUID) {
            this
                .tilSkatt(skjæringstidspunkt, meldingsreferanseId)
                .forEach { (arbeidsgiver, inntekter) ->
                    person.lagreInntekter(hendelse, arbeidsgiver, inntekter)
                }
        }

        private fun List<ArbeidsgiverInntekt>.tilSkatt(skjæringstidspunkt: LocalDate, meldingsreferanseId: UUID) = this
            .groupBy({ it.arbeidsgiver }) { opplysning ->
                SkattComposite(UUID.randomUUID(), opplysning.inntekter.map {
                    it.somInntekt(skjæringstidspunkt, meldingsreferanseId)
                })
            }

        internal fun List<ArbeidsgiverInntekt>.kilder(antallMåneder: Int) = this
            .map { ArbeidsgiverInntekt(it.arbeidsgiver, it.inntekter.nylig(månedFørSlutt(this, antallMåneder))) }
            .count { it.harInntekter() }

        internal fun List<ArbeidsgiverInntekt>.utenOffentligeYtelser() =
            map { ArbeidsgiverInntekt(it.arbeidsgiver, it.inntekter.utenOffentligeYtelser()) }

        internal fun List<ArbeidsgiverInntekt>.harInntektFor(orgnummer: String, måned: YearMonth) =
            this.any { it.arbeidsgiver == orgnummer && it.inntekter.harInntektFor(måned) }

        private fun månedFørSlutt(inntekter: List<ArbeidsgiverInntekt>, antallMåneder: Int) =
            MånedligInntekt.månedFørSlutt(inntekter.flatMap { it.inntekter }, antallMåneder)

        internal fun List<ArbeidsgiverInntekt>.antallMåneder() =
            MånedligInntekt.antallMåneder(flatMap { it.inntekter })
    }

    sealed class MånedligInntekt(
        protected val yearMonth: YearMonth,
        protected val inntekt: Inntekt,
        protected val type: Inntekttype,
        protected val fordel: String,
        protected val beskrivelse: String
    ) {
        internal abstract fun somInntekt(skjæringstidspunkt: LocalDate, meldingsreferanseId: UUID): Skatt

        class RapportertInntekt(
            yearMonth: YearMonth,
            inntekt: Inntekt,
            type: Inntekttype,
            fordel: String,
            beskrivelse: String
        ) : MånedligInntekt(yearMonth, inntekt, type, fordel, beskrivelse) {
            override fun somInntekt(skjæringstidspunkt: LocalDate, meldingsreferanseId: UUID): Skatt {
                return Skatt.RapportertInntekt(
                    skjæringstidspunkt,
                    meldingsreferanseId,
                    inntekt,
                    yearMonth,
                    enumValueOf(type.name),
                    fordel,
                    beskrivelse
                )
            }
        }

        class Sykepengegrunnlag(
            yearMonth: YearMonth,
            inntekt: Inntekt,
            type: Inntekttype,
            fordel: String,
            beskrivelse: String
        ) : MånedligInntekt(yearMonth, inntekt, type, fordel, beskrivelse) {
            override fun somInntekt(skjæringstidspunkt: LocalDate, meldingsreferanseId: UUID): Skatt {
                return Skatt.Sykepengegrunnlag(
                    skjæringstidspunkt,
                    meldingsreferanseId,
                    inntekt,
                    yearMonth,
                    enumValueOf(type.name),
                    fordel,
                    beskrivelse
                )
            }

        }

        companion object {
            internal fun List<MånedligInntekt>.nylig(månedFørSlutt: YearMonth?): List<MånedligInntekt> {
                return månedFørSlutt
                    ?.let { førsteMåned -> this@nylig.filter { it.yearMonth >= førsteMåned } }
                    ?: emptyList()
            }

            internal fun List<MånedligInntekt>.harInntektFor(måned: YearMonth) = this.any { it.yearMonth == måned }

            internal fun List<MånedligInntekt>.utenOffentligeYtelser() = filter { it.type != YTELSE_FRA_OFFENTLIGE }

            internal fun månedFørSlutt(inntekter: List<MånedligInntekt>, antallMåneder: Int) =
                inntekter.maxOfOrNull { it.yearMonth }?.minusMonths(antallMåneder.toLong() - 1)

            internal fun antallMåneder(inntekter: List<MånedligInntekt>): Long {
                if (inntekter.isEmpty()) return 0
                return ChronoUnit.MONTHS.between(inntekter.minMonth(), inntekter.maxMonth()) + 1
            }

            private fun List<MånedligInntekt>.minMonth() = minOfOrNull { it.yearMonth }
            private fun List<MånedligInntekt>.maxMonth() = maxOfOrNull { it.yearMonth }
        }

        enum class Inntekttype {
            LØNNSINNTEKT,
            NÆRINGSINNTEKT,
            PENSJON_ELLER_TRYGD,
            YTELSE_FRA_OFFENTLIGE
        }
    }
}

