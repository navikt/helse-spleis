package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory
import org.slf4j.MDC

internal class V192FjerneUtbetalingFraForkastetPeriode: JsonMigration(192) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private fun withMDC(context: Map<String, String>, block: () -> Unit) {
            val contextMap = MDC.getCopyOfContextMap() ?: emptyMap()
            try {
                MDC.setContextMap(contextMap + context)
                block()
            } finally {
                MDC.setContextMap(contextMap)
            }
        }

    }
    override val description = "DRY RUN - Migrerer vedtaksperiodeutbetalinger til å ha vilkårsgrunnlagId"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        withMDC(mapOf("aktørId" to aktørId)) {
            utførMigrering(jsonNode)
        }
    }

    private fun utførMigrering(jsonNode: ObjectNode) {
       jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach {
                loggManglendeVilkårsgrunnlag(it)
            }
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                val periode = forkastet.path("vedtaksperiode")
                fjernUtbetalingFraPerioderGåttTilInfotrygd(periode)
                loggManglendeVilkårsgrunnlag(periode, true)
            }
        }
    }

    private fun fjernUtbetalingFraPerioderGåttTilInfotrygd(vedtaksperiode: JsonNode) {
        val tilstand = vedtaksperiode.path("tilstand").asText()
        if (tilstand !in setOf("AVSLUTTET_UTEN_UTBETALING", "TIL_INFOTRYGD")) return
        val ikkeManglende = vedtaksperiode
            .path("utbetalinger")
            .filter { it.path("vilkårsgrunnlagId").isTextual }

        (vedtaksperiode as ObjectNode).replace("utbetalinger", serdeObjectMapper.createArrayNode().apply {
            addAll(ikkeManglende)
        })
    }

    private fun loggManglendeVilkårsgrunnlag(vedtaksperiode: JsonNode, forkastet: Boolean = false) {
        val manglende = vedtaksperiode
            .path("utbetalinger")
            .filterNot { it.path("vilkårsgrunnlagId").isTextual }
            .takeIf { it.isNotEmpty() } ?: return
        val id = vedtaksperiode.path("id").asText()
        val tilstand = vedtaksperiode.path("tilstand").asText()
        sikkerlogg.info("[V192] vedtaksperiode=$id tilstand=$tilstand erForkastet=$forkastet mangler vilkårsgrunnlag for ${manglende.size} utbetaling(er)")
    }
}