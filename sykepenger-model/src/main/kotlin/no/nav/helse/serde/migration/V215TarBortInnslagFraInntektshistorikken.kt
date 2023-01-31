package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V215TarBortInnslagFraInntektshistorikken: JsonMigration(215) {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override val description = "fjerner innslag fra inntektshistorikken"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val inntektshistorikk = arbeidsgiver.path("inntektshistorikk") as ArrayNode
            val inntektsmeldinger = inntektshistorikk.deepCopy().flatMap {
                it.path("inntektsopplysninger")
            }.distinctBy {
                it.path("id").asText()
            }
            inntektshistorikk.removeAll()
            inntektshistorikk.addAll(inntektsmeldinger)
        }
    }
}