package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V6LeggerTilGrad : JsonMigration(version = 6) {

    override val description = "Legger til grad på sykedager og utbetalingsdager"

    private val sykdomsdagerMedGrad = listOf("SYKEDAG_SYKMELDING", "SYKEDAG_SØKNAD", "SYK_HELGEDAG")
    private val utbetalingsdagerMedGrad = listOf("ArbeidsgiverperiodeDag", "NavDag", "NavHelgDag")

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                periode.path("sykdomshistorikk").forEach { sykdomshistorikk ->
                    migrerSykdomstidslinjetidslinje(sykdomshistorikk.path("hendelseSykdomstidslinje"))
                    migrerSykdomstidslinjetidslinje(sykdomshistorikk.path("beregnetSykdomstidslinje"))
                }
            }

            arbeidsgiver.path("utbetalingstidslinjer").forEach { utbetalingstidslinje ->
                migrerUtbetalingstidslinje(utbetalingstidslinje.path("dager"))
            }
        }
    }

    private fun migrerSykdomstidslinjetidslinje(tidslinje: JsonNode) {
        tidslinje.forEach { dag ->
            if (dag["type"].textValue() in sykdomsdagerMedGrad) {
                (dag as ObjectNode).put("grad", 100.0)
            }
        }
    }

    private fun migrerUtbetalingstidslinje(tidslinje: JsonNode) {
        tidslinje.forEach { dag ->
            if (dag["type"].textValue() in utbetalingsdagerMedGrad) {
                (dag as ObjectNode).put("grad", 100.0)
            }
        }
    }
}
