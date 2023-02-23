import java.util.UUID
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Utbetaling

interface AnnullerUtbetalingPort: IAktivitetslogg {
    fun vurdering(): Utbetaling.Vurdering
    fun erRelevant(fagsystemId: String): Boolean
}

interface UtbetalingsgodkjenningPort: IAktivitetslogg {
    fun erRelevant(id: UUID): Boolean
    fun valider()
    fun vurdering(): Utbetaling.Vurdering
}
