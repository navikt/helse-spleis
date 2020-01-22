package no.nav.helse.serde.reflection

import no.nav.helse.person.Arbeidsgiver
import java.util.*

internal class ArbeidsgiverReflect(arbeidsgiver: Arbeidsgiver) {
    private val organisasjonsnummer: String = arbeidsgiver.getProp("organisasjonsnummer")
    private val id: UUID = arbeidsgiver.getProp("id")

    internal fun toMap(): Map<String, Any?> = mapOf(
        "organisasjonsnummer" to organisasjonsnummer,
        "id" to id
    )
}
