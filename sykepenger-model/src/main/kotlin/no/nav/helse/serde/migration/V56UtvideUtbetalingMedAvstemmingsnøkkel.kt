package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime

internal class V56UtvideUtbetalingMedAvstemmingsnøkkel : JsonMigration(version = 56) {
    private companion object {
        private const val vedtaksperiodeKontekst = "Vedtaksperiode"
        private const val vedtaksperiodeId = "vedtaksperiodeId"
        private const val overføringstekst = "Utbetalingen ble overført til Oppdrag/UR"

        private val tidspunktRegex = "Oppdrag/UR\\s+([^,]+),".toRegex()
        private val avstemmingsnøkkelRegex = "avstemmingsnøkkel\\s+([0-9]+)".toRegex()
    }

    override val description: String = "Utvider Utbetaling med avstemmingsnøkkel"

    override fun doMigration(jsonNode: ObjectNode) {
        val konteksttyper = konteksttyper(jsonNode)
        val kontekstdetaljer = kontekstdetaljer(jsonNode)

        val p = jsonNode
            .path("aktivitetslogg")
            .path("aktiviteter")
            .filter { it.path("melding").asText().contains(overføringstekst) }
            .map { aktivitet ->
                val vedtaksperiodedetaljerindeks = aktivitet
                    .path("kontekster")
                    .map { it.intValue() }
                    .first { indeks -> konteksttyper[indeks] == vedtaksperiodeKontekst }
                val vedtaksperiodeId = kontekstdetaljer[vedtaksperiodedetaljerindeks].getValue(vedtaksperiodeId)
                val melding = aktivitet.path("melding").asText()
                val tidspunkt = tidspunktRegex.find(melding)?.let {
                    it.groupValues.last()
                }?.let { LocalDateTime.parse(it) }
                val avstemmingsnøkkel = avstemmingsnøkkelRegex.find(melding)?.let {
                    it.groupValues.last().toLongOrNull()
                }
                vedtaksperiodeId to Pair(tidspunkt, avstemmingsnøkkel)
            }.toMap()

        jsonNode
            .path("arbeidsgivere")
            .forEach { arbeidsgiver ->
                val aktive = arbeidsgiver.path("vedtaksperioder").toList()
                val forkastede = arbeidsgiver.path("forkastede").map { it.path("vedtaksperiode") }
                val overførteUtbetalinger = (aktive + forkastede)
                    .filter { p.containsKey(it.path("id").asText()) }
                    .filter { it.hasNonNull("utbetalingId") }
                    .map { it.path("utbetalingId").asText() to p.getValue(it.path("id").asText()) }
                    .toMap()

            arbeidsgiver
                .path("utbetalinger")
                .filter { overførteUtbetalinger.containsKey(it.path("id").asText()) }
                .map { it as ObjectNode }
                .onEach {
                    val (overføringstidspunkt, avstemmingsnøkkel) = overførteUtbetalinger.getValue(it.path("id").asText())
                    if (overføringstidspunkt != null) it.put("overføringstidspunkt", "$overføringstidspunkt")
                    if (avstemmingsnøkkel != null) it.put("avstemmingsnøkkel", "$avstemmingsnøkkel")
                }
        }
    }

    private fun kontekstdetaljer(jsonNode: ObjectNode): List<Map<String, String>> {
        return jsonNode
            .path("aktivitetslogg")
            .path("kontekster")
            .map { kontekst ->
                kontekst.path("kontekstMap")
                    .fields()
                    .asSequence()
                    .map { (key, value) -> key to value.asText() }
                    .toMap()
            }
    }

    private fun konteksttyper(jsonNode: ObjectNode): List<String> {
        return jsonNode
            .path("aktivitetslogg")
            .path("kontekster")
            .map { kontekst -> kontekst.path("kontekstType").asText() }
    }
}

