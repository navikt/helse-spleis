package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.AktivitetsloggObserver

internal class V170TildelingUtbetaling: JsonMigration(170) {
    private companion object {
        private val utbetalinger = mapOf(
            "3880e514-0f96-4d5b-a57b-747146f62b19" to setOf(
                "0fa725eb-5a7c-4889-ae69-152691904100"
            )
        )
    }
    override val description = "Fikse tildeling av utbetaling"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                val vedtaksperiodeId = vedtaksperiode.path("id").asText()
                val utbetalingId = utbetalinger.entries.firstOrNull { (_, vedtaksperioder) -> vedtaksperiodeId in vedtaksperioder }
                if (utbetalingId != null) (vedtaksperiode.path("utbetalinger") as ArrayNode).add(utbetalingId.key)
            }
        }
    }
}