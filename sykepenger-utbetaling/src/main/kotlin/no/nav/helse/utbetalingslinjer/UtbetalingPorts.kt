import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus

interface OverføringsinformasjonPort: IAktivitetslogg {
    val avstemmingsnøkkel: Long
    val overføringstidspunkt: LocalDateTime
    val status: Oppdragstatus
    fun erRelevant(fagsystemId: String): Boolean
    fun erRelevant(arbeidsgiverFagsystemId: String, personFagsystemId: String, utbetaling: UUID): Boolean
}

interface SimuleringPort {
    val simuleringResultat: SimuleringResultat?
    fun valider(oppdrag: Oppdrag): SimuleringPort
    fun erRelevantFor(fagområde: Fagområde, fagsystemId: String): Boolean
}