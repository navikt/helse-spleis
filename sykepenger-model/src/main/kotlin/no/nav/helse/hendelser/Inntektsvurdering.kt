package no.nav.helse.hendelser

import no.nav.helse.hendelser.Inntektsvurdering.ArbeidsgiverInntekt.MånedligInntekt.Companion.nylig
import no.nav.helse.hendelser.Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
import no.nav.helse.hendelser.Inntektsvurdering.Inntektsgrunnlag.SYKEPENGEGRUNNLAG
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.InntekthistorikkVol2
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Periodetype.FORLENGELSE
import no.nav.helse.person.Periodetype.INFOTRYGDFORLENGELSE
import no.nav.helse.person.Person
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.avg
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Prosent.Companion.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.*

class Inntektsvurdering(
    private val inntekter: List<ArbeidsgiverInntekt>
) {
    private val sammenligningsgrunnlag = this.inntekter.avg()

    private var avviksprosent: Prosent? = null

    internal fun sammenligningsgrunnlag() = sammenligningsgrunnlag

    internal fun avviksprosent() = avviksprosent

    internal fun valider(
        aktivitetslogg: Aktivitetslogg,
        beregnetInntekt: Inntekt,
        periodetype: Periodetype
    ): Aktivitetslogg {
        if (inntekter.antallMåneder() > 12) aktivitetslogg.error("Forventer 12 eller færre inntektsmåneder")
        if (inntekter.kilder(3) > 1) {
            val melding =
                "Brukeren har flere inntekter de siste tre måneder. Kontroller om brukeren har flere arbeidsforhold eller andre ytelser på sykmeldingstidspunktet som påvirker utbetalingen."
            if (periodetype in listOf(INFOTRYGDFORLENGELSE, FORLENGELSE)) aktivitetslogg.info(melding)
            else aktivitetslogg.warn(melding)
        }
        if (sammenligningsgrunnlag <= Inntekt.INGEN) return aktivitetslogg.apply { error("sammenligningsgrunnlaget er <= 0") }
        avviksprosent(beregnetInntekt).also { avvik ->
            avviksprosent = avvik
            if (avvik > MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT) aktivitetslogg.error(
                "Har mer enn %.0f %% avvik",
                MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.prosent()
            )
            else aktivitetslogg.info(
                "Har %.0f %% eller mindre avvik i inntekt (%.2f %%)",
                MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.prosent(),
                avvik.prosent()
            )
        }
        return aktivitetslogg
    }

    private fun avviksprosent(beregnetInntekt: Inntekt) =
        beregnetInntekt.avviksprosent(sammenligningsgrunnlag)

    internal fun lagreInntekter(person: Person, vilkårsgrunnlag: Vilkårsgrunnlag) =
        ArbeidsgiverInntekt.lagreInntekter(inntekter, person, vilkårsgrunnlag)

    class ArbeidsgiverInntekt(
        private val arbeidsgiver: String,
        private val inntekter: List<MånedligInntekt>
    ) {
        internal fun lagreInntekter(inntekthistorikk: InntekthistorikkVol2, meldingsreferanseId: UUID, tidsstempel: LocalDateTime = LocalDateTime.now()) {
            MånedligInntekt.lagreInntekter(inntekter, inntekthistorikk, meldingsreferanseId, tidsstempel)
        }

        private fun harInntekter() = inntekter.isNotEmpty()

        companion object {
            fun lagreInntekter(
                inntekter: List<ArbeidsgiverInntekt>,
                person: Person,
                vilkårsgrunnlag: Vilkårsgrunnlag
            ) {
                inntekter.forEach { person.lagreInntekter(it.arbeidsgiver, it, vilkårsgrunnlag) }
            }

            internal fun kilder(inntekter: List<ArbeidsgiverInntekt>, antallMåneder: Int) =
                inntekter
                    .map {
                        ArbeidsgiverInntekt(
                            it.arbeidsgiver,
                            it.inntekter.nylig(månedFørSlutt(inntekter, antallMåneder))
                        )
                    }
                    .filter { it.harInntekter() }
                    .size

            private fun månedFørSlutt(inntekter: List<ArbeidsgiverInntekt>, antallMåneder: Int) =
                MånedligInntekt.månedFørSlutt(inntekter.flatMap { it.inntekter }, antallMåneder)

            internal fun antallMåneder(inntekter: List<ArbeidsgiverInntekt>) =
                MånedligInntekt.antallMåneder(inntekter.flatMap { it.inntekter })

            internal fun avg(inntekter: List<ArbeidsgiverInntekt>) =
                MånedligInntekt.avg(inntekter.flatMap { it.inntekter })
        }

        class MånedligInntekt(
            private val yearMonth: YearMonth,
            private val inntekt: Inntekt,
            private val type: Inntekttype,
            private val inntektsgrunnlag: Inntektsgrunnlag
        ) {

            companion object {
                internal fun List<MånedligInntekt>.nylig(månedFørSlutt: YearMonth?): List<MånedligInntekt> {
                    return månedFørSlutt
                        ?.let { førsteMåned -> this@nylig.filter { it.yearMonth >= førsteMåned } }
                        ?: emptyList()
                }

                internal fun månedFørSlutt(inntekter: List<MånedligInntekt>, antallMåneder: Int) =
                    inntekter.map { it.yearMonth }.max()?.minusMonths(antallMåneder.toLong() - 1)

                internal fun avg(inntekter: List<MånedligInntekt>): Inntekt =
                    inntekter
                        .groupBy { it.yearMonth }
                        .map { (_, månedsinntekter) -> summer(månedsinntekter) }
                        .avg()

                internal fun summer(inntekter: List<MånedligInntekt>) = inntekter.map { it.inntekt }.summer()

                internal fun antallMåneder(inntekter: List<MånedligInntekt>): Long {
                    if (inntekter.isEmpty()) return 0
                    return ChronoUnit.MONTHS.between(inntekter.maxMonth(), inntekter.minMonth())
                }

                private fun List<MånedligInntekt>.minMonth() = map { it.yearMonth }.min()
                private fun List<MånedligInntekt>.maxMonth() = map { it.yearMonth }.max()

                internal fun lagreInntekter(
                    inntekter: List<MånedligInntekt>,
                    inntekthistorikk: InntekthistorikkVol2,
                    meldingsreferanseId: UUID,
                    tidsstempel: LocalDateTime
                ) {
                    inntekter
                        .forEach {
                            inntekthistorikk.add(
                                it.yearMonth.atDay(1),
                                meldingsreferanseId,
                                it.inntekt,
                                when (it.inntektsgrunnlag) {
                                    SAMMENLIGNINGSGRUNNLAG -> InntekthistorikkVol2.Inntektsendring.Kilde.SKATT_SAMMENLIGNINSGRUNNLAG
                                    SYKEPENGEGRUNNLAG -> InntekthistorikkVol2.Inntektsendring.Kilde.SKATT_SYKEPENGEGRUNNLAG
                                },
                                enumValueOf(it.type.name),
                                "", //TODO: må hentes fra sparkel-inntekt
                                "",
                                "",
                                tidsstempel
                            )
                        }
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

    private fun List<ArbeidsgiverInntekt>.kilder(antallMåneder: Int) = ArbeidsgiverInntekt.kilder(this, antallMåneder)
    private fun List<ArbeidsgiverInntekt>.avg() = ArbeidsgiverInntekt.avg(this)
    private fun List<ArbeidsgiverInntekt>.antallMåneder() = ArbeidsgiverInntekt.antallMåneder(this)
}
