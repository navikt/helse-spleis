package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V212FjerneRapporterteInntekterFraInntektshistorikk: JsonMigration(212) {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override val description = "fjerner rapporterte inntekter fra inntektshistorikken"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("inntektshistorikk")
                .filter {
                    val opplysninger = it.path("inntektsopplysninger")
                    opplysninger.isArray && opplysninger.size() > 0
                }
                .forEach { innslag ->
                    val oppdatert = innslag.path("inntektsopplysninger")
                        .deepCopy<JsonNode>()
                        .filterNot { opplysning ->
                            opplysning.path("skatteopplysninger").any { skatteopplysning ->
                                skatteopplysning.path("kilde").asText() == "SKATT_SAMMENLIGNINGSGRUNNLAG"
                            }
                        }
                    (innslag as ObjectNode).putArray("inntektsopplysninger").apply {
                        addAll(oppdatert)
                    }
                }
        }
    }

}