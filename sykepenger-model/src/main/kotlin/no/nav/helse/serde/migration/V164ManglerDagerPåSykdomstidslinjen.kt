package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V164ManglerDagerPåSykdomstidslinjen: JsonMigration(version = 164) {
    override val description = "Rydder opp i vedtaksperioder som inneholder dager som ikke er på sykdomstidslinjen"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val fødselsnummer = jsonNode.path("fødselsnummer").asText()
        val aktørId = jsonNode.path("aktørId").asText()

        jsonNode["arbeidsgivere"]
            .forEach { arbeidsgiver ->
                arbeidsgiver as ObjectNode
                val organisasjonsnummer = arbeidsgiver.path("organisasjonsnummer").asText()
                val forkastede = mutableListOf<JsonNode>()
                trimVedtaksperiodeor(fødselsnummer, aktørId, organisasjonsnummer, arbeidsgiver)
                (arbeidsgiver.path("forkastede") as ArrayNode)
                    .addAll(forkastede.map { vedtaksperiode ->
                        serdeObjectMapper.createObjectNode()
                            .put("årsak", "IKKE_STØTTET")
                            .set("vedtaksperiode", vedtaksperiode)
                    })

            }
    }

    private fun trimVedtaksperiodeor(fødselsnummer: String, aktørId: String, organisasjonsnummer: String, arbeidsgiver: ObjectNode) {
        arbeidsgiver["vedtaksperioder"].forEach { vedtaksperiode ->
            (vedtaksperiode as ObjectNode)
            val periode = vedtaksperiode.periode()
            val sykdomstidslinje = vedtaksperiode["sykdomstidslinje"]
                .path("dager")
                .filterNot { it["kilde"]["type"].asText() == "Sykmelding" }

            (vedtaksperiode.path("sykdomstidslinje") as ObjectNode)
                .replace("dager", serdeObjectMapper.createArrayNode().addAll(sykdomstidslinje))

            val vedtaksperiodetidslinje = sykdomstidslinje.flatMap { it.dagPeriode() }
            val sykdomstidslinjeperiode = vedtaksperiodetidslinje.first() til vedtaksperiodetidslinje.last()

            if (sykdomstidslinjeperiode != periode) {
                vedtaksperiode as ObjectNode
                vedtaksperiode.put("fom", sykdomstidslinjeperiode.start.toString())
                vedtaksperiode.put("tom", sykdomstidslinjeperiode.endInclusive.toString())

                (vedtaksperiode.path("sykdomstidslinje").path("periode") as ObjectNode).also {
                    it.put("fom", sykdomstidslinjeperiode.start.toString())
                    it.put("tom", sykdomstidslinjeperiode.endInclusive.toString())
                }

                sikkerlogg.info(
                    "Trimmet perioden for {}, {}, {}, {}, {}",
                    keyValue("vedtaksperiodeId", vedtaksperiode.path("id").asText()),
                    keyValue("fødselsnummer", fødselsnummer),
                    keyValue("aktørId", aktørId),
                    keyValue("organisasjonsnummer", organisasjonsnummer),
                    keyValue("tilstand", vedtaksperiode.path("tilstand").asText()),
                )
            }
        }
    }

    internal companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        private fun JsonNode.dagPeriode(): Periode {
            val dato = path("dato").takeIf { it.isTextual }?.let { LocalDate.parse(it.asText()) }
            return dato?.let { Periode(it, it) } ?: periode()
        }

        private fun JsonNode.periode() =
            Periode(LocalDate.parse(path("fom").asText()), LocalDate.parse(path("tom").asText()))
    }
}
