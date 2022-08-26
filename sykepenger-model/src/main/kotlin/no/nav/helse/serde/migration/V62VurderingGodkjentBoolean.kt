package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V62VurderingGodkjentBoolean : JsonMigration(version = 62) {
    override val description: String = "Setter godkjent boolean"
    private val godkjenteTilstander = listOf(
        "GODKJENT", "GODKJENT_UTEN_UTBETALING", "SENDT", "OVERFØRT", "UTBETALING_FEILET", "UTBETALT", "ANNULLERT"
    )

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode
            .path("arbeidsgivere")
            .forEach { arbeidsgiver ->
                arbeidsgiver
                    .path("utbetalinger")
                    .filter { it.hasNonNull("vurdering") }
                    .onEach {
                        val godkjent = it.path("status").asText() in godkjenteTilstander
                        (it.path("vurdering") as ObjectNode).put("godkjent", godkjent)
                    }
            }
    }
}

