package no.nav.helse.utbetalingslinjer

import java.time.Duration
import java.util.UUID
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.serde.reflection.Utbetalingstatus

interface UtbetalingpåminnelsePort: IAktivitetslogg {
    fun erRelevant(id: UUID): Boolean
    fun harOversteget(makstid: Duration): Boolean
    fun gjelderStatus(tilstand: Utbetalingstatus): Boolean

}
