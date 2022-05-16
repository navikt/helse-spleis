package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import org.slf4j.LoggerFactory

internal class V156ManglerDagerPåSykdomstidslinjenDryRun: JsonMigration(version = 156) {
    override val description =
        "Logger alle vedtaksperioder som inneholder dager som ikke er på sykdomstidslinjen"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val fødselsnummer = jsonNode.path("fødselsnummer").asText()
        val aktørId = jsonNode.path("aktørId").asText()

        val vedtaksperioder = jsonNode["arbeidsgivere"].flatMap { it["vedtaksperioder"] }

        jsonNode["arbeidsgivere"]
            .forEach arbeidsgiverLoop@ { arbeidsgiver ->
                val organisasjonsnummer = arbeidsgiver.path("organisasjonsnummer").asText()
                val dagerPåSykdomstidslinjen = arbeidsgiver["sykdomshistorikk"]
                    .firstOrNull()
                    ?.path("beregnetSykdomstidslinje")
                    ?.path("dager")

                val datoerPåSykdomstidslinjen = (dagerPåSykdomstidslinjen as? ArrayNode)?.flatMap { it.dagPeriode() } ?: emptyList()

                arbeidsgiver["vedtaksperioder"].forEach vedtaksperiodeLoop@ { vedtaksperiode ->
                    val periode = vedtaksperiode.periode()
                    val datoerIVedtaksperiode = periode.datoer().toList()
                    val sykdomstidslinjeForPeriode = datoerPåSykdomstidslinjen.subset(periode).toSet()

                    val manglerPåSykdomstidslinjen = datoerIVedtaksperiode.minus(sykdomstidslinjeForPeriode).sorted()
                    if (manglerPåSykdomstidslinjen.isEmpty()) return@vedtaksperiodeLoop

                    val skjæringstidspunkt = LocalDate.parse(vedtaksperiode["skjæringstidspunkt"].asText())

                    sikkerlogg.info(
                        "Vedtaksperiode med periode=$periode inneholder dager som ikke er på sykdomstidslinjen: $manglerPåSykdomstidslinjen. {}, {}, {}, {}, {}, {}, {}, {}, {}, {}",
                        keyValue("fødselsnummer", fødselsnummer),
                        keyValue("aktørId", aktørId),
                        keyValue("organisasjonsnummer", organisasjonsnummer),
                        keyValue("vedtaksperiodeId", vedtaksperiode.path("id").asText()),
                        keyValue("tilstand", vedtaksperiode.path("tilstand").asText()),
                        keyValue("trimmerDagerPåSlutten", manglerPåSykdomstidslinjen.manglerDagerPåSlutten(periode)),
                        keyValue("trimmerDagerPåStarten", manglerPåSykdomstidslinjen.manglerDagerPåStarten(periode)),
                        keyValue("trimmerDagerMidtInni", manglerPåSykdomstidslinjen.manglerDagerMidtInni(periode)),
                        keyValue("blirForlengetAvAndrePerioder", vedtaksperioder.forlengerPeriode(skjæringstidspunkt, periode)),
                        keyValue("forlengerAndrePerioder", vedtaksperioder.blirForlengetAvPeriode(skjæringstidspunkt, periode)),
                    )
                }
            }
    }
    private fun List<LocalDate>.manglerDagerMidtInni(periode: Periode): Boolean {
        val grupperte = grupperSammenhengendePerioder().toMutableList()
        if (grupperte.isEmpty()) return false
        if (manglerDagerPåStarten(periode)) grupperte.removeAt(0)
        if (manglerDagerPåSlutten(periode)) grupperte.removeLastOrNull()
        return grupperte.isNotEmpty()
    }

    private fun List<LocalDate>.manglerDagerPåSlutten(periode: Periode): Boolean {
        if (this.isEmpty()) return false
        return this.last() == periode.endInclusive
    }

    private fun List<LocalDate>.manglerDagerPåStarten(periode: Periode): Boolean {
        if (this.isEmpty()) return false
        return this.first() == periode.start
    }

    private fun List<JsonNode>.forlengerPeriode(skjæringstidspunkt: LocalDate, periode: Periode) =
        filter { it.periode().start > periode.start }
            .any { LocalDate.parse(it["skjæringstidspunkt"].asText()) == skjæringstidspunkt }
    private fun List<JsonNode>.blirForlengetAvPeriode(skjæringstidspunkt: LocalDate, periode: Periode) =
        filter { it.periode().start < periode.start }
            .any { LocalDate.parse(it["skjæringstidspunkt"].asText()) == skjæringstidspunkt }

    internal companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        private fun List<LocalDate>.subset(periode: Periode) =
            filter { it >= periode.start && it <= periode.endInclusive }

        private fun JsonNode.dagPeriode(): Periode {
            val dato = path("dato").takeIf { it.isTextual }?.let { LocalDate.parse(it.asText()) }
            return dato?.let { Periode(it, it) } ?: periode()
        }

        private fun JsonNode.periode() =
            Periode(LocalDate.parse(path("fom").asText()), LocalDate.parse(path("tom").asText()))

        private fun Periode.datoer() = start.datesUntil(endInclusive.plusDays(1)).toList()
    }
}
