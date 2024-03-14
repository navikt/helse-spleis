package no.nav.helse.serde

import no.nav.helse.person.Person

fun Person.serialize(pretty: Boolean = false): SerialisertPerson {
    return dto().tilPersonData().tilSerialisertPerson(pretty)
}
