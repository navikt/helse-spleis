package no.nav.helse.utbetalingslinjer

import java.time.LocalDateTime
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.Grunnbeløpsregulering
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.økonomi.Økonomi

internal fun AnnullerUtbetaling.utbetalingport() = AnnullerUtbetalingAdapter(this)
class AnnullerUtbetalingAdapter(private val original: AnnullerUtbetaling): AnnullerUtbetalingPort, IAktivitetslogg by original {
    override fun vurdering(): Utbetaling.Vurdering = original.vurdering()
    override fun erRelevant(fagsystemId: String): Boolean = original.erRelevant(fagsystemId)
}
class OverføringsinformasjonAdapter(private val hendelse: UtbetalingHendelse): OverføringsinformasjonPort {
    override val avstemmingsnøkkel: Long = hendelse.avstemmingsnøkkel
    override val overføringstidspunkt: LocalDateTime = hendelse.overføringstidspunkt
    override val status: Oppdragstatus = hendelse.status
    override fun erRelevant(fagsystemId: String): Boolean = hendelse.erRelevant(fagsystemId)
}

class OverføringsinformasjonOverførtAdapter(private val hendelse: UtbetalingOverført): OverføringsinformasjonPort {
    override val avstemmingsnøkkel: Long = hendelse.avstemmingsnøkkel
    override val overføringstidspunkt: LocalDateTime = hendelse.overføringstidspunkt
    override val status: Oppdragstatus = Oppdragstatus.OVERFØRT
    override fun erRelevant(fagsystemId: String): Boolean = hendelse.erRelevant(fagsystemId)
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

internal fun Grunnbeløpsregulering.utbetalingsport() = GrunnbeløpsreguleringAdapter(this)
class GrunnbeløpsreguleringAdapter(private val grunnbeløpsregulering: Grunnbeløpsregulering): GrunnbeløpsreguleringPort, IAktivitetslogg by grunnbeløpsregulering {
    override fun erRelevant(fagsystemId: String): Boolean = grunnbeløpsregulering.erRelevant(fagsystemId)
    override fun fødselsnummer(): String  = grunnbeløpsregulering.fødselsnummer()
    override fun organisasjonsnummer(): String = grunnbeløpsregulering.organisasjonsnummer()
}