package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID

internal class V344LeggeTilBeregningId(private val idGenerator:()-> UUID = UUID::randomUUID) : JsonMigration(344) {
    override val description = "Legger til beregningId på alle behandlinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                migrerVedtaksperiode(periode)
            }
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrerVedtaksperiode(forkastet.path("vedtaksperiode"))
            }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        vedtaksperiode.path("behandlinger").forEach { behandling ->
            var trengerNy = true
            var nesteBeregningId:String? = null
            behandling.path("endringer").forEach { endring ->
                val denneUtbetalingId = endring.path("utbetalingId").takeUnless { it.isNull || it.isMissingNode }?.asText()
                endring as ObjectNode
                if (trengerNy) {
                    nesteBeregningId = idGenerator().toString()
                }
                endring.put("beregningId", nesteBeregningId!!)
                trengerNy = denneUtbetalingId != null
            }
        }
    }

}
