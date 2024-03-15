package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID

internal class V75UtbetalingstidslinjeberegningId : JsonMigration(version = 75) {
    override val description: String = "Legger på id Utbetalingstidslinjeberegning"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("beregnetUtbetalingstidslinjer").forEach { element ->
                (element as ObjectNode).put("id", "${UUID.randomUUID()}")
            }
        }
    }
}

