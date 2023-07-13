package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V256SpissetMigreringForÅForkastePeriode : JsonMigration(version = 256) {
    override val description = "forkaster periode som har en forkastet utbetaling"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val vedtaksperiodeindeks = arbeidsgiver.path("vedtaksperioder").indexOfFirst { it.path("id").asText() == trøbleteVedtaksperioder }
            if (vedtaksperiodeindeks == -1) return@forEach
            val perioder = arbeidsgiver.path("vedtaksperioder") as ArrayNode
            val vedtaksperiode = perioder[vedtaksperiodeindeks].deepCopy<JsonNode>()
            perioder.remove(vedtaksperiodeindeks)
            val forkastede = arbeidsgiver.path("forkastede") as ArrayNode
            forkastede.addObject().apply {
                set<JsonNode>("vedtaksperiode", vedtaksperiode)
            }
        }
    }

    private companion object {
        private val trøbleteVedtaksperioder = "99a16183-1249-40c6-b668-854851678568"
    }
}