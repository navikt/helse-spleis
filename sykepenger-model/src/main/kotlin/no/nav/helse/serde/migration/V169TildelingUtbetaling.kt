package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V169TildelingUtbetaling: JsonMigration(169) {
    private companion object {
        private val utbetalinger = mapOf(
            "390ee2a3-a2f9-4dd4-a30b-ea16affbd2ad" to setOf(
                "479de83b-0c87-4d26-ba99-b109764ac823",
                "3be0f9f4-3e4c-4f0a-a5e1-5d99f68c3c3b"
            ),
            "0c24c57f-52a1-4b78-a0c2-0099b65e0cd3" to setOf(
                "1b2ade5f-dbb1-4114-97a2-ec6a92644090",
                "f7b9f8de-8150-4d23-b087-ab2666144061"
            ),
            "0fb7b67f-e121-4867-8dc0-068e2070cef3" to setOf(
                "f3e6807d-797a-4b34-9daf-8bd6c78f5084"
            )
        )
    }
    override val description = "Fikse tildeling av utbetaling"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                val vedtaksperiodeId = vedtaksperiode.path("id").asText()
                val utbetalingId = utbetalinger.entries.firstOrNull { (_, vedtaksperioder) -> vedtaksperiodeId in vedtaksperioder }
                if (utbetalingId != null) (vedtaksperiode.path("utbetalinger") as ArrayNode).add(utbetalingId.key)
            }
        }
    }
}