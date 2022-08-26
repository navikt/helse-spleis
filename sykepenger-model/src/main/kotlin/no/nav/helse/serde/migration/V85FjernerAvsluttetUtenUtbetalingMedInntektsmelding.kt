package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V85FjernerAvsluttetUtenUtbetalingMedInntektsmelding : JsonMigration(version = 85) {
    override val description: String = "Fjerner AvsluttetUtenUtbetalingMedInntektsmelding"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"].flatMap { arbeidsgiver -> arbeidsgiver["vedtaksperioder"] }.byttTilstand()
        jsonNode["arbeidsgivere"].flatMap { arbeidsgiver -> arbeidsgiver["forkastede"] }.map { forkastet -> forkastet["vedtaksperiode"] }.byttTilstand()
    }

    private fun Iterable<JsonNode>.byttTilstand() = this
        .filter { it["tilstand"].textValue() == "AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING" }
        .forEach { (it as ObjectNode).put("tilstand", "AVSLUTTET_UTEN_UTBETALING") }
}
