package no.nav.helse.serde.reflection

import no.nav.helse.person.Person
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import java.time.LocalDateTime

internal class PersonReflect(person: Person) {
    private val aktørId: String = person["aktørId"]
    private val fødselsnummer: String = person["fødselsnummer"]
    private val opprettet: LocalDateTime = person["opprettet"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "aktørId" to aktørId,
        "fødselsnummer" to fødselsnummer,
        "opprettet" to opprettet
    )

    internal fun toSpeilMap() = toMap()
}
