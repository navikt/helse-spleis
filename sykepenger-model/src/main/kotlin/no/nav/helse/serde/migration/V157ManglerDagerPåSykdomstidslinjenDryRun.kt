package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import org.slf4j.LoggerFactory

internal class V157ManglerDagerPåSykdomstidslinjenDryRun: JsonMigration(version = 157) {
    override val description =
        "Logger alle vedtaksperioder som inneholder dager som ikke er på sykdomstidslinjen"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val fødselsnummer = jsonNode.path("fødselsnummer").asText()
        val aktørId = jsonNode.path("aktørId").asText()

        jsonNode["arbeidsgivere"]
            .forEach arbeidsgiverLoop@ { arbeidsgiver ->
                val organisasjonsnummer = arbeidsgiver.path("organisasjonsnummer").asText()
                val dagerPåArbeidsgivertidslinjen = arbeidsgiver["sykdomshistorikk"]
                    .firstOrNull()
                    ?.path("beregnetSykdomstidslinje")
                    ?.path("dager")

                val datoerPåArbeidsgivertidslinjen = (dagerPåArbeidsgivertidslinjen as? ArrayNode)?.flatMap { it.dagPeriode() } ?: emptyList()

                arbeidsgiver["vedtaksperioder"].forEach vedtaksperiodeLoop@ { vedtaksperiode ->
                    val periode = vedtaksperiode.periode()
                    val vedtaksperiodetidslinje = vedtaksperiode["sykdomstidslinje"]
                        .path("dager")
                        .filterNot { it["kilde"]["type"].asText() == "Sykmelding" }
                        .flatMap { it.dagPeriode() }

                    if (vedtaksperiodetidslinje.isEmpty()) {
                        sikkerlogg.info(
                            "Sykdomstidslinjen for periode=$periode er tom. {}, {}, {}, {}, {}",
                            keyValue("fødselsnummer", fødselsnummer),
                            keyValue("aktørId", aktørId),
                            keyValue("organisasjonsnummer", organisasjonsnummer),
                            keyValue("vedtaksperiodeId", vedtaksperiode.path("id").asText()),
                            keyValue("tilstand", vedtaksperiode.path("tilstand").asText()),
                        )
                        return@vedtaksperiodeLoop
                    }

                    val sykdomstidslinjeperiode = vedtaksperiodetidslinje.first() til vedtaksperiodetidslinje.last()

                    val dagerSomManglerPåArbeidsgivertidslinjen = vedtaksperiodetidslinje.filterNot { it in datoerPåArbeidsgivertidslinjen }
                    if (dagerSomManglerPåArbeidsgivertidslinjen.isEmpty()) {
                        if (sykdomstidslinjeperiode != periode) {
                            sikkerlogg.info(
                                "Perioden for sykdomstidslinjen samsvarer ikke med vedtaksperiodens periode: periode=$periode, sykdomstidslinje=${vedtaksperiodetidslinje.grupperSammenhengendePerioder()}. {}, {}, {}, {}, {}",
                                keyValue("fødselsnummer", fødselsnummer),
                                keyValue("aktørId", aktørId),
                                keyValue("organisasjonsnummer", organisasjonsnummer),
                                keyValue("vedtaksperiodeId", vedtaksperiode.path("id").asText()),
                                keyValue("tilstand", vedtaksperiode.path("tilstand").asText()),
                            )
                        }
                        return@vedtaksperiodeLoop
                    }

                    val gruppertTidslinje = dagerSomManglerPåArbeidsgivertidslinjen.map { dato ->
                        dato to vedtaksperiode["sykdomstidslinje"].path("dager").first { dato in it.dagPeriode() }["kilde"]["type"].asText()
                    }.fold(listOf<Pair<Periode, String>>()) { list, entry ->
                        val (dato, kildetype) = entry
                        if (list.isNotEmpty() && list.last().first.endInclusive.plusDays(1) == dato && list.last().second == kildetype) {
                            return@fold list.dropLast(1) + listOf((list.last().first.start til dato) to kildetype)
                        }
                        list + listOf(dato.somPeriode() to kildetype)
                    }

                    sikkerlogg.info(
                        "Vedtaksperiode med periode=$periode, sykdomstidslinjeperiode=$sykdomstidslinjeperiode inneholder dager som ikke finnes på arbeidsgivers sykdomstidslinje: $gruppertTidslinje. {}, {}, {}, {}, {}, {}",
                        keyValue("fødselsnummer", fødselsnummer),
                        keyValue("periodeforskjell", periode != sykdomstidslinjeperiode),
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
            return dato?.let { Periode(it, it) } ?: periode()
        }

        private fun JsonNode.periode() =
            Periode(LocalDate.parse(path("fom").asText()), LocalDate.parse(path("tom").asText()))
    }
}
