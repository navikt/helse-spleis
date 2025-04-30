package no.nav.helse.hendelser

import java.time.LocalDateTime
import no.nav.helse.hendelser.Avsender.SYSTEM

class PersonPåminnelse(meldingsreferanseId: MeldingsreferanseId) : Hendelse {
    override val behandlingsporing = Behandlingsporing.IngenYrkesaktivitet
    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SYSTEM,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = true
        )
    }

}
