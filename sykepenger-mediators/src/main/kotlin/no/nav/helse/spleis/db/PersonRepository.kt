package no.nav.helse.spleis.db

import no.nav.helse.person.Person
import java.util.*

internal interface PersonRepository {
    fun hentPerson(fødselsnummer: String): Person?
}
