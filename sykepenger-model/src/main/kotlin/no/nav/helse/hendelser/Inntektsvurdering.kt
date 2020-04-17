package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import java.math.BigDecimal
import java.math.MathContext
import java.time.YearMonth
import kotlin.math.roundToInt

class Inntektsvurdering(
    private val perioder: Map<YearMonth, List<Pair<String?, Double>>>
) {
    private companion object {
        private const val MAKSIMALT_TILLATT_AVVIK = .25
    }

    private val sammenligningsgrunnlag = perioder.flatMap { it.value }.sumByDouble { it.second }
    private var avviksprosent = Double.POSITIVE_INFINITY

    internal fun sammenligningsgrunnlag(): Double =
        (sammenligningsgrunnlag * 100).roundToInt() / 100.0 // behold to desimaler
    internal fun avviksprosent() = avviksprosent

    internal fun valider(aktivitetslogg: Aktivitetslogg, beregnetInntekt: BigDecimal): Aktivitetslogg {
        if (antallPerioder() > 12) aktivitetslogg.error("Forventer 12 eller færre inntektsmåneder")
        if (flereVirksomheter()) aktivitetslogg.warn("Finnes inntekter fra flere virksomheter siste tre måneder")
        if (sammenligningsgrunnlag <= 0.0) return aktivitetslogg.apply { error("sammenligningsgrunnlaget er <= 0") }
        avviksprosent = avviksprosent(beregnetInntekt)
        if (avviksprosent > MAKSIMALT_TILLATT_AVVIK) aktivitetslogg.error("Har mer enn %.0f %% avvik", MAKSIMALT_TILLATT_AVVIK * 100)
        else aktivitetslogg.info("Har %.0f %% eller mindre avvik i inntekt (%.2f %%)", MAKSIMALT_TILLATT_AVVIK * 100, avviksprosent * 100)
        return aktivitetslogg
    }

    private fun flereVirksomheter(): Boolean {
        val førsteMåned = perioder.maxBy { it.key }?.key?.minusMonths(2) ?: return false
        return perioder
            .filterKeys { it >= førsteMåned }
            .flatMap { it.value }
            .map { it.first }
            .distinct().size > 1
    }

    private fun antallPerioder() = perioder.keys.size
    private fun avviksprosent(beregnetInntekt: BigDecimal) =
        sammenligningsgrunnlag.toBigDecimal().let { sammenligningsgrunnlag ->
            beregnetInntekt.omregnetÅrsinntekt()
                .minus(sammenligningsgrunnlag)
                .abs()
                .divide(sammenligningsgrunnlag, MathContext.DECIMAL128)
        }.toDouble()

    private fun BigDecimal.omregnetÅrsinntekt() = this * 12.toBigDecimal()
}
