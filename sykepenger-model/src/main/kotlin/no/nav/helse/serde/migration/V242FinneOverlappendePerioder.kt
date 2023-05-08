package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.nesteDag
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V243FinneOverlappendePerioder: JsonMigration(243) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override val description = "finner overlappende vedtaksperioder"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val fnr = jsonNode.path("fødselsnummer").asText()
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val orgnummer = arbeidsgiver.path("organisasjonsnummer").asText()
            var måJustereLåser = false
            arbeidsgiver
                .path("vedtaksperioder")
                .fold(emptyList<Pair<Periode, JsonNode>>()) { perioder, vedtaksperiode ->
                    val periode = LocalDate.parse(vedtaksperiode.path("fom").asText()) til LocalDate.parse(vedtaksperiode.path("tom").asText())
                    val overlappende = perioder.firstOrNull { it.first.overlapperMed(periode) }
                    if (overlappende == null) {
                        perioder.plusElement(periode to vedtaksperiode)
                    } else if (periode.count() == 1) {
                        sikkerlogg.info("{} vedtaksperiode {} ($periode) i {} hos {} overlapper med tidligere periode ${overlappende.first} - må justeres manuelt fordi perioden består av én dag",
                            keyValue("fødselsnummer", fnr),
                            keyValue("vedtaksperiodeId", vedtaksperiode.path("id").asText()),
                            keyValue("tilstand", vedtaksperiode.path("tilstand").asText()),
                            keyValue("orgnummer", orgnummer)
                        )
                        perioder
                    } else {
                        måJustereLåser = true
                        val nyFom = if (overlappende.first == periode) {
                            sikkerlogg.info("{} vedtaksperiode {} ($periode) i {} hos {} overlapper med tidligere periode ${overlappende.first} - må justere tom på forrige og fom på gjeldende",
                                keyValue("fødselsnummer", fnr),
                                keyValue("vedtaksperiodeId", vedtaksperiode.path("id").asText()),
                                keyValue("tilstand", vedtaksperiode.path("tilstand").asText()),
                                keyValue("orgnummer", orgnummer)
                            )
                            (overlappende.second as ObjectNode).put("tom", overlappende.first.start.toString())
                            overlappende.first.start.nesteDag
                        } else {
                            sikkerlogg.info("{} vedtaksperiode {} ($periode) i {} hos {} overlapper med tidligere periode ${overlappende.first} - endrer fom på gjeldende",
                                keyValue("fødselsnummer", fnr),
                                keyValue("vedtaksperiodeId", vedtaksperiode.path("id").asText()),
                                keyValue("tilstand", vedtaksperiode.path("tilstand").asText()),
                                keyValue("orgnummer", orgnummer)
                            )
                            overlappende.first.endInclusive.nesteDag
                        }

                        (vedtaksperiode as ObjectNode).put("fom", nyFom.toString())
                        perioder.plusElement((nyFom til periode.endInclusive) to vedtaksperiode)
                    }
                }
            if (måJustereLåser) {
                // sikre at alle avsluttede perioder har låste perioder
                val låstePerioder = arbeidsgiver.path("sykdomshistorikk").path(0).path("beregnetSykdomstidslinje").path("låstePerioder") as ArrayNode
                låstePerioder.removeAll()

                val avsluttedePerioder = arbeidsgiver
                    .path("vedtaksperioder")
                    .filter { it.path("tilstand").asText() in setOf("AVSLUTTET", "AVSLUTTET_UTEN_UTBETALING") }
                    .map { it.path("fom").asText() to it.path("tom").asText() }
                    .map { (fom, tom) -> serdeObjectMapper.createObjectNode().apply {
                        put("fom", fom)
                        put("tom", tom)
                    } }

                låstePerioder.addAll(avsluttedePerioder)
            }
        }
    }
}