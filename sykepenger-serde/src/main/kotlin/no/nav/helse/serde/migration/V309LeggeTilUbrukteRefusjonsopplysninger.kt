package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V309LeggeTilUbrukteRefusjonsopplysninger : JsonMigration(version = 309) {
    override val description = "legger til ubrukte refusjonsopplysninger på arberidsgivere"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier
    ) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver as ObjectNode
            arbeidsgiver.putObject("ubrukteRefusjonsopplysninger")
        }
    }
}
