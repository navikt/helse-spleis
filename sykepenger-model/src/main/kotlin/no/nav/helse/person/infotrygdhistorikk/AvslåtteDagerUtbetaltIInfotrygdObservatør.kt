package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioderMedHensynTilHelg
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal class AvslåtteDagerUtbetaltIInfotrygdObservatør(
    private val infotrygdtidslinje: Utbetalingstidslinje
) {
    private val feilutbetalt = mutableMapOf<Begrunnelse, MutableSet<LocalDate>>()

    internal fun avslåttDag(dato: LocalDate, begrunnelse: Begrunnelse) {
        if (infotrygdtidslinje[dato] !is Utbetalingsdag.NavDag) return
        feilutbetalt.compute(begrunnelse) { _, datoer ->
            (datoer ?: mutableSetOf()).also { it.add(dato) }
        }
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg) {
        if (feilutbetalt.isEmpty()) return
        feilutbetalt.forEach { (begrunnelse, datoer) ->
            val perioder =  datoer.grupperSammenhengendePerioderMedHensynTilHelg()
            val dagformulering = if (datoer.size == 1) "dag" else "dager"
            val periodeformulering = if (perioder.size == 1) "perioden" else "periodene"
            aktivitetslogg.info("Utbetalt ${datoer.size} $dagformulering i Infotrygd i $periodeformulering ${perioder.joinToString()}. Skulle vært avslått pga. ${begrunnelse::class.simpleName}")
        }
    }
}
