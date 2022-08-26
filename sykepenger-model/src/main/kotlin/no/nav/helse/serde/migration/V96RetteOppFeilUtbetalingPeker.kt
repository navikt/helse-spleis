package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V96RetteOppFeilUtbetalingPeker : JsonMigration(version = 96) {
    override val description: String = "Setter riktig peker til utbetaling"

    private val vedtaksperiodeId = "a6558358-f2fd-4d33-aa37-d85907fcf05e"
    private val utbetalingIdSomSkalErstattes = "a4b86e02-4621-42d8-a2a4-17c226e92d3a"
    private val utbetalingIdSomErstatter = "67dd0668-5dd6-417b-a149-8b9576ae0301"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("forkastede").map { it.path("vedtaksperiode") }.firstOrNull {
                vedtaksperiodeId == it.path("id").asText()
            }?.let { vedtaksperiode ->
                if (!arbeidsgiver.path("utbetalinger").any { it.path("id").asText() == utbetalingIdSomSkalErstattes }) return
                if (!arbeidsgiver.path("utbetalinger").any { it.path("id").asText() == utbetalingIdSomErstatter }) return
                vedtaksperiode.withArray<ArrayNode>("utbetalinger").apply {
                    removeAll { it.asText() == utbetalingIdSomSkalErstattes }
                    add(utbetalingIdSomErstatter)
                }
            }
        }
    }
}
