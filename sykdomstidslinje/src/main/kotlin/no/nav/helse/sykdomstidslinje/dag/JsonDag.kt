package no.nav.helse.sykdomstidslinje.dag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelse.*
import java.time.LocalDate

internal data class JsonDag(
    val type: JsonDagType,
    val dato: LocalDate,
    val hendelse: JsonHendelsesReferanse,
    val erstatter: List<JsonDag>
)

internal data class JsonHendelsesReferanse(
    val type: String,
    val hendelseid: String
)

//internal data class JsonHendelse(
//    val type: String,
//    val json: JsonNode
//) {
//    fun toHendelse(): Sykdomshendelse = when (enumValueOf<Sykdomshendelse.Type>(type)) {
//        Sykdomshendelse.Type.Inntektsmelding -> Inntektsmelding(json)
//        Sykdomshendelse.Type.NySykepengesøknad -> NySykepengesøknad(json)
//        Sykdomshendelse.Type.SendtSykepengesøknad -> SendtSykepengesøknad(json)
//        Sykdomshendelse.Type.Sykepengehistorikk -> Sykepengehistorikk(json)
//    }
//}

enum class JsonDagType(internal val creator: (LocalDate, Sykdomshendelse) -> Dag) {
    ARBEIDSDAG({ dato, hendelse -> Arbeidsdag(dato, hendelse) }),
    EGENMELDINGSDAG({ dato, hendelse -> Egenmeldingsdag(dato, hendelse) }),
    FERIEDAG({ dato, hendelse -> Feriedag(dato, hendelse) }),
    IMPLISITT_DAG({ dato, hendelse -> ImplisittDag(dato, hendelse) }),
    PERMISJONSDAG({ dato, hendelse -> Permisjonsdag(dato, hendelse) }),
    STUDIEDAG({ dato, hendelse -> Studiedag(dato, hendelse) }),
    SYKEDAG({ dato, hendelse -> Sykedag(dato, hendelse) }),
    SYK_HELGEDAG({ dato, hendelse -> SykHelgedag(dato, hendelse) }),
    UBESTEMTDAG({ dato, hendelse -> Ubestemtdag(dato, hendelse) }),
    UTENLANDSDAG({ dato, hendelse -> Utenlandsdag(dato, hendelse) })
}
