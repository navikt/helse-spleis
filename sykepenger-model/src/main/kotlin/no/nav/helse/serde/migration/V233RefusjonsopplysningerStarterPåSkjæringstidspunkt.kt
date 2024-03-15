package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import no.nav.helse.serde.serdeObjectMapper

internal class V233RefusjonsopplysningerStarterPåSkjæringstidspunkt: JsonMigration(233) {

    override val description = "setter startskuddet på alle refusjonsopplysninger til å være skjæringstidspunktet"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { innslag ->
            innslag.path("vilkårsgrunnlag").forEach { element ->
                val skjæringstidspunkt = LocalDate.parse(element.path("skjæringstidspunkt").asText())
                element.path("sykepengegrunnlag").path("arbeidsgiverInntektsopplysninger").forEach { opplysning ->
                    val endret = opplysning.path("refusjonsopplysninger").deepCopy<JsonNode>()
                        .filter { refusjon ->
                            refusjon.tom() >= skjæringstidspunkt
                        }
                        .map { refusjon ->
                            (refusjon as ObjectNode).put("fom", maxOf(refusjon.fom(), skjæringstidspunkt).toString())
                        }

                    (opplysning as ObjectNode).replace("refusjonsopplysninger", serdeObjectMapper.createArrayNode().apply {
                        addAll(endret)
                    })
                }
            }
        }
    }
    private fun JsonNode.fom() = path("fom").let { LocalDate.parse(it.asText()) }
    private fun JsonNode.tom() = path("tom").takeIf { it.isTextual }?.let { LocalDate.parse(it.asText()) } ?: LocalDate.MAX

}