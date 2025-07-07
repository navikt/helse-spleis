package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SYSTEM

// Dette er en syntetisk hendelse vi generer for å forkaste vedtaksperioder der vi ikke sender noe til OS, og ikke får kvittering
class AnnullerTomUtbetaling(
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
) : Hendelse, SyntetiskHendelse {
    override val metadata = HendelseMetadata(
        meldingsreferanseId = MeldingsreferanseId(UUID.fromString("00000000-0000-0000-0000-000000000000")),
        avsender = SYSTEM,
        innsendt = LocalDateTime.now(),
        registrert = LocalDateTime.now(),
        automatiskBehandling = true
    )
}

interface SyntetiskHendelse
