package no.nav.helse.hendelser.utbetaling

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Utbetaling.Vurdering

interface Utbetalingsavgjørelse: IAktivitetslogg {
    fun relevantUtbetaling(id: UUID): Boolean
    fun relevantVedtaksperiode(id: UUID): Boolean
    fun godkjent(): Boolean
    fun avgjørelsestidspunkt(): LocalDateTime
    fun automatisert(): Boolean
    fun vurdering(): Vurdering
    fun valider() {}
}
fun Utbetalingsavgjørelse.avvist() = !godkjent()