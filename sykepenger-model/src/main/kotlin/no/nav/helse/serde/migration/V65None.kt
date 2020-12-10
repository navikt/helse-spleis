package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V65None : JsonMigration(version = 65) {
    override val description: String = "[replaced]"

    override fun doMigration(jsonNode: ObjectNode) { /* replaced by V66 */ }
}
