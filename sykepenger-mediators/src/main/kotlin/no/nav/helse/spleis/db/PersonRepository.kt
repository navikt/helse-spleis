package no.nav.helse.spleis.db

import no.nav.helse.person.Person

internal interface PersonRepository {

    fun hentPerson(aktørId: String): Person?

}
