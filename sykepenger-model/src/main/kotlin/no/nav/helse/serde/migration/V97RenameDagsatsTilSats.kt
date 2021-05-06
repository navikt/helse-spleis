package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V97RenameDagsatsTilSats : JsonMigration(version = 97) {
    override val description: String = "Endrer navn fra dagsats til sats"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"]
            .flatMap { it["utbetalinger"] }
            .forEach {
                migrerOppdrag(it["arbeidsgiverOppdrag"])
                migrerOppdrag(it["personOppdrag"])
            }
    }

    private fun migrerOppdrag(node: JsonNode) = node["linjer"]
        .forEach {
            it as ObjectNode
            it.set<ObjectNode>("sats", it.remove("dagsats"))
        }
}
