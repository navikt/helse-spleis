package no.nav.helse.sykdomstidslinje.dag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelse.*
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
) {
    fun toHendelse(): Sykdomshendelse = when (enumValueOf<Event.Type>(type)) {
        Event.Type.Inntektsmelding -> Inntektsmelding(json)
        Event.Type.NySykepengesøknad-> NySykepengesøknad(json)
        Event.Type.SendtSykepengesøknad -> SendtSykepengesøknad(json)
    }
}

enum class JsonDagType(internal val creator: (JsonDag) -> Dag) {
    ARBEIDSDAG({ Arbeidsdag(it.dato, it.hendelse.toHendelse()) }),
    EGENMELDINGSDAG({ Egenmeldingsdag(it.dato, it.hendelse.toHendelse()) }),
    FERIEDAG({ Feriedag(it.dato, it.hendelse.toHendelse()) }),
    HELGEDAG({ Helgedag(it.dato, it.hendelse.toHendelse())}),
    PERMISJONSDAG({ Permisjonsdag(it.dato, it.hendelse.toHendelse()) }),
    STUDIEDAG({ Studiedag(it.dato, it.hendelse.toHendelse()) }),
    SYKEDAG( { Sykedag(it.dato, it.hendelse.toHendelse()) }),
    SYK_HELGEDAG({ SykHelgedag(it.dato, it.hendelse.toHendelse()) }),
    UBESTEMTDAG({ Ubestemtdag(it.dato, it.hendelse.toHendelse()) }),
    UTENLANDSDAG({ Utenlandsdag(it.dato, it.hendelse.toHendelse()) })
}
