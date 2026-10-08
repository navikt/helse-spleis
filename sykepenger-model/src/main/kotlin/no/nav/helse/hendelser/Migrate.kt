package no.nav.helse.hendelser

import no.nav.helse.hendelser.Avsender.SYSTEM
import java.time.LocalDateTime

class Migrate(
    meldingsreferanseId: MeldingsreferanseId,
) : Hendelse {
    override val behandlingsporing = Behandlingsporing.IngenYrkesaktivitet
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
