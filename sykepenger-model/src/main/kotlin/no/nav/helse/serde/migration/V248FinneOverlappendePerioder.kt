package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import org.slf4j.LoggerFactory

internal class V248FinneOverlappendePerioder: JsonMigration(248) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override val description = "finner overlappende vedtaksperioder"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val fnr = jsonNode.path("fødselsnummer").asText()
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val orgnummer = arbeidsgiver.path("organisasjonsnummer").asText()
            arbeidsgiver
                .path("vedtaksperioder")
                .fold(emptyList<Periode>()) { perioder, vedtaksperiode ->
                    val periode = LocalDate.parse(vedtaksperiode.path("fom").asText()) til LocalDate.parse(vedtaksperiode.path("tom").asText())
                    val overlappende = perioder.lastOrNull { it.overlapperMed(periode) }
                    if (overlappende != null) {
                        sikkerlogg.info("V248 {} vedtaksperiode {} ($periode) i {} hos {} overlapper med tidligere periode $overlappende",
                            keyValue("fødselsnummer", fnr),
                            keyValue("vedtaksperiodeId", vedtaksperiode.path("id").asText()),
                            keyValue("tilstand", vedtaksperiode.path("tilstand").asText()),
                            keyValue("orgnummer", orgnummer)
                        )
                        perioder.plusElement(periode)
                    } else {
                        perioder
                    }
                }
        }
    }
}