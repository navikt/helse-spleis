package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.Periode
import org.slf4j.LoggerFactory

internal class V156ManglerDagerPåSykdomstidslinjenDryRun: JsonMigration(version = 156) {
    override val description =
        "Logger alle vedtaksperioder som inneholder dager som ikke er på sykdomstidslinjen"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val fødselsnummer = jsonNode.path("fødselsnummer").asText()
        val aktørId = jsonNode.path("aktørId").asText()

        jsonNode["arbeidsgivere"]
            .forEach arbeidsgiverLoop@ { arbeidsgiver ->
                val organisasjonsnummer = arbeidsgiver.path("organisasjonsnummer").asText()
                val dagerPåSykdomstidslinjen = arbeidsgiver["sykdomshistorikk"]
                    .firstOrNull()
                    ?.path("beregnetSykdomstidslinje")
                    ?.path("dager")
                    ?: return@arbeidsgiverLoop

                val datoerPåSykdomstidslinjen = (dagerPåSykdomstidslinjen as ArrayNode)
                    .map { it.dagPeriode() }
                    .flatMap { it.datoer() }

                arbeidsgiver["vedtaksperioder"].forEach vedtaksperiodeLoop@ { vedtaksperiode ->
                    val periode = vedtaksperiode.vedtaksperiodePeriode()
                    val datoerIVedtaksperiode = periode.datoer()
                    val manglerPåSykdomstidslinjen = datoerIVedtaksperiode.minus(datoerPåSykdomstidslinjen.toSet()).sorted()
                    if (manglerPåSykdomstidslinjen.isEmpty()) return@vedtaksperiodeLoop

                    sikkerlogg.info("Vedtaksperiode med periode=$periode inneholder dager som ikke er på sykdomstidslinjen: $manglerPåSykdomstidslinjen. {}, {}, {}, {}, {}",
                        keyValue("fødselsnummer", fødselsnummer),
                        keyValue("aktørId", aktørId),
                        keyValue("organisasjonsnummer", organisasjonsnummer),
                        keyValue("vedtaksperiodeId", vedtaksperiode.path("id").asText()),
                        keyValue("tilstand", vedtaksperiode.path("tilstand").asText())
                    )
                }
            }
    }

    internal companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        private fun JsonNode.dagPeriode(): Periode {
            val dato = path("dato").takeIf { it.isTextual }?.let { LocalDate.parse(it.asText()) }
            val fom = dato ?: LocalDate.parse(path("fom").asText())
            val tom = dato ?: LocalDate.parse(path("tom").asText())
            return Periode(fom, tom)
        }

        private fun JsonNode.vedtaksperiodePeriode() =
            Periode(LocalDate.parse(path("fom").asText()), LocalDate.parse(path("tom").asText()))

        private fun Periode.datoer() = start.datesUntil(endInclusive.plusDays(1)).toList()
    }
}
