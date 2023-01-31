package no.nav.helse.utbetalingslinjer

import AnnullerUtbetalingPort
import GrunnbeløpsreguleringPort
import OverføringsinformasjonPort
import SimuleringPort
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.Grunnbeløpsregulering
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.hendelser.utbetaling.Utbetalingpåminnelse
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.økonomi.Økonomi

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

/**
 * Tilpasser Økonomi så det passer til Beløpkilde-porten til utbetalingslinjer
 */
internal class BeløpkildeAdapter(private val økonomi: Økonomi): Beløpkilde {
    override fun arbeidsgiverbeløp(): Int = økonomi.medAvrundetData { _, _, _, _, _, _, arbeidsgiverbeløp, _, _ -> arbeidsgiverbeløp!! }
    override fun personbeløp(): Int = økonomi.medAvrundetData { _, _, _, _, _, _, _, personbeløp, _ -> personbeløp!! }
}

class SimuleringAdapter(private val simulering: Simulering): SimuleringPort {
    override val simuleringResultat: SimuleringResultat? = simulering.simuleringResultat
    override fun valider(oppdrag: Oppdrag): SimuleringPort = this.apply { simulering.valider(oppdrag) }
    override fun erRelevantFor(fagområde: Fagområde, fagsystemId: String): Boolean = simulering.erRelevantFor(fagområde, fagsystemId)
}

internal fun Grunnbeløpsregulering.utbetalingport() = GrunnbeløpsreguleringAdapter(this)
class GrunnbeløpsreguleringAdapter(private val grunnbeløpsregulering: Grunnbeløpsregulering): GrunnbeløpsreguleringPort, IAktivitetslogg by grunnbeløpsregulering {
    override fun erRelevant(fagsystemId: String): Boolean = grunnbeløpsregulering.erRelevant(fagsystemId)
    override fun fødselsnummer(): String  = grunnbeløpsregulering.fødselsnummer()
    override fun organisasjonsnummer(): String = grunnbeløpsregulering.organisasjonsnummer()
}

internal fun Utbetalingpåminnelse.utbetalingport() = UtbetalingpåminnelseAdapter(this)
class UtbetalingpåminnelseAdapter(private val utbetalingpåminnelse: Utbetalingpåminnelse): UtbetalingpåminnelsePort, IAktivitetslogg by utbetalingpåminnelse {
    override fun erRelevant(id: UUID): Boolean = utbetalingpåminnelse.erRelevant(id)
    override fun harOversteget(makstid: Duration): Boolean = utbetalingpåminnelse.harOversteget(makstid)
    override fun gjelderStatus(tilstand: Utbetalingstatus): Boolean = utbetalingpåminnelse.gjelderStatus(tilstand)

}