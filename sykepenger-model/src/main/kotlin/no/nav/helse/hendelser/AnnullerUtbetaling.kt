package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.utbetalingslinjer.Utbetaling

class AnnullerUtbetaling(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    override val utbetalingId: UUID,
    private val saksbehandlerIdent: String,
    private val saksbehandlerEpost: String,
    internal val opprettet: LocalDateTime
) : PersonHendelse(), AnnullerUtbetalingHendelse {
    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
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

    fun erAutomatisk() = this.saksbehandlerIdent == "Automatisk behandlet"
}
