package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.Behandlinger
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg

/* trigger at vi omfordeler masse opplysninger fra Refusjonshistorikk og inn i relevante behandlinger på relevante vedtaksperioder
    og at vi sparer på eventuelle rester til senere bruk
 */
class OmfordelRefusjonsopplysninger(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, Aktivitetslogg()) {
}