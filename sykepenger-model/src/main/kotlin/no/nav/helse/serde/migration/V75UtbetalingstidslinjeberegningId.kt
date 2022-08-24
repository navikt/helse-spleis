package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID
import no.nav.helse.person.AktivitetsloggObserver

internal class V75UtbetalingstidslinjeberegningId : JsonMigration(version = 75) {
    override val description: String = "Legger på id Utbetalingstidslinjeberegning"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("beregnetUtbetalingstidslinjer").forEach { element ->
                (element as ObjectNode).put("id", "${UUID.randomUUID()}")
            }
        }
    }
}

