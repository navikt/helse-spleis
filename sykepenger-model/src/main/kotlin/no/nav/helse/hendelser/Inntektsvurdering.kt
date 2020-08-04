package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
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

    private val inntekter: List<MånedligInntekt>
    private val antallMåneder =
        if (perioder.isEmpty()) 0
        else perioder.map { it.key }.let { ChronoUnit.MONTHS.between(it.max(), it.min()) }
    private val sammenligningsgrunnlag =
        perioder.values.flatten().fold(Inntekt.INGEN) { acc, (_, inntekt) -> (inntekt.tilMånedligDouble() + acc.tilMånedligDouble()).månedlig }
    private lateinit var avviksprosent: Prosentdel

    init {
        inntekter = perioder.flatMap { entry ->
            entry.value.map { pair -> MånedligInntekt(entry.key, pair.first, pair.second) }
        }
    }

    internal fun sammenligningsgrunnlag() = sammenligningsgrunnlag

    internal fun avviksprosent() = avviksprosent

    internal fun valider(aktivitetslogg: Aktivitetslogg, beregnetInntekt: Inntekt, periodetype: Periodetype): Aktivitetslogg {
        if (antallMåneder > 12) aktivitetslogg.error("Forventer 12 eller færre inntektsmåneder")
        if (inntekter.kilder(3) > 1) {
            val melding =
                "Brukeren har flere inntekter de siste tre måneder. Kontroller om brukeren har flere arbeidsforhold eller andre ytelser på sykmeldingstidspunktet som påvirker utbetalingen."
            if (periodetype in listOf(INFOTRYGDFORLENGELSE, FORLENGELSE)) aktivitetslogg.info(melding)
            else aktivitetslogg.warn(melding)
        }
        if (sammenligningsgrunnlag <= Inntekt.INGEN) return aktivitetslogg.apply { error("sammenligningsgrunnlaget er <= 0") }
        avviksprosent = avviksprosent(beregnetInntekt)
        if (avviksprosent > MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT) aktivitetslogg.error(
            "Har mer enn %.0f %% avvik",
            MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.toDouble()
        )
        else aktivitetslogg.info(
            "Har %.0f %% eller mindre avvik i inntekt (%.2f %%)",
            MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.toDouble(),
            avviksprosent.toDouble()
        )
        return aktivitetslogg
    }

    private fun avviksprosent(beregnetInntekt: Inntekt) =
        (beregnetInntekt*12).avviksprosent(sammenligningsgrunnlag)

    private class MånedligInntekt(private val yearMonth: YearMonth, private val orgnummer: String?, inntekt: Inntekt) {

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
