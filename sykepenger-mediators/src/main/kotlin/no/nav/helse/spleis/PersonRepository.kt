package no.nav.helse.spleis

import no.nav.helse.person.Person

internal interface PersonRepository {

    fun hentPerson(aktørId: String): Person?

}
