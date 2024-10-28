package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.hendelser.Avsender.SYSTEM

class PersonPåminnelse(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String
) : PersonHendelse() {
    override val behandlingsporing = Behandlingsporing.Person(
        fødselsnummer = fødselsnummer,
        aktørId = aktørId
    )
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
