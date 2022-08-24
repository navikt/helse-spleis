package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.person.AktivitetsloggObserver
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

internal class V160FikserDoubleGrad : JsonMigration(version = 160) {
    override val description = "Spisset migrering for å runde av alle grader med desimal. Grad oppgis bare som heltall i søknad, sykmelding og overstyringer." +
            "Desimalet skyldes unøyaktigheter med doubles."


    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        val aktørId = jsonNode.path("aktørId").asText()
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val orgnr = arbeidsgiver.path("organisasjonsnummer").asText()
            arbeidsgiver.path("sykdomshistorikk").forEach { element ->
                migrerSykdomstidslinje(aktørId, orgnr, element.path("hendelseSykdomstidslinje"))
                migrerSykdomstidslinje(aktørId, orgnr, element.path("beregnetSykdomstidslinje"))
            }
        }
    }

    private fun migrerSykdomstidslinje(aktørId: String, orgnr: String, tidslinje: JsonNode) {
        tidslinje.path("dager")
            .forEach { dag ->
                val gradFør = dag.path("grad").asDouble()
                val gradEtter = gradFør.roundToInt().toDouble()
                val kilde = dag.path("kilde").path("type").asText()
                if (gradFør != gradEtter) {
                    (dag as ObjectNode).put("grad", gradEtter)
                    log.info("{} {} runder av $gradFør til $gradEtter med {}", keyValue("aktørId", aktørId), keyValue("organisasjonsnummer", orgnr), keyValue("kilde", kilde))
                }
            }
    }

    private companion object {
        private val log = LoggerFactory.getLogger("tjenestekall")
    }
}