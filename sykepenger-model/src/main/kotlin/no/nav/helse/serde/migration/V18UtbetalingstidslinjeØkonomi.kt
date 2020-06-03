package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V18UtbetalingstidslinjeØkonomi : JsonMigration(version = 18) {
    override val description = "befolke økonomifelt i Utbetalalingstidslinjer"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                periode.path("sykdomshistorikk").forEach { element ->
                    element.path("hendelseSykdomstidslinje").path("dager").forEach { dag ->
                        dag as ObjectNode
                        dag.put("arbeidsgiverBetalingProsent", 100.0)
                    }
                    element.path("beregnetSykdomstidslinje").path("dager").forEach { dag ->
                        dag as ObjectNode
                        dag.put("arbeidsgiverBetalingProsent", 100.0)
                    }
                }
            }
        }
    }
}
