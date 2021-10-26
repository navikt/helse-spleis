package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.Grunnbeløp
import java.time.LocalDate

internal class V118LeggTilBegrensningPåSykepengegrunnlag : JsonMigration(version = 118) {

    override val description: String = "Legg til begrensning på sykepengegrunnlag"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["vilkårsgrunnlagHistorikk"].forEach { historikk ->
            historikk["vilkårsgrunnlag"].forEach { vilkårsgrunnlag ->
                val sykepengegrunnlag = vilkårsgrunnlag.get("sykepengegrunnlag")
                val grunnlagForSykepengegrunnlag = sykepengegrunnlag.get("grunnlagForSykepengegrunnlag").asDouble()
                val skjæringstidspunkt: LocalDate = vilkårsgrunnlag.get("skjæringstidspunkt").asLocalDate()
                val objectNode = sykepengegrunnlag as ObjectNode
                val er6Gbegrenset = grunnlagForSykepengegrunnlag > Grunnbeløp.`6G`.beløp(skjæringstidspunkt).reflection { årlig, _, _, _ -> årlig }

                if (vilkårsgrunnlag.get("type").asText() == "Infotrygd") {
                    objectNode.put("begrensning", "VURDERT_I_INFOTRYGD")
                } else if (er6Gbegrenset) {
                    objectNode.put("begrensning", "ER_6G_BEGRENSET")
                } else {
                    objectNode.put("begrensning", "ER_IKKE_6G_BEGRENSET")
                }
            }
        }
    }

    private fun JsonNode.asLocalDate(): LocalDate =
        asText().let { LocalDate.parse(it) }
}
