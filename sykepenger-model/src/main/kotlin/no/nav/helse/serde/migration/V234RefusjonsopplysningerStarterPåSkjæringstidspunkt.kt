package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate

internal class V234RefusjonsopplysningerStarterPåSkjæringstidspunkt: JsonMigration(234) {

    override val description = "setter startskuddet på alle refusjonsopplysninger til å være skjæringstidspunktet"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { innslag ->
            innslag.path("vilkårsgrunnlag").forEach { element ->
                val skjæringstidspunkt = LocalDate.parse(element.path("skjæringstidspunkt").asText())
                element.path("sykepengegrunnlag").path("arbeidsgiverInntektsopplysninger").forEach { opplysning ->
                    opplysning.path("refusjonsopplysninger")
                        .minByOrNull { refusjon -> refusjon.fom() }
                        ?.also { refusjon ->
                            (refusjon as ObjectNode).put("fom", skjæringstidspunkt.toString())
                        }
                }
            }
        }
    }
    private fun JsonNode.fom() = path("fom").let { LocalDate.parse(it.asText()) }
}