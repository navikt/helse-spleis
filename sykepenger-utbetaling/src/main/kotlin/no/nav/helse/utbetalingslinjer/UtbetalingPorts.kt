import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetalingstatus

interface UtbetalingHendelsePort: OverføringsinformasjonPort {
    fun valider()
    fun skalForsøkesIgjen(): Boolean
}

interface GrunnbeløpsreguleringPort: IAktivitetslogg {
    fun erRelevant(fagsystemId: String): Boolean
    fun fødselsnummer(): String
    fun organisasjonsnummer(): String
}

interface OverføringsinformasjonPort: IAktivitetslogg {
    val avstemmingsnøkkel: Long
    val overføringstidspunkt: LocalDateTime
    val status: Oppdragstatus
    fun erRelevant(fagsystemId: String): Boolean
    fun erRelevant(arbeidsgiverFagsystemId: String, personFagsystemId: String, utbetaling: UUID): Boolean
}

interface SimuleringPort: IAktivitetslogg {
    val simuleringResultat: SimuleringResultat?
    fun valider(oppdrag: Oppdrag): SimuleringPort
    fun erRelevantFor(fagområde: Fagområde, fagsystemId: String): Boolean
    fun erRelevantForUtbetaling(id: UUID): Boolean
}

interface UtbetalingpåminnelsePort: IAktivitetslogg {
    fun erRelevant(id: UUID): Boolean
    fun harOversteget(makstid: Duration): Boolean
    fun gjelderStatus(tilstand: Utbetalingstatus): Boolean
}