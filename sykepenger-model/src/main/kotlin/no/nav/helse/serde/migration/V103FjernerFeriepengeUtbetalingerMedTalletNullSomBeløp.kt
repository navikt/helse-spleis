package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.AktivitetsloggObserver

internal class V103FjernerFeriepengeUtbetalingerMedTalletNullSomBeløp : JsonMigration(version = 103) {
    override val description: String = "Fjerner sendte feriepengeutbetalinger med 0 som beløp"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        jsonNode["arbeidsgivere"]
            .map { it["feriepengeutbetalinger"] }
            .filterIsInstance<ArrayNode>()
            .forEach { feriepengeutbetalinger ->
                feriepengeutbetalinger.removeAll {
                    it["oppdrag"]["nettoBeløp"].asInt() == 0 && it["sendTilOppdrag"].booleanValue()
                }
            }
    }
}
