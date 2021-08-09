package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V112FjernTrailingCarriageReturnFraFagsystemId : JsonMigration(version = 112) {
    override val description: String = "Vi hadde en bug i genereringen av fagsystemId, som gjorde at \\r\\n ble lagt til på slutten"


    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere")
            .flatMap { arbeidsgiver -> arbeidsgiver.path("utbetalinger") }
            .flatMap { listOf(it["arbeidsgiverOppdrag"], it["personOppdrag"]) }
            .trimFagsystemId()

        jsonNode.path("aktivitetslogg").path("aktiviteter")
            .map { it["detaljer"] }
            .filter { it.hasNonNull("fagsystemId") }
            .trimFagsystemId()
    }

    private fun Iterable<JsonNode>.trimFagsystemId() = this
        .onEach {
            val fagsystemId = it["fagsystemId"].textValue()
            if (fagsystemId.endsWith("\n")) (it as ObjectNode).put("fagsystemId", fagsystemId.trim())
        }
        .flatMap { it.path("linjer").takeUnless(JsonNode::isMissingNode) ?: emptyList() }
        .forEach {
            val refFagsystemId = it["refFagsystemId"].takeUnless(JsonNode::isNull)?.textValue() ?: return@forEach
            if (refFagsystemId.endsWith("\n")) (it as ObjectNode).put("refFagsystemId", refFagsystemId.trim())
        }
}
