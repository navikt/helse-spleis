package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.til
import org.slf4j.LoggerFactory

internal class V251SykdomstidslinjeForForkastedeUtenSykdomstidslinje: JsonMigration(251) {

    override val description = "Fikse sykdomstidslinje for forkastede vedtaksperioder som ikke har."

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val fødselsnummer = jsonNode.path("fødselsnummer").asText()
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("forkastede").filter {
                val vedtaksperiode = it.path("vedtaksperiode")
                vedtaksperiode.path("sykdomstidslinje").path("dager").isEmpty
            }.forEach { forkastet ->
                val vedtaksperiode = forkastet.path("vedtaksperiode")
                val sykdomstidslinje = vedtaksperiode.path("sykdomstidslinje")

                val periodeFom = vedtaksperiode.path("fom").asText().dato
                val periodeTom = vedtaksperiode.path("tom").asText().dato
                val periode = periodeFom til periodeTom
                val dager = sykdomstidslinje.path("dager") as ArrayNode

                val søknadId = vedtaksperiode.path("hendelser").firstOrNull {
                    it.path("dokumenttype").asText() == "Søknad"
                }?.path("dokumentId")?.asText()

                val historikkElement = arbeidsgiver.path("sykdomshistorikk").firstOrNull {
                    it.path("hendelseId").asText() == søknadId
                }

                if (søknadId == null) {
                    sikkerLogg.error("[V251] fant ikke søknadId for forkastet vedtaksperiode ${vedtaksperiode.path("id").asText()}",
                        keyValue("fødselsnummer", fødselsnummer)
                    )
                } else if (historikkElement == null) {
                    sikkerLogg.error("[V251] fant ikke historikkElement for forkastet vedtaksperiode ${vedtaksperiode.path("id").asText()}",
                        keyValue("fødselsnummer", fødselsnummer)
                    )
                } else {
                    sikkerLogg.info("[V251] migrerer sykdomstidslinje for forkastet vedtaksperiode ${vedtaksperiode.path("id").asText()} med periode=$periode",
                        keyValue("fødselsnummer", fødselsnummer)
                    )
                    val hendelsedager = (historikkElement.path("hendelseSykdomstidslinje").path("dager") as ArrayNode).deepCopy().filter {
                        val dagperiode = it.periode
                        dagperiode.overlapperMed(periode)
                    }
                    dager.addAll(hendelsedager)
                }
            }
        }
    }

    private companion object {
        val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}