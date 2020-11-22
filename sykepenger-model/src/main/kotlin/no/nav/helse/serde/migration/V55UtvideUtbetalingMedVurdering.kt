package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class V55UtvideUtbetalingMedVurdering : JsonMigration(version = 55) {
    private companion object {
        private val tidsstempelformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        private const val ukjentIdent = "Z999999"
        private const val ukjentEpost = "ukjent@nav.no"

        private const val vedtaksperiodeKontekst = "Vedtaksperiode"
        private const val vedtaksperiodeId = "vedtaksperiodeId"
        private const val ikkeGodkjentTekst = "Utbetaling markert som ikke godkjent"
    }

    override val description: String = "Utvider Utbetaling med vurdering"

    override fun doMigration(jsonNode: ObjectNode) {
        val kontekster = kontekster(jsonNode)
        val aktiviteter = jsonNode
            .path("aktivitetslogg")
            .path("aktiviteter")

        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val aktive = arbeidsgiver.path("vedtaksperioder").toList()
            val forkastede = arbeidsgiver.path("forkastede").map { it.path("vedtaksperiode") }
            val perioder = (aktive + forkastede)

            val vedtaksperiodekontekstIndeks = kontekstindeksForVedtaksperioder(perioder, kontekster)

            val ikkeGodkjenteUtbetalinger = ikkeGodkjenteUtbetalinger(perioder, vedtaksperiodekontekstIndeks, aktiviteter)
            val aktiveUtbetalinger = aktiveUtbetalinger(aktive, forkastede)

            arbeidsgiver
                .path("utbetalinger")
                .filter {
                    val id = it.path("id").asText()
                    ikkeGodkjenteUtbetalinger.containsKey(id) || !aktiveUtbetalinger.containsKey(id)
                }
                .map { it as ObjectNode }
                .forEach {
                    val id = it.path("id").asText()
                    if (ikkeGodkjenteUtbetalinger.containsKey(id)) it.put("status", "IKKE_GODKJENT")
                    val ikkeGodkjentTidspunkt = ikkeGodkjenteUtbetalinger[id] ?: LocalDateTime.parse(it.path("tidsstempel").asText())
                    lagVurdering(it, ukjentIdent, ukjentEpost, "$ikkeGodkjentTidspunkt", false)
                }

            arbeidsgiver
                .path("utbetalinger")
                .filter { aktiveUtbetalinger.containsKey(it.path("id").asText()) }
                .map { it as ObjectNode }
                .onEach {
                    val id = it.path("id").asText()
                    val vurdering = aktiveUtbetalinger.getValue(id)
                    if (vurdering == null) it.putNull("vurdering")
                    else {
                        val (ident, tidspunkt, automatiskBehandling) = vurdering
                        lagVurdering(it, ident, ukjentEpost, tidspunkt, automatiskBehandling)
                    }
                }
        }
    }

    private fun lagVurdering(
        it: ObjectNode,
        ident: String,
        epost: String,
        tidspunkt: String,
        automatiskBehandling: Boolean
    ) {
        it.putObject("vurdering")
            .put("ident", ident)
            .put("epost", epost)
            .put("tidspunkt", tidspunkt)
            .put("automatiskBehandling", automatiskBehandling)
    }

    private fun aktiveUtbetalinger(aktive: List<JsonNode>, forkastede: List<JsonNode>): Map<String, Triple<String, String, Boolean>?> {
        val godkjentForkastede = forkastede.filter { it.hasNonNull("utbetalingId") && it.hasNonNull("godkjentAv") }
        return (godkjentForkastede + aktive.filter { it.hasNonNull("utbetalingId") })
            .map { periode ->
                periode.path("utbetalingId").asText() to Triple(
                    first = periode.path("godkjentAv").asText(),
                    second = periode.path("godkjenttidspunkt").asText(),
                    third = periode.path("automatiskBehandling").asBoolean()
                ).takeIf { periode.hasNonNull("godkjentAv") }
            }.toMap()
    }

    private fun ikkeGodkjenteUtbetalinger(
        perioder: List<JsonNode>,
        vedtaksperiodekontekstIndeks: Map<String, Int>,
        aktiviteter: JsonNode
    ): Map<String, LocalDateTime?> {
        return perioder
            .filter { it.hasNonNull("utbetalingId") }
            .filter { it.path("tilstand").asText() == "TIL_INFOTRYGD" }
            .map { periode ->
                val id = periode.path("id").asText()
                val tidspunkt = finnAktivitetForVedtaksperiode(vedtaksperiodekontekstIndeks, id, ikkeGodkjentTekst, aktiviteter)
                    ?.let { aktivitet ->
                        LocalDateTime.parse(aktivitet.path("tidsstempel").asText(), tidsstempelformat)
                    }
                periode.path("utbetalingId").asText() to tidspunkt
            }.toMap()
    }

    private fun finnAktivitetForVedtaksperiode(
        vedtaksperiodekontekstIndeks: Map<String, Int>,
        vedtaksperiodeId: String,
        melding: String,
        aktiviteter: JsonNode
    ): JsonNode? {
        val indeks = vedtaksperiodekontekstIndeks[vedtaksperiodeId] ?: return null
        return aktiviteter.firstOrNull { aktivitet ->
            indeks in aktivitet.path("kontekster").map(JsonNode::asInt) &&
                aktivitet.path("melding").asText() == melding
        }
    }

    private fun kontekster(jsonNode: ObjectNode): List<Pair<String, Map<String, String>>> {
        return jsonNode
            .path("aktivitetslogg")
            .path("kontekster")
            .map { kontekst ->
                kontekst.path("kontekstType").asText() to kontekst.path("kontekstMap")
                    .fields()
                    .asSequence()
                    .map { (key, value) -> key to value.asText() }
                    .toMap()
            }
    }

    private fun kontekstindeksForVedtaksperioder(
        perioder: List<JsonNode>,
        kontekster: List<Pair<String, Map<String, String>>>
    ): Map<String, Int> {
        return perioder.map { periode ->
            val id = periode.path("id").asText()
            val kontekstIndeks = kontekster.indexOfFirst { (konteksttype, detaljer) ->
                konteksttype == vedtaksperiodeKontekst && detaljer.getValue(vedtaksperiodeId) == id
            }
            id to kontekstIndeks
        }.toMap()
    }
}

