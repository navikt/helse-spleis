package no.nav.helse.person

import no.nav.helse.person.domain.Person

internal interface PersonRepository {

    fun hentPerson(aktørId: String): Person?

    fun hentPersonJson(aktørId: String): String?

}
