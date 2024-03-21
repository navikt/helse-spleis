package no.nav.helse.hendelser.utbetaling

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.ArbeidstakerHendelse
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.utbetalingslinjer.Utbetaling

class AnnullerUtbetaling(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    private val fagsystemId: String?,
    private val utbetalingId: UUID?,
    private val saksbehandlerIdent: String,
    private val saksbehandlerEpost: String,
    internal val opprettet: LocalDateTime,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, aktivitetslogg) {

    init {
        check(fagsystemId != null || utbetalingId != null) {
            "fagsystemId eller utbetalingId må være satt"
        }
    }

    fun erRelevant(utbetalingId: UUID, fagsystemId: String) =
        utbetalingId == this.utbetalingId || this.fagsystemId == fagsystemId

    fun erAutomatisk() = this.saksbehandlerIdent == "Automatisk behandlet"

    fun vurdering() = Utbetaling.Vurdering(
        true,
        saksbehandlerIdent,
        saksbehandlerEpost,
        opprettet,
        false
    )

    override fun avsender() = SAKSBEHANDLER
}
