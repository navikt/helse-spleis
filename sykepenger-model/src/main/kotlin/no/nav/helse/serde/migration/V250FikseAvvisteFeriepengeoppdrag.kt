package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.utbetalingslinjer.genererUtbetalingsreferanse
import org.slf4j.LoggerFactory

internal class V250FikseAvvisteFeriepengeoppdrag: JsonMigration(250) {

    override val description = "Fikse avviste feriepengeoppdrag"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val fødselsnummer = jsonNode.path("fødselsnummer").asText()
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val organisasjonsnummer = arbeidsgiver.path("organisasjonsnummer").asText()
            val arbeidsgiveroppdrag = arbeidsgiver.path("feriepengeutbetalinger")
                .firstOrNull { it.path("opptjeningsår").asText() == "2022" }
                ?.path("oppdrag")
                ?.let { it as ObjectNode }
                ?: return@forEach // vi kan ha mottatt søknad på arbeidsgivere som ikke var registrert 19.mai
            val gammelFagsystemId = arbeidsgiveroppdrag.path("fagsystemId").asText()
            val nyFagsystemId = genererUtbetalingsreferanse(UUID.randomUUID())
            sikkerLogg.info("Endrer fagsystemId på arbeidsgiveroppdrag for feriepenger {} {} {} {}",
                keyValue("organisasjonsnummer", organisasjonsnummer),
                keyValue("fødselsnummer", fødselsnummer),
                keyValue("gammelFagsystemId", gammelFagsystemId),
                keyValue("nyFagsystemId", nyFagsystemId)
            )
            arbeidsgiveroppdrag.put("mottaker", organisasjonsnummer)
            arbeidsgiveroppdrag.put("fagsystemId", nyFagsystemId)
        }
    }

    private companion object {
        val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}