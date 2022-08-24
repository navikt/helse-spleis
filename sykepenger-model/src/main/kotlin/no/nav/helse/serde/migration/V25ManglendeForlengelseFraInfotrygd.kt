package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import no.nav.helse.erRettFør
import no.nav.helse.person.AktivitetsloggObserver

internal class V25ManglendeForlengelseFraInfotrygd : JsonMigration(version = 25) {
    override val description = "Legger til riktig periodetype"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").zipWithNext().forEach { (periodeA, periodeB) ->
                if (periodeA.erInfotrygdforlengelse() && periodeA.etterfølgesAv(periodeB)) {
                    periodeB as ObjectNode
                    periodeB.put("forlengelseFraInfotrygd", "JA")
                }
            }
        }
    }
}

private fun JsonNode.erInfotrygdforlengelse() = this["forlengelseFraInfotrygd"].asText() == "JA"

private fun JsonNode.etterfølgesAv(other: JsonNode) =
    this["tom"].asLocalDate().erRettFør(other["fom"].asLocalDate())

private fun JsonNode.asLocalDate(): LocalDate =
    asText().let { LocalDate.parse(it) }
