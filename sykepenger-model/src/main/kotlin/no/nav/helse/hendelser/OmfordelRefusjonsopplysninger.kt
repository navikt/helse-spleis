package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SYSTEM

/* trigger at vi omfordeler masse opplysninger fra Refusjonshistorikk og inn i relevante behandlinger på relevante vedtaksperioder
    og at vi sparer på eventuelle rester til senere bruk
 */
class OmfordelRefusjonsopplysninger(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String
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

}