package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_18
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_19
import no.nav.helse.utbetalingslinjer.Utbetaling.Vurdering

interface UtbetalingsavgjørelseHendelse {
    fun saksbehandler(): Saksbehandler
    val utbetalingId: UUID
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

val UtbetalingsavgjørelseHendelse.avvist get() = !godkjent
val UtbetalingsavgjørelseHendelse.vurdering
    get() = saksbehandler().vurdering(
        godkjent = godkjent,
        avgjørelsestidspunkt = avgjørelsestidspunkt,
        automatisert = automatisert
    )
private val UtbetalingsavgjørelseHendelse.manueltBehandlet get() = !automatisert
fun UtbetalingsavgjørelseHendelse.valider(aktivitetslogg: IAktivitetslogg) {
    when {
        avvist && manueltBehandlet -> {
            aktivitetslogg.funksjonellFeil(RV_UT_19)
            aktivitetslogg.info("Utbetaling markert som ikke godkjent av saksbehandler ${saksbehandler()} $avgjørelsestidspunkt")
        }

        avvist && automatisert -> {
            aktivitetslogg.funksjonellFeil(RV_UT_18)
            aktivitetslogg.info("Utbetaling markert som ikke godkjent automatisk $avgjørelsestidspunkt")
        }

        godkjent && manueltBehandlet ->
            aktivitetslogg.info("Utbetaling markert som godkjent av saksbehandler ${saksbehandler()} $avgjørelsestidspunkt")

        else ->
            aktivitetslogg.info("Utbetaling markert som godkjent automatisk $avgjørelsestidspunkt")
    }
}
