package no.nav.helse.sykdomstidslinje.dag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelse.*
import java.lang.RuntimeException
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
    fun toHendelse(): DokumentMottattHendelse = when (type) {
        DokumentMottattHendelse.Type.InntektsmeldingMottatt.name -> InntektsmeldingMottatt(json)
        DokumentMottattHendelse.Type.NySøknadOpprettet.name -> NySøknadOpprettet(json)
        DokumentMottattHendelse.Type.SendtSøknadMottatt.name -> SendtSøknadMottatt(json)
        Sykepengehistorikk::javaClass.name -> Sykepengehistorikk(json)
        else -> throw RuntimeException("ukjent type")
    }
}

enum class JsonDagType(internal val creator: (JsonDag) -> Dag) {
    ARBEIDSDAG({ Arbeidsdag(it.dato, it.hendelse.toHendelse()) }),
    EGENMELDINGSDAG({ Egenmeldingsdag(it.dato, it.hendelse.toHendelse()) }),
    FERIEDAG({ Feriedag(it.dato, it.hendelse.toHendelse()) }),
    IMPLISITT_DAG({ ImplisittDag(it.dato, it.hendelse.toHendelse()) }),
    PERMISJONSDAG({ Permisjonsdag(it.dato, it.hendelse.toHendelse()) }),
    STUDIEDAG({ Studiedag(it.dato, it.hendelse.toHendelse()) }),
    SYKEDAG({ Sykedag(it.dato, it.hendelse.toHendelse()) }),
    SYK_HELGEDAG({ SykHelgedag(it.dato, it.hendelse.toHendelse()) }),
    UBESTEMTDAG({ Ubestemtdag(it.dato, it.hendelse.toHendelse()) }),
    UTENLANDSDAG({ Utenlandsdag(it.dato, it.hendelse.toHendelse()) })
}
