package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Periodetype.FORLENGELSE
import no.nav.helse.person.Periodetype.INFOTRYGDFORLENGELSE
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class Inntektsvurdering(
    perioder: Map<YearMonth, List<Pair<String?, Double>>>
) {
    private companion object {
        private const val MAKSIMALT_TILLATT_AVVIK = .25
    }

    private val inntekter: List<MånedligInntekt>
    private val antallMåneder =
        if (perioder.isEmpty()) 0
        else perioder.map { it.key }.let { ChronoUnit.MONTHS.between(it.max(), it.min()) }
    private val sammenligningsgrunnlag = perioder.flatMap { it.value }.sumByDouble { it.second }
    private var avviksprosent = Double.POSITIVE_INFINITY

    init {
        inntekter = perioder.flatMap { entry ->
            entry.value.map { pair -> MånedligInntekt(entry.key, pair.first, pair.second) }
        }
    }

    internal fun sammenligningsgrunnlag(): Double =
        (sammenligningsgrunnlag * 100).roundToInt() / 100.0 // behold to desimaler

    internal fun avviksprosent() = avviksprosent

    internal fun valider(aktivitetslogg: Aktivitetslogg, beregnetInntekt: Double, periodetype: Periodetype): Aktivitetslogg {
        if (antallMåneder > 12) aktivitetslogg.error("Forventer 12 eller færre inntektsmåneder")
        if (inntekter.kilder(3) > 1) {
            val melding =
                "Brukeren har flere inntekter de siste tre måneder. Kontroller om brukeren har flere arbeidsforhold eller andre ytelser på sykmeldingstidspunktet som påvirker utbetalingen."
            if (periodetype in listOf(INFOTRYGDFORLENGELSE, FORLENGELSE)) aktivitetslogg.info(melding)
            else aktivitetslogg.warn(melding)
        }
        if (sammenligningsgrunnlag <= 0.0) return aktivitetslogg.apply { error("sammenligningsgrunnlaget er <= 0") }
        avviksprosent = avviksprosent(beregnetInntekt)
        if (avviksprosent > MAKSIMALT_TILLATT_AVVIK) aktivitetslogg.error(
            "Har mer enn %.0f %% avvik",
            MAKSIMALT_TILLATT_AVVIK * 100
        )
        else aktivitetslogg.info(
            "Har %.0f %% eller mindre avvik i inntekt (%.2f %%)",
            MAKSIMALT_TILLATT_AVVIK * 100,
            avviksprosent * 100
        )
        return aktivitetslogg
    }

    private fun avviksprosent(beregnetInntekt: Double) =
            (beregnetInntekt * 12.0 - sammenligningsgrunnlag)
                .absoluteValue / sammenligningsgrunnlag

    private class MånedligInntekt(private val yearMonth: YearMonth, private val orgnummer: String?, inntekt: Double) {

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
        }
    }

    private fun List<MånedligInntekt>.kilder(antallMåneder: Int) = MånedligInntekt.kilder(inntekter, antallMåneder)

}
