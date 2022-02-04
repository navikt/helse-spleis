package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.PersonHendelse
import java.util.*

class Infotrygdendring(
    id: UUID,
    fødselsnummer: String,
    aktørId: String
) : PersonHendelse(id, fødselsnummer, aktørId, Aktivitetslogg())
