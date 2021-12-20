package no.nav.helse.hendelser

import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt.Companion.harInntektFor
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt.Companion.nylig
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.*

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
    internal fun lagreInntekter(inntektshistorikk: Inntektshistorikk, skjæringstidspunkt: LocalDate, meldingsreferanseId: UUID) {
        MånedligInntekt.lagreInntekter(inntekter, inntektshistorikk, skjæringstidspunkt, meldingsreferanseId)
    }

    private fun harInntekter() = inntekter.isNotEmpty()

    companion object {
        fun lagreSykepengegrunnlag(inntekter: List<ArbeidsgiverInntekt>, person: Person, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) {
            inntekter.forEach { person.lagreGrunnlagForSykepengegrunnlag(it.arbeidsgiver, it, skjæringstidspunkt, hendelse) }
        }

        fun lagreSammenligningsgrunnlag(inntekter: List<ArbeidsgiverInntekt>, person: Person, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) {
            inntekter.forEach { person.lagreGrunnlagForSammenligningsgrunnlag(it.arbeidsgiver, it, skjæringstidspunkt, hendelse) }
        }

        internal fun List<ArbeidsgiverInntekt>.kilder(antallMåneder: Int) = this
            .map { ArbeidsgiverInntekt(it.arbeidsgiver, it.inntekter.nylig(månedFørSlutt(this, antallMåneder))) }
            .count { it.harInntekter() }

        internal fun List<ArbeidsgiverInntekt>.harInntektFor(orgnummer: String) = this.any { it.arbeidsgiver == orgnummer }

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

        internal abstract fun lagreInntekter(
            inntektshistorikk: Inntektshistorikk.AppendMode,
            skjæringstidspunkt: LocalDate,
            meldingsreferanseId: UUID
        )

        class Sammenligningsgrunnlag(
            yearMonth: YearMonth,
            inntekt: Inntekt,
            type: Inntekttype,
            fordel: String,
            beskrivelse: String
        ) : MånedligInntekt(yearMonth, inntekt, type, fordel, beskrivelse) {

            override fun lagreInntekter(
                inntektshistorikk: Inntektshistorikk.AppendMode,
                skjæringstidspunkt: LocalDate,
                meldingsreferanseId: UUID,
            ) {
                inntektshistorikk.addSkattSammenligningsgrunnlag(
                    dato = skjæringstidspunkt,
                    hendelseId = meldingsreferanseId,
                    beløp = inntekt,
                    måned = yearMonth,
                    type = enumValueOf(type.name),
                    fordel = fordel,
                    beskrivelse = beskrivelse
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

            override fun lagreInntekter(
                inntektshistorikk: Inntektshistorikk.AppendMode,
                skjæringstidspunkt: LocalDate,
                meldingsreferanseId: UUID,
            ) {
                inntektshistorikk.addSkattSykepengegrunnlag(
                    dato = skjæringstidspunkt,
                    hendelseId = meldingsreferanseId,
                    beløp = inntekt,
                    måned = yearMonth,
                    type = enumValueOf(type.name),
                    fordel = fordel,
                    beskrivelse = beskrivelse
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

            internal fun månedFørSlutt(inntekter: List<MånedligInntekt>, antallMåneder: Int) =
                inntekter.maxOfOrNull { it.yearMonth }?.minusMonths(antallMåneder.toLong() - 1)

            private fun summer(inntekter: List<MånedligInntekt>) = inntekter.map { it.inntekt }.summer()

            internal fun antallMåneder(inntekter: List<MånedligInntekt>): Long {
                if (inntekter.isEmpty()) return 0
                return ChronoUnit.MONTHS.between(inntekter.minMonth(), inntekter.maxMonth()) + 1
            }

            private fun List<MånedligInntekt>.minMonth() = minOfOrNull { it.yearMonth }
            private fun List<MånedligInntekt>.maxMonth() = maxOfOrNull { it.yearMonth }


            internal fun lagreInntekter(
                inntekter: List<MånedligInntekt>,
                inntektshistorikk: Inntektshistorikk,
                skjæringstidspunkt: LocalDate,
                meldingsreferanseId: UUID
            ) {
                inntektshistorikk.append {
                    inntekter.forEach {
                        it.lagreInntekter(this, skjæringstidspunkt, meldingsreferanseId)
                    }
                }
            }
        }

        enum class Inntekttype {
            LØNNSINNTEKT,
            NÆRINGSINNTEKT,
            PENSJON_ELLER_TRYGD,
            YTELSE_FRA_OFFENTLIGE
        }
    }
}

