package no.nav.helse.hendelser

import java.time.YearMonth
import java.time.temporal.ChronoUnit
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt.Companion.harInntektFor
import no.nav.helse.person.inntekt.Skatteopplysning
import no.nav.helse.økonomi.Inntekt

data class ArbeidsgiverInntekt(
    val arbeidsgiver: String,
    val inntekter: List<MånedligInntekt>
) {
    internal companion object {
        internal fun List<ArbeidsgiverInntekt>.harInntektFor(orgnummer: String, måned: YearMonth) =
            this.any { it.arbeidsgiver == orgnummer && it.inntekter.harInntektFor(måned) }

        internal fun List<ArbeidsgiverInntekt>.harInntektI(måned: YearMonth) =
            this.any { it.inntekter.harInntektFor(måned) }

        internal fun List<ArbeidsgiverInntekt>.antallMåneder() =
            MånedligInntekt.antallMåneder(flatMap { it.inntekter })
    }

    data class MånedligInntekt(
        val yearMonth: YearMonth,
        val inntekt: Inntekt,
        val type: Inntekttype,
        val fordel: String,
        val beskrivelse: String
    ) {
        internal fun somInntekt(meldingsreferanseId: MeldingsreferanseId) = Skatteopplysning(
            meldingsreferanseId,
            inntekt,
            yearMonth,
            enumValueOf(type.name),
            fordel,
            beskrivelse
        )

        companion object {
            internal fun List<MånedligInntekt>.harInntektFor(måned: YearMonth) =
                this.any { it.yearMonth == måned && it.inntekt > Inntekt.INGEN }

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

