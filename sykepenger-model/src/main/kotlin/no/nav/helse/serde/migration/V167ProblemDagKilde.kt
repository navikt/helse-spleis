package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.serde.serdeObjectMapper
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse

internal class V167ProblemDagKilde: JsonMigration(167) {
    private companion object {
        private val ingenKilde = SykdomshistorikkHendelse.Hendelseskilde(SykdomshistorikkHendelse::class, UUID.randomUUID(), LocalDateTime.now()).let {
            serdeObjectMapper.convertValue<ObjectNode>(it.toJson())
        }
        private val sammeKilde = { dag: JsonNode -> (dag.path("kilde") as ObjectNode) }
        private const val problemdag = "PROBLEMDAG"
    }

    override val description = "ProblemDag peker på begge kildene"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val problemdager = mutableMapOf<LocalDate, ObjectNode>()

            arbeidsgiver.path("sykdomshistorikk").reversed().forEach { element ->
                val hendelseSykdomstidslinje = element.path("hendelseSykdomstidslinje")
                val beregnetTidslinje = element.path("beregnetSykdomstidslinje")

                migrerProblemdager(problemdager, hendelseSykdomstidslinje, sammeKilde)
                migrerProblemdager(problemdager, beregnetTidslinje) { dag ->
                    val dato = dag.førsteDag()

                    hendelseSykdomstidslinje.path("dager")
                        .firstOrNull { otherDag -> otherDag.førsteDag() == dato }
                        ?.takeIf { it.path("type").asText() == problemdag }
                        ?.path("kilde") as? ObjectNode
                }

                beregnetTidslinje.path("dager").forEach { dag ->
                    problemdager[dag.førsteDag()] = dag.path("kilde") as ObjectNode
                }
            }

            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                vedtaksperiode.path("sykdomstidslinje").path("dager")
                    .filter { it.path("type").asText() == problemdag }
                    .forEach { dag ->
                        val kilde = problemdager[dag.førsteDag()] ?: ingenKilde
                        (dag as ObjectNode).putObject("other").apply {
                            setAll<ObjectNode>(kilde)
                        }
                    }
            }

            arbeidsgiver.path("forkastede").forEach { forkastet ->
                forkastet.path("vedtaksperiode").path("sykdomstidslinje").path("dager")
                    .filter { it.path("type").asText() == problemdag }
                    .forEach { dag ->
                        val kilde = problemdager[dag.førsteDag()] ?: ingenKilde
                        (dag as ObjectNode).putObject("other").apply {
                            setAll<ObjectNode>(kilde)
                        }
                    }
            }
        }
    }

    private fun JsonNode.førsteDag() =
        LocalDate.parse((path("dato").takeIf { it.isTextual } ?: path("fom")).asText())

    private fun migrerProblemdager(problemdager: MutableMap<LocalDate, ObjectNode>, tidslinje: JsonNode, annenKildeStrategi: (dag: JsonNode) -> ObjectNode?) {
        tidslinje.path("dager")
            .filter { dag ->
                dag.path("type").asText() == problemdag
            }
            .forEach { dag ->
                val annenKilde = problemdager.getOrPut(dag.førsteDag()) { annenKildeStrategi(dag) ?: ingenKilde }.deepCopy()
                (dag as ObjectNode).putObject("other").apply {
                    setAll<ObjectNode>(annenKilde)
                }
            }
    }
}