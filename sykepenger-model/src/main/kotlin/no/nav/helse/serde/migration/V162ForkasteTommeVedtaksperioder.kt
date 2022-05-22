package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.Periode
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V162ForkasteTommeVedtaksperioder: JsonMigration(version = 162) {
    override val description = "Rydder opp i vedtaksperioder som har tom sykdomstidslinje"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val fødselsnummer = jsonNode.path("fødselsnummer").asText()
        val aktørId = jsonNode.path("aktørId").asText()

        jsonNode["arbeidsgivere"]
            .forEach { arbeidsgiver ->
                arbeidsgiver as ObjectNode
                val organisasjonsnummer = arbeidsgiver.path("organisasjonsnummer").asText()
                val forkastede = mutableListOf<JsonNode>()
                sletteTommeVedtaksperioder(fødselsnummer, aktørId, organisasjonsnummer, arbeidsgiver, forkastede)
                (arbeidsgiver.path("forkastede") as ArrayNode)
                    .addAll(forkastede.map { vedtaksperiode ->
                        serdeObjectMapper.createObjectNode()
                            .put("årsak", "IKKE_STØTTET")
                            .set("vedtaksperiode", vedtaksperiode)
                    })

            }
    }

    private fun sletteTommeVedtaksperioder(
        fødselsnummer: String,
        aktørId: String,
        organisasjonsnummer: String,
        arbeidsgiver: ObjectNode,
        forkastede: MutableList<JsonNode>
    ) {
        val før = arbeidsgiver["vedtaksperioder"].size()
        val (perioder, tommePerioder) = arbeidsgiver["vedtaksperioder"].partition { vedtaksperiode ->
            vedtaksperiode["sykdomstidslinje"]
                .path("dager")
                .filterNot { it["kilde"]["type"].asText() == "Sykmelding" }
                .isNotEmpty()
        }
        val etter = perioder.size
        if (før == etter) return

        arbeidsgiver.replace("vedtaksperioder", serdeObjectMapper.createArrayNode().addAll(perioder))
        forkastede.addAll(tommePerioder)

        sikkerlogg.info("Forkaster ${før - etter} tomme vedtaksperioder. {}, {}, {}",
            keyValue("fødselsnummer", fødselsnummer), keyValue("aktørId", aktørId), keyValue("organisasjonsnummer", organisasjonsnummer))
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
