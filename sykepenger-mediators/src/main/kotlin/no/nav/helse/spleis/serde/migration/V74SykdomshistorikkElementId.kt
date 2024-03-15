package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID

internal class V74SykdomshistorikkElementId : JsonMigration(version = 74) {
    override val description: String = "Lager id på sykdomshistorikkelement"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("sykdomshistorikk").forEach { element ->
                (element as ObjectNode).put("id", "${UUID.randomUUID()}")
            }
        }
    }
}

