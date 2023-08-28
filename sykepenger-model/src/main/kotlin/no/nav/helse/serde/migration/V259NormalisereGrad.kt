package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

internal class V259NormalisereGrad : JsonMigration(version = 259) {
    override val description = "normaliserer grad til å være double"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("infotrygdhistorikk").forEach { element ->
            element.path("arbeidsgiverutbetalingsperioder").forEach { periode ->
                migrer(periode)
            }
            element.path("personutbetalingsperioder").forEach { periode ->
                migrer(periode)
            }
        }
    }

    private fun migrer(periode: JsonNode) {
        periode as ObjectNode
        val grad = periode.path("grad").asDouble()
        periode.put("grad", grad)
        val inntekt = periode.path("inntekt").asDouble().månedlig.rundTilDaglig()
        val daglig = inntekt.reflection { _, _, _, dagligInt -> dagligInt }
        periode.put("inntekt", daglig)
    }
}