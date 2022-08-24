package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.person.AktivitetsloggObserver
import no.nav.helse.serde.serdeObjectMapper

internal class V158LeggerTilPersonoppdragForFeriepenger : JsonMigration(version = 158) {
    override val description: String = "Legger til personoppdrag i feriepengerutbetalinger"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        val fødselsnummer = jsonNode["fødselsnummer"].asText()
        jsonNode["arbeidsgivere"]
            .map { it["feriepengeutbetalinger"] }
            .filterIsInstance<ArrayNode>()
            .forEach { feriepengeutbetalinger ->
                feriepengeutbetalinger.forEach { feriepengeutbetaling ->

                    feriepengeutbetaling as ObjectNode
                    feriepengeutbetaling.put("sendPersonoppdragTilOS", false)

                    val personoppdrag: ObjectNode = mapOf(
                        "mottaker" to fødselsnummer,
                        "fagområde" to "SP",
                        "linjer" to emptyList<String>(),
                        "fagsystemId" to "IKKE-RELEVANT",
                        "endringskode" to "NY",
                        "sisteArbeidsgiverdag" to null,
                        "tidsstempel" to LocalDateTime.now(),
                        "nettoBeløp" to 0,
                        "stønadsdager" to 0,
                        "avstemmingsnøkkel" to null,
                        "status" to null,
                        "overføringstidspunkt" to null,
                        "fom" to LocalDate.MIN,
                        "tom" to LocalDate.MIN,
                        "erSimulert" to false,
                        "simuleringsResultat" to null,
                    ).let(serdeObjectMapper::convertValue)

                    feriepengeutbetaling.set<ObjectNode>("personoppdrag", personoppdrag)
                }
            }
    }
}
