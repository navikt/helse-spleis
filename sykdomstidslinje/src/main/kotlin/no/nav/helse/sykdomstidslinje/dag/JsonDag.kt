package no.nav.helse.sykdomstidslinje.dag

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate

internal data class JsonDag(
    val type: JsonDagType,
    val dato: LocalDate,
    val hendelse: JsonHendelse,
    val erstatter: List<JsonDag>
)

internal data class JsonHendelse(
    val type: String,
    val json: JsonNode
)

enum class JsonDagType {
    ARBEIDSDAG,
    EGENMELDINGSDAG,
    FERIEDAG,
    HELGEDAG,
    NULLDAG,
    PERMISJONSDAG,
    STUDIEDAG,
    SYKEDAG,
    SYK_HELGEDAG,
    UBESTEMTDAG,
    UTENLANDSDAG,
    FYLLDAG
}
