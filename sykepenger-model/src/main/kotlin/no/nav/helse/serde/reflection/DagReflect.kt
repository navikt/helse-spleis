package no.nav.helse.serde.reflection

import no.nav.helse.serde.PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType
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
        map["type"] = dag.toJsonType()
        map["kilde"] = kilde.toJson()
        dag.maybe<Økonomi?>("økonomi")?.let { økonomi ->
            map.putAll(serialiserØkonomi(økonomi))
        }
        map.compute("melding") { _, _ -> melding }
    }

private fun SykdomstidslinjeHendelse.Hendelseskilde.toJson() = mapOf(
    "type" to toString(),
    "id" to meldingsreferanseId(),
    "tidsstempel" to tidsstempel()
)

private fun Dag.toJsonType() = when (this) {
    is Dag.Sykedag -> JsonDagType.SYKEDAG
    is Dag.UkjentDag -> JsonDagType.UKJENT_DAG
    is Dag.Arbeidsdag -> JsonDagType.ARBEIDSDAG
    is Dag.Arbeidsgiverdag -> JsonDagType.ARBEIDSGIVERDAG
    is Dag.Feriedag -> JsonDagType.FERIEDAG
    is Dag.FriskHelgedag -> JsonDagType.FRISK_HELGEDAG
    is Dag.ArbeidsgiverHelgedag -> JsonDagType.ARBEIDSGIVERDAG
    is Dag.ForeldetSykedag -> JsonDagType.FORELDET_SYKEDAG
    is Dag.SykHelgedag -> JsonDagType.SYKEDAG
    is Dag.Permisjonsdag -> JsonDagType.PERMISJONSDAG
    is Dag.ProblemDag -> JsonDagType.PROBLEMDAG
    is Dag.AvslåttDag -> JsonDagType.AVSLÅTT_DAG
}
