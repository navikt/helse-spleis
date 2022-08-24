package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.person.AktivitetsloggObserver
import org.slf4j.LoggerFactory

internal class V155FjerneProblemdager : JsonMigration(version = 155) {
    override val description = """Spisset migrering for å ordne opp i en person med korrupt tidslinje"""
    private val targetArbeidsgiverId = "e0bb34bb-3452-46dd-b837-4e2c7949fd1b"
    private val targetElementId = "8b2454f7-7083-44ce-bb1c-b28405bd5326"
    private val targetVedtaksperiodeId = "8dc174a9-0a58-483b-9287-63a6dfd14896"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
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