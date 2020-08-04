package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.avg
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Periodetype.FORLENGELSE
import no.nav.helse.person.Periodetype.INFOTRYGDFORLENGELSE
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class Inntektsvurdering(
    perioder: Map<YearMonth, List<Pair<String?, Inntekt>>>
) {

    private val inntekter: List<MånedligInntekt> = perioder.flatMap { (måned, inntektListe) ->
        inntektListe.map { (orgnummer, inntekt) -> MånedligInntekt(måned, orgnummer, inntekt) }
    }

    private val sammenligningsgrunnlag = inntekter.avg()

    private var avviksprosent: Prosentdel? = null

    internal fun sammenligningsgrunnlag() = sammenligningsgrunnlag

    internal fun avviksprosent() = avviksprosent

    internal fun valider(aktivitetslogg: Aktivitetslogg, beregnetInntekt: Inntekt, periodetype: Periodetype): Aktivitetslogg {
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
                MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.toDouble()
            )
            else aktivitetslogg.info(
                "Har %.0f %% eller mindre avvik i inntekt (%.2f %%)",
                MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.toDouble(),
                avvik.toDouble()
            )
        }
        return aktivitetslogg
    }

    private fun avviksprosent(beregnetInntekt: Inntekt) =
        beregnetInntekt.avviksprosent(sammenligningsgrunnlag)

    private class MånedligInntekt(
        private val yearMonth: YearMonth,
        private val orgnummer: String?,
        private val inntekt: Inntekt
    ) {

        companion object {
            internal fun kilder(inntekter: List<MånedligInntekt>, antallMåneder: Int) =
                inntekter.nylig(antallMåneder).distinctBy { it.orgnummer }.size

            private fun List<MånedligInntekt>.nylig(antallMåneder: Int): List<MånedligInntekt> {
                return this.månedFørSlutt(antallMåneder)
                    ?.let { førsteMåned -> this@nylig.filter { it.yearMonth >= førsteMåned } }
                    ?: emptyList()
            }

            private fun List<MånedligInntekt>.månedFørSlutt(antallMåneder: Int) =
                this.map { it.yearMonth }.max()?.minusMonths(antallMåneder.toLong() - 1)

            internal fun avg(inntekter: List<MånedligInntekt>) = inntekter.map { it.inntekt }.avg()

            internal fun antallMåneder(inntekter: List<MånedligInntekt>): Long {
                if (inntekter.isEmpty()) return 0
                return ChronoUnit.MONTHS.between(inntekter.maxMonth(), inntekter.minMonth())
            }

            private fun List<MånedligInntekt>.minMonth() = map { it.yearMonth }.min()
            private fun List<MånedligInntekt>.maxMonth() = map { it.yearMonth }.max()
        }
    }

    private fun List<MånedligInntekt>.kilder(antallMåneder: Int) = MånedligInntekt.kilder(this, antallMåneder)
    private fun List<MånedligInntekt>.avg() = MånedligInntekt.avg(this)
    private fun List<MånedligInntekt>.antallMåneder() = MånedligInntekt.antallMåneder(this)
}
