package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.person.AktivitetsloggObserver
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V171FjerneForkastedePerioderUtenSykdomstidslinje : JsonMigration(version = 171) {
    override val description = """Fjerner forkastede perioder med tom sykdomstidslinje"""

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            val kopi = arbeidsgiver.path("forkastede").deepCopy<ArrayNode>()
            val resultat = kopi.filterNot { forkastetPeriode ->
                forkastetPeriode.path("vedtaksperiode").path("sykdomstidslinje").path("dager").isEmpty
            }.let { ArrayNode(serdeObjectMapper.nodeFactory, it) }

            val diff = kopi.size() - resultat.size()
            if (diff > 0) {
                sikkerlogg.info("Fjerner {} forkastede perioder med tom sykdomstidslinje for {}", diff, keyValue("aktørId", jsonNode.path("aktørId").asText()))
                (arbeidsgiver as ObjectNode).replace("forkastede", resultat)
            }
        }
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}