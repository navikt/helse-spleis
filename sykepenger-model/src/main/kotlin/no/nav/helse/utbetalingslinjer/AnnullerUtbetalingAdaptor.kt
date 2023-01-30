package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

class AnnullerUtbetalingAdaptor(private val original: AnnullerUtbetaling): AnnullerUtbetalingPort, IAktivitetslogg by original {
    override fun vurdering(): Utbetaling.Vurdering = original.vurdering()
    override fun erRelevant(fagsystemId: String): Boolean = original.erRelevant(fagsystemId)
}

internal fun AnnullerUtbetaling.utbetalingport() = AnnullerUtbetalingAdaptor(this)