package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.spleis.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V183UtbetalingerOgVilkårsgrunnlag: JsonMigration(183) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
    override val description = "Migrerer vedtaksperiodeutbetalinger til å ha vilkårsgrunnlagId"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach(::migrerVedtaksperiode)
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrerVedtaksperiode(forkastet.path("vedtaksperiode"))
            }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        val utbetalinger = vedtaksperiode
            .path("utbetalinger")
            .map(JsonNode::asText)
            .map { utbetalingId ->
                serdeObjectMapper.createObjectNode().apply {
                    putNull("vilkårsgrunnlagId")
                    put("utbetalingId", utbetalingId)
                }
            }.let { utbetalinger ->
                serdeObjectMapper.createArrayNode().apply {
                    addAll(utbetalinger)
                }
            }
        (vedtaksperiode as ObjectNode).replace("utbetalinger", utbetalinger)
    }
}