package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID

internal class V175IdPåAktiviteter : JsonMigration(version = 175) {
    override val description = """Legg inn unike ider på alle aktiviteter"""

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktiviteter = jsonNode.path("aktivitetslogg").path("aktiviteter")
        aktiviteter.forEach {
            (it as ObjectNode).put("id", UUID.randomUUID().toString())
        }
    }
}