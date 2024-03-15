package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V271RenamerUtbetalingerTilGenerasjon: JsonMigration(271) {
    override val description = "endrer navn på <utbetalinger> på Vedtaksperiode til <generasjoner>"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode -> migrer(periode) }
            arbeidsgiver.path("forkastede").forEach { periode -> migrer(periode.path("vedtaksperiode")) }
        }
    }

    private fun migrer(periode: JsonNode) {
        periode as ObjectNode
        val generasjoner = periode.remove("utbetalinger")
        periode.set<ArrayNode>("generasjoner", generasjoner)
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
