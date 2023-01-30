package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V214FjernerInfotrygdOgSkattSykepengegrunnlagFraInntektshistorikken: JsonMigration(214) {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override val description = "fjerner infotrygd og SkattSykepengegrunnlag inntektshistorikken"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("inntektshistorikk")
                .forEach { innslag ->
                    val oppdatert = innslag.path("inntektsopplysninger")
                        .deepCopy<JsonNode>()
                        .filter { opplysning ->
                                opplysning.path("kilde").asText() == "INNTEKTSMELDING"
                        }
                    (innslag as ObjectNode).putArray("inntektsopplysninger").apply {
                        addAll(oppdatert)
                    }
                }
        }
    }

}