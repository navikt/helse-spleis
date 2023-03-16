package no.nav.helse.utbetalingslinjer

import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.Utbetalingpåminnelse
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal fun Utbetalingpåminnelse.utbetalingport() = UtbetalingpåminnelseAdapter(this)
class UtbetalingpåminnelseAdapter(private val utbetalingpåminnelse: Utbetalingpåminnelse): UtbetalingpåminnelsePort, IAktivitetslogg by utbetalingpåminnelse {
    override fun erRelevant(id: UUID): Boolean = utbetalingpåminnelse.erRelevant(id)
    override fun harOversteget(makstid: Duration): Boolean = utbetalingpåminnelse.harOversteget(makstid)
    override fun gjelderStatus(tilstand: Utbetalingstatus): Boolean = utbetalingpåminnelse.gjelderStatus(tilstand)

}

internal fun UtbetalingHendelse.utbetalingport() = UtbetalingHendelseAdapter(this)
class UtbetalingHendelseAdapter(private val hendelse: UtbetalingHendelse): UtbetalingHendelsePort, IAktivitetslogg by hendelse {
    override val avstemmingsnøkkel: Long = hendelse.avstemmingsnøkkel
    override val overføringstidspunkt: LocalDateTime = hendelse.overføringstidspunkt
    override val status: Oppdragstatus = hendelse.status
    override fun erRelevant(fagsystemId: String): Boolean = hendelse.erRelevant(fagsystemId)
    override fun erRelevant(arbeidsgiverFagsystemId: String, personFagsystemId: String, utbetaling: UUID): Boolean = hendelse.erRelevant(arbeidsgiverFagsystemId, personFagsystemId, utbetaling)
    override fun skalForsøkesIgjen(): Boolean = hendelse.skalForsøkesIgjen()
    override fun valider() {
        hendelse.valider()
    }
}