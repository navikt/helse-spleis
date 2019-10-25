package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import java.time.LocalDate

internal data class JsonDag(
    val type: JsonDagType,
    val dato: LocalDate,
    val hendelseId: String,
    val erstatter: List<JsonDag>
)

enum class JsonDagType(internal val creator: (LocalDate, SykdomstidslinjeHendelse) -> Dag) {
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
