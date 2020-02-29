package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V6LeggerTilGrad : JsonMigration(version = 6) {

    override val description = "Legger til grad på sykedager og utbetalingsdager"

    private val dagerMedGrad = listOf("SYKEDAG_SYKMELDING", "SYKEDAG_SØKNAD", "SYK_HELGEDAG")

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                periode.path("sykdomshistorikk").forEach { sykdomshistorikk ->
                    migrerTidslinje(sykdomshistorikk.path("hendelseSykdomstidslinje"))
                    migrerTidslinje(sykdomshistorikk.path("beregnetSykdomstidslinje"))
                }
            }

            arbeidsgiver.path("utbetalingstidslinjer").forEach { utbetalingstidslinje ->
                migrerTidslinje(utbetalingstidslinje.path("dager"))
            }
        }
    }

    private fun migrerTidslinje(tidslinje: JsonNode) {
        tidslinje.forEach { dag ->
            if (dag["type"].textValue() in dagerMedGrad) {
                (dag as ObjectNode).put("grad", 100.0)
            }
        }
    }
}
