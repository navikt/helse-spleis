package no.nav.helse.serde.reflection

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import java.util.*

internal class ArbeidsgiverReflect(arbeidsgiver: Arbeidsgiver) {
    private val organisasjonsnummer: String = arbeidsgiver["organisasjonsnummer"]
    private val id: UUID = arbeidsgiver["id"]

    internal fun toMap(): Map<String, Any?> = mapOf(
        "organisasjonsnummer" to organisasjonsnummer,
        "id" to id
    )

    internal fun toSpeilMap() = toMap()
}
