package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.LoggerFactory

internal class V210EndreGamlePerioderMedKildeSykmelding: JsonMigration(210) {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override val description = "Endrer jukde fra Sykmelding til Søknad"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("sykdomshistorikk").path(0).path("beregnetSykdomstidslinje")
                .path("dager").forEach { dag ->
                    val kilde = dag.path("kilde")
                    val type = kilde.path("type").asText()
                    if (type == "Sykmelding"){
                        sikkerlogg.info("Endrer kilde fra Sykmelding til Søknad for $dag og {}", keyValue("aktørId", jsonNode.path("aktørId").asText()))
                        (kilde as ObjectNode).put("type", "Søknad")
                    }
                }
        }
    }

}