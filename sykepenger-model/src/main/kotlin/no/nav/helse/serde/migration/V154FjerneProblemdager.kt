package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.LoggerFactory

internal class V154FjerneProblemdager : JsonMigration(version = 154) {
    override val description = """Spisset migrering for å ordne opp i en person med korrupt tidslinje"""
    private val targetArbeidsgiverId = "6933446b-d461-465b-a215-7d4cec6c8fba"
    private val targetElementId = "05fb51c4-db5c-4c36-8081-e69ced380bf5"
    private val targetVedtaksperiodeId = "c8dd1965-7515-4e76-b341-618f7990e2d8"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val arbeidsgiver = jsonNode["arbeidsgivere"].singleOrNull { it.path("id").asText() == targetArbeidsgiverId } ?: return
        val element = arbeidsgiver.path("sykdomshistorikk").single { it.path("id").asText() == targetElementId } as ObjectNode
        val vedtaksperiode = arbeidsgiver.path("vedtaksperioder").single { it.path("id").asText() == targetVedtaksperiodeId }

        val sykdomstidslinje = vedtaksperiode.path("sykdomstidslinje").deepCopy<JsonNode>()
        element.replace("beregnetSykdomstidslinje", sykdomstidslinje)
        sikkerlogg.info("Overskriver tidslinjen og retter opp korrupte dager for {}", keyValue("vedtaksperiodeId", targetVedtaksperiodeId))
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}