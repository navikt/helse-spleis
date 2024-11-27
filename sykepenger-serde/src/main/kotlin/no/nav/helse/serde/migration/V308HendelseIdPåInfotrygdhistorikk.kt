package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V308HendelseIdPåInfotrygdhistorikk : JsonMigration(version = 308) {
    override val description = "lager en default id på infotrygdhistorikk uten hendelseId"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier
    ) {
        jsonNode.path("infotrygdhistorikk").forEach { historikk ->
            if (!historikk.hasNonNull("hendelseId")) {
                (historikk as ObjectNode).put("hendelseId", "00000000-0000-0000-0000-000000000000")
            }
        }
    }
}
