package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class V69SetteOpprettetOgOppdatertTidspunkt : JsonMigration(version = 69) {
    private companion object {
        private val tidsstempelformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        private const val vedtaksperiodeKontekst = "Vedtaksperiode"
        private const val vedtaksperiodeId = "vedtaksperiodeId"
    }

    override val description: String = "Sette opprettet- og oppdatert-tidspunkt for Vedtaksperiode"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val kontekster = kontekster(jsonNode)
        val aktiviteter = jsonNode
            .path("aktivitetslogg")
            .path("aktiviteter")

        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val aktive = arbeidsgiver.path("vedtaksperioder").toList()
            val forkastede = arbeidsgiver.path("forkastede").map { it.path("vedtaksperiode") }
            val perioder = (aktive + forkastede)

            val vedtaksperiodekontekstIndeks = kontekstindeksForVedtaksperioder(perioder, kontekster)

            perioder.forEach { periode ->
                val vedtaksperiodeaktiviteter = finnAktiviteterForVedtaksperiode(vedtaksperiodekontekstIndeks, periode.path("id").asText(), aktiviteter)
                var tidligste: LocalDateTime = LocalDateTime.MAX
                var største: LocalDateTime = LocalDateTime.MIN
                vedtaksperiodeaktiviteter.forEach {
                    val tidsstempel = LocalDateTime.parse(it.path("tidsstempel").asText(), tidsstempelformat)
                    if (tidsstempel > største) største = tidsstempel
                    if (tidsstempel < tidligste) tidligste = tidsstempel
                }

                periode as ObjectNode
                periode.put("opprettet", "${tidligste.takeUnless { it == LocalDateTime.MAX } ?: LocalDateTime.MIN}")
                periode.put("oppdatert", "$største")
            }
        }
    }

    private fun finnAktiviteterForVedtaksperiode(
        vedtaksperiodekontekstIndeks: Map<String, Int>,
        vedtaksperiodeId: String,
        aktiviteter: JsonNode
    ): List<JsonNode> {
        val indeks = vedtaksperiodekontekstIndeks[vedtaksperiodeId] ?: return emptyList()
        return aktiviteter.filter { aktivitet -> indeks in aktivitet.path("kontekster").map(JsonNode::asInt) }
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
