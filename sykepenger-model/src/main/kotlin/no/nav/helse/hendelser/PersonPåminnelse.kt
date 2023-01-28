package no.nav.helse.hendelser

import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import java.util.*

class PersonPåminnelse(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, Aktivitetslogg())
