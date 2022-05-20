package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.PersonHendelse

class Migrate(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, Aktivitetslogg())
