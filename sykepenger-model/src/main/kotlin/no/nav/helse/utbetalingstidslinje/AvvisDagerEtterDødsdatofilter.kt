package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.Alder
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.nesteDag
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvisteDager

internal class AvvisDagerEtterDødsdatofilter(
    private val alder: Alder,
) : UtbetalingstidslinjerFilter {

    override fun filter(
        arbeidsgivere: List<Arbeidsgiverberegning>,
        periode: Periode,
        aktivitetslogg: IAktivitetslogg,
        subsumsjonslogg: Subsumsjonslogg
    ): List<Arbeidsgiverberegning> {
        val avvisteTidslinjer = alder.avvisDager(arbeidsgivere)

        val avvisteDager = avvisteDager(avvisteTidslinjer.map { it.samletVedtaksperiodetidslinje }, periode, Begrunnelse.EtterDødsdato)
        if (avvisteDager.isNotEmpty()) aktivitetslogg.info("Utbetaling stoppet etter ${avvisteDager.first().dato} grunnet dødsfall")

        return avvisteTidslinjer
    }

    private fun Alder.avvisDager(arbeidsgivere: List<Arbeidsgiverberegning>): List<Arbeidsgiverberegning> {
        if (dødsdato == null) return arbeidsgivere
        return arbeidsgivere.avvis(listOf(dødsdato.nesteDag til LocalDate.MAX), Begrunnelse.EtterDødsdato)
    }
}
