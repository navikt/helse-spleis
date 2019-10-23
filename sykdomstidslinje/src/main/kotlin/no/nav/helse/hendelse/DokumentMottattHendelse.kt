package no.nav.helse.hendelse

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

interface Sykepengehendelse {

}

interface DokumentMottattHendelse: Sykepengehendelse, Comparable<DokumentMottattHendelse> {
    enum class Type {
        SendtSøknadMottatt,
        NySøknadOpprettet,
        InntektsmeldingMottatt,
        Sykepengehistorikk
    }

    fun hendelsetype(): Type
    fun aktørId(): String
    fun rapportertdato(): LocalDateTime
    fun organisasjonsnummer(): String?
    fun sykdomstidslinje(): Sykdomstidslinje
    fun hendelseId(): String

    fun toJson(): JsonNode
    override fun compareTo(other: DokumentMottattHendelse) = this.rapportertdato().compareTo(other.rapportertdato())
    override fun equals(other: Any?): Boolean
}

fun JsonNode.safelyUnwrapDate(): LocalDate? {
    return if (isNull) {
        null
    } else {
        LocalDate.parse(textValue())
    }
}
