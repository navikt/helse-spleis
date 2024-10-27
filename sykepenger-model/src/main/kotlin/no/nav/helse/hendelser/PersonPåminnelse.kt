package no.nav.helse.hendelser

import java.util.*

class PersonPåminnelse(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId)
