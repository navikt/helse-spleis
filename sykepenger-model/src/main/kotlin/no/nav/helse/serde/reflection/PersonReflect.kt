package no.nav.helse.serde.reflection

import no.nav.helse.person.Person

internal class PersonReflect(person: Person) {
    private val aktørId: String = person.getProp("aktørId")
    private val fødselsnummer: String = person.getProp("fødselsnummer")

    internal fun toMap(): Map<String, Any?> = mapOf(
        "aktørId" to aktørId,
        "fødselsnummer" to fødselsnummer
    )
}
