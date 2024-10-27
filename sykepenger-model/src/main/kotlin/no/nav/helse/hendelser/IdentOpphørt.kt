package no.nav.helse.hendelser

import java.util.UUID

class IdentOpphørt(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId) {

}
