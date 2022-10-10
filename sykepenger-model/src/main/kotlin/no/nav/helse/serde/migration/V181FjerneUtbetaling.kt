package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V181FjerneUtbetaling: JsonMigration(181) {
    private companion object {
        private val feilaktigePerioder = setOf(
            "cabc6b07-7a9a-464e-8e49-fec97f1ad45a",
            "36aff5d0-e911-44c2-a75e-c60946164449",
            "afe08581-b3f5-4998-979a-c40b5674ba97",
            "6a3b53bf-f094-40b9-9ca8-97e70313c49f",
            "eb2f0bd3-1230-4301-9188-614d3c79fddf",
            "ae3e94d0-b76e-4580-96a9-9cb2d26ceb1e",
            "ac5277ed-6bb7-4eea-87ea-82e3e79b6959",
            "bf2ea9dc-f870-4657-9107-27946f475fa5"
        )
        private val feilaktigUtbetaling = "912ef275-556d-4a76-97d3-4e4f558c2a86"
    }
    override val description = "Fikse tildeling av utbetaling"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                val vedtaksperiodeId = vedtaksperiode.path("id").asText()
                if (vedtaksperiodeId in feilaktigePerioder) {
                    val utbetalinger = vedtaksperiode.path("utbetalinger") as ArrayNode
                    utbetalinger.removeAll { utbetaling -> utbetaling.asText() == feilaktigUtbetaling }
                }
            }
        }
    }
}