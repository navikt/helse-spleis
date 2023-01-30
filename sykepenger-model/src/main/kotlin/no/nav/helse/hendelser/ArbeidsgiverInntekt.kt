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
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag
import no.nav.helse.person.inntekt.Skatteopplysning
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt

class ArbeidsgiverInntekt(
    private val arbeidsgiver: String,
    private val inntekter: List<MånedligInntekt>
) {
    internal fun ansattVedSkjæringstidspunkt(opptjening: Opptjening) =
        opptjening.ansattVedSkjæringstidspunkt(arbeidsgiver)

    internal fun tilSykepengegrunnlag(skjæringstidspunkt: LocalDate, meldingsreferanseId: UUID) =
        SkattSykepengegrunnlag(
            id = UUID.randomUUID(),
            dato = skjæringstidspunkt,
            inntektsopplysninger = inntekter.map { it.somInntekt(meldingsreferanseId) }
        )

    internal fun tilSammenligningsgrunnlag(meldingsreferanseId: UUID) =
        ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(
            orgnummer = arbeidsgiver,
            inntektsopplysninger = inntekter.map { it.somInntekt(meldingsreferanseId) }
        )

    private fun harInntekter() = inntekter.isNotEmpty()

    internal companion object {
        internal fun List<ArbeidsgiverInntekt>.beregnSykepengegrunnlag(hendelse: IAktivitetslogg, person: Person, skjæringstidspunkt: LocalDate, meldingsreferanseId: UUID, subsumsjonObserver: SubsumsjonObserver) =
            person.avklarSykepengegrunnlag(hendelse, skjæringstidspunkt, this.associateBy({ it.arbeidsgiver }) { it.tilSykepengegrunnlag(skjæringstidspunkt, meldingsreferanseId) }, subsumsjonObserver)

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

    class MånedligInntekt(
        private val yearMonth: YearMonth,
        private val inntekt: Inntekt,
        private val type: Inntekttype,
        private val fordel: String,
        private val beskrivelse: String
    ) {
        internal fun somInntekt(meldingsreferanseId: UUID) = Skatteopplysning(
            meldingsreferanseId,
            inntekt,
            yearMonth,
            enumValueOf(type.name),
            fordel,
            beskrivelse
        )

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

