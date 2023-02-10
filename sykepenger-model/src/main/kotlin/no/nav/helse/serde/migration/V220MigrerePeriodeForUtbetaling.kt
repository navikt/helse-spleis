package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import org.slf4j.LoggerFactory

internal class V220MigrerePeriodeForUtbetaling: JsonMigration(220) {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private fun JsonNode.asLocalDate() = LocalDate.parse(asText())
        private fun JsonNode.asOptionalLocalDate() = takeIf { it.isTextual }?.asLocalDate()
    }

    override val description = "oppdaterer utbetaling med et konkret periodeobjekt"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger")
                .filter { utbetaling ->
                    utbetaling.path("opprinneligPeriodeFom").isNull || utbetaling.path("opprinneligPeriodeFom").isMissingNode
                }
                .forEach { utbetaling ->
                    val utbetalingId = utbetaling.path("id").asText()
                    val utbetalingstidslinjeSisteDag = utbetaling.path("utbetalingstidslinje").path("dager").lastOrNull()?.let { dag ->
                        when {
                            dag.hasNonNull("dato") -> dag.path("dato").asLocalDate()
                            else -> dag.path("tom").asLocalDate()
                        }
                    }?.somPeriode()
                    val arbeidsgiveroppdrag = oppdragsperiode(aktørId, utbetalingId, utbetaling.path("arbeidsgiverOppdrag"))
                    val personoppdrag = oppdragsperiode(aktørId, utbetalingId, utbetaling.path("personOppdrag"))

                    val periode = listOfNotNull(utbetalingstidslinjeSisteDag, arbeidsgiveroppdrag, personoppdrag).periode()

                    if (periode == null) {
                        sikkerlogg.info("utbetaling {} for {} har ingen periode i det hele tatt",
                            keyValue("utbetalingId", utbetalingId),
                            keyValue("aktørId", aktørId)
                        )
                    } else {
                        (utbetaling as ObjectNode)
                        utbetaling.put("opprinneligPeriodeFom", periode.start.toString())
                        utbetaling.put("opprinneligPeriodeTom", periode.endInclusive.toString())
                    }
                }
        }
    }

    private fun oppdragsperiode(aktørId: String, utbetalingId: String, oppdrag: JsonNode): Periode? {
        val sisteArbeidsgiverdag = oppdrag.path("sisteArbeidsgiverdag")
            .asOptionalLocalDate()
            ?.somPeriode()
        val linjeperiode = oppdrag.path("linjer").map { linje ->
            val datoStatusFom = linje.path("datoStatusFom").asOptionalLocalDate()
            val fom = (datoStatusFom ?: linje.path("fom").asLocalDate())
            fom til linje.path("tom").asLocalDate()
        }
        if (sisteArbeidsgiverdag == null) {
            sikkerlogg.info("utbetaling {} for {} har oppdrag med sisteArbeidsgiverdag=null",
                keyValue("utbetalingId", utbetalingId),
                keyValue("aktørId", aktørId)
            )
        }
        return (listOfNotNull(sisteArbeidsgiverdag) + linjeperiode).periode()
    }
}