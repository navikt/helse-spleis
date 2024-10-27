package no.nav.helse.hendelser

import java.util.UUID

class Infotrygdendring(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId)
