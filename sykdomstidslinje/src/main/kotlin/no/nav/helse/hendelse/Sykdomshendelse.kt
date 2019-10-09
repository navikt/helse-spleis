package no.nav.helse.hendelse

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDate
import java.time.LocalDateTime

interface Sykdomshendelse: Comparable<Sykdomshendelse> {
    fun aktørId(): String
    fun rapportertdato(): LocalDateTime
    fun organisasjonsnummer(): String?
    fun sykdomstidslinje(): Sykdomstidslinje
    fun toJson(): JsonNode

    override fun compareTo(other: Sykdomshendelse) = this.rapportertdato().compareTo(other.rapportertdato())
}

interface Event {
    enum class Type {
        SendtSykepengesøknad,
        NySykepengesøknad,
        Inntektsmelding
    }

    fun eventType(): Type
}

fun JsonNode.safelyUnwrapDate(): LocalDate? {
    return if (isNull) {
        null
    } else {
        LocalDate.parse(textValue())
    }
}
