package no.nav.helse.serde.reflection

import no.nav.helse.serde.mapping.JsonDagType
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.serde.reflection.ReflectInstance.Companion.maybe
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.økonomi.Økonomi

internal fun serialisertSykdomstidslinjedag(
    dag: Dag,
    kilde: SykdomstidslinjeHendelse.Hendelseskilde,
    melding: String? = null
) =
    mutableMapOf<String, Any>().also { map ->
        map["dato"] = dag["dato"]
        map["type"] = dag.toJsonType()
        map["kilde"] = kilde.toJson()
        dag.maybe<Økonomi?>("økonomi")?.let { økonomi ->
            map.putAll(serialiserØkonomi(økonomi))
        }
        map.compute("melding") { _, _ -> melding }
    }

private fun SykdomstidslinjeHendelse.Hendelseskilde.toJson() = mapOf(
    "type" to toString(),
    "id" to meldingsreferanseId()
)

private fun Dag.toJsonType() = when (this) {
    is Dag.Sykedag -> JsonDagType.SYKEDAG
    is Dag.UkjentDag -> JsonDagType.UKJENT_DAG
    is Dag.Arbeidsdag -> JsonDagType.ARBEIDSDAG
    is Dag.Arbeidsgiverdag -> JsonDagType.ARBEIDSGIVERDAG
    is Dag.Feriedag -> JsonDagType.FERIEDAG
    is Dag.FriskHelgedag -> JsonDagType.FRISK_HELGEDAG
    is Dag.ArbeidsgiverHelgedag -> JsonDagType.ARBEIDSGIVER_HELGEDAG
    is Dag.ForeldetSykedag -> JsonDagType.FORELDET_SYKEDAG
    is Dag.SykHelgedag -> JsonDagType.SYK_HELGEDAG
    is Dag.Permisjonsdag -> JsonDagType.PERMISJONSDAG
    is Dag.Studiedag -> JsonDagType.STUDIEDAG
    is Dag.Utenlandsdag -> JsonDagType.UTENLANDSDAG
    is Dag.ProblemDag -> JsonDagType.PROBLEMDAG
}
