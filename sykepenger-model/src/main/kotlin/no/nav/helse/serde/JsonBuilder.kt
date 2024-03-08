package no.nav.helse.serde

import no.nav.helse.person.Person

fun Person.serialize(pretty: Boolean = false) = tilSerialisertPerson(pretty)
