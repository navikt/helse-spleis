package no.nav.helse.serde.reflection

import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get

internal class AktivitetsloggerReflect(aktivitetslogger: Aktivitetslogger) {
    private val originalMessage: String? = aktivitetslogger["originalMessage"]
    private val aktiviteter: List<ReflectInstance> = aktivitetslogger["Aktivitet", "aktiviteter"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "aktiviteter" to aktiviteter.map { AktivitetReflect(it).toMap() },
        "originalMessage" to originalMessage
    )

    private class AktivitetReflect(aktivitet: ReflectInstance) {
        val alvorlighetsgrad: Enum<*> = aktivitet["alvorlighetsgrad"]
        val melding: String = aktivitet["melding"]
        val tidsstempel: String = aktivitet["tidsstempel"]

        internal fun toMap() = mutableMapOf<String, Any?>(
            "alvorlighetsgrad" to alvorlighetsgrad.name,
            "melding" to melding,
            "tidsstempel" to tidsstempel
        )
    }
}
