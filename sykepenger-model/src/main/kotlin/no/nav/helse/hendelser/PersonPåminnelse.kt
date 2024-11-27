package no.nav.helse.hendelser

import no.nav.helse.hendelser.Avsender.SYSTEM
import java.time.LocalDateTime
import java.util.*

class PersonPåminnelse(
    meldingsreferanseId: UUID,
) : Hendelse {
    override val behandlingsporing = Behandlingsporing.IngenArbeidsgiver
    override val metadata =
        LocalDateTime.now().let { nå ->
            HendelseMetadata(
                meldingsreferanseId = meldingsreferanseId,
                avsender = SYSTEM,
                innsendt = nå,
                registrert = nå,
                automatiskBehandling = true,
            )
        }
}
