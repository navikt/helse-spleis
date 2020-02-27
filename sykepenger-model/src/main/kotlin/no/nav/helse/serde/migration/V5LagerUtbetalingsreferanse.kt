package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.utbetalingstidslinje.genererUtbetalingsreferanse
import java.util.*

internal class V5LagerUtbetalingsreferanse : JsonMigration(version = 5) {

    override val description = "Lager utbetalingsreferanse på perioder hvor utbetalingsreferanse er null"

    private val vedtaksperiodeIdKey = "id"
    private val utbetalingsreferanseKey = "utbetalingsreferanse"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                migrerVedtaksperiode(periode)
            }
        }
    }

    private fun migrerVedtaksperiode(periode: JsonNode) {
        if (periode.hasNonNull(utbetalingsreferanseKey)) return
        val vedtaksperiodeId = UUID.fromString(periode[vedtaksperiodeIdKey].asText())
        (periode as ObjectNode).put(utbetalingsreferanseKey, genererUtbetalingsreferanse(vedtaksperiodeId))
    }
}
