package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.hendelser.Avsender.SYSTEM

class Dødsmelding(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    private val dødsdato: LocalDate
) : PersonHendelse(fødselsnummer, aktørId) {
    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SYSTEM,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = true
        )
    }

    internal fun dødsdato(alder: Alder) = alder.medDød(this.dødsdato)
}
