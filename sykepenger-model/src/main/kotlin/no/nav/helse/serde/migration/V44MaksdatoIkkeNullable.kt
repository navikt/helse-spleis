package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate

internal class V44MaksdatoIkkeNullable : JsonMigration(version = 44) {
    override val description: String = "gjør maksdato til ikke-null vha LocalDate.MAX som nullobjekt"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            settMaksdatoDefault(arbeidsgiver.path("vedtaksperioder"))
            settMaksdatoDefaultForkastede(arbeidsgiver.path("forkastede"))
        }
    }

    private fun settMaksdatoDefault(perioder: JsonNode) {
        perioder.forEach(::migrer)
    }

    private fun settMaksdatoDefaultForkastede(perioder: JsonNode) {
        perioder.forEach { periode ->
            migrer(periode.path("vedtaksperiode"))
        }
    }

    private fun migrer(periode: JsonNode) {
        periode as ObjectNode
        if (!periode.path("maksdato").isTextual)
            periode.put("maksdato", "${LocalDate.MAX}")
    }
}
