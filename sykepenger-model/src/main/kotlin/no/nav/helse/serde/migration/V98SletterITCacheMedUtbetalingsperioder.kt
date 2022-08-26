package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.serdeObjectMapper

internal class V98SletterITCacheMedUtbetalingsperioder : JsonMigration(version = 98) {
    override val description: String = "Endrer navn fra dagsats til sats"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val infotrygdHistorikkElementerForSletting = mutableListOf<JsonNode>()

        jsonNode["infotrygdhistorikk"].forEach { historikkElement ->
            if (historikkElement["utbetalingsperioder"].isEmpty) {
                (historikkElement as ObjectNode).set<ArrayNode>("arbeidsgiverutbetalingsperioder", serdeObjectMapper.createArrayNode())
                historikkElement.set<ArrayNode>("personutbetalingsperioder", serdeObjectMapper.createArrayNode())
                historikkElement.remove("utbetalingsperioder")
            } else {
                infotrygdHistorikkElementerForSletting.add(historikkElement)
            }
        }

        jsonNode["infotrygdhistorikk"].removeAll { it in infotrygdHistorikkElementerForSletting }
    }
}
