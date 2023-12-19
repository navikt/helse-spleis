package no.nav.helse.hendelser.utbetaling

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_18
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_19
import no.nav.helse.utbetalingslinjer.Utbetaling.Vurdering

interface Utbetalingsavgjørelse: Hendelse {
    fun relevantUtbetaling(id: UUID): Boolean
    fun relevantVedtaksperiode(id: UUID): Boolean
    fun saksbehandler(): Saksbehandler
    val godkjent: Boolean
    val avgjørelsestidspunkt: LocalDateTime
    val automatisert: Boolean
}
class Saksbehandler(private val ident: String, private val epost: String) {
    override fun toString() = ident
    internal fun vurdering(godkjent: Boolean, avgjørelsestidspunkt: LocalDateTime, automatisert: Boolean) = Vurdering(
        godkjent = godkjent,
        tidspunkt = avgjørelsestidspunkt,
        automatiskBehandling = automatisert,
        ident = ident,
        epost = epost
    )
}
val Utbetalingsavgjørelse.avvist get() = !godkjent
val Utbetalingsavgjørelse.vurdering get() = saksbehandler().vurdering(
    godkjent = godkjent,
    avgjørelsestidspunkt = avgjørelsestidspunkt,
    automatisert = automatisert
)
private val Utbetalingsavgjørelse.manueltBehandlet get() = !automatisert
fun Utbetalingsavgjørelse.valider() {
    when {
        avvist && manueltBehandlet -> {
            funksjonellFeil(RV_UT_19)
            info("Utbetaling markert som ikke godkjent av saksbehandler ${saksbehandler()} $avgjørelsestidspunkt")
        }
        avvist && automatisert -> {
            funksjonellFeil(RV_UT_18)
            info("Utbetaling markert som ikke godkjent automatisk $avgjørelsestidspunkt")
        }
        godkjent && manueltBehandlet ->
            info("Utbetaling markert som godkjent av saksbehandler ${saksbehandler()} $avgjørelsestidspunkt")
        else ->
            info("Utbetaling markert som godkjent automatisk $avgjørelsestidspunkt")
    }
}