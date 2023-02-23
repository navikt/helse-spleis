package no.nav.helse.utbetalingslinjer

import AnnullerUtbetalingPort
import OverføringsinformasjonPort
import SimuleringPort
import UtbetalingHendelsePort
import UtbetalingpåminnelsePort
import UtbetalingsgodkjenningPort
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.hendelser.utbetaling.Utbetalingpåminnelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal fun AnnullerUtbetaling.utbetalingport() = AnnullerUtbetalingAdapter(this)
class AnnullerUtbetalingAdapter(private val original: AnnullerUtbetaling): AnnullerUtbetalingPort, IAktivitetslogg by original {
    override fun vurdering(): Utbetaling.Vurdering = original.vurdering()
    override fun erRelevant(fagsystemId: String): Boolean = original.erRelevant(fagsystemId)
}
class OverføringsinformasjonAdapter(private val hendelse: UtbetalingHendelse): OverføringsinformasjonPort, IAktivitetslogg by hendelse {
    override val avstemmingsnøkkel: Long = hendelse.avstemmingsnøkkel
    override val overføringstidspunkt: LocalDateTime = hendelse.overføringstidspunkt
    override val status: Oppdragstatus = hendelse.status
    override fun erRelevant(fagsystemId: String): Boolean = hendelse.erRelevant(fagsystemId)
    override fun erRelevant(arbeidsgiverFagsystemId: String, personFagsystemId: String, utbetaling: UUID): Boolean = hendelse.erRelevant(arbeidsgiverFagsystemId, personFagsystemId, utbetaling)
}

fun UtbetalingOverført.utbetalingport() = OverføringsinformasjonOverførtAdapter(this)
class OverføringsinformasjonOverførtAdapter(private val hendelse: UtbetalingOverført): OverføringsinformasjonPort, IAktivitetslogg by hendelse {
    override val avstemmingsnøkkel: Long = hendelse.avstemmingsnøkkel
    override val overføringstidspunkt: LocalDateTime = hendelse.overføringstidspunkt
    override val status: Oppdragstatus = Oppdragstatus.OVERFØRT
    override fun erRelevant(fagsystemId: String): Boolean = hendelse.erRelevant(fagsystemId)
    override fun erRelevant(arbeidsgiverFagsystemId: String, personFagsystemId: String, utbetaling: UUID): Boolean = hendelse.erRelevant(arbeidsgiverFagsystemId, personFagsystemId, utbetaling)
}

internal fun Simulering.utbetalingport() = SimuleringAdapter(this)
class SimuleringAdapter(private val simulering: Simulering): SimuleringPort, IAktivitetslogg by simulering {
    override val simuleringResultat: SimuleringResultat? = simulering.simuleringResultat
    override fun valider(oppdrag: Oppdrag): SimuleringPort = this.apply { simulering.valider(oppdrag) }
    override fun erRelevantFor(fagområde: Fagområde, fagsystemId: String): Boolean = simulering.erRelevantFor(fagområde, fagsystemId)
    override fun erRelevantForUtbetaling(id: UUID): Boolean = simulering.erRelevantForUtbetaling(id)
}

internal fun Utbetalingpåminnelse.utbetalingport() = UtbetalingpåminnelseAdapter(this)
class UtbetalingpåminnelseAdapter(private val utbetalingpåminnelse: Utbetalingpåminnelse): UtbetalingpåminnelsePort, IAktivitetslogg by utbetalingpåminnelse {
    override fun erRelevant(id: UUID): Boolean = utbetalingpåminnelse.erRelevant(id)
    override fun harOversteget(makstid: Duration): Boolean = utbetalingpåminnelse.harOversteget(makstid)
    override fun gjelderStatus(tilstand: Utbetalingstatus): Boolean = utbetalingpåminnelse.gjelderStatus(tilstand)

}

internal fun Utbetalingsgodkjenning.utbetalingport() = UtbetalingsgodkjenningAdapter(this)
class UtbetalingsgodkjenningAdapter(private val utbetalingsgodkjenning: Utbetalingsgodkjenning): UtbetalingsgodkjenningPort, IAktivitetslogg by utbetalingsgodkjenning {
    override fun erRelevant(id: UUID): Boolean = utbetalingsgodkjenning.erRelevant(id)
    override fun valider() { utbetalingsgodkjenning.valider() }
    override fun vurdering(): Utbetaling.Vurdering = utbetalingsgodkjenning.vurdering()
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