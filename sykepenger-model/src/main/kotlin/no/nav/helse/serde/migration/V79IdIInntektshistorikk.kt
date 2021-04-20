package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.*

internal class V79IdIInntektshistorikk : JsonMigration(version = 79) {
    override val description: String = "Legger på ID i inntektshistorikk for innslag og opplysninger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"]
            .flatMap { it["inntektshistorikk"] }
            .onEach {
                it as ObjectNode
                it.put("id", UUID.randomUUID().toString())
            }
            .flatMap { it["inntektsopplysninger"] }
            .groupBy { it.hendelseId() }
            .forEach { (_, innslag) ->
                val id = UUID.randomUUID().toString()
                innslag.forEach {
                    it as ObjectNode
                    it.put("id", id)
                }
            }
    }

    private fun JsonNode.hendelseId() = (path("skatteopplysninger").firstOrNull() ?: this).get("hendelseId").asText()
}
