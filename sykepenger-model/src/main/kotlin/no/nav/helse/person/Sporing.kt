package no.nav.helse.person

import java.util.*

internal data class Sporing(private val id: UUID, private val type: Type) {

    companion object {
        internal fun Iterable<Sporing>.toMap() = fold(mutableMapOf<UUID, Type>()) { acc, sporingsId -> acc[sporingsId.id] = sporingsId.type; acc}
        internal fun Iterable<Sporing>.ider() = map { it.id }.toSet()
        internal fun Map<UUID, Type>.tilSporing() = map { Sporing(it.key, it.value) }.toSet()
    }

    internal enum class Type {
        Sykmelding,
        Søknad,
        Inntektsmelding,
        OverstyrTidslinje,
        OverstyrInntekt,
        OverstyrArbeidsforhold,
    }

    internal fun toMap() = mapOf(id.toString() to type.name)
}

