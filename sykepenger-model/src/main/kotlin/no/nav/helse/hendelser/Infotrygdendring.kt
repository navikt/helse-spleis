package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.PersonHendelse
import java.util.*

class Infotrygdendring(
    id: UUID,
    private val fødselsnummer: String,
    private val aktørId: String
) : PersonHendelse(id, Aktivitetslogg()) {

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
}
