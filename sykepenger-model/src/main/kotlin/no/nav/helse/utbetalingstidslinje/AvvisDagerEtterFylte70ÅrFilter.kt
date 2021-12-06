package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat.Companion.`§8-3 ledd 1 punktum 2`
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvisteDager
import java.time.LocalDate

internal class AvvisDagerEtterFylte70ÅrFilter(
    private val tidslinjer: List<Utbetalingstidslinje>,
    private val periode: Periode,
    alder: Alder,
    private val aktivitetslogg: IAktivitetslogg
) {

    private val syttiårsdagen = alder.søttiårsdagen

    internal fun filter() {
        val avvisteDager = avvisteDager(tidslinjer, listOf(syttiårsdagen til LocalDate.MAX), periode, listOf(Begrunnelse.Over70))

        val førsteDatoITidslinje = tidslinjer.minOf { it.periode().start }
        val sisteDatoITidslinje = tidslinjer.maxOf { it.periode().endInclusive }
        val førsteAvvisteDag = avvisteDager.minOfOrNull { it.dato }

        if (førsteDatoITidslinje < syttiårsdagen) {
            aktivitetslogg.`§8-3 ledd 1 punktum 2`(
                oppfylt = true,
                syttiårsdagen = syttiårsdagen,
                vurderingFom = førsteDatoITidslinje,
                vurderingTom = førsteAvvisteDag?.minusDays(1) ?: sisteDatoITidslinje,
                tidslinjeFom = førsteDatoITidslinje,
                tidslinjeTom = sisteDatoITidslinje,
                avvisteDager = emptyList()
            )
        }
        if (syttiårsdagen <= sisteDatoITidslinje) {
            aktivitetslogg.info("Utbetaling stoppet etter $syttiårsdagen, søker fylte 70 år.")
            aktivitetslogg.`§8-3 ledd 1 punktum 2`(
                oppfylt = false,
                syttiårsdagen = syttiårsdagen,
                vurderingFom = maxOf(syttiårsdagen, førsteDatoITidslinje),
                vurderingTom = sisteDatoITidslinje,
                tidslinjeFom = førsteDatoITidslinje,
                tidslinjeTom = sisteDatoITidslinje,
                avvisteDager = avvisteDager.map { it.dato }
            )
        }
    }
}
