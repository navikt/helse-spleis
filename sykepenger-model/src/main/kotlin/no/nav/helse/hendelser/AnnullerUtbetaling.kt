package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.utbetalingslinjer.Utbetaling

class AnnullerUtbetaling(
    meldingsreferanseId: MeldingsreferanseId,
    organisasjonsnummer: String,
    override val utbetalingId: UUID,
    private val saksbehandlerIdent: String,
    saksbehandlerEpost: String,
    internal val opprettet: LocalDateTime
) : Hendelse, AnnullerUtbetalingHendelse {
    override val behandlingsporing = Behandlingsporing.Arbeidstaker(
        organisasjonsnummer = organisasjonsnummer
    )
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = SAKSBEHANDLER,
        innsendt = opprettet,
        registrert = LocalDateTime.now(),
        automatiskBehandling = erAutomatisk()
    )

    override val vurdering: Utbetaling.Vurdering = Utbetaling.Vurdering(true, saksbehandlerIdent, saksbehandlerEpost, opprettet, false)

    private fun erAutomatisk() = this.saksbehandlerIdent == "Automatisk behandlet"
}
