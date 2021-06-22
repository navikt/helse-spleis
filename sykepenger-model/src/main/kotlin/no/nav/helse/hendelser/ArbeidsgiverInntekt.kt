package no.nav.helse.hendelser

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

class ArbeidsgiverInntekt(
    private val arbeidsgiver: String,
    private val inntekter: List<MånedligInntekt>
) {
    internal fun lagreInntekter(
        inntektshistorikk: Inntektshistorikk,
        skjæringstidspunkt: LocalDate,
        meldingsreferanseId: UUID
    ) {
        MånedligInntekt.lagreInntekterForSammeligningsgrunnlag(inntekter, inntektshistorikk, skjæringstidspunkt, meldingsreferanseId)
    }

    private fun harInntekter() = inntekter.isNotEmpty()

    companion object {
        fun lagreInntekterForSammeligningsgrunnlag(
            inntekter: List<ArbeidsgiverInntekt>,
            person: Person,
            skjæringstidspunkt: LocalDate,
            hendelse: PersonHendelse
        ) {
            inntekter.forEach { person.lagreInntekter(it.arbeidsgiver, it, skjæringstidspunkt, hendelse) }
        }

        fun lagreInntekterForSykepengegrunnlag(inntekter: List<ArbeidsgiverInntekt>, person: Person, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) {
            inntekter.forEach { person.lagreInntekter(it.arbeidsgiver, it, skjæringstidspunkt, hendelse) }
        }

        internal fun List<ArbeidsgiverInntekt>.kilder(antallMåneder: Int) =
            map {
                    ArbeidsgiverInntekt(
                        it.arbeidsgiver,
                        it.inntekter.nylig(månedFørSlutt(this, antallMåneder))
                    )
                }
                .filter { it.harInntekter() }
                .size

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

        companion object {
            internal fun List<MånedligInntekt>.nylig(månedFørSlutt: YearMonth?): List<MånedligInntekt> {
                return månedFørSlutt
                    ?.let { førsteMåned -> this@nylig.filter { it.yearMonth >= førsteMåned } }
                    ?: emptyList()
            }

            internal fun månedFørSlutt(inntekter: List<MånedligInntekt>, antallMåneder: Int) =
                inntekter.maxOfOrNull { it.yearMonth }?.minusMonths(antallMåneder.toLong() - 1)

            private fun summer(inntekter: List<MånedligInntekt>) = inntekter.map { it.inntekt }.summer()

            internal fun antallMåneder(inntekter: List<MånedligInntekt>): Long {
                if (inntekter.isEmpty()) return 0
                return ChronoUnit.MONTHS.between(inntekter.maxMonth(), inntekter.minMonth())
            }

            private fun List<MånedligInntekt>.minMonth() = minOfOrNull { it.yearMonth }
            private fun List<MånedligInntekt>.maxMonth() = maxOfOrNull { it.yearMonth }

            internal fun lagreInntekterForSammeligningsgrunnlag(
                inntekter: List<MånedligInntekt>,
                inntektshistorikk: Inntektshistorikk,
                skjæringstidspunkt: LocalDate,
                meldingsreferanseId: UUID
            ) {
                inntektshistorikk {
                    inntekter.forEach {
                        addSkattSammenligningsgrunnlag(
                            dato = skjæringstidspunkt,
                            hendelseId = meldingsreferanseId,
                            beløp = it.inntekt,
                            måned = it.yearMonth,
                            type = enumValueOf(it.type.name),
                            fordel = it.fordel,
                            beskrivelse = it.beskrivelse
                        )
                    }
                }
            }

            internal fun lagreInntekterForSykepengegrunnlag(
                inntekter: List<MånedligInntekt>,
                inntektshistorikk: Inntektshistorikk,
                skjæringstidspunkt: LocalDate,
                meldingsreferanseId: UUID
            ) {
                inntektshistorikk {
                    inntekter.forEach {
                        addSkattSykepengegrunnlag(
                            dato = skjæringstidspunkt,
                            hendelseId = meldingsreferanseId,
                            beløp = it.inntekt,
                            måned = it.yearMonth,
                            type = enumValueOf(it.type.name),
                            fordel = it.fordel,
                            beskrivelse = it.beskrivelse
                        )

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

        enum class Inntektsgrunnlag {
            SAMMENLIGNINGSGRUNNLAG, SYKEPENGEGRUNNLAG
        }
    }
}

