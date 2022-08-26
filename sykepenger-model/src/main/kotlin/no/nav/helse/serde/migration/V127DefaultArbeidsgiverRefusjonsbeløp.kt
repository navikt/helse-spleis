package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V127DefaultArbeidsgiverRefusjonsbeløp : JsonMigration(version = 127) {

    override val description = "Setter defaultvalue på arbeidsgiverRefusjonsbeløp"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger").forEach { utbetaling ->
                migrerTidslinje(utbetaling.path("utbetalingstidslinje"))
            }
            arbeidsgiver.path("beregnetUtbetalingstidslinjer").forEach { beregning ->
                migrerTidslinje(beregning.path("utbetalingstidslinje"))
            }
            arbeidsgiver.path("sykdomshistorikk").forEach { sykdomshistorikk ->
                migrerTidslinje(sykdomshistorikk.path("hendelseSykdomstidslinje"))
                migrerTidslinje(sykdomshistorikk.path("beregnetSykdomstidslinje"))
            }

            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                migrerTidslinje(vedtaksperiode.path("utbetalingstidslinje"))
                migrerTidslinje(vedtaksperiode.path("sykdomstidslinje"))
            }
        }
    }

    private fun migrerTidslinje(tidslinje: JsonNode) {
        tidslinje.path("dager")
            .filter { it.path("arbeidsgiverRefusjonsbeløp").let { it.isNull || it.isMissingNode }}
            .forEach { dag ->
                dag as ObjectNode
                val refusjonsbeløp = dag.path("aktuellDagsinntekt").takeUnless { it.isNull || it.isMissingNode }?.asDouble() ?: 0.0
                dag.put("arbeidsgiverRefusjonsbeløp", refusjonsbeløp)
            }
    }
}
