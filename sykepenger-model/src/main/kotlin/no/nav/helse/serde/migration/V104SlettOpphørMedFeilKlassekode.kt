package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V104SlettOpphørMedFeilKlassekode : JsonMigration(version = 104) {
    override val description: String = "Sletter utbetalinger ved opphør med feil klassekode"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"]
            .map { it["feriepengeutbetalinger"] }
            .filterIsInstance<ArrayNode>()
            .forEach { feriepengeutbetalinger ->
                feriepengeutbetalinger.removeAll {
                    it["oppdrag"]["linjer"].single()["statuskode"].asText() == "OPPH" &&
                    it["oppdrag"]["linjer"].single()["klassekode"].asText() == "SPREFAG-IOP"
                }
            }
    }
}
